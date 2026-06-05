package com.dodo.todo.reminder.service;

import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.domain.ReminderError;
import com.dodo.todo.reminder.domain.ReminderFactory;
import com.dodo.todo.reminder.dto.ReminderCreateRequest;
import com.dodo.todo.reminder.dto.ReminderUpdateRequest;
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
    private final ReminderScheduleService reminderScheduleService;

    /**
     * 알림 생성
     * 알림 타입별 설정값을 검증한 뒤 Todo에 알림을 추가한다.
     */
    @Transactional
    public Long saveReminder(Long memberId, ReminderCreateRequest request) {
        Member member = memberService.findById(memberId);
        Todo todo = findTodo(request.todoId(), memberId);
        validateReminderLimit(todo.getId(), 1);

        Reminder reminder = reminderRepository.save(ReminderFactory.create(todo, member, request));
        reminderScheduleService.schedule(reminder);

        return reminder.getId();
    }

    /**
     * 알림 목록 생성
     * Todo 생성 시 전달된 초기 알림 설정들을 저장한다.
     */
    @Transactional
    public List<Long> saveReminders(Todo todo, Member member, List<ReminderCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        validateReminderLimit(todo.getId(), requests.size());

        List<Reminder> reminders = requests.stream()
                .map(request -> ReminderFactory.create(todo, member, request))
                .toList();
        List<Reminder> savedReminders = reminderRepository.saveAll(reminders);
        savedReminders.forEach(reminderScheduleService::schedule);

        return savedReminders.stream()
                .map(Reminder::getId)
                .toList();
    }

    /**
     * 알림 수정
     * 기존 알림 row를 유지하고 타입별 설정값만 변경한다.
     */
    @Transactional
    public void updateReminder(Long memberId, Long reminderId, ReminderUpdateRequest request) {
        Reminder reminder = findReminder(memberId, reminderId);

        reminder.update(request);
        reminderScheduleService.schedule(reminder);
    }

    /**
     * 알림 삭제
     * 요청한 회원의 알림을 삭제한다.
     */
    @Transactional
    public void deleteReminder(Long memberId, Long reminderId) {
        Reminder reminder = findReminder(memberId, reminderId);

        reminderScheduleService.cancel(reminderId);
        reminderRepository.delete(reminder);
    }

    /**
     * Todo 알림 삭제
     * Todo 일정이 제거되거나 Todo가 삭제될 때 연결된 알림을 정리한다.
     */
    @Transactional
    public void deleteRemindersByTodoId(Long todoId) {
        reminderRepository.findIdsByTodoId(todoId)
                .forEach(reminderScheduleService::cancel);
        reminderRepository.deleteByTodoId(todoId);
    }

    /**
     * Todo Reminder 재예약
     * Todo 일정이 변경된 경우 연결된 알림 예약을 다시 계산한다.
     */
    @Transactional(readOnly = true)
    public void rescheduleRemindersByTodoId(Long todoId) {
        reminderRepository.findAllByTodoId(todoId)
                .forEach(reminderScheduleService::schedule);
    }

    /**
     * 하위 Todo 알림 삭제
     * 부모 Todo 삭제 전에 하위 Todo에 연결된 알림을 정리한다.
     */
    @Transactional
    public void deleteRemindersByParentTodoId(Long parentTodoId) {
        reminderRepository.findIdsByParentTodoId(parentTodoId)
                .forEach(reminderScheduleService::cancel);
        reminderRepository.deleteByParentTodoId(parentTodoId);
    }

    private void validateReminderLimit(Long todoId, int newReminderCount) {
        int savedReminderCount = reminderRepository.countByTodoId(todoId);
        if (savedReminderCount + newReminderCount > MAX_REMINDER_COUNT) {
            throw new BusinessException(ReminderError.REMINDER_LIMIT_EXCEEDED);
        }
    }

    private Reminder findReminder(Long memberId, Long reminderId) {
        return reminderRepository.findByIdAndMemberId(reminderId, memberId)
                .orElseThrow(() -> new BusinessException(ReminderError.REMINDER_NOT_FOUND));
    }

    private Todo findTodo(Long todoId, Long memberId) {
        return todoRepository.findByIdAndMemberId(todoId, memberId)
                .orElseThrow(() -> new BusinessException(TodoError.TODO_NOT_FOUND));
    }
}
