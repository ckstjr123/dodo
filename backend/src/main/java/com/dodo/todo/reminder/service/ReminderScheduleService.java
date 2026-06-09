package com.dodo.todo.reminder.service;

import com.dodo.todo.notification.service.NotificationService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class ReminderScheduleService {

    private final TaskScheduler taskScheduler;
    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final Map<Long, ScheduledTask> schedules = new ConcurrentHashMap<>();

    private record ScheduledTask(ScheduledFuture<?> future, LocalDateTime remindAt) {}

    /**
     * Reminder 예약
     * 계산된 알림 시각이 미래인 경우에만 메모리 예약을 등록한다.
     * 이미 동일한 시각으로 예약된 경우 재등록하지 않는다(No-op).
     */
    public void schedule(Reminder reminder) {
        LocalDateTime remindAt = reminder.calculateRemindAt();
        ScheduledTask existingTask = schedules.get(reminder.getId());

        // 이미 동일한 시각으로 예약되어 있다면 무시
        if (existingTask != null && existingTask.remindAt().equals(remindAt)) {
            return;
        }

        // 변경된 시각이 과거라면 기존 예약을 취소하고 새로 예약하지 않음
        if (!remindAt.isAfter(LocalDateTime.now())) {
            cancel(reminder.getId());
            return;
        }

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> remind(reminder.getId(), remindAt),
                remindAt.atZone(ZoneId.systemDefault()).toInstant()
        );

        ScheduledTask oldTask = schedules.put(reminder.getId(), new ScheduledTask(future, remindAt));
        if (oldTask != null) {
            oldTask.future().cancel(false);
        }
    }

    /**
     * Reminder 예약 취소
     * 메모리에 등록된 예약 작업이 있으면 취소한다.
     */
    public void cancel(Long reminderId) {
        ScheduledTask task = schedules.remove(reminderId);
        if (task != null) {
            task.future().cancel(false); // 작업이 이미 시작된 경우 완료되도록 둠
        }
    }

    /**
     * 예약된 Reminder 발송
     * 메모리 누수 방지를 위한 삭제 로직
     * computeIfPresent는 Map에 해당 reminderId가 있을 때만 람다식을 실행합니다.
     * 람다식이 null을 반환하면 Map에서 해당 Entry를 삭제(remove)하고,
     * 기존 task를 그대로 반환하면 아무 일도 일어나지 않습니다(유지).
     * 즉, "Map에 들어있는 예약 시간이 현재 스케줄러 스레드가 쥐고 있는 예약 시간과 같다면 지우고,
     * 시간이 다르다면(발송 직전에 새로운 시간으로 갱신되었다면) 지우지 마라"는 의미입니다.
     * @param reminderId 발송 대상 리마인더 식별자
     * @param remindAt 해당 스케줄 작업이 예약될 당시에 캡처된 알림 발송 시각. Map에 저장된 최신 예약 시간과 비교하기 위해 사용됨
     */
    void remind(Long reminderId, LocalDateTime remindAt) {
        schedules.computeIfPresent(reminderId, (id, task) ->
                task.remindAt().equals(remindAt) ? null : task
        );

        reminderRepository.findById(reminderId)
                .filter(this::isSendable)
                .ifPresent(this::send);
    }

    private boolean isSendable(Reminder reminder) {
        Todo todo = reminder.getTodo();

        return todo.getStatus() == TodoStatus.TODO
                && !reminder.calculateRemindAt().isAfter(LocalDateTime.now());
    }

    private void send(Reminder reminder) {
        notificationService.send(
                reminder.getMember().getFcmToken(),
                reminder.getTodo().getTitle(),
                reminder.getTodo().getMemo()
        );
    }
}
