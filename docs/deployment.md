# AWS Docker Deployment

이 문서는 FE 없이 Spring Boot API 서버만 EC2 + Docker + RDS MySQL로 1차 배포하는 절차를 설명합니다.

## 1. 사전 준비

- AWS에서 RDS MySQL을 생성합니다.
- AWS에서 EC2 인스턴스를 생성합니다.
- EC2 보안그룹은 SSH 22번 포트를 본인 IP에만 열고, API 확인용으로 8080번 포트를 필요한 범위에만 엽니다.
- RDS 보안그룹은 MySQL 3306번 포트를 EC2 보안그룹에서만 접근 가능하게 엽니다.
- Google OAuth client secret, JWT secret, RDS 비밀번호는 Git에 커밋하지 않습니다.

현재 로컬 `src/main/resources/application.properties`에 운영성 secret이 있다면 이미 노출된 값으로 보고 교체하세요.

## 2. EC2 Docker 설치

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin git
sudo usermod -aG docker ubuntu
newgrp docker
```

Amazon Linux 계열을 사용한다면 패키지 명령만 배포판에 맞게 바꾸면 됩니다.

## 3. 프로젝트 배포

```bash
git clone <repository-url>
cd todo
cp .env.example .env
vi .env
```

`.env`에는 실제 운영 값을 입력합니다.

```dotenv
SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/dodo?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-password>
JWT_SECRET=<32-bytes-or-longer-secret>
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=1800
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=604800
```

컨테이너를 빌드하고 실행합니다.

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker logs -f dodo-todo-api
```

## 4. 배포 확인

```bash
docker ps
curl -i http://<ec2-public-ip>:8080/swagger-ui/index.html
curl -i http://<ec2-public-ip>:8080/v3/api-docs
```

보호된 API는 JWT 없이 호출하면 401을 반환해야 합니다. 애플리케이션 로그에서 Flyway migration 성공과 RDS 연결 성공도 확인합니다.

## 5. 운영 후속 작업

- 도메인을 연결합니다.
- Nginx reverse proxy를 앞단에 둡니다.
- HTTPS 인증서를 적용합니다.
- Google OAuth redirect URI를 실제 운영 도메인으로 추가합니다.
- 수동 배포가 안정화되면 GitHub Actions 또는 ECR 기반 배포로 전환합니다.
