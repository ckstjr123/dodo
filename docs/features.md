# Features

## Todo

- Todo API는 `POST /api/v1/todos`, `GET /api/v1/todos`, `GET /api/v1/todos/{todoId}`를 제공한다.
- 완료 처리는 `PATCH /api/v1/todos/{todoId}/complete`, `PATCH /api/v1/todos/{todoId}/undo`로 처리한다.
- 삭제 처리는 `DELETE /api/v1/todos/{todoId}`로 처리한다.
- 완료 이력은 `GET /api/v1/todos/histories`로 조회한다.
- Todo 상태는 `TODO`, `DONE`만 사용한다.
- 메인 목록은 `TODO` 상태의 루트 Todo만 조회한다.
- 일반 Todo 완료 시 `DONE` 상태가 되고 메인 목록에서 제외된다.
- 반복 Todo는 완료 후 다음 반복일이 있으면 `scheduledDate`를 이동시키고 다음 반복일이 없으면 `DONE` 상태가 된다.
- Todo는 마지막 완료 시각인 `completedAt`을 가진다.
- 태그 기능과 priority 기능은 사용하지 않는다.
- 응답 DTO의 식별자 필드는 엔티티명을 포함한 형식(`todoId`, `memberId`, `historyId`)으로 통일한다.

## Todo Create

- 생성 요청 필드: `categoryId`, `parentTodoId(mainTodoId)`, `title`, `memo`, `sortOrder`, `dueAt`, `scheduledDate`, `scheduledTime`, `recurrence`
- `categoryId`, `title`은 필수다.
- `title` 최대 길이는 200자다.
- `memo` 최대 길이는 1000자다.
- `mainTodoId`는 선택 값이다.
- `mainTodoId`는 현재 회원이 소유한 루트 Todo만 허용한다.
- 하위 Todo 아래에 다시 하위 Todo를 생성할 수 없다.
- `recurrence`가 없는 Todo는 `scheduledDate`가 선택 값이다.
- `recurrence`가 있는 Todo는 `scheduledDate`가 필수다.

## Todo Update

- 수정 API는 `PATCH /api/v1/todos/{todoId}`를 사용한다.
- 수정 요청 필드: `categoryId`, `title`, `memo`, `sortOrder`, `dueAt`, `scheduledDate`, `scheduledTime`, `recurrence`
- `parentTodoId`와 `subTodos`는 수정 요청 필드에 포함하지 않는다.
- `status`, `completedAt`은 완료/취소 API에서만 변경한다.
- `TodoHistory`는 완료 시점의 이력 snapshot이므로 수정 API에서 변경하지 않는다.
- 수정 대상 Todo는 현재 회원이 소유한 Todo여야 한다.
- `categoryId`는 현재 회원이 소유한 카테고리만 허용한다.
- `categoryId`, `title`은 필수다.
- `title` 최대 길이는 200자다.
- `memo` 최대 길이는 1000자다.
- `recurrence`가 없는 Todo는 `scheduledDate`가 선택 값이다.
- `recurrence`가 있는 Todo는 `scheduledDate`가 필수다.
- `recurrence=null`이면 반복 설정을 제거한다.

## Todo Delete

- 삭제 API는 `DELETE /api/v1/todos/{todoId}`를 사용한다.
- 삭제는 물리 삭제로 처리하며 복구 기능은 제공하지 않는다.
- 삭제 대상 Todo는 현재 회원이 소유한 Todo여야 한다.
- 완료 여부와 반복 여부와 관계없이 삭제할 수 있다.
- 메인 Todo를 삭제하면 하위 Todo를 먼저 직접 삭제한 뒤 메인 Todo를 삭제한다.
- 하위 Todo를 직접 삭제하면 해당 하위 Todo만 삭제한다.
- 삭제된 Todo의 `TodoHistory`는 삭제하지 않는다.
- Todo 삭제에 DB `ON DELETE CASCADE`와 JPA remove cascade는 사용하지 않는다.

## Recurrence Rule

- 반복 설정은 `recurrence` JSON 컬럼 하나로 저장한다.
- Java 값 객체는 `RecurrenceRule`을 사용한다.
- 요일 약어는 RFC 5545 값을 따른다: `MO`, `TU`, `WE`, `TH`, `FR`, `SA`, `SU`
- `until`은 반복 종료일이다.
- `toRRuleText()`는 RFC 5545 형식의 RRULE 문자열을 만든다.
- 주/일 반복의 다음 날짜 계산은 ical4j를 사용한다.
- 월 반복의 말일 보정, 5주차 보정은 직접 계산한다.

지원 필드:

- `frequency`
- `interval`
- `byDay`
  - `offset`
  - `days`
- `byMonthDay`
- `until`
- `criteria`

지원 규칙:

