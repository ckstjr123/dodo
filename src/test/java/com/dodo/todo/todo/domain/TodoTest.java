package com.dodo.todo.todo.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.tag.domain.Tag;
import com.dodo.todo.todo.reminder.domain.ReminderType;
import com.dodo.todo.todo.repeat.domain.TodoRepeatType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TodoTest {

    @Test
    @DisplayName("Todo는 체크리스트와 태그 매핑을 하위 요소로 등록한다")
    void addChecklistAndTag() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        Tag tag = Tag.create(todo.getMember(), "중요");

        todo.addChecklist("초안 작성");
        todo.addTag(tag);

        assertThat(todo.getChecklists()).extracting(checklist -> checklist.getContent())
                .containsExactly("초안 작성");
        assertThat(todo.getTodoTags()).extracting(todoTag -> todoTag.getTag().getName())
                .containsExactly("중요");
    }

    @Test
    @DisplayName("Todo는 반복 설정을 하위 요소로 등록한다")
    void setRepeat() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));

        todo.setWeeklyRepeat(1, Set.of(DayOfWeek.MONDAY));

        assertThat(todo.getRepeat().getTodo()).isSameAs(todo);
        assertThat(todo.getRepeat().getRepeatType()).isEqualTo(TodoRepeatType.WEEKLY);
        assertThat(todo.getRepeat().getDaysOfWeek()).containsExactly(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("Todo는 알림 설정을 하위 요소로 등록한다")
    void addReminder() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 4, 7, 18, 0);
        Todo todo = todo(dueAt);

        todo.addRelativeReminder(30);

        assertThat(todo.getReminders()).hasSize(1);
        assertThat(todo.getReminders().get(0).getTodo()).isSameAs(todo);
        assertThat(todo.getReminders().get(0).getReminderType()).isEqualTo(ReminderType.RELATIVE_TO_DUE);
        assertThat(todo.getReminders().get(0).getRemindBefore()).isEqualTo(30);
    }

    @Test
    @DisplayName("마감 시각이 없으면 Todo에 마감 기준 알림을 등록할 수 없다")
    void rejectRelativeReminderWithoutDueAt() {
        Todo todo = todo(null);

        assertThatThrownBy(() -> todo.addRelativeReminder(30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Due date is required for relative reminder");
    }

    @Test
    @DisplayName("같은 마감 기준 알림은 중복 등록할 수 없다")
    void rejectDuplicateRelativeReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));

        todo.addRelativeReminder(30);

        assertThatThrownBy(() -> todo.addRelativeReminder(30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate relative reminder");
    }

    @Test
    @DisplayName("같은 절대 시각 알림은 중복 등록할 수 없다")
    void rejectDuplicateAbsoluteReminder() {
        Todo todo = todo(LocalDateTime.of(2026, 4, 7, 18, 0));
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 9, 0);

        todo.addAbsoluteReminder(remindAt);

        assertThatThrownBy(() -> todo.addAbsoluteReminder(remindAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate absolute reminder");
    }

    private Todo todo(LocalDateTime dueAt) {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");

        return Todo.builder()
                .member(member)
                .category(category)
                .title("문서 정리")
                .status(TodoStatus.OPEN)
                .priority("MEDIUM")
                .dueAt(dueAt)
                .build();
    }
}
