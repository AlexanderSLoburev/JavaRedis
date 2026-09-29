package com.example.javaredis.protocol.resp.values;

import java.util.List;

public record RespArray(List<RespValue> elements) implements RespValue {
  public RespArray { elements = List.copyOf(elements); }
}
