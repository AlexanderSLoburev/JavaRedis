package com.example.javaredis.protocol.resp.values;

public final class RespNull implements RespValue {
  public final static RespNull INSTANCE = new RespNull();

  private RespNull() {}
}
