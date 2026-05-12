package com.dodo.todo.reminder.service;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.domain.ReminderError;
import com.dodo.todo.reminder.dto.ReminderRequest;
import com.dodo.todo.reminder.dto.ReminderResponse;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoError;
import com.dodo.todo.todo.repository.TodoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final int MAX_REMINDER_COUNT = 5;

    private final MemberService memberService;
    private final TodoRepository todoRepository;
    private final ReminderRepository reminderRepository;

    /**
     * 알림 생성
     * Todo에 날짜와 시간이 있을 때만 단건 알림을 추가한다.
     */
    @Transactional
    public ReminderResponse createReminder(Long memberId, Long todoId, ReminderRequest request) {
        Member member = memberService.findById(memberId);
        Todo todo = findTodo(todoId, memberId);
        List<ReminderRequest> requests = List.of(request);

        validateSchedule(todo);
        validateReminderLimit(todo.getId(), requests.size());
        validateDuplicateMinuteOffset(todo.getId(), request.minuteOffset());

        Reminder reminder = reminderRepository.save(Reminder.create(todo, member, request.minuteOffset()));

        return ReminderResponse.from(reminder);
    }

    /**
     * 알림 목록 생성
     * Todo 생성 시 전달된 초기 알림을 저장한다.
     */
    @Transactional
    public void createReminders(Todo todo, Member member, List<ReminderRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        validateSchedule(todo);
        validateReminderLimit(todo.getId(), requests.size());
        validateDistinctMinuteOffsets(requests.stream()
                .map(ReminderRequest::minuteOffset)
                .toList());

        List<Reminder> reminders = requests.stream()
                .map(request -> Reminder.create(todo, member, request.minuteOffset()))
                .toList();
        reminderRepository.saveAll(reminders);
    }

    /**
     * 알림 수정
     * 기존 알림 row를 유지하고 offset과 알림 시각을 갱신한다.
     */
    @Transactional
    public ReminderResponse updateReminder(Long memberId, Long todoId, Long reminderId, ReminderRequest request) {
        Reminder reminder = findReminder(memberId, todoId, reminderId);
        validateSchedule(reminder.getTodo());
        if (reminder.getMinuteOffset() != request.minuteOffset()) {
            validateDuplicateMinuteOffset(todoId, request.minuteOffset());
        }

        reminder.updateMinuteOffset(request.minuteOffset());

        return ReminderResponse.from(reminder);
    }

    /**
     * 알림 삭제
     * 요청한 Todo에 속한 알림만 삭제한다.
     */
    @Transactional
    public void deleteReminder(Long memberId, Long todoId, Long reminderId) {
        Reminder reminder = findReminder(memberId, todoId, reminderId);

        reminderRepository.delete(reminder);
    }

    /**
     * Todo 알림 삭제
     * Todo 일정이 제거되거나 Todo가 삭제될 때 연결된 알림을 정리한다.
     */
    @Transactional
    public void deleteRemindersByTodoId(Long todoId) {
        reminderRepository.deleteByTodoId(todoId);
    }

    /**
     * 하위 Todo 알림 삭제
     * 부모 Todo 삭제 전에 하위 Todo에 연결된 알림을 정리한다.
     */
    @Transactional
    public void deleteRemindersByParentTodoId(Long parentTodoId) {
        reminderRepository.deleteByParentTodoId(parentTodoId);
    }

    private void validateDistinctMinuteOffsets(List<Integer> minuteOffsets) {
        long distinctCount = minuteOffsets.stream()
                .distinct()
                .count();

        if (distinctCount != minuteOffsets.size()) {
            throw new BusinessException(ReminderError.REMINDER_OFFSET_DUPLICATED);
        }
    }

    private void validateReminderLimit(Long todoId, int newReminderCount) {
        int savedReminderCount = reminderRepository.countByTodoId(todoId);
        if (savedReminderCount + newReminderCount > MAX_REMINDER_COUNT) {
            throw new BusinessException(ReminderError.REMINDER_LIMIT_EXCEEDED);
        }
    }

    private void validateSchedule(Todo todo) {
        if (todo.getScheduledDate() == null || todo.getScheduledTime() == null) {
            throw new BusinessException(ReminderError.REMINDER_SCHEDULE_REQUIRED);
        }
    }

    private void validateDuplicateMinuteOffset(Long todoId, int minuteOffset) {
        if (reminderRepository.existsByTodoIdAndMinuteOffset(todoId, minuteOffset)) {
            throw new BusinessException(ReminderError.REMINDER_OFFSET_DUPLICATED);
        }
    }

    private Reminder findReminder(Long memberId, Long todoId, Long reminderId) {
        return reminderRepository.findByIdAndTodoIdAndMemberId(reminderId, todoId, memberId)
                .orElseThrow(() -> new BusinessException(ReminderError.REMINDER_NOT_FOUND));
    }

    private Todo findTodo(Long todoId, Long memberId) {
        return todoRepository.findByIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));
    }
}