- Daily: `{ "frequency": "DAILY", "interval": 1 }`
- Weekly: `{ "frequency": "WEEKLY", "interval": 1, "byDay": { "days": ["MO", "WE"] } }`
- Monthly 고정일: `{ "frequency": "MONTHLY", "interval": 1, "byMonthDay": 31 }`
- Monthly 특정 주차 특정 요일: `{ "frequency": "MONTHLY", "interval": 1, "byDay": { "offset": 2, "days": ["MO"] } }`
- `interval`은 1 이상 정수다.
- API에서 `byDay`와 `byMonthDay` 동시 사용은 허용하지 않는다.
- Java 값 객체인 `RecurrenceRule`은 순수 반복 규칙으로 `byDay`, `byMonthDay`를 모두 필드로 가지며, Todo 완료 기준은 `TodoRecurrence`에서 관리한다.
- `byDay.days`는 `MO`~`SU`만 허용한다.
- Weekly `byDay`는 `offset`을 사용할 수 없다.
- Monthly `byDay`는 `offset` 1~5와 `MO`~`SU` 조합만 허용한다.
- 하나의 Monthly 요일 반복은 특정 주차의 특정 요일 하나만 가질 수 있다. 예를 들어 2주차 월요일은 가능하지만, 2주차 월/화 또는 2주차 월과 3주차 화를 하나의 반복 규칙에 함께 담을 수 없다.
- Monthly `byMonthDay`는 1~31 사이 값만 허용한다.
- `criteria`는 다음 반복일 계산 기준이며 `SCHEDULED_DATE`, `COMPLETED_DATE`를 지원한다.
- `criteria`를 생략하면 `SCHEDULED_DATE`로 처리한다.

반복 기준:

- `SCHEDULED_DATE`는 현재 `scheduledDate`를 기준으로 다음 반복일을 계산한다.
- `COMPLETED_DATE`는 실제 완료일인 `completedAt.toLocalDate()`를 기준으로 다음 반복일을 계산한다.
- `scheduledDate`는 생성 시 첫 예정일이며, 완료 후에는 현재 또는 다음 회차 예정일로 갱신된다.
- `COMPLETED_DATE` 기준 반복은 `scheduledDate`가 오늘보다 미래이면 완료할 수 없다.
- `SCHEDULED_DATE` 기준 반복은 기존 동작을 유지하며 미래 `scheduledDate`도 완료할 수 있다.

월 반복 보정:

- `byMonthDay=31`에서 해당 월에 31일이 없으면 말일로 보정한다.
- 다음 월 계산 시 현재 `dayOfMonth`가 아니라 원래 `byMonthDay` 기준으로 계산해 드리프트가 발생하지 않도록 한다.
- `byDay={ "offset": 5, "days": ["FR"] }`에서 해당 월에 5번째 금요일이 없으면 마지막 금요일로 보정한다.

종료일 규칙:

- `nextDate()`는 다음 반복 후보가 없거나 종료일을 넘으면 `Optional.empty()`를 반환한다.
- 주/일 반복은 ical4j의 `getNextDate(...)` 결과가 `null`이면 반복 종료로 보고 `Optional.empty()`를 반환한다.
- 월 반복은 계산된 다음 후보 날짜가 `until` 이후면 `Optional.empty()`를 반환한다.

## Complete

- 일반 Todo 완료 시 `TodoHistory`를 생성하고 Todo 상태를 `DONE`으로 변경한다.
- 반복 Todo 완료 시 `TodoHistory`를 생성한다.
- Todo 완료 시 `completedAt`을 완료 시각으로 설정한다.
- 반복 Todo 완료 시 `nextDate()`가 값을 반환하면 `scheduledDate`를 그 날짜로 이동한다.
- 반복 Todo 완료 시 `nextDate()`가 `Optional.empty()`이면 `DONE`으로 변경한다.
- 다음 반복일이 있는 반복 Todo는 `status=TODO`를 유지하고 `completedAt`은 마지막 완료 시각으로 유지한다.
- 반복 Todo 완료 여부와 관계없이 Todo를 생성하지 않는다.
- Todo 완료 시 완료 API를 호출한 기준 Todo에 대해서 `TodoHistory`를 단건 생성한다.
- 이미 `DONE`인 Todo를 다시 완료할 수 없다.
- `undo`는 `DONE` 상태를 `TODO`로 복구하고 `completedAt`을 초기화한다.
- 이미 `TODO`인 Todo는 `undo`할 수 없다.

## SubTodos

- 일반 mainTodo를 완료하면 subTodo도 함께 `DONE`으로 변경한다.
- mainTodo 영구 완료로 subTodo도 함께 완료되면 subTodo의 `completedAt`도 같은 완료 시각으로 설정한다.
- 반복 mainTodo를 완료했을 때 다음 반복일이 있으면 mainTodo의 `scheduledDate`를 이동하고 subTodo는 모두 `TODO`로 초기화한다.
- 반복 mainTodo의 다음 반복일이 있어 subTodo를 `TODO`로 초기화하면 subTodo의 `completedAt`도 초기화한다.
- 반복 mainTodo를 완료했을 때 다음 반복일이 없으면 mainTodo와 subTodo를 함께 `DONE`으로 변경한다.
- mainTodo를 undo하면 mainTodo와 subTodo를 모두 `TODO`로 초기화한다.
- subTodo를 undo하면 해당 subTodo와 mainTodo를 `TODO`로 복구하고, 다른 subTodo 상태는 변경하지 않는다.
- 개별 하위 Todo의 반복 규칙은 별도로 독립적이다.
- mainTodo가 영구 완료되는 경우 subTodo의 반복 여부와 관계없이 함께 종료한다.

## Todo History

- `TodoHistory`는 완료 이력을 표현한다.
- `TodoHistory`는 `todoId`를 저장하고, 완료 당시 `title`을 별도로 저장한다.
- 완료 API를 호출한 Todo에 대해서 완료 이력을 남긴다.
- mainTodo 영구 완료로 subTodo가 함께 완료되어도 subTodo별 완료 이력은 별도로 저장하지 않는다.
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
