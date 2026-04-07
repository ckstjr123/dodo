# Features

## Authentication
- 인증은 JWT access token과 refresh token 조합으로 처리한다.
- 로컬 회원가입과 로컬 로그인은 제거된 상태다.
- 회원은 현재 `email`만 보유하고, 소셜 로그인 계정 식별의 기준으로 사용한다.
- 인증이 필요한 요청은 `Authorization: Bearer {accessToken}` 헤더로 access token을 전달한다.
- 현재 제공하는 인증 API는 `POST /api/v1/auth/social/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/auth/me`다.
- `GET /api/v1/auth/me`는 현재 로그인한 회원의 `id`, `email`을 반환한다.

### Google Social Login
- 프론트엔드는 Google 로그인 후 받은 `authorizationCode`를 `POST /api/v1/auth/social/login`으로 전달한다.
- 요청 본문에는 `provider`, `authorizationCode`, `redirectUri`를 포함한다.
- 백엔드는 Google token endpoint에서 authorization code를 검증하고, userinfo endpoint에서 사용자 정보를 조회한다.
- Google 인증이 성공하면 Google 토큰을 그대로 반환하지 않고, 우리 서버가 새 access token과 refresh token을 발급해 JSON으로 응답한다.
- Google에서 전달받은 이메일이 없거나, 이메일 인증이 되어 있지 않으면 로그인에 실패한다.
- 현재는 별도 소셜 식별 테이블 없이 이메일 기준으로 기존 회원을 재사용한다.
- 처음 로그인한 이메일이면 회원을 자동 생성한다.

### Refresh Token
- refresh token은 `refresh_token` 테이블에 저장하고 서버가 유효성을 직접 검증한다.
- refresh token에는 `jti`를 포함해 같은 회원에게 연속 발급해도 서로 다른 JWT가 만들어지도록 한다.
- refresh token 재발급 시 기존 세션 row를 유지한 채 token, `expired_at`, `updated_at`만 갱신한다.
- 회원당 최대 2개의 refresh token 세션만 유지한다.
- 세션 수가 2개를 초과하면 `updated_at` 기준으로 가장 오래 사용되지 않은 refresh token부터 삭제한다.
- `refresh_token.token`에는 인덱스만 두고, unique 제약은 두지 않는다.

## Todo
- `todo`는 기본 작업 단위다.
- Todo는 Todo Aggregate의 루트다.
- Todo는 제목과 상태, 우선순위, 정렬 순서를 가진다.
- Todo 상태는 현재 `OPEN`, `DONE`을 사용한다.
- Todo는 간단한 설명을 위한 `memo`를 가진다.
- Todo는 시간까지 포함하는 마감 시각 `due_at`을 가질 수 있다.
- Todo 생성 시 체크리스트, 반복 설정, 알림, 태그 연결을 함께 요청할 수 있다.
- Todo 생성 요청에서 `tagIds`, `checklists`, `repeat`, `reminders`는 선택 필드이며, 생략하면 해당 하위 요소를 만들지 않는다.

## Repeat
- 반복 설정은 Todo Aggregate의 하위 요소이며, DB에서는 `todo_repeat` 테이블로 분리해 저장한다.
- 반복이 없는 Todo는 `todo_repeat` 레코드가 없다.
- 하나의 Todo는 최대 하나의 반복 규칙만 가진다.
- 공통 필드는 `repeat_type`, `repeat_interval`이다.
- 현재 지원하는 반복 타입은 `DAILY`, `WEEKLY`다.
- `DAILY`는 `repeat_interval`만 사용한다.
- `WEEKLY`는 `repeat_interval`과 `days_of_week_json`을 함께 사용한다.
- `days_of_week_json`은 DB에는 JSON 배열로 저장하고, 애플리케이션에서는 `Set<DayOfWeek>`로 다룬다.
- `DAILY`는 `days_of_week_json`이 비어 있어야 한다.
- `WEEKLY`는 최소 하나 이상의 요일이 필요하다.
- `repeat_interval`은 1 이상이어야 한다.

