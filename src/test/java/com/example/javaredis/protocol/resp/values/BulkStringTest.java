package com.example.javaredis.protocol.resp.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BulkStringTest {

  @Nested
  @DisplayName("equals() tests")
  class EqualityTest {
    @Test
    @DisplayName("Two identical instances are considered equals")
    void sameBulkStringsAreEqual() {
      BulkString str = new BulkString("foo".getBytes());
      assertTrue(str.equals(str));
    }

    @Test
    @DisplayName("Two equivalent but distinct objects with the same content "
                 + "should be treated as being 'equal'")
    void
    differentBulkStringsWithSameContentAreEqual() {
      BulkString str1 = new BulkString("foo".getBytes());
      BulkString str2 = new BulkString("foo".getBytes());
      assertTrue(str1.equals(str2));
    }

    @Test
    @DisplayName("Bulk strings should have reflective equality")
    void bulkStringEqualityIsReflexive() {
      BulkString str = new BulkString("foo".getBytes());
      assertTrue(str.equals(str));
    }

    @Test
    @DisplayName("Bulk strings should have symmetrical equality")
    void bulkStringEqualityIsSymmetrical() {
      BulkString str1 = new BulkString("foo".getBytes());
      BulkString str2 = new BulkString("foo".getBytes());
      assertTrue(str1.equals(str2) && str2.equals(str1));
    }

    @Test
    @DisplayName("Bulk strings should have transitive equality")
    void bulkStringEqualityAreTransitive() {
      BulkString str1 = new BulkString("foo".getBytes());
      BulkString str2 = new BulkString("foo".getBytes());
      BulkString str3 = new BulkString("foo".getBytes());
      assertTrue(str1.equals(str2) && str2.equals(str3) && str1.equals(str3));
    }

    @Test
    @DisplayName("Bulk strings should not be equal to null")
    void bulkStringAreNotEqualToNull() {
      BulkString str = new BulkString("foo".getBytes());
      assertFalse(str.equals(null));
    }

    @Test
    @DisplayName("Bulk strings with different contents should not be equal")
    void bulkStringsWithDifferentContentAreNotEqual() {
      BulkString str1 = new BulkString("foo".getBytes());
      BulkString str2 = new BulkString("bar".getBytes());
      assertFalse(str1.equals(str2));
    }

    @Test
    @DisplayName("Equal bulk string hash codes should match")
    void equalBulkStringsHaveTheSameHashCode() {
      BulkString str1 = new BulkString("foo".getBytes());
      BulkString str2 = new BulkString("foo".getBytes());
      assertEquals(str1.hashCode(), str2.hashCode());
    }
  }

  @Nested
  @DisplayName("asText() tests")
  class AsTextTests {

    @Test
    @DisplayName(
        "asText() should return an empty string for an empty byte array")
    void
    asTextShouldReturnEmptyStringForEmptyByteArray() {
      BulkString bulkString = new BulkString(new byte[0]);

      assertEquals("", bulkString.asText());
    }

    @Test
    @DisplayName("asText() correctly decodes ASCII")
    void asTextDecodesAscii() {
      byte[] value = "hello".getBytes(StandardCharsets.US_ASCII);

      BulkString bulkString = new BulkString(value);

      assertEquals("hello", bulkString.asText());
    }

    @Test
    @DisplayName("asText() correctly decodes multibyte UTF-8")
    void asTextDecodesUtf8Multibyte() {
      String original = "Привет, мир!";
      byte[] value = original.getBytes(StandardCharsets.UTF_8);

      BulkString bulkString = new BulkString(value);

      assertEquals(original, bulkString.asText());
    }

    @Test
    @DisplayName("asText() correctly decodes emojis from UTF-8")
    void asTextDecodesEmoji() {
      String original = "😀🚀";
      byte[] value = original.getBytes(StandardCharsets.UTF_8);

      BulkString bulkString = new BulkString(value);

      assertEquals(original, bulkString.asText());
    }

    @Test
    @DisplayName("asText() replaces invalid UTF-8 with U+FFFD")
    void asTextReplacesMalformedUtf8() {
      byte[] malformed = {(byte)0xFF};

      BulkString bulkString = new BulkString(malformed);

      assertEquals("\uFFFD", bulkString.asText());
    }

    @Test
    @DisplayName("asText() throws a NullPointerException if value is null")
    void asTextThrowsNpeWhenValueIsNull() {
      BulkString bulkString = new BulkString(null);

      assertThrows(NullPointerException.class, bulkString::asText);
    }
  }
}