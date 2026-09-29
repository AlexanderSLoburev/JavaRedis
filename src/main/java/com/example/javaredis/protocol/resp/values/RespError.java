package com.example.javaredis.protocol.resp.values;

public record RespError(String message) implements RespValue {
  private static String sanitize(String str) {
    StringBuilder sb = new StringBuilder();

    for (char c : str.toCharArray()) {
      // Remove control characters, including ‘\r’ and '\n'
      sb.append(c < 0x20 ? '?' : c);
    }

    return sb.toString();
  }

  public RespError {
    if (message.contains("\r") || message.contains("\n")) {
      throw new IllegalArgumentException("RespError must not contain CR or LF");
    }
  }

  public static RespError emptyCommand() {
    return new RespError("ERR empty command");
  }

  public static RespError err(String detail) {
    return new RespError("ERR " + detail);
  }

  public static RespError unknownCommand(String name) {
    return new RespError("ERR unknown command \'" + sanitize(name) + "\'");
  }

  public static RespError wrongArgCount(String command) {
    return new RespError("ERR wrong number of arguments for \'" + command +
                         "\' command");
  }

  public static RespError wrongType() {
    return new RespError(
        "WRONGTYPE Operation against a key holding the wrong kind of value");
  }
}
