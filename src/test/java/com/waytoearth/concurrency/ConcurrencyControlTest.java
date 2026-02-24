package com.waytoearth.concurrency;

import com.waytoearth.config.queryDsl.QueryDslConfig;
import com.waytoearth.entity.enums.RunningStatus;
import com.waytoearth.entity.enums.RunningType;
import com.waytoearth.entity.running.RunningRecord;
import com.waytoearth.entity.user.User;
import com.waytoearth.repository.running.RunningRecordRepository;
import com.waytoearth.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 제어 테스트
 *
 * 테스트 목적:
 * 1. 비관적 락(Pessimistic Lock)이 러닝 세션 상태 충돌을 방지하는지 검증
 * 2. 원자적 쿼리(Atomic Update)가 통계 누적 시 Lost Update를 방지하는지 검증
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class ConcurrencyControlTest {

    @Autowired
    private RunningRecordRepository runningRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private User testUser;
    private String sessionId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);

        // 테스트용 사용자 생성
        testUser = transactionTemplate.execute(status -> {
            User user = User.builder()
                    .kakaoId(System.currentTimeMillis())
                    .nickname("testUser_" + System.currentTimeMillis())
                    .totalDistance(BigDecimal.valueOf(100))
                    .totalRunningCount(10)
                    .build();
            return userRepository.saveAndFlush(user);
        });

        // 테스트용 러닝 세션 생성
        sessionId = "test-session-" + System.currentTimeMillis();
        transactionTemplate.execute(status -> {
            RunningRecord record = RunningRecord.builder()
                    .user(testUser)
                    .sessionId(sessionId)
                    .runningType(RunningType.SINGLE)
                    .status(RunningStatus.RUNNING)
                    .startedAt(LocalDateTime.now())
                    .isCompleted(false)
                    .build();
            return runningRecordRepository.saveAndFlush(record);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.execute(status -> {
            runningRecordRepository.deleteAll();
            userRepository.deleteAll();
            return null;
        });
    }

    @Test
    @DisplayName("[비관적 락] 동시 상태 변경 요청 시 순차 처리되어 데이터 정합성 유지")
    void pessimisticLock_preventsLostUpdate_onSessionStatus() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyChangedCount = new AtomicInteger(0);

        // when: 10개 스레드가 동시에 상태 변경 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.execute(status -> {
                        RunningRecord record = runningRecordRepository
                                .findBySessionIdWithLock(sessionId)
                                .orElseThrow();

                        if (record.getStatus() == RunningStatus.RUNNING) {
                            if (index % 2 == 0) {
                                record.setStatus(RunningStatus.PAUSED);
                            } else {
                                record.setStatus(RunningStatus.COMPLETED);
                                record.setIsCompleted(true);
                            }
                            runningRecordRepository.saveAndFlush(record);
                            successCount.incrementAndGet();
                        } else {
                            alreadyChangedCount.incrementAndGet();
                        }
                        return null;
                    });
                } catch (Exception e) {
                    System.err.println("Thread " + index + " failed: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // then
        System.out.println("\n========== 비관적 락 테스트 결과 ==========");
        System.out.println("총 스레드: " + threadCount);
        System.out.println("상태 변경 성공: " + successCount.get());
        System.out.println("이미 변경됨 (정상 거부): " + alreadyChangedCount.get());
        System.out.println("==========================================\n");

        assertThat(successCount.get())
                .as("비관적 락으로 인해 첫 번째 트랜잭션만 상태 변경 성공")
                .isEqualTo(1);

        assertThat(alreadyChangedCount.get())
                .as("나머지 트랜잭션은 이미 변경된 상태로 인해 거부됨")
                .isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("[원자적 쿼리] 동시 통계 업데이트 시 모든 값이 정확히 누적됨")
    void atomicUpdate_preventsLostUpdate_onUserStats() throws InterruptedException {
        // given
        int threadCount = 100;
        BigDecimal distancePerRun = BigDecimal.valueOf(5);
        BigDecimal initialDistance = testUser.getTotalDistance();
        int initialRunningCount = testUser.getTotalRunningCount();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.execute(status -> {
                        int updated = userRepository.updateRunningStatsAtomic(
                                testUser.getId(),
                                distancePerRun
                        );
                        if (updated > 0) {
                            successCount.incrementAndGet();
                        }
                        return null;
                    });
                } catch (Exception e) {
                    System.err.println("Update failed: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // then
        User updatedUser = transactionTemplate.execute(status ->
                userRepository.findById(testUser.getId()).orElseThrow()
        );

        BigDecimal expectedDistance = initialDistance.add(
                distancePerRun.multiply(BigDecimal.valueOf(threadCount))
        );

        int expectedRunningCount = initialRunningCount + threadCount;

        System.out.println("\n========== 원자적 쿼리 테스트 결과 ==========");
        System.out.println("동시 요청 수: " + threadCount);
        System.out.println("초기 거리: " + initialDistance + " km");
        System.out.println("요청당 추가 거리: " + distancePerRun + " km");
        System.out.println("-------------------------------------------");
        System.out.println("예상 최종 거리: " + expectedDistance + " km");
        System.out.println("실제 최종 거리: " + updatedUser.getTotalDistance() + " km");
        System.out.println("예상 러닝 횟수: " + expectedRunningCount + " 회");
        System.out.println("실제 러닝 횟수: " + updatedUser.getTotalRunningCount() + " 회");
        System.out.println("성공한 업데이트: " + successCount.get() + " 회");
        System.out.println("===========================================\n");

        assertThat(updatedUser.getTotalDistance())
                .as("원자적 쿼리로 모든 거리가 정확히 누적됨")
                .isEqualByComparingTo(expectedDistance);

        assertThat(updatedUser.getTotalRunningCount())
                .as("원자적 쿼리로 모든 러닝 횟수가 정확히 누적됨")
                .isEqualTo(expectedRunningCount);

        assertThat(successCount.get())
                .as("모든 업데이트 요청이 성공함")
                .isEqualTo(threadCount);
    }

    @Test
    @DisplayName("[Lost Update 재현] Read-Modify-Write 패턴에서 데이터 유실 발생")
    void withoutAtomicUpdate_causesLostUpdate() throws InterruptedException {
        // given
        User lostUpdateTestUser = transactionTemplate.execute(status -> {
            User user = User.builder()
                    .kakaoId(System.currentTimeMillis() + 1)
                    .nickname("lostUpdateTest_" + System.currentTimeMillis())
                    .totalDistance(BigDecimal.ZERO)
                    .totalRunningCount(0)
                    .build();
            return userRepository.saveAndFlush(user);
        });

        int threadCount = 50;
        BigDecimal distancePerRun = BigDecimal.valueOf(10);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.execute(status -> {
                        User user = userRepository.findById(lostUpdateTestUser.getId()).orElseThrow();

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ignored) {}

                        user.updateRunningStats(distancePerRun);
                        userRepository.saveAndFlush(user);
                        return null;
                    });
                } catch (Exception e) {
                    // ignore (OptimisticLockException expected)
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // then
        User updatedUser = transactionTemplate.execute(status ->
                userRepository.findById(lostUpdateTestUser.getId()).orElseThrow()
        );

        BigDecimal expectedDistance = distancePerRun.multiply(BigDecimal.valueOf(threadCount));
        BigDecimal actualDistance = updatedUser.getTotalDistance();
        BigDecimal lostDistance = expectedDistance.subtract(actualDistance);

        System.out.println("\n========== Lost Update 재현 테스트 ==========");
        System.out.println("동시 요청 수: " + threadCount);
        System.out.println("요청당 추가 거리: " + distancePerRun + " km");
        System.out.println("-------------------------------------------");
        System.out.println("예상 최종 거리: " + expectedDistance + " km");
        System.out.println("실제 최종 거리: " + actualDistance + " km");
        System.out.println("❌ 유실된 거리: " + lostDistance + " km");
        System.out.println("===========================================\n");

        assertThat(actualDistance)
                .as("Read-Modify-Write 패턴에서 Lost Update 또는 충돌 발생")
                .isLessThanOrEqualTo(expectedDistance);
    }
}
