package com.dodo.todo.todo.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.common.exception.BusinessException;
import com.dodo.todo.todo.domain.TodoError;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.dto.TodoHistoryListResponse;
import com.dodo.todo.todo.dto.TodoHistoryResponse;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/v1/todos/histories")
@RequiredArgsConstructor
public class TodoHistoryController implements TodoHistoryApiDocs {

    private static final int DEFAULT_HISTORY_SIZE = 30;
    private static final int MAX_HISTORY_SIZE = 30;

    private final TodoHistoryRepository todoHistoryRepository;

    /**
     * 완료 이력 조회
     * cursor 기반으로 최대 30건씩 완료 이력을 조회한다.
     */
    @Override
    @GetMapping
    public TodoHistoryListResponse getHistories(
            @LoginMember Long memberId,
            @RequestParam(required = false) Long todoId,
            @RequestParam(required = false) LocalDateTime cursorCompletedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "30") Integer size
    ) {
        if ((cursorCompletedAt == null) != (cursorId == null)) {
            throw new BusinessException(
                    TodoError.INVALID_CURSOR.code(),
                    TodoError.INVALID_CURSOR.status(),
                    TodoError.INVALID_CURSOR.message()
            );
        }

        int pageSize = size < 1 ? DEFAULT_HISTORY_SIZE : Math.min(size, MAX_HISTORY_SIZE);
        Slice<TodoHistory> historySlice = todoHistoryRepository.findHistories(
                memberId,
                todoId,
                cursorCompletedAt,
                cursorId,
                PageRequest.of(0, pageSize)
        );
        List<TodoHistory> histories = historySlice.getContent();
        List<TodoHistoryResponse> historyResponses = histories.stream()
                .map(TodoHistoryResponse::from)
                .toList();

        if (!historySlice.hasNext() || histories.isEmpty()) {
            return new TodoHistoryListResponse(historyResponses, null, null, false);
        }

        TodoHistory lastHistory = histories.get(histories.size() - 1);
        return new TodoHistoryListResponse(
                historyResponses,
                lastHistory.getCompletedAt(),
                lastHistory.getId(),
                true
        );
    }

}
