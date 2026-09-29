package com.example.javaredis.context;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


public class ExecutionContextTest {
  private ExecutionContext context;

  private static byte[] toBytes(String str) {
    return str.getBytes(StandardCharsets.UTF_8);
  }

  @BeforeEach
  void setUp() {
    context = new ExecutionContext();
  }

  @Nested
  @DisplayName("put() and get()")
  class PutGetTest {

    @Test
    @DisplayName("get() returns null for a missing key")
    void getReturnsNullForMissingKey() {
      assertNull(context.get(toBytes("missing")));
    }

    @Test
    @DisplayName("put() followed by get() returns the same value")
    void putFollowedByGetReturnsSameValue() {
      context.put(toBytes("key"), toBytes("value"));

      assertArrayEquals(toBytes("value"), context.get(toBytes("key")));
    }

    @Test
    @DisplayName("Different keys do not interfere with each other")
    void differentKeysDoNotInterfereWithEachOther() {
      context.put(toBytes("key1"), toBytes("value1"));
      context.put(toBytes("key2"), toBytes("value2"));

      assertArrayEquals(toBytes("value1"), context.get(toBytes("key1")));
      assertArrayEquals(toBytes("value2"), context.get(toBytes("key2")));
    }

    @Test
    @DisplayName("put() overwrites the existing value")
    void putOverwritesExistingValues() {
      context.put(toBytes("key"), toBytes("value1"));
      context.put(toBytes("key"), toBytes("value2"));

      assertArrayEquals(toBytes("value2"), context.get(toBytes("key")));
    }

    @Test
    @DisplayName("An empty array is allowed as a key")
    void emptyArrayIsAllowedAsKey() {
      byte[] key = new byte[0];
      context.put(key, toBytes("value"));

      assertArrayEquals(toBytes("value"), context.get(key));
    }

    @Test
    @DisplayName("A null value as a key throws a NPE")
    void nullValueAsKeyThrowsNpe() {
      assertThrows(NullPointerException.class,
                   () -> context.put(null, toBytes("value")));
    }

    @Test
    @DisplayName("A null value as a value throws a NPE")
    void nullValueAsValueThrowsNpe() {
      assertThrows(NullPointerException.class,
                   () -> context.put(toBytes("key"), null));
    }

    @Test
    @DisplayName("get(null) throws a NPE")
    void getOnNullThrowsNpe() {
      assertThrows(NullPointerException.class, () -> context.get(null));
    }
  }

  @Nested
  @DisplayName("delete()")
  class DeleteTest {

    @Test
    @DisplayName(
        "delete() on an existing key returns true and deletes the value")
    void
    deleteOnExistingKeyReturnsTrueAndDeletesValue() {
      context.put(toBytes("key"), toBytes("value"));

      assertTrue(context.delete(toBytes("key")));
      assertNull(context.get(toBytes("key")));
    }

    @Test
    @DisplayName("delete() on a nonexistent key returns false")
    void deleteOnNonExistentKeyReturnsFalse() {
      assertFalse(context.delete(toBytes("key")));
    }

    @Test
    @DisplayName("delete() uses a content-based comparison")
    void deleteUsesContentBasedComparison() {
      context.put(toBytes("key"), toBytes("value"));

      assertTrue(context.delete(toBytes("key")));
    }

    @Test
    @DisplayName("delete on null throws a NPE")
    void deleteOnNullTrowsNpe() {
      assertThrows(NullPointerException.class, () -> context.delete((null)));
    }
  }

  @Nested
  @DisplayName("BytesKey")
  class BytesKeyTest {

    @Test
    @DisplayName("equals is reflective")
    void equalsIsReflexive() {
      ExecutionContext.BytesKey key =
          new ExecutionContext.BytesKey(toBytes("a"));

      assertTrue(key.equals(key));
    }

    @Test
    @DisplayName("equals is symmetrical")
    void equalsIsSymmetrical() {
      ExecutionContext.BytesKey key1 =
          new ExecutionContext.BytesKey(toBytes("a"));
      ExecutionContext.BytesKey key2 =
          new ExecutionContext.BytesKey(toBytes("a"));

      assertTrue(key1.equals(key2) && key2.equals(key1));
      assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    @DisplayName("equals is transitive")
    void equalsIsTransitive() {
      ExecutionContext.BytesKey key1 =
          new ExecutionContext.BytesKey(toBytes("a"));
      ExecutionContext.BytesKey key2 =
          new ExecutionContext.BytesKey(toBytes("a"));
      ExecutionContext.BytesKey key3 =
          new ExecutionContext.BytesKey(toBytes("a"));

      assertTrue(key1.equals(key2) && key2.equals(key3) && key1.equals(key3));
    }

    @Test
    @DisplayName("BytesKey object does not equal null")
    void doesNotEqualNull() {
      ExecutionContext.BytesKey key =
          new ExecutionContext.BytesKey(toBytes("a"));

      assertFalse(key.equals(null));
    }
  }
}
