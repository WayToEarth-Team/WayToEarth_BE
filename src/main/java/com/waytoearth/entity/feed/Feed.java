package com.waytoearth.entity.feed;

import com.waytoearth.entity.running.RunningRecord;
import com.waytoearth.entity.user.User;
import com.waytoearth.entity.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feeds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feed extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 - user 테이블을 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 연관 러닝 기록 - RunningRecord를 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_record_id")
    private RunningRecord runningRecord;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "image_url", length = 2000)
    private String imageUrl;

    // S3 삭제를 위해 Key 보관
    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Version //충돌이 나지 않을거라고 가정(잠금 없다가 업데이트 시점에만 충돌 여부 확인)
    @Column(name = "version")
    private Long version;

}
