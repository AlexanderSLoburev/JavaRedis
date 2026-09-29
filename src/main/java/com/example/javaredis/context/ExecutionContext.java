package com.example.javaredis.context;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionContext {
  private final ConcurrentHashMap<BytesKey, byte[]> storage =
      new ConcurrentHashMap<>();

  record BytesKey(byte[] bytes) {
    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }

      return other instanceof BytesKey &&
          Arrays.equals(bytes, ((BytesKey)(other)).bytes());
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }

  public boolean delete(byte[] key) {
    Objects.requireNonNull(key);
    return storage.remove(new BytesKey(key)) != null;
  }

  public byte[] get(byte[] key) {
    Objects.requireNonNull(key);
    return storage.get(new BytesKey(key));
  }

  public void put(byte[] key, byte value[]) {
    Objects.requireNonNull(key);
    storage.put(new BytesKey(key), value);
  }
}
