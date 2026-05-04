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
- Frontend:
  - Vue.js
  - Vite
  - Vue Router
  - Pinia

## Commands
- `./gradlew build`
- `./gradlew test`

## Source of Truth
- Database schema migrations: `src/main/resources/db/migration`
- Functional specification: `docs/features.md`
- Deployment: `docs/deployment.md`

## Architecture
- The project targets a monolithic architecture.
- Use package-by-domain structure.
- Must organize code primarily by domain and let each domain grow around its controller, service, repository, and domain model.
- Separate persistence interfaces into a dedicated `repository` package instead of placing them under `domain`.
- Keep dependencies one-directional where practical: controller -> service -> domain/repository.
  - Treat `domain` as the package for entities, value objects, enums, and domain behavior.

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

### SQL
- Write SQL and JPQL keywords such as `SELECT`, `FROM`, `WHERE`, `JOIN`, `ORDER BY` in uppercase.
```sql
SELECT COUNT(m), m.city 
FROM Member m 
GROUP BY m.city 
HAVING COUNT(m) > 5
```

## Implementation Guidance
- Follow the CQS (Command Query Separation) principle so commands change state without returning query data, and queries do not mutate state.
- Before modifying application code, explain the implementation options and trade-offs.  
  must wait for explicit user approval; questions, reviews, discussions, or objections are not requests to edit files.
- Use the defaults and built-in solutions recommended by the framework or library.
    ```java
    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByIdAndDeletedFalse(Long id); // unnecessary query method for a simple primary key lookup
    }
    ```
- When implementing or changing a feature, complete the implementation and tests in the same work.
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
  void getProductTest() {
      Long productId = 100L;
      Product product = new Product(productId, "MacBook Pro", 3000000);
      when(productRepository.findById(productId)).thenReturn(Optional.of(product));
      
      productService.getProduct(productId);
      
      verify(productRepository, times(1)).findById(productId);
  }  
  ```
  **Better:**
  ```java
  @ExtendWith(MockitoExtension.class)
  class ProductServiceTest {

      @Mock
      private ProductRepository productRepository;
  
      @Spy
      private DiscountPolicy discountPolicy;

      @InjectMocks
      private ProductService productService;

      @Test
      @DisplayName("등록된 상품을 조회한다")
      void getProduct_ReturnsProductDetails() {
          // given
          Long productId = 100L;
          Product product = new Product(productId, "MacBook Pro", 3000000);
          when(productRepository.findById(productId)).thenReturn(Optional.of(product));

          // when
          ProductResponse response = productService.getProduct(productId);

          // then
          assertThat(response.getId()).isEqualTo(product.getId());
          assertThat(response.getName()).isEqualTo(product.getName());
          assertThat(response.getPrice()).isEqualTo(product.getPrice());
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
    void successTest() {
        jobApplicant.pass();
        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.PASS);
    }
  
    @Test
    void failTest() {
        jobApplicant.fail();
        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.FAIL);
    }
  }
  ```
  **Better:**
  ```java
  class JobApplicantTest {

    @Test
    @DisplayName("지원자를 최종 합격시킨다")
    void should_pass_applicant() {
        JobApplicant jobApplicant = JobApplicantFixture.create(JobApplicantStatus.IN_PROGRESS);

        jobApplicant.pass();

        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.PASS);
    }

    @Test
    @DisplayName("지원서를 불합격시킨다")
    void should_fail_applicant() {
        JobApplicant jobApplicant = JobApplicantFixture.create(JobApplicantStatus.IN_PROGRESS);

        jobApplicant.fail();

        assertThat(jobApplicant.getStatus()).isEqualTo(JobApplicantStatus.FAIL);
    }

    @Test
    @DisplayName("지원서를 불합격 상태일 때 보관할 수 있다")
    void store_failed_applicant() {
        JobApplicant jobApplicant = JobApplicantFixture.create(JobApplicantStatus.FAIL);

        jobApplicant.putStorage();

        assertThat(jobApplicant.isStorage()).isTrue();
    }
  }

  class JobApplicantFixture {
    public static JobApplicant create(JobApplicantStatus status) {
        return create(status, "haru");
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

## Notes
- `src/main/resources/application.properties` is intended for local or deployment-specific runtime values and should remain ignored by Git.
- Do not commit secrets or production credentials to the repository.
