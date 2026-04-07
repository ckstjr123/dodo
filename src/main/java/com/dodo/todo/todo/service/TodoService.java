package com.dodo.todo.todo.service;

import com.dodo.todo.category.domain.Category;
import com.dodo.todo.category.domain.CategoryRepository;
import com.dodo.todo.common.exception.ApiException;
import com.dodo.todo.member.domain.Member;
import com.dodo.todo.member.service.MemberService;
import com.dodo.todo.tag.domain.Tag;
import com.dodo.todo.tag.domain.TagRepository;
import com.dodo.todo.todo.domain.Todo;
import com.dodo.todo.todo.domain.TodoRepository;
import com.dodo.todo.todo.domain.TodoStatus;
import com.dodo.todo.todo.dto.TodoCreateRequest;
import com.dodo.todo.todo.dto.TodoListResponse;
import com.dodo.todo.todo.dto.TodoResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final MemberService memberService;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final TodoRepository todoRepository;
    private final ReminderPolicy reminderPolicy;

    /**
     * Todo 생성
     * 현재 회원 기준으로 카테고리, 태그, 체크리스트, 반복, 알림을 함께 저장함.
     */
    @Transactional
    public TodoResponse createTodo(Long memberId, TodoCreateRequest request) {
        Member member = memberService.findById(memberId);
        Category category = resolveCategory(member, request.categoryId());
        List<Tag> tags = resolveTags(member, request.tagIds());

        Todo todo = Todo.builder()
                .member(member)
                .category(category)
                .title(request.title())
                .memo(request.memo())
                .status(TodoStatus.OPEN)
                .priority(request.priority())
                .sortOrder(request.sortOrder())
                .dueAt(request.dueAt())
                .build();

        tags.forEach(todo::addTag);
        addChecklists(todo, request.checklists());
        registerRepeat(todo, request.repeat());
        addReminders(todo, request.reminders());

        return TodoResponse.from(todoRepository.save(todo));
    }

    /**
     * Todo 목록 조회
     * 현재 회원이 소유한 Todo만 조회함.
     */
    @Transactional(readOnly = true)
    public TodoListResponse getTodos(Long memberId) {
        List<TodoResponse> todos = todoRepository.findWithCategoryAndRepeatByMemberId(memberId).stream()
                .map(TodoResponse::from)
                .toList();

        return new TodoListResponse(todos);
    }

    /**
     * Todo 단건 조회
     * Todo 엔티티를 조회한 뒤 현재 회원 소유인지 검증함.
     */
    @Transactional(readOnly = true)
    public TodoResponse getTodo(Long memberId, Long todoId) {
        Member member = memberService.findById(memberId);
        Todo todo = todoRepository.findWithCategoryAndRepeatById(todoId)
                .orElseThrow(() -> new ApiException("TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo not found"));
        validateTodoOwner(todo, member);

        return TodoResponse.from(todo);
    }

    private Category resolveCategory(Member member, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Category not found"));

        if (category.isOwnedBy(member)) {
            return category;
        }

        return categoryRepository.save(Category.create(member, category.getName()));
    }

    private List<Tag> resolveTags(Member member, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = new ArrayList<>();
        Set<Long> uniqueTagIds = new LinkedHashSet<>(tagIds);
        for (Long tagId : uniqueTagIds) {
            tags.add(resolveTag(member, tagId));
        }

        return tags;
    }

    private Tag resolveTag(Member member, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ApiException("TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "Tag not found"));

        if (tag.isOwnedBy(member)) {
            return tag;
        }

        return tagRepository.save(Tag.create(member, tag.getName()));
    }

    private void validateTodoOwner(Todo todo, Member member) {
        if (!todo.isOwnedBy(member)) {
            throw new ApiException("TODO_NOT_FOUND", HttpStatus.NOT_FOUND, "Todo not found");
        }
    }

    /**
     * 체크리스트 적용
     * 요청된 체크리스트 항목을 Todo 하위 요소로 등록함.
     */
    private void addChecklists(Todo todo, List<TodoCreateRequest.ChecklistRequest> requests) {
        if (requests == null) {
            return;
        }

        requests.forEach(request -> todo.addChecklist(request.content()));
    }

    /**
     * 반복 설정 적용
     * 요청된 반복 타입에 맞춰 Todo 반복 규칙을 등록함.
     */
    private void registerRepeat(Todo todo, TodoCreateRequest.RepeatRequest request) {
        if (request == null) {
            return;
        }

        switch (request.repeatType()) {
            case DAILY -> todo.setDailyRepeat(request.repeatInterval());
            case WEEKLY -> todo.setWeeklyRepeat(
                    request.repeatInterval(),
                    request.daysOfWeek() == null ? Set.of() : request.daysOfWeek()
            );
        }
    }

    /**
     * 알림 적용
     * 요청된 알림 목록을 Todo 하위 알림 규칙으로 등록함.
     */
    private void addReminders(Todo todo, List<TodoCreateRequest.ReminderRequest> requests) {
        if (requests == null) {
            return;
        }

        requests.forEach(request -> reminderPolicy.addReminder(todo, request));
    }

}
