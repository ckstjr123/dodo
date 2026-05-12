# Features

## Todo

- Todo API는 `POST /api/v1/todos`, `GET /api/v1/todos`, `GET /api/v1/todos/{todoId}`를 제공한다.
- Todo 수정은 `PATCH /api/v1/todos/{todoId}`를 사용한다.
- 완료 처리는 `PATCH /api/v1/todos/{todoId}/complete`, 완료 취소는 `PATCH /api/v1/todos/{todoId}/undo`를 사용한다.
- 삭제 처리는 `DELETE /api/v1/todos/{todoId}`를 사용한다.
- 완료 이력은 `GET /api/v1/todos/histories`로 조회한다.
- Todo 상태는 `TODO`, `DONE`만 사용한다.
- 메인 목록은 `TODO` 상태의 루트 Todo만 조회한다.
- Todo 목록 응답에는 알림 상세와 알림 아이콘 표시용 필드를 포함하지 않는다.
- Todo 상세 응답에는 현재 Todo에 설정된 알림 상세를 포함한다.
- 하위 Todo도 알림을 가질 수 있지만, 부모 Todo 상세 응답의 하위 Todo 목록에는 하위 Todo 알림 상세를 포함하지 않는다.
- 태그 기능과 priority 기능은 사용하지 않는다.
- 응답 DTO의 식별자 필드는 엔티티명 기반 형식(`todoId`, `memberId`, `historyId`, `reminderId`)으로 통일한다.

## Category

- Category API는 `POST /api/v1/categories`, `GET /api/v1/categories`, `PATCH /api/v1/categories/{categoryId}`, `DELETE /api/v1/categories/{categoryId}`를 제공한다.
- 생성 요청 필드는 `name`이며 필수 값이고 최대 길이는 100자다.
- 생성 시 현재 회원에게 동일한 이름의 카테고리가 이미 있으면 새로 생성하지 않고 기존 `categoryId`를 반환한다.
- 목록 조회는 현재 회원이 소유한 카테고리만 반환하고, `createdAt ASC`, `id ASC` 순서로 정렬한다.
- 수정/삭제 대상 카테고리는 현재 회원이 소유한 카테고리여야 한다.
- Todo가 연결된 카테고리는 삭제할 수 없고 `CATEGORY_IN_USE` 오류를 반환한다.
- 카테고리는 물리 삭제하며 DB unique 제약은 사용하지 않는다.

## Todo Create

- 생성 요청 필드: `categoryId`, `parentTodoId`, `title`, `memo`, `sortOrder`, `dueAt`, `scheduledDate`, `scheduledTime`, `recurrence`, `reminders`
- `categoryId`, `title`은 필수다.
- `title` 최대 길이는 200자다.
- `memo` 최대 길이는 1000자다.
- `parentTodoId`는 선택 값이다.
- `parentTodoId`는 현재 회원이 소유한 루트 Todo만 허용한다.
- 하위 Todo 아래에 다시 하위 Todo를 생성할 수 없다.
- `recurrence`가 없는 Todo는 `scheduledDate`, `scheduledTime`이 선택 값이다.
- `recurrence`가 있는 Todo는 `scheduledDate`가 필수다.
- 생성 시 초기 알림을 함께 등록할 수 있다.
- 생성 후 알림 추가/수정/삭제는 별도 Reminder API를 사용한다.

## Todo Update

- 수정 요청 필드: `categoryId`, `title`, `memo`, `sortOrder`, `dueAt`, `scheduledDate`, `scheduledTime`, `recurrence`
- Todo 수정 API는 알림 배열을 받지 않는다.
- `parentTodoId`와 `subTodos`는 수정 요청 필드에 포함하지 않는다.
- `status`, `completedAt`은 완료/취소 API에서만 변경한다.
- `TodoHistory`는 완료 시점의 이력 snapshot이므로 수정 API에서 변경하지 않는다.
- 수정 대상 Todo는 현재 회원이 소유한 Todo여야 한다.
- `categoryId`는 현재 회원이 소유한 카테고리만 허용한다.
- `categoryId`, `title`은 필수다.
- `title` 최대 길이는 200자다.
- `memo` 최대 길이는 1000자다.
- `recurrence`가 없는 Todo는 `scheduledDate`, `scheduledTime`이 선택 값이다.
- `recurrence`가 있는 Todo는 `scheduledDate`가 필수다.
- `recurrence=null`이면 반복 설정을 제거한다.
- `scheduledDate`를 제거하면 `scheduledTime`, `recurrence`, 연결된 알림을 함께 무효화한다.
- `scheduledTime`을 제거하면 연결된 알림을 무효화한다.
- `scheduledDate` 또는 `scheduledTime`이 변경되고 둘 다 존재하면 기존 알림의 `minuteOffset`을 유지한 채 `remindAt`을 재계산한다.