## Reminder
- 알림 설정은 Todo Aggregate의 하위 요소이며, DB에서는 `reminder` 테이블로 분리해 저장한다.
- 하나의 Todo는 여러 개의 알림을 가질 수 있다.
- 알림 타입은 `RELATIVE_TO_DUE`, `ABSOLUTE_AT` 두 가지다.
- 알림 타입별 필수값 검증과 Todo 등록 규칙은 알림 정책으로 분리해 적용한다.
- `RELATIVE_TO_DUE`는 `todo.due_at` 기준의 상대 알림이다.
- `RELATIVE_TO_DUE`는 분 단위 정수 필드 `remind_before`를 사용한다.
- `ABSOLUTE_AT`는 특정 시각을 직접 지정하는 알림이며 `remind_at`을 사용한다.
- `RELATIVE_TO_DUE`는 `remind_before >= 1`이어야 한다.
- `ABSOLUTE_AT`는 `remind_at`이 필요하다.
- 동일 Todo 안에서는 같은 `RELATIVE_TO_DUE` `remind_before` 또는 같은 `ABSOLUTE_AT` `remind_at`을 가진 알림을 중복 생성할 수 없다.
- 알림이 필요할 때만 `reminder`를 생성하고, 해제는 삭제로 처리한다.
- 반복 Todo에 대한 자동 알림 생성과 발송은 현재 범위에 포함하지 않는다.

## Checklist
- `checklist`는 Todo Aggregate의 하위 요소이며, DB에서는 `checklist` 테이블로 분리해 저장한다.
- 체크리스트 항목은 완료 처리할 수 있다.
- 체크리스트 항목은 삭제할 수 있다.

## Category
- `category`는 Todo의 큰 분류다.
- Todo는 정확히 하나의 `category`를 가진다.
- 카테고리는 회원 소유 데이터이며 다른 회원과 공유하지 않는다.
- Todo 생성/수정 시 선택한 `category`의 소유 회원과 현재 회원이 일치해야 한다.

## Tag
- `tag`는 세부 분류와 검색 보조를 위한 데이터다.
- 태그는 회원 소유 데이터이며 다른 회원과 공유하지 않는다.
- 하나의 Todo는 여러 태그를 가질 수 있다.
- `todo_tag`는 Todo와 Tag의 연결을 나타내는 매핑 테이블이다.
- Todo에 태그를 연결할 때 선택한 `tag`의 소유 회원과 Todo의 소유 회원이 일치해야 한다.

## Modeling Notes
- 현재 JPA 엔티티는 package-by-domain 구조를 따른다.
- 연관관계는 필요한 곳에만 둔다.
- Todo Aggregate 내부의 반복 설정, 알림, 체크리스트, Todo-Tag 연결은 루트인 Todo가 소유하고 생명주기를 관리한다.
- Todo Aggregate 내부 하위 엔티티는 Cascade와 orphanRemoval을 적용해 Todo를 통해 생성, 수정, 삭제되도록 한다.
- `todo`는 반복 설정, 알림, 체크리스트, `todo_tag`에 대한 연관관계를 가진다.
- `todo`와 `tag`는 직접 `ManyToMany`로 연결하지 않고 `todo_tag` 매핑 엔티티를 통해 연결한다.
- `member`, `category`, `tag`는 Todo Aggregate 외부 참조이며 Todo에서 생명주기를 관리하지 않는다.
- 회원 소유권 무결성은 복합 FK 대신 서비스 계층 검증으로 보장한다.
- `created_at`, `updated_at`이 모두 필요한 엔티티는 Spring Data JPA Auditing 기반 공통 엔티티를 사용한다.
- `created_at`만 필요한 엔티티는 각 엔티티에서 `@CreatedDate`로 관리한다.

## Planned Features

### Notifications
- 푸시 알림 발송 기능은 아직 구현하지 않았다.
- 현재는 Todo 알림 규칙 모델만 정의되어 있다.
- 실제 알림 발송 이력과 채널 관리는 이후 `notification`, `notification_delivery`, `notification_preference`, `push_subscription` 등의 모델로 확장할 수 있다.
