---
inclusion: always
---
# Testing Guidelines - AgenticDriverAcademy

## Core Principles
- Tests MUST be deterministic (same input = same output)
- Tests MUST be isolated (no shared mutable state)
- Tests MUST verify behavior, not implementation details
- No flaky tests allowed in CI

## Java Backend Testing

### Unit Tests (JUnit 5 + Mockito)
```java
// Pattern: shouldExpectedBehavior_whenCondition
@Test
void shouldReturnLesson_whenLessonExists() {
    // Arrange
    when(repository.findById(lessonId)).thenReturn(Optional.of(lesson));
    
    // Act
    var result = service.findById(lessonId);
    
    // Assert
    assertThat(result).isPresent();
}
```

**DO:**
- Mock external dependencies only
- Use `@ExtendWith(MockitoExtension.class)`
- Constructor injection with `@InjectMocks`
- Test edge cases and error paths

**DON'T:**
- Load Spring context (`@SpringBootTest`) for unit tests
- Use `Thread.sleep()` or real timers
- Test getters/setters
- Mock the class under test

### Integration Tests
- Use `@WebMvcTest` for controller tests
- Use `@DataJpaTest` for repository tests
- Use `@Transactional` with rollback for cleanup
- Mock external services (not repositories)

### Time Handling
```java
// Inject Clock for testable time
private final Clock clock;

public LocalDateTime now() {
    return LocalDateTime.now(clock);
}
```

## Angular Frontend Testing (Vitest)

### What to Test
- Pure functions and utilities
- Service business logic
- Data transformations and mappers
- Validation logic
- RxJS stream transformations

### What NOT to Test with Vitest
- Angular component templates (use E2E)
- DOM interactions
- CSS/layout behavior

### Pattern
```typescript
describe('formatLocalDateString', () => {
  it('should format date in local timezone', () => {
    const date = new Date('2024-02-14T19:00:00');
    expect(formatLocalDateString(date)).toBe('2024-02-14');
  });
});
```

**DO:**
- Use `vi.useFakeTimers()` for time-dependent tests
- Test edge cases (null, empty, boundary values)
- Keep tests fast (< 5s for full suite)

**DON'T:**
- Test private methods
- Use real HTTP calls
- Depend on test execution order

## Coverage Targets
- Line coverage: 80%
- Branch coverage: 75%
- Focus on business logic, not boilerplate
