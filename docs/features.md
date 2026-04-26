# Features

## Todo

- Todo API는 `POST /api/v1/todos`, `GET /api/v1/todos`, `GET /api/v1/todos/{todoId}`를 제공한다.
- 완료 처리는 `PATCH /api/v1/todos/{todoId}/complete`, `PATCH /api/v1/todos/{todoId}/undo`로 처리한다.
- 완료 이력은 `GET /api/v1/todos/histories`로 조회한다.
- Todo 상태는 `TODO`, `DONE`만 사용한다.
- 메인 목록은 `TODO` 상태의 루트 Todo만 조회한다.
- 일반 Todo 완료 시 `DONE` 상태가 되고 메인 목록에서 제외된다.
- 반복 Todo는 완료 후 다음 반복일이 있으면 `scheduledDate`를 이동시키고 다음 반복일이 없으면 `DONE` 상태가 된다.
- 태그 기능과 priority 기능은 사용하지 않는다.
- 응답 DTO의 식별자 필드는 엔티티명을 포함한 형식(`todoId`, `memberId`, `historyId`)으로 통일한다.

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
- Java 값 객체는 `RecurrenceRule`을 사용한다.
- 요일 약어는 RFC 5545 값을 따른다: `MO`, `TU`, `WE`, `TH`, `FR`, `SA`, `SU`
- `until`은 반복 종료일이다.
- `toRRuleText()`는 RFC 5545 형식의 RRULE 문자열을 만든다.
- 주/일 반복의 다음 날짜 계산은 ical4j를 사용한다.
- 월 반복의 말일 보정, 5주차 보정은 직접 계산한다.

지원 필드:

- `freq`
- `interval`
- `byDay`
- `byMonthDay`
- `until`

지원 규칙:

- Daily: `{ "freq": "DAILY", "interval": 1 }`
- Weekly: `{ "freq": "WEEKLY", "interval": 1, "byDay": ["MO", "WE"] }`
- Monthly 고정일: `{ "freq": "MONTHLY", "interval": 1, "byMonthDay": 31 }`
- Monthly n주차 요일: `{ "freq": "MONTHLY", "interval": 1, "byDay": ["4FR"] }`
- `interval`은 1 이상 정수다.
- `byDay`와 `byMonthDay`는 동시에 사용할 수 없다.
- Weekly `byDay`는 `MO`~`SU`만 허용한다.
- Monthly `byDay`는 `1MO`~`5SU`만 허용한다.
- Monthly `byMonthDay`는 1~31 사이 값만 허용한다.

월 반복 보정:

- `byMonthDay=31`에서 해당 월에 31일이 없으면 말일로 보정한다.
- 다음 월 계산 시 현재 `dayOfMonth`가 아니라 원래 `byMonthDay` 기준으로 계산해 드리프트가 발생하지 않도록 한다.
- `byDay=5FR`에서 해당 월에 5번째 금요일이 없으면 마지막 금요일로 보정한다.

종료일 규칙:

- `nextDate()`는 다음 반복 후보가 없거나 종료일을 넘으면 `null`을 반환한다.
- 주/일 반복은 ical4j의 `getNextDate(...)` 결과가 `null`이면 반복 종료로 본다.
- 월 반복은 계산된 다음 후보 날짜가 `until` 이후면 `null`을 반환한다.

## Complete

- 일반 Todo 완료 시 `TodoHistory`를 생성하고 Todo 상태를 `DONE`으로 변경한다.
- 반복 Todo 완료 시 `TodoHistory`를 생성한다.
- 반복 Todo 완료 시 `nextDate()`가 값을 반환하면 `scheduledDate`를 그 날짜로 이동한다.
- 반복 Todo 완료 시 `nextDate()`가 `null`이면 `DONE`으로 변경한다.
- 반복 Todo 완료 여부와 관계없이 Todo를 생성하지 않는다.
- 이미 `DONE`인 Todo를 다시 완료할 수 없다.
- `undo`는 `DONE` 상태를 `TODO`로 복구한다.
- 이미 `TODO`인 Todo는 `undo`할 수 없다.

## SubTodos

- mainTodo를 완료하면 subTodo도 함께 `DONE`으로 변경한다.
- mainTodo를 undo하면 subTodo도 함께 `TODO`로 복구한다.
- 개별 하위 Todo의 반복 규칙은 별도로 독립적이다.
- 다만 mainTodo 완료 시 subTodo의 반복 여부와 관계없이 함께 종료한다.

## Todo History

- `TodoHistory`는 완료 이력을 표현한다.
- `TodoHistory`는 `todoId`를 저장하고, 완료 당시 `title`을 별도로 저장한다.
- history는 반복 여부를 구분하지 않는다.
- history 조회는 최대 30건씩 조회한다.
- history 조회는 커서 기반 페이징을 사용한다.
- 정렬 기준은 `completedAt DESC`, `id DESC`다.
- 커서는 `completedAt`, `id` 쌍으로 사용한다.
- 참조 Todo가 삭제되어도 history 자체는 조회할 수 있다.
- history에서 연결된 Todo가 필요하면 `todoId`로 기존 Todo 단건 조회 API를 재사용한다.
- `ON DELETE CASCADE`는 사용하지 않는다.

## Reminder

- 알림 기능은 추후 추가 예정 범위다.
- 초기 범위에는 일회성 알림과 반복 알림을 포함할 수 있도록 고려한다.
- 알림 방식은 초기에는 웹 푸시 기반으로 시작할 수 있다.
- 이후 FCM 푸시 알림 등 다른 전송 수단도 추가할 수 있도록 확장성을 고려해 설계한다.