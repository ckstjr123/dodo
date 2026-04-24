# Features

## Todo

- Todo API는 `POST /api/v1/todos`, `GET /api/v1/todos`, `GET /api/v1/todos/{todoId}`를 제공한다.
- 완료 처리는 `PATCH /api/v1/todos/{todoId}/complete`, `PATCH /api/v1/todos/{todoId}/undo`로 처리한다.
- 완료 이력은 `GET /api/v1/todos/histories`로 조회한다.
- Todo 상태는 `TODO`, `DONE`을 사용한다.
- 메인 목록은 `TODO` 상태의 루트 Todo만 조회한다.
- 완료된 일반 Todo는 `DONE` 상태가 되고 메인 목록에서 제외된다.
- 반복 Todo는 완료 시 다음 반복일이 있으면 `scheduledDate`를 이동시키고, 다음 반복일이 없으면 `DONE` 상태가 된다.
- 태그 기능과 priority 기능은 사용하지 않는다.

## Todo Create

- 생성 요청 필드: `categoryId`, `mainTodoId`, `title`, `memo`, `sortOrder`, `dueAt`, `scheduledDate`, `scheduledTime`, `recurrenceRule`
- `categoryId`, `title`은 필수다.
- `title` 최대 길이는 200자다.
- `memo` 최대 길이는 1000자다.
- `mainTodoId`는 선택 값이다.
- `mainTodoId`는 현재 회원이 소유한 루트 Todo만 허용한다.
- 하위 Todo 아래에 다시 하위 Todo를 생성할 수 없다.
- `recurrenceRule`이 없는 Todo는 `scheduledDate`가 선택 값이다.
- `recurrenceRule`이 있는 Todo는 `scheduledDate`가 필수다.

## Recurrence Rule

- 반복 설정은 `recurrence_rule` JSON 컬럼 하나로 저장한다.
- Java 값 타입은 `RecurrenceRule`을 사용한다.
- 요일 약어는 RFC 5545 값을 따른다: `MO`, `TU`, `WE`, `TH`, `FR`, `SA`, `SU`
- `until`은 반복 종료일이다.
- `toRRuleText()`는 RFC 5545 형식의 RRULE 문자열을 만든다.
- 일/주 반복의 다음 날짜 계산은 ical4j를 사용한다.
- 월 반복의 마지막 날 보정, 5주차 보정은 직접 계산한다.

지원 필드:

- `freq`
- `interval`
- `byDay`
- `byMonthDay`
- `until`

지원 규칙:

- Daily: `{ "freq": "DAILY", "interval": 1 }`
- Weekly: `{ "freq": "WEEKLY", "interval": 1, "byDay": ["MO", "WE"] }`
- Monthly 특정일: `{ "freq": "MONTHLY", "interval": 1, "byMonthDay": 31 }`
- Monthly n주차 요일: `{ "freq": "MONTHLY", "interval": 1, "byDay": ["4FR"] }`
- `interval`은 1 이상의 정수다.
- `byDay`와 `byMonthDay`는 동시에 사용할 수 없다.
- Weekly `byDay`는 `MO`~`SU`만 허용한다.
- Monthly `byDay`는 `1MO`~`5SU`만 허용한다.
- Monthly `byMonthDay`는 1~31 사이 값만 허용한다.

월 반복 보정:

- `byMonthDay=31`에서 해당 월에 31일이 없으면 마지막 날로 보정한다.
- `byDay=5FR`에서 해당 월에 5번째 금요일이 없으면 마지막 금요일로 보정한다.

종료일 규칙:

- `nextDate()`는 다음 반복 후보가 없거나 종료일을 넘으면 `null`을 반환한다.
- 일/주 반복은 ical4j의 `getNextDate(...)` 결과가 `null`이면 반복 종료로 본다.
- 월 반복은 계산된 다음 후보 날짜가 `until` 이후면 `null`을 반환한다.

## Complete

- 일반 Todo 완료 시 `TodoHistory`를 생성하고 Todo 상태를 `DONE`으로 변경한다.
- 반복 Todo 완료 시 `TodoHistory`를 생성한다.
- 반복 Todo 완료 후 `nextDate()`가 값을 반환하면 `scheduledDate`를 그 날짜로 이동한다.
- 반복 Todo 완료 후 `nextDate()`가 `null`이면 `DONE`으로 변경한다.
- 반복 Todo 완료 시 새 Todo를 생성하지 않는다.
- `undo`는 `DONE` 상태를 `TODO`로 복구한다.

## SubTodos

- mainTodo를 완료하면 subTodo도 함께 `DONE`으로 변경한다.
- mainTodo를 undo하면 subTodo도 함께 `TODO`로 복구한다.
- 상위와 하위 Todo의 반복 설정은 서로 독립적이다.
- 다만 mainTodo 완료 시 subTodo는 반복 여부와 관계없이 함께 종료된다.

## Todo History

- `TodoHistory`는 완료 이력을 나타낸다.
- `TodoHistory`는 Todo를 참조하고, 완료 당시 `title`을 별도로 저장한다.
- history는 반복 여부를 구분하지 않는다.
- history 조회는 최대 30건씩 조회한다.
- history 조회는 커서 기반 페이징을 사용한다.
- 정렬 기준은 `completedAt DESC`, `id DESC`다.
- 참조 Todo가 삭제된 경우 해당 history는 조회할 수 없도록 예외 처리한다.
- `ON DELETE CASCADE`는 사용하지 않는다.

## Removed Scope

- Tag
- TodoTag
- priority
- Todo completedAt 컬럼
- cycleGroupId
- CycleScheduleCalculator
- 반복 Todo 복제 방식
- 하위 Todo 복제 방식
- 반복 전체 수정/중단
- 동시성 락
- 반복 횟수 제한
- 여러 회차 미리 생성
- 반복 완료 취소