## Todo Delete

- 삭제 API는 `DELETE /api/v1/todos/{todoId}`를 사용한다.
- 삭제는 물리 삭제로 처리하며 복구 기능은 제공하지 않는다.
- 삭제 대상 Todo는 현재 회원이 소유한 Todo여야 한다.
- 완료 여부와 반복 여부와 관계없이 삭제할 수 있다.
- 메인 Todo를 삭제하면 하위 Todo 알림을 먼저 삭제하고, 하위 Todo를 삭제한 뒤, 메인 Todo 알림과 메인 Todo를 삭제한다.
- 하위 Todo를 직접 삭제하면 해당 하위 Todo의 알림을 먼저 삭제하고 해당 하위 Todo만 삭제한다.
- 삭제된 Todo의 `TodoHistory`는 삭제하지 않는다.
- Todo 삭제에는 DB `ON DELETE CASCADE`와 JPA remove cascade를 사용하지 않는다.

## Recurrence Rule

- 반복 설정은 `recurrence` JSON 컬럼 하나로 저장한다.
- Java 값 객체는 `RecurrenceRule`을 사용한다.
- 요일 약어는 RFC 5545 값을 따른다: `MO`, `TU`, `WE`, `TH`, `FR`, `SA`, `SU`
- `until`은 반복 종료일이다.
- `toRRuleText()`는 RFC 5545 형식의 RRULE 문자열을 만든다.
- 주간 반복의 다음 날짜 계산은 ical4j를 사용한다.
- 월간 말일 보정과 5주차 보정은 직접 계산한다.

지원 필드:

- `frequency`
- `interval`
- `byDay`
- `byMonthDay`
- `until`
- `criteria`

지원 규칙:

- Daily: `{ "frequency": "DAILY", "interval": 1 }`
- Weekly: `{ "frequency": "WEEKLY", "interval": 1, "byDay": { "days": ["MO", "WE"] } }`
- Monthly 고정일: `{ "frequency": "MONTHLY", "interval": 1, "byMonthDay": 31 }`
- Monthly 특정 주차 특정 요일: `{ "frequency": "MONTHLY", "interval": 1, "byDay": { "offset": 2, "days": ["MO"] } }`
- `interval`은 1 이상의 정수다.
- API에서 `byDay`와 `byMonthDay` 동시 사용은 허용하지 않는다.
- `byDay.days`는 `MO`~`SU`만 허용한다.
- Weekly `byDay`는 `offset`을 사용할 수 없다.
- Monthly `byDay`는 `offset` 1~5와 `MO`~`SU` 조합만 허용한다.
- Monthly `byMonthDay`는 1~31 사이 값만 허용한다.
- `criteria`는 다음 반복일 계산 기준이며 `SCHEDULED_DATE`, `COMPLETED_DATE`를 지원한다.
- `criteria`를 생략하면 `SCHEDULED_DATE`로 처리한다.

반복 기준:

- `SCHEDULED_DATE`는 현재 `scheduledDate`를 기준으로 다음 반복일을 계산한다.
- `COMPLETED_DATE`는 실제 완료일인 `completedAt.toLocalDate()`를 기준으로 다음 반복일을 계산한다.
- `COMPLETED_DATE` 기준 반복은 `scheduledDate`가 오늘보다 미래이면 완료할 수 없다.
- `SCHEDULED_DATE` 기준 반복은 기존 동작을 유지하며 미래 `scheduledDate`도 완료할 수 있다.

월 반복 보정:

- `byMonthDay=31`에서 해당 월에 31일이 없으면 말일로 보정한다.
- 다음 월 계산 시 현재 `dayOfMonth`가 아니라 원래 `byMonthDay` 기준으로 계산해 드리프트를 방지한다.
- `byDay={ "offset": 5, "days": ["FR"] }`에서 해당 월에 5번째 금요일이 없으면 마지막 금요일로 보정한다.

종료일 규칙:

