package com.dodo.todo.category.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String name;

    private Category(Member member, String name) {
        validateMember(member);
        this.member = member;
        this.name = name;
    }

    public static Category create(Member member, String name) {
        return new Category(member, name);
    }

    /**
     * 카테고리명 변경
     * 현재 카테고리의 이름을 요청한 이름으로 변경함.
     */
    public void updateName(String name) {
        this.name = name;
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
