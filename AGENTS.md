# AGENTS.md

## Project Overview
- A full-stack Todo application.
- The initial implementation targets a web application. However, the system should be designed to support mobile app expansion.

## Tech Stack
- Backend:
  - Java 17
  - Spring Boot
  - Spring Data JPA
  - MySQL
- Frontend delivery:
  - Vue.js
  - Vite
  - Vue Router
  - Pinia

## Source of Truth
- Database schema migrations: `src/main/resources/db/migration`
- Functional specification: `docs/features.md`

## Architecture
- The project targets a monolithic architecture.
- Use package-by-domain structure.
- Must organize code primarily by domain and let each domain grow around its controller, service, repository, and domain model.
- Keep dependencies one-directional where practical: controller -> service -> repository/domain.

## Code Style

### Comment
- Add concise Korean comments to the signatures and core logic of business methods.

```java
/**
 * 쿠폰 적용
 * 쿠폰을 통해 최종 결제 금액을 계산함.
 * 최소 주문 금액을 만족하지 못하면 할인은 적용되지 않음.
 */
public int applyCoupon(int totalPrice, Coupon coupon) {
    if (totalPrice < coupon.getMinimumOrderAmount()) {
        return totalPrice;
    }

    // 할인 금액이 총 금액을 초과하면 결제 금액이 음수가 되는 것을 방지
    int discount = Math.min(coupon.getDiscountAmount(), totalPrice);

    return totalPrice - discount;
}
```

### Naming & Readability
- Use variable, method, and type names that follow the conventions of the language and ecosystem in use, stay consistent and describe the actual behavior being performed.
- If a new naming pattern could become a broader convention, align with the user before spreading it further.
- Write code so that the core flow reads naturally from top to bottom with minimal cognitive overhead.

```java
public void updateMember(MemberUpdateRequest request) {
    validateDuplicateNickname(nickname);
    Member member = findById(request.memberId());
    
    member.update(request.nickname(), request.email());
}
```

### Object Creation
- Choose the appropriate object creation approach based on the complexity of creation.

```java
UserDto dto = new UserDto("username");

User user = User.from("email@example.com", "username", 26);

Order order = Order.builder()
        .memberId(1L)
        .productId(100L)
        .quantity(10)
        .price(Money.won(15000))
        .status(OrderStatus.CREATED)
        .createdAt(Instant.now())
        .build();

Payment payment = PaymentFactory.create(card, amount, currency);
```
### Guard Clauses
- Prefer guard clauses to reduce unnecessary nesting and make exit conditions explicit.

```java
public void processOrder(Order order) {
    if (order == null || !order.isReady()) {
        return;
    }
    
    if (order.getPayment() == null) {
        throw new ApiException(NOT_FOUND_PAYMENT);
    }

    order.ship();
}
```

### Loop
- Use Stream when it makes the code more clear and readable.

```java
public List<String> getActiveMemberEmails(List<Member> members) {
    return members.stream()
            .filter(Member::isActive)
            .map(Member::getEmail)
            .toList();
}
```

### simplicity
- Choose the simplest approach possible. For example, follow the defaults and built-in solutions recommended by the framework or library.

**Bad:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<Member> findByIdAndDeletedFalse(Long id); // unnecessary query method for a simple primary key lookup
    List<User> findAllByOrderByIdAsc(); // use findAll()
}
```
**Good:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // custom
}
```

## Implementation Guidance
- Before modifying application code, first explain the planned changes and wait for user approval.
- When implementing or changing a feature, complete the implementation and tests in the same work.
- Do not treat implementation as complete until the tests pass.
- Before finalizing code changes, check for obvious mistakes, regressions, or missing updates.

## Testing
- Add unit tests for the service layer and domain logic.
- For controller behavior, request validation, and response mapping, add web tests such as `@WebMvcTest`.
- Tests must include the essential happy paths and only the important edge cases and failure cases.
- Use mocking only for external APIs, infrastructure boundaries, or cases where isolation is otherwise difficult or unnecessary complexity would be introduced.
- Avoid using reflection in tests unless there is no reasonable alternative.
- If tests fail, review the cause first and suggest production code refactoring only when it is necessary and justified.  


- Fundamentally, tests should verify the results (state changes or return values) rather than the implementation details.  
  **Bad:**
  ```java
  @Test
  @DisplayName("할인 정책을 적용한다")
  void calculateTotal_AppliesDiscountPolicy() { 
      // given
      Order order = new Order(10000);
      when(discountPolicy.getDiscountRate(order)).thenReturn(10);

      // when
      orderService.calculateTotal(order);

      // then
      verify(discountPolicy, times(1)).getDiscountRate(order);
  }  
  ```
  **Better:**
  ```java
    @ExtendWith(MockitoExtension.class)
    class OrderServiceTest {

    @Mock
    private DiscountPolicy discountPolicy;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문에 할인 정책이 적용되면 할인된 금액을 반환한다")
    void calculateTotal_ReturnsDiscountedPrice() {
        // given
        Order order = new Order(10000);
        when(discountPolicy.getDiscountRate(order)).thenReturn(10);

        // when
        int totalAmount = orderService.calculateTotal(order);

        // then
        assertThat(totalAmount).isEqualTo(9000); 
    }
  } 
  ```
- Write Descriptive and Meaningful Phrases.  
  **Bad:**
  ```java
  class JobApplicantTest {

    private JobApplicant jobApplicant;
    
    @BeforeEach
    void setUp() {
        String name = "haru";
        JobApplicantStatus status = JobApplicantStatus.IN_PROGRESS;
        jobApplicant = JobApplicant.create(name, status);
    }
    
    @Test
    @DisplayName("지원자를 최종 합격시킨다")
    void passApplicant() {
        // when
        jobApplicant.pass();

        // then
        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.PASS);
    }
  }
  ```
  **Better:**
  ```java
  class JobApplicantTest {

    @Test
    @DisplayName("지원자를 최종 합격시킨다")
    void passApplicant() {
        // given
        JobApplicant jobApplicant = JobApplicantFixture.create(JobApplicantStatus.IN_PROGRESS);

        // when
        jobApplicant.pass();

        // then
        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.PASS);
    }
  }

  class JobApplicantFixture {
      public static JobApplicant create(JobApplicantStatus status) {
          return JobApplicant.create("haru", status);
      }

      public static JobApplicant create(JobApplicantStatus status, String name) {
        return JobApplicant.create(name, status);
      }
  }
  ```

## Commit Convention
- Commit messages must follow the Conventional Commits format.
- Use types such as `feat`, `fix`, `refactor`, `test`, `docs`, and `chore` appropriately.
- Write the commit message description in Korean.

## Deployment Strategy
- Keep production deployment simple for the current project size.
- Use a single Spring Boot application instance unless concrete scaling needs appear.
- Package the application with Docker for consistent runtime and deployment.
- For AWS, start with one EC2 instance for the application.
- For production data, prefer AWS RDS MySQL over running MySQL in a container on the same EC2 instance.
- Pass runtime configuration through environment variables or deployment secrets, not committed property files.
- If HTTPS or custom domain setup is needed, place Nginx or another reverse proxy in front of the app on the EC2 instance.

## Deployment Boundaries
- Production:
  - Run the Spring Boot app as a Docker container.
  - Keep the database outside the app container and treat it as persistent infrastructure.
  - Avoid introducing distributed systems, multiple app nodes, or container orchestration before real traffic or operational needs justify them.

## Notes
- `src/main/resources/application.properties` is intended for local or deployment-specific runtime values and should remain ignored by Git.
- Do not commit secrets or production credentials to the repository.
