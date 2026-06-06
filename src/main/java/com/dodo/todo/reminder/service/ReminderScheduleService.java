package com.dodo.todo.reminder.service;

import com.dodo.todo.notification.service.NotificationService;
import com.dodo.todo.reminder.domain.Reminder;
import com.dodo.todo.reminder.repository.ReminderRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderScheduleService {

    private final TaskScheduler taskScheduler;
    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final Map<Long, ScheduledFuture<?>> schedules = new ConcurrentHashMap<>();

    /**
     * Reminder 예약
     * 계산된 알림 시각이 미래인 경우에만 메모리 예약을 등록한다.
     */
    public void schedule(Reminder reminder) {
        cancel(reminder.getId());

        LocalDateTime remindAt = reminder.calculateRemindAt();
        if (!remindAt.isAfter(LocalDateTime.now())) {
            return;
        }

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> send(reminder.getId()),
                remindAt.atZone(ZoneId.systemDefault()).toInstant()
        );
        schedules.put(reminder.getId(), future);
    }

    /**
     * Reminder 예약 취소
     * 메모리에 등록된 예약 작업이 있으면 취소한다.
     */
    public void cancel(Long reminderId) {
        ScheduledFuture<?> future = schedules.remove(reminderId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 예약된 Reminder 발송
     * 발송 직전에 Reminder와 Todo 상태를 다시 확인한다.
     */
    @Transactional(readOnly = true)
    public void send(Long reminderId) {
        reminderRepository.findById(reminderId)
                .filter(this::isSendable)
                .ifPresent(this::sendPush);
    }

    private boolean isSendable(Reminder reminder) {
        Todo todo = reminder.getTodo();

        return todo.getStatus() == TodoStatus.TODO
                && !reminder.calculateRemindAt().isAfter(LocalDateTime.now());
    }

    private void sendPush(Reminder reminder) {
        notificationService.send(
                reminder.getMember().getFcmToken(),
                "Todo 알림",
                reminder.getTodo().getTitle()
        );
    }
}
