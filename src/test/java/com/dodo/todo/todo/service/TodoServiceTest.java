package com.dodo.todo.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.dodo.todo.todo.reminder.domain.ReminderType;
import com.dodo.todo.todo.repeat.domain.TodoRepeatType;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TodoRepository todoRepository;

    @Spy
    private ReminderPolicy reminderPolicy = new ReminderPolicy();

    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("현재 회원 소유가 아닌 카테고리로 Todo를 생성하면 예외가 발생한다")
    void createTodoRejectsForeignCategory() {
        Member member = Member.of("member@example.com");
        Member otherMember = Member.of("other@example.com");
        Category category = Category.create(otherMember, "업무");

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> todoService.createTodo(1L, createRequest(10L, List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("현재 회원 소유가 아닌 태그로 Todo를 생성하면 예외가 발생한다")
    void createTodoRejectsForeignTag() {
        Member member = Member.of("member@example.com");
        Member otherMember = Member.of("other@example.com");
        Category category = Category.create(member, "업무");
        Tag tag = Tag.create(otherMember, "중요");

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(tagRepository.findById(20L)).thenReturn(Optional.of(tag));

        assertThatThrownBy(() -> todoService.createTodo(1L, createRequest(10L, List.of(20L))))
                .isInstanceOf(ApiException.class)
                .hasMessage("Tag not found");
    }

    @Test
    @DisplayName("현재 회원 소유 카테고리와 태그를 Todo에 연결한다")
    void createTodoUsesOwnedCategoryAndTag() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "개인");
        Tag tag = Tag.create(member, "home");
        TodoCreateRequest request = createRequest(10L, List.of(20L));

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(tagRepository.findById(20L)).thenReturn(Optional.of(tag));
        saveTodoAsRequested();

        TodoResponse response = todoService.createTodo(1L, request);

        assertThat(response.categoryName()).isEqualTo("개인");
        assertThat(response.tags()).extracting(TodoResponse.TagResponse::name)
                .containsExactly("home");
    }

    @Test
    @DisplayName("체크리스트와 반복과 알림을 Todo 생성과 함께 저장한다")
    void createTodoWithChildOptions() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");
        LocalDateTime dueAt = LocalDateTime.of(2026, 4, 7, 18, 0);
        LocalDateTime remindAt = LocalDateTime.of(2026, 4, 7, 9, 0);
        TodoCreateRequest request = new TodoCreateRequest(
                10L,
                "보고서 작성",
                "초안부터 작성",
                "HIGH",
                1,
                dueAt,
                List.of(),
                List.of(new TodoCreateRequest.ChecklistRequest("초안 작성")),
                new TodoCreateRequest.RepeatRequest(TodoRepeatType.WEEKLY, 1, Set.of(DayOfWeek.MONDAY)),
                List.of(new TodoCreateRequest.ReminderRequest(ReminderType.ABSOLUTE_AT, null, remindAt))
        );

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        saveTodoAsRequested();

        TodoResponse response = todoService.createTodo(1L, request);

        assertThat(response.checklists()).extracting(TodoResponse.ChecklistResponse::content)
                .containsExactly("초안 작성");
        assertThat(response.repeat().repeatType()).isEqualTo(TodoRepeatType.WEEKLY);
        assertThat(response.reminders()).extracting(TodoResponse.ReminderResponse::remindAt)
                .containsExactly(remindAt);
    }

    @Test
    @DisplayName("카테고리 ID가 실제 엔티티를 찾지 못하면 예외가 발생한다")
    void createTodoRejectsMissingCategory() {
        Member member = Member.of("member@example.com");

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.createTodo(1L, createRequest(99L, List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("태그 ID가 실제 엔티티를 찾지 못하면 예외가 발생한다")
    void createTodoRejectsMissingTag() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");

        when(memberService.findById(1L)).thenReturn(member);
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.createTodo(1L, createRequest(10L, List.of(99L))))
                .isInstanceOf(ApiException.class)
                .hasMessage("Tag not found");
    }

    @Test
    @DisplayName("현재 회원의 Todo 목록을 조회한다")
    void getTodosReturnsCurrentMemberTodos() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");
        Todo todo = todo(member, category, "보고서 작성");

        when(todoRepository.findWithCategoryAndRepeatByMemberId(1L)).thenReturn(List.of(todo));

        TodoListResponse response = todoService.getTodos(1L);

        assertThat(response.todos()).hasSize(1);
        assertThat(response.todos().get(0).title()).isEqualTo("보고서 작성");
    }

    @Test
    @DisplayName("현재 회원 소유 Todo 단건을 조회한다")
    void getTodoReturnsCurrentMemberTodo() {
        Member member = Member.of("member@example.com");
        Category category = Category.create(member, "업무");
        Todo todo = todo(member, category, "보고서 작성");

        when(memberService.findById(1L)).thenReturn(member);
        when(todoRepository.findWithCategoryAndRepeatById(7L)).thenReturn(Optional.of(todo));

        TodoResponse response = todoService.getTodo(1L, 7L);

        assertThat(response.categoryName()).isEqualTo("업무");
        assertThat(response.title()).isEqualTo("보고서 작성");
    }

    @Test
    @DisplayName("현재 회원 소유가 아닌 Todo 단건은 조회할 수 없다")
    void getTodoRejectsForeignTodo() {
        Member member = Member.of("member@example.com");
        Member otherMember = Member.of("other@example.com");
        Category category = Category.create(otherMember, "업무");
        Todo todo = todo(otherMember, category, "보고서 작성");

        when(memberService.findById(1L)).thenReturn(member);
        when(todoRepository.findWithCategoryAndRepeatById(7L)).thenReturn(Optional.of(todo));

        assertThatThrownBy(() -> todoService.getTodo(1L, 7L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Todo not found");
    }

    private TodoCreateRequest createRequest(Long categoryId, List<Long> tagIds) {
        return new TodoCreateRequest(
                categoryId,
                "보고서 작성",
                "초안부터 작성",
                "HIGH",
                1,
                LocalDateTime.of(2026, 4, 7, 18, 0),
                tagIds,
                List.of(),
                null,
                List.of()
        );
    }

    private Todo todo(Member member, Category category, String title) {
        return Todo.builder()
                .member(member)
                .category(category)
                .title(title)
                .status(TodoStatus.OPEN)
                .priority("HIGH")
                .build();
    }

    private void saveTodoAsRequested() {
        when(todoRepository.save(any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Todo.class));
    }
}
