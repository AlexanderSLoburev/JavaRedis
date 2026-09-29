package com.example.javaredis.protocol.resp.values;

public record SimpleString(String value) implements RespValue {
  public static final SimpleString OK = new SimpleString("OK");
  public static final SimpleString PONG = new SimpleString("PONG");
}
