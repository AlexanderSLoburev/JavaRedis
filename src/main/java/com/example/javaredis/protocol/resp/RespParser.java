package com.example.javaredis.protocol.resp;

import com.example.javaredis.protocol.ProtocolException;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespInteger;
import com.example.javaredis.protocol.resp.values.RespNull;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RespParser {
  private static final int MAX_BULK_BYTES = 512 * 1024 * 1024;
  private static final int MAX_ARRAY_LENGTH = 1024 * 1024;
  private static final int MAX_ARRAY_DEPTH = 32;
  private static final int READ_BUFFER_CAPACITY = 64;
  private static final int MAX_INITIAL_LIST_SIZE = 16;
  private final InputStream in;
  private int depth = 0;

  private void expectCRLF() throws IOException {
    if (in.read() != '\r' || in.read() != '\n') {
      throw new ProtocolException("expected CRLF after the bulk payload");
    }
  }

  private int parseInt(String str, String what) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      throw new ProtocolException(what + ": " + str);
    }
  }

  private RespValue parseLine(String line) throws IOException {
    char type = line.charAt(0);
    String payload = line.substring(1);

    return switch (type) {
      case '+' -> new SimpleString(payload);
      case '-' -> new RespError(payload);
      case ':' -> new RespInteger(parseLong(payload, "invalid integer"));
      case '$' -> readBulk(payload);
      case '*' -> readArray(payload);
      default ->
        throw new ProtocolException("invalid type byte '" + printable(type) +
                                    "'");
    };
  }

  private long parseLong(String str, String what) {
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException e) {
      throw new ProtocolException(what + ": " + str);
    }
  }

  private String printable(char chr) {
    return (chr >= 0x20 && chr <= 0x7F) ? String.valueOf(chr)
                                        : String.format("\\u%04X", (int)chr);
  }

  private RespValue readArray(String payload) throws IOException {
    int len = parseInt(payload, "invalid multibulk len");
    if (len < 0) {
      throw new ProtocolException("null arrays not expected in requests");
    }
    if (len > MAX_ARRAY_LENGTH) {
      throw new ProtocolException("invalid multibulk length " + len);
    }

    if (depth >= MAX_ARRAY_DEPTH) {
      throw new ProtocolException("array nesting too deep");
    }

    ++depth;
    try {
      List<RespValue> elements =
          new ArrayList<>(Math.min(len, MAX_INITIAL_LIST_SIZE));

      for (int i = 0; i < len; ++i) {
        String line = readLine();
        if (line == null) {
          throw new ProtocolException("unexpected EOF inside the array");
        }
        elements.add(parseLine(line));
      }

      return new RespArray(elements);
    } finally {
      --depth;
    }
  }

  private RespValue readBulk(String payload) throws IOException {
    int len = parseInt(payload, "Invalid bulk length");
    if (len == -1) {
      return RespNull.INSTANCE;
    }

    if (len < 0 || len > MAX_BULK_BYTES) {
      throw new ProtocolException("invalid bulk length " + len);
    }

    byte[] data = readFully(len);
    expectCRLF();

    return new BulkString(data);
  }

  private byte[] readFully(int nBytes) throws IOException {
    byte[] data = new byte[nBytes];
    int offset = 0;

    while (offset < nBytes) {
      int nRead = in.read(data, offset, nBytes - offset);
      if (nRead == -1) {
        throw new ProtocolException("unexpected EOF in bulk payload");
      }
      offset += nRead;
    }

    return data;
  }

  private String readLine() throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream(READ_BUFFER_CAPACITY);
    int byteRead = in.read();

    while (byteRead != -1 && byteRead != '\r') {
      buf.write(byteRead);
      byteRead = in.read();
    }

    if (byteRead == -1) {
      if (buf.size() == 0) {
        // A single EOF is the end of the connection
        return null;
      }
      throw new ProtocolException("unexpected EOF: unterminated line");
    }

    if (in.read() != '\n') {
      throw new ProtocolException("expected LF after CR");
    }

    if (buf.size() == 0) {
      throw new ProtocolException("empty line");
    }

    return buf.toString(StandardCharsets.US_ASCII);
  }

  public RespParser(InputStream in) {
    this.in = Objects.requireNonNull(in, "input stream can not be null");
  }

  public RespValue readNext() throws IOException {
    String line = readLine();
    return line == null ? null : parseLine(line);
  }
}
