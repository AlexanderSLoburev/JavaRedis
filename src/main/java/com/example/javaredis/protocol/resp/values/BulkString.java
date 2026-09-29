package com.example.javaredis.protocol.resp.values;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record BulkString(byte[] value) implements RespValue {
  public String asText() { return new String(value, StandardCharsets.UTF_8); }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof BulkString &&
        Arrays.equals(value, ((BulkString)other).value());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }
}