- `nextDate()`는 다음 반복 후보가 없거나 종료일을 넘으면 `Optional.empty()`를 반환한다.
- 주간 반복은 ical4j의 `getNextDate(...)` 결과가 `null`이면 반복 종료로 보고 `Optional.empty()`를 반환한다.
- 월 반복은 계산한 다음 후보 날짜가 `until` 이후이면 `Optional.empty()`를 반환한다.

## Complete

- 일반 Todo 완료 시 `TodoHistory`를 생성하고 Todo 상태를 `DONE`으로 변경한다.
- 반복 Todo 완료 시 `TodoHistory`를 생성한다.
- Todo 완료 시 `completedAt`을 완료 시각으로 설정한다.
- 반복 Todo 완료 시 `nextDate()`가 값을 반환하면 `scheduledDate`를 그 날짜로 이동하고 상태는 `TODO`를 유지한다.
- 반복 Todo 완료 시 `nextDate()`가 `Optional.empty()`이면 `DONE`으로 변경한다.
- 다음 반복일이 있는 반복 Todo는 `status=TODO`를 유지하고 `completedAt`은 마지막 완료 시각으로 유지한다.
- Todo 완료 여부와 관계없이 Todo를 생성하지 않는다.
- 이미 `DONE`인 Todo를 다시 완료할 수 없다.
- `undo`는 `DONE` 상태를 `TODO`로 복구하고 `completedAt`을 초기화한다.
- 이미 `TODO`인 Todo는 `undo`할 수 없다.
- 완료/undo 시 알림 설정은 삭제하지 않는다.
- 완료 상태 Todo의 알림은 향후 발송 후보에서 제외한다.
- undo하면 기존 알림 설정은 그대로 유지된다.
- Todo 알림은 별도 발송 이력 테이블을 두지 않고 `reminder` 테이블 하나로 관리한다.
- 실제 발송 기능을 추가할 때도 `reminder`는 사용자의 알림 설정과 현재 회차 발송 상태를 함께 보유한다.
- 반복 Todo가 다음 회차로 이동하거나 일정이 변경되면 기존 `reminder` row를 유지하고 `remindAt`을 재계산한다.
- 향후 발송 상태가 필요해지면 `sentAt` 같은 현재 회차 발송 상태 컬럼을 `reminder`에 추가하고, 다음 회차 재계산 시 초기화한다.

## SubTodos

- 일반 mainTodo를 완료하면 subTodo도 함께 `DONE`으로 변경한다.
- mainTodo 영구 완료로 subTodo도 함께 완료되면 subTodo의 `completedAt`도 같은 완료 시각으로 설정한다.
- 반복 mainTodo를 완료했을 때 다음 반복일이 있으면 mainTodo의 `scheduledDate`를 이동하고 subTodo를 모두 `TODO`로 초기화한다.
- 반복 mainTodo의 다음 반복일이 있어 subTodo를 `TODO`로 초기화하면 subTodo의 `completedAt`도 초기화한다.
- 반복 mainTodo를 완료했을 때 다음 반복일이 없으면 mainTodo와 subTodo를 함께 `DONE`으로 변경한다.
- mainTodo를 undo하면 mainTodo와 subTodo를 모두 `TODO`로 초기화한다.
- subTodo를 undo하면 해당 subTodo와 mainTodo를 `TODO`로 복구하고, 다른 subTodo 상태는 변경하지 않는다.
- 개별 하위 Todo의 반복 규칙은 독립적이다.
- mainTodo가 영구 완료되는 경우 subTodo는 반복 여부와 관계없이 함께 종료된다.

## Todo History

- `TodoHistory`는 완료 이력을 표현한다.
- `TodoHistory`는 `todoId`를 저장하고, 완료 당시 `title`을 별도로 저장한다.
- 완료 API를 호출한 Todo에 대해서만 완료 이력을 생성한다.
- mainTodo 영구 완료로 subTodo가 함께 완료되어도 subTodo별 완료 이력은 별도로 저장하지 않는다.
- history는 반복 여부를 구분하지 않는다.
- history 조회는 최대 30건씩 조회한다.
- history 조회는 커서 기반 페이지네이션을 사용한다.
- 정렬 기준은 `completedAt DESC`, `id DESC`다.
- 커서는 `completedAt`, `id` 쌍으로 사용한다.
- 참조 Todo가 삭제되어도 history 자체는 조회할 수 있다.
- history에서 연결된 Todo가 필요하면 `todoId`로 기존 Todo 단건 조회 API를 사용한다.
- `ON DELETE CASCADE`는 사용하지 않는다.

