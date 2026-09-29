package com.example.javaredis.protocol.resp.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RespErrorTest {
  @Test
  @DisplayName(
      "RespError Constructor Throws IAE When Message Contains CR or LF")
  public void
  throwsIaeWhenMessageContainsCrLf() {
    assertThrows(IllegalArgumentException.class,
                 () -> new RespError("foo\rbar"));
    assertThrows(IllegalArgumentException.class,
                 () -> new RespError("foo\nbar"));
  }

  @Test
  @DisplayName("RespError can sanitize strings")
  void canSanitizeStrings() {
    assertEquals(RespError.unknownCommand("h\u0006llo"),
                 new RespError("ERR unknown command \'h?llo\'"));
  }
}