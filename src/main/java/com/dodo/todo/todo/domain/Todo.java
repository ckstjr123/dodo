package com.dodo.todo.todo.domain;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.common.entity.BaseEntity;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.tag.domain.Tag;
import com.dodo.todo.todo.checklist.domain.Checklist;
import com.dodo.todo.todo.reminder.domain.Reminder;
import com.dodo.todo.todo.repeat.domain.TodoRepeat;
import com.dodo.todo.todo.tag.domain.TodoTag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Getter
@Entity
@Table(name = "todo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TodoStatus status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @OneToOne(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private TodoRepeat repeat;

    @BatchSize(size = 100)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reminder> reminders = new ArrayList<>();

    @BatchSize(size = 100)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Checklist> checklists = new ArrayList<>();

    @BatchSize(size = 100)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "todo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TodoTag> todoTags = new ArrayList<>();

    @Builder
    private Todo(
            Member member,
            Category category,
            String title,
            String memo,
            TodoStatus status,
            String priority,
            Integer sortOrder,
            LocalDateTime dueAt
    ) {
        validateMember(member);
        validateCategory(category);
        validateStatus(status);
        this.member = member;
        this.category = category;
        this.title = title;
        this.memo = memo;
        this.status = status;
        this.priority = priority;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.dueAt = dueAt;
    }

    public Long getMemberId() {
        return member.getId();
    }

    public Long getCategoryId() {
        return category.getId();
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

    public List<TodoTag> getTodoTags() {
        return Collections.unmodifiableList(todoTags);
    }

    public List<Reminder> getReminders() {
        return Collections.unmodifiableList(reminders);
    }

    public List<Checklist> getChecklists() {
        return Collections.unmodifiableList(checklists);
    }

    public void updateSchedule(String memo, LocalDateTime dueAt) {
        this.memo = memo;
        this.dueAt = dueAt;
    }

    /**
     * Todo 태그 연결
     * Todo Aggregate 내부 매핑 엔티티로 태그를 연결함.
     */
    public void addTag(Tag tag) {
        TodoTag todoTag = TodoTag.create(this, tag);
        todoTags.add(todoTag);
    }

    /**
     * 체크리스트 추가
     * Todo 하위 체크리스트 항목을 생성하고 루트 컬렉션에 등록함.
     */
    public Checklist addChecklist(String content) {
        Checklist checklist = Checklist.create(this, content);
        checklists.add(checklist);
        return checklist;
    }

    /**
     * 일간 반복 설정
     * Todo에 기존 반복 설정을 대체하는 일간 반복 규칙을 등록함.
     */
    public TodoRepeat setDailyRepeat(int repeatInterval) {
        this.repeat = TodoRepeat.daily(this, repeatInterval);
        return repeat;
    }

    /**
     * 주간 반복 설정
     * Todo에 기존 반복 설정을 대체하는 주간 반복 규칙을 등록함.
     */
    public TodoRepeat setWeeklyRepeat(int repeatInterval, Set<DayOfWeek> daysOfWeek) {
        this.repeat = TodoRepeat.weekly(this, repeatInterval, daysOfWeek);
        return repeat;
    }

    /**
     * 마감 기준 알림 추가
     * Todo의 마감 시각을 기준으로 상대 알림 규칙을 등록함.
     */
    public Reminder addRelativeReminder(int remindBefore) {
        if (dueAt == null) {
            throw new IllegalArgumentException("Due date is required for relative reminder");
        }

        if (hasRelativeReminder(remindBefore)) {
            throw new IllegalArgumentException("Duplicate relative reminder");
        }

        Reminder reminder = Reminder.relativeToDue(this, remindBefore);
        reminders.add(reminder);
        return reminder;
    }

    /**
     * 절대 시각 알림 추가
     * Todo에 특정 시각 알림 규칙을 등록함.
     */
    public Reminder addAbsoluteReminder(LocalDateTime remindAt) {
        if (hasAbsoluteReminder(remindAt)) {
            throw new IllegalArgumentException("Duplicate absolute reminder");
        }

        Reminder reminder = Reminder.absoluteAt(this, remindAt);
        reminders.add(reminder);
        return reminder;
    }

    public boolean hasRelativeReminder(int remindBefore) {
        return reminders.stream()
                .anyMatch(reminder -> reminder.isRelativeToDue(remindBefore));
    }

    public boolean hasAbsoluteReminder(LocalDateTime remindAt) {
        return reminders.stream()
                .anyMatch(reminder -> reminder.isAbsoluteAt(remindAt));
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member is required");
        }
    }

    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
    }

    private void validateStatus(TodoStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Todo status is required");
        }
    }
}