## Reminder

- Reminder API는 `POST /api/v1/todos/{todoId}/reminders`, `PATCH /api/v1/todos/{todoId}/reminders/{reminderId}`, `DELETE /api/v1/todos/{todoId}/reminders/{reminderId}`를 제공한다.
- 전체 삭제 API는 제공하지 않는다.
- Todo 생성 시 초기 알림은 `reminders` 배열로 함께 생성할 수 있다.
- Todo 수정 API에서는 알림 배열을 받지 않고, 알림 생성/수정/삭제는 Reminder API에서 관리한다.
- 알림은 `scheduledDate`와 `scheduledTime`이 모두 있는 Todo에만 설정할 수 있다.
- 알림 요청이 있는데 `scheduledDate` 또는 `scheduledTime` 중 하나라도 없으면 400 응답을 반환한다.
- Todo 하나당 알림은 최대 5개까지 허용한다.
- `minuteOffset`은 일정 시각 기준 몇 분 전 알림인지 나타내는 0 이상의 정수다.
- `minuteOffset`은 요청에서 반드시 전달해야 하며 `null`은 허용하지 않는다.
- 동일 Todo 안에서 같은 `minuteOffset`을 가진 알림은 중복 등록할 수 없다.
- 서버는 UI 옵션명이나 라벨을 저장하지 않고 `minuteOffset`과 계산된 `remindAt`만 저장한다.
- 저장 시 `remindAt = scheduledDate + scheduledTime - minuteOffset`으로 계산한다.
- 계산된 `remindAt`이 현재보다 과거여도 예외를 던지지 않고 저장한다.
- 응답에는 `reminderId`, `minuteOffset`, `remindAt`을 포함한다.
- 응답에는 `expired` 필드를 두지 않는다.
- 알림 수정은 기존 reminder row를 유지하고 `minuteOffset`과 `remindAt`을 갱신한다.
- 알림 수정 시 `minuteOffset`이 변경된 경우에만 동일 Todo 내 중복 여부를 검증한다.
- Todo 일정이 변경되고 `scheduledDate`, `scheduledTime`이 모두 존재하면 기존 알림의 `minuteOffset`을 유지한 채 `remindAt`을 재계산한다.
- `scheduledDate`가 제거되면 `scheduledTime`, `recurrence`, 연결된 알림을 함께 무효화한다.
- `scheduledTime`이 제거되면 연결된 알림을 무효화한다.
- Todo 완료/undo 시 알림 설정은 삭제하지 않는다.
- 반복 Todo 완료 후 다음 회차가 있으면 이동된 `scheduledDate`와 기존 `scheduledTime` 기준으로 알림의 `remindAt`을 재계산한다.
- 반복 Todo의 다음 회차가 없어 Todo가 `DONE`이 되면 알림 설정은 유지하되 향후 발송 후보에서는 제외한다.
- Todo 삭제 시 연결 알림을 먼저 삭제한 뒤 Todo를 삭제한다.
- 메인 Todo 삭제 시 하위 Todo를 벌크 삭제하기 전에 하위 Todo 알림을 먼저 삭제한다.
- Todo 알림은 공지/시스템 알림처럼 별도 이력 보존 대상이 아니므로, 발송 이력 테이블을 따로 두지 않는다.
- 향후 실제 발송 기능을 구현하면 `reminder`에 현재 회차 발송 여부를 나타내는 컬럼을 추가해 중복 발송을 방지한다.
- 반복 Todo 다음 회차 이동 또는 일정 변경 시에는 같은 `reminder` row의 `remindAt`을 갱신하고 현재 회차 발송 상태를 초기화한다.
- 목록 조회는 기존 subTodo fetch join 구조를 유지하고 알림을 조회하지 않는다.
- Todo 목록의 알림 아이콘 표시용 필드 또는 `hasReminders` 제공은 후속 과제로 보류한다.
- 상세 조회는 현재 Todo의 알림 상세만 포함한다.
- 알림 컬렉션은 fetch join하지 않고 지연 로딩과 batch loading으로 조회한다.
- 실제 FCM 발송, 디바이스 토큰, 중복 발송 방지용 `sentAt` 또는 발송 이력 테이블은 후속 설계 범위다.
