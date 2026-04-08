package com.dodo.todo.tag.domain;

import com.dodo.todo.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Tag(Member member, String name) {
        validateMember(member);
        this.member = member;
        this.name = name;
    }

    public static Tag create(Member member, String name) {
        return new Tag(member, name);
    }

    public Long getMemberId() {
        return member.getId();
    }

    public boolean isOwnedBy(Long memberId) {
        return member != null && member.getId() != null && member.getId().equals(memberId);
    }

    public boolean isOwnedBy(Member member) {
        if (member == null || this.member == null) {
            return false;
        }

        if (this.member == member) {
            return true;
        }

        return this.member.getId() != null && this.member.getId().equals(member.getId());
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member is required");
        }
    }
}
