# Features

## Authentication
- 인증은 JWT access token과 refresh token 조합으로 처리한다.
- 현재 제공하는 인증 API는 `POST /api/v1/auth/social/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`이다.
- 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더로 access token을 전달한다.
- 회원은 현재 `email`만 저장한다.

### Google Social Login
- 클라이언트는 Google 로그인 후 받은 `authorizationCode`와 `redirectUri`를 `POST /api/v1/auth/social/login`으로 전달한다.
- 요청 본문은 `provider`, `authorizationCode`, `redirectUri`를 포함한다.
- 현재 지원하는 소셜 로그인 제공자는 `GOOGLE`이다.
- 백엔드는 Google token endpoint에서 authorization code를 교환하고, userinfo endpoint에서 사용자 정보를 조회한다.
- Google 응답에 provider user id가 없으면 로그인에 실패한다.
- Google 응답에 email이 없으면 로그인에 실패한다.
- Google 응답의 email verification이 거짓이면 로그인에 실패한다.
- 이미 가입된 이메일이면 기존 회원으로 로그인한다.
- 처음 로그인한 이메일이면 회원을 자동 생성한다.
- 로그인 성공 시 자체 발급한 access token과 refresh token을 반환한다.

### Refresh Token
- refresh token은 `refresh_token` 테이블에 저장한다.
- refresh token 재발급 전 JWT 형식과 만료 여부를 검사한다.
- 저장된 refresh token이 없거나 만료되었으면 재발급에 실패한다.
- refresh token 재발급 시 access token과 refresh token을 모두 새로 발급한다.
- 기존 refresh token row를 회전 방식으로 갱신한다.
- 한 회원은 최대 2개의 refresh token 세션만 유지한다.
- 세션이 2개를 초과하면 최근 사용 순서 기준으로 오래된 세션부터 제거한다.

### Current Member
- `GET /api/v1/auth/me`는 현재 로그인한 회원의 `id`, `email`을 반환한다.

## Todo
- 현재 제공하는 Todo API는 `POST /api/v1/todos`, `GET /api/v1/todos`, `GET /api/v1/todos/{todoId}`이다.
- Todo는 Aggregate Root이다.
- Todo는 `title`, `memo`, `status`, `priority`, `sortOrder`, `dueAt`을 가진다.
- Todo 생성 시 상태는 항상 `OPEN`으로 시작한다.
- Todo는 정확히 하나의 Category에 속한다.
- Todo는 여러 Tag와 연결될 수 있다.
- Todo는 여러 Checklist 항목을 가질 수 있다.
- Todo는 최대 하나의 Repeat 규칙을 가질 수 있다.
- Todo는 여러 Reminder 규칙을 가질 수 있다.

### Todo Create
- Todo 생성 요청은 `categoryId`, `title`, `priority`가 필수이다.
- `title` 최대 길이는 200자이다.
- `memo` 최대 길이는 1000자이다.
- `priority` 최대 길이는 20자이다.
- `sortOrder`, `dueAt`, `tagIds`, `checklists`, `repeat`, `reminders`는 선택이다.
- `tagIds`는 중복이 들어와도 중복 없이 처리한다.
- `categoryId`는 현재 회원이 소유한 Category여야 한다.
- `tagIds`의 모든 Tag는 현재 회원이 소유한 Tag여야 한다.
- 존재하지 않거나 현재 회원 소유가 아닌 Category는 `CATEGORY_NOT_FOUND`로 처리한다.
- 존재하지 않거나 현재 회원 소유가 아닌 Tag는 `TAG_NOT_FOUND`로 처리한다.

### Todo Read
- `GET /api/v1/todos`는 현재 회원이 소유한 Todo만 반환한다.
- `GET /api/v1/todos/{todoId}`는 현재 회원이 소유한 Todo만 반환한다.
- Todo가 없거나 현재 회원 소유가 아니면 `TODO_NOT_FOUND`로 처리한다.

## Category
- Category는 회원 소유 리소스이다.
- Todo 생성 시 Category 자동 생성은 지원하지 않는다.
- Todo 생성 시 Category 소유권 검증은 서비스 계층에서 처리한다.

## Tag
- Tag는 회원 소유 리소스이다.
- Todo와 Tag는 직접 `ManyToMany`로 연결하지 않고 `todo_tag` 매핑 엔티티로 연결한다.
- Todo 생성 시 Tag 자동 생성은 지원하지 않는다.
- Todo 생성 시 Tag 소유권 검증은 서비스 계층에서 처리한다.

## Checklist
- Checklist는 Todo 하위 엔티티이다.
- Checklist 항목은 `content`, `completed` 상태를 가진다.
- `content`는 비어 있을 수 없다.
- `content` 최대 길이는 255자이다.
- Checklist 완료 여부는 boolean 값으로만 관리한다.
- 완료 시각은 저장하지 않는다.

## Repeat
- Repeat는 Todo 하위 엔티티이며 DB에서는 `todo_repeat` 테이블로 분리한다.
- 한 Todo는 최대 하나의 Repeat 규칙만 가진다.
- 현재 지원하는 반복 타입은 `DAILY`, `WEEKLY`이다.
- `repeat_interval`은 1 이상이어야 한다.

### Daily Repeat
- `DAILY`는 `repeat_interval`만 사용한다.
- `days_of_week_json`은 비어 있어야 한다.

### Weekly Repeat
- `WEEKLY`는 `repeat_interval`과 `daysOfWeek`를 함께 사용한다.
- `daysOfWeek`는 최소 하루 이상 필요하다.
- 요일 목록은 null을 제거하고 정렬된 집합으로 저장한다.
- DB에는 `days_of_week_json` 컬럼에 JSON 배열로 저장한다.

## Reminder
- Reminder는 Todo 하위 엔티티이며 DB에서는 `reminder` 테이블로 분리한다.
- 한 Todo는 여러 Reminder를 가질 수 있다.
- 현재 지원하는 Reminder 타입은 `RELATIVE_TO_DUE`, `ABSOLUTE_AT`이다.

### Relative Reminder
- `RELATIVE_TO_DUE`는 Todo의 `dueAt`이 있어야 한다.
- `remindBefore` 값이 필요하다.
- 같은 Todo 안에서 같은 `remindBefore` 값은 중복 등록할 수 없다.
- `dueAt`이 없으면 `DUE_AT_REQUIRED`로 처리한다.
- `remindBefore`가 없으면 `REMIND_BEFORE_REQUIRED`로 처리한다.
- 중복이면 `DUPLICATE_REMINDER`로 처리한다.

### Absolute Reminder
- `ABSOLUTE_AT`는 `remindAt` 값이 필요하다.
- 같은 Todo 안에서 같은 `remindAt` 값은 중복 등록할 수 없다.
- `remindAt`이 없으면 `REMIND_AT_REQUIRED`로 처리한다.
- 중복이면 `DUPLICATE_REMINDER`로 처리한다.

## Response Shape
- Todo 응답은 Category 정보, Tag 목록, Checklist 목록, Repeat 정보, Reminder 목록을 함께 반환한다.
- Checklist 응답에는 `id`, `content`, `completed`를 포함한다.
- Checklist 응답에는 완료 시각을 포함하지 않는다.

## Current Scope
- Todo 수정 API는 아직 제공하지 않는다.
- Todo 삭제 API는 아직 제공하지 않는다.
- Category 생성/조회 API는 아직 제공하지 않는다.
- Tag 생성/조회 API는 아직 제공하지 않는다.
- 실제 알림 발송 기능은 아직 구현하지 않았다.
