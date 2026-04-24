package com.dodo.todo.todo.controller;

import com.dodo.todo.auth.resolver.LoginMember;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.todo.domain.TodoHistory;
import com.dodo.todo.todo.dto.TodoHistoryListResponse;
import com.dodo.todo.todo.dto.TodoHistoryResponse;
import com.dodo.todo.todo.repository.TodoHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(readOnly = true)
    public TodoHistoryListResponse getHistories(
            @LoginMember Long memberId,
            @RequestParam(required = false) Long todoId,
            @RequestParam(required = false) LocalDateTime cursorCompletedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) Integer size
    ) {
        int pageSize = size == null || size < 1 ? DEFAULT_HISTORY_SIZE : Math.min(size, MAX_HISTORY_SIZE);
        List<TodoHistory> histories = todoHistoryRepository.findHistories(
                memberId,
                todoId,
                cursorCompletedAt,
                cursorId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = histories.size() > pageSize;
        if (hasNext) {
            histories = histories.subList(0, pageSize);
        }

        if (histories.stream().anyMatch(history -> history.getTodo() == null)) {
            throw new ApiException("TODO_HISTORY_TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo history cannot be viewed because todo was deleted");
        }

        LocalDateTime nextCursorCompletedAt = null;
        Long nextCursorId = null;
        if (hasNext && !histories.isEmpty()) {
            TodoHistory lastHistory = histories.get(histories.size() - 1);
            nextCursorCompletedAt = lastHistory.getCompletedAt();
            nextCursorId = lastHistory.getId();
        }

        return new TodoHistoryListResponse(
                histories.stream().map(TodoHistoryResponse::from).toList(),
                nextCursorCompletedAt,
                nextCursorId,
                hasNext
        );
    }
}
