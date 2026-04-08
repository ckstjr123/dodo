package com.dodo.todo.todo.dto;

import com.dodo.todo.todo.checklist.domain.Checklist;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.reminder.domain.Reminder;
import com.dodo.todo.todo.reminder.domain.ReminderType;
import com.dodo.todo.todo.repeat.domain.TodoRepeat;
import com.dodo.todo.todo.repeat.domain.TodoRepeatType;
import com.dodo.todo.todo.tag.domain.TodoTag;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record TodoResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        String memo,
        TodoStatus status,
        String priority,
        int sortOrder,
        LocalDateTime dueAt,
        List<TagResponse> tags,
        List<ChecklistResponse> checklists,
        RepeatResponse repeat,
        List<ReminderResponse> reminders
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getCategoryId(),
                todo.getCategory().getName(),
                todo.getTitle(),
                todo.getMemo(),
                todo.getStatus(),
                todo.getPriority(),
                todo.getSortOrder(),
                todo.getDueAt(),
                todo.getTodoTags().stream()
                        .map(TagResponse::from)
                        .toList(),
                todo.getChecklists().stream()
                        .map(ChecklistResponse::from)
                        .toList(),
                RepeatResponse.from(todo.getRepeat()),
                todo.getReminders().stream()
                        .map(ReminderResponse::from)
                        .toList()
        );
    }

    public record TagResponse(
            Long id,
            String name
    ) {

        private static TagResponse from(TodoTag todoTag) {
            return new TagResponse(todoTag.getTagId(), todoTag.getTag().getName());
        }
    }

    public record ChecklistResponse(
            Long id,
            String content,
            boolean completed
    ) {

        private static ChecklistResponse from(Checklist checklist) {
            return new ChecklistResponse(
                    checklist.getId(),
                    checklist.getContent(),
                    checklist.isCompleted()
            );
        }
    }

    public record RepeatResponse(
            Long id,
            TodoRepeatType repeatType,
            int repeatInterval,
            Set<DayOfWeek> daysOfWeek
    ) {

        private static RepeatResponse from(TodoRepeat repeat) {
            if (repeat == null) {
                return null;
            }

            return new RepeatResponse(
                    repeat.getId(),
                    repeat.getRepeatType(),
                    repeat.getRepeatInterval(),
                    repeat.getDaysOfWeek()
            );
        }
    }

    public record ReminderResponse(
            Long id,
            ReminderType reminderType,
            Integer remindBefore,
            LocalDateTime remindAt
    ) {

        private static ReminderResponse from(Reminder reminder) {
            return new ReminderResponse(
                    reminder.getId(),
                    reminder.getReminderType(),
                    reminder.getRemindBefore(),
                    reminder.getRemindAt()
            );
        }
    }
}
