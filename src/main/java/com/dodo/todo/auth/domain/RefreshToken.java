package com.dodo.todo.auth.domain;

import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    public RefreshToken(Member member, String token, LocalDateTime expiredAt) {
        validateMember(member);
        this.member = member;
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public boolean isExpired(LocalDateTime now) {
        return expiredAt.isBefore(now) || expiredAt.isEqual(now);
    }

    /**
     * 리프레시 토큰 교체
     * 같은 세션 row를 재사용하면서 토큰과 만료 시각만 갱신함.
     */
    public void rotate(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member is required");
        }
    }
}
