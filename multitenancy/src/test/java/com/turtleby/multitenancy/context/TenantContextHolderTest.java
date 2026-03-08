package com.turtleby.multitenancy.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.turtleby.multitenancy.core.IsolationMode;
import com.turtleby.multitenancy.core.Tenant;

/**
 * Unit tests for {@link TenantContextHolder}. Verifies thread-local behavior and proper context
 * management.
 */
class TenantContextHolderTest {

  @AfterEach
  void cleanup() {
    // Always clean up after each test
    TenantContextHolder.clear();
  }

  @Test
  void shouldReturnNullWhenNoContextSet() {
    TenantContext context = TenantContextHolder.getContext();
    assertThat(context).isNull();
  }

  @Test
  void shouldSetAndGetContext() {
    TestTenant tenant = new TestTenant("test-tenant", "test_schema");
    TenantContext context = new TenantContext(tenant);

    TenantContextHolder.setContext(context);

    TenantContext retrievedContext = TenantContextHolder.getContext();
    assertThat(retrievedContext).isEqualTo(context);
    assertThat(retrievedContext.getTenant()).isEqualTo(tenant);
  }

  @Test
  void shouldClearContext() {
    TestTenant tenant = new TestTenant("test-tenant", "test_schema");
    TenantContext context = new TenantContext(tenant);

    TenantContextHolder.setContext(context);
    assertThat(TenantContextHolder.getContext()).isEqualTo(context);

    TenantContextHolder.clear();
    assertThat(TenantContextHolder.getContext()).isNull();
  }

  @Test
  void shouldMaintainContextInSameThread() {
    TestTenant tenant1 = new TestTenant("tenant1", "schema1");
    TestTenant tenant2 = new TestTenant("tenant2", "schema2");

    // Set first context
    TenantContextHolder.setContext(new TenantContext(tenant1));
    assertThat(TenantContextHolder.getContext().getTenant().getTenantId()).isEqualTo("tenant1");

    // Change to second context
    TenantContextHolder.setContext(new TenantContext(tenant2));
    assertThat(TenantContextHolder.getContext().getTenant().getTenantId()).isEqualTo("tenant2");

    // Verify the context changed
    assertThat(TenantContextHolder.getContext().getTenant().getSchema()).isEqualTo("schema2");
  }

  @Test
  void shouldIsolateContextBetweenThreads() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    AtomicReference<String> thread1TenantId = new AtomicReference<>();
    AtomicReference<String> thread2TenantId = new AtomicReference<>();

    ExecutorService executor = Executors.newFixedThreadPool(2);

    // Thread 1 sets and reads tenant1
    executor.submit(
        () -> {
          try {
            TestTenant tenant = new TestTenant("tenant1", "schema1");
            TenantContextHolder.setContext(new TenantContext(tenant));

            // Small delay to increase chance of interference if isolation is broken
            Thread.sleep(10);

            TenantContext context = TenantContextHolder.getContext();
            thread1TenantId.set(context != null ? context.getTenant().getTenantId() : null);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            TenantContextHolder.clear();
            latch.countDown();
          }
        });

    // Thread 2 sets and reads tenant2
    executor.submit(
        () -> {
          try {
            TestTenant tenant = new TestTenant("tenant2", "schema2");
            TenantContextHolder.setContext(new TenantContext(tenant));

            // Small delay to increase chance of interference if isolation is broken
            Thread.sleep(10);

            TenantContext context = TenantContextHolder.getContext();
            thread2TenantId.set(context != null ? context.getTenant().getTenantId() : null);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            TenantContextHolder.clear();
            latch.countDown();
          }
        });

    // Wait for both threads to complete
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

    // Verify each thread maintained its own context
    assertThat(thread1TenantId.get()).isEqualTo("tenant1");
    assertThat(thread2TenantId.get()).isEqualTo("tenant2");

    executor.shutdown();
  }

  @Test
  void shouldInheritContextInChildThread() throws InterruptedException {
    TestTenant parentTenant = new TestTenant("parent-tenant", "parent_schema");
    TenantContextHolder.setContext(new TenantContext(parentTenant));

    AtomicReference<String> childTenantId = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    // Create child thread that should inherit the parent's context
    Thread childThread =
        new Thread(
            () -> {
              try {
                TenantContext context = TenantContextHolder.getContext();
                childTenantId.set(context != null ? context.getTenant().getTenantId() : null);
              } finally {
                latch.countDown();
              }
            });

    childThread.start();
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

    // Verify child thread inherited parent's context
    assertThat(childTenantId.get()).isEqualTo("parent-tenant");
  }

  @Test
  void shouldHandleNullContext() {
    TenantContextHolder.setContext(null);
    assertThat(TenantContextHolder.getContext()).isNull();

    // Clear should not throw exception even when context is null
    TenantContextHolder.clear();
    assertThat(TenantContextHolder.getContext()).isNull();
  }

  @Test
  void shouldHandleEmptyContext() {
    TenantContext emptyContext = new TenantContext(null);
    TenantContextHolder.setContext(emptyContext);

    TenantContext retrievedContext = TenantContextHolder.getContext();
    assertThat(retrievedContext).isEqualTo(emptyContext);
    assertThat(retrievedContext.getTenant()).isNull();
  }

  /** Simple test implementation of Tenant interface. */
  private static class TestTenant implements Tenant {
    private final String tenantId;
    private final String schema;

    public TestTenant(String tenantId, String schema) {
      this.tenantId = tenantId;
      this.schema = schema;
    }

    @Override
    public String getTenantId() {
      return tenantId;
    }

    @Override
    public IsolationMode getIsolationMode() {
      return IsolationMode.SCHEMA;
    }

    @Override
    public String getSchema() {
      return schema;
    }

    @Override
    public String getDatabase() {
      return null;
    }
  }
}
