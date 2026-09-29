package com.example.javaredis.protocol.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespInteger;
import com.example.javaredis.protocol.resp.values.RespNull;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


public class RespSerializerTest {
  private static byte[] serialize(RespValue respValue) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new RespSerializer().write(out, respValue);
    return out.toByteArray();
  }

  private static byte[] toBytes(String str) {
    return str.getBytes(StandardCharsets.ISO_8859_1);
  }

  @Nested
  class WriteBulkTest {
    @Test
    @DisplayName(
        "RespSerializer writes bulk string as $<len> CRLF payload CRLF")
    void
    writesBulkString() throws IOException {
      byte[] actual = serialize(new BulkString(toBytes("hello")));

      assertArrayEquals(toBytes("$5\r\nhello\r\n"), actual);
    }

    @Test
    @DisplayName("RespSerializer writes empty bulk string as $0 CRLF CRLF")
    void writesEmptyBulkString() throws IOException {
      byte[] actual = serialize(new BulkString(new byte[0]));

      assertArrayEquals(toBytes("$0\r\n\r\n"), actual);
    }

    @Test
    @DisplayName(
        "RespSerializer writes bulk string with binary payload unchanged")
    void
    writesBinaryBulkString() throws IOException {
      byte[] payload = {0x00, (byte)0xFF, 0x7F, '\r', '\n'};

      byte[] actual = serialize(new BulkString(payload));

      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.write(toBytes("$5\r\n"));
      expected.write(payload);
      expected.write(toBytes("\r\n"));

      assertArrayEquals(expected.toByteArray(), actual);
    }

    @Test
    @DisplayName("CRLF inside payload is written as data, not as terminator")
    void writesPayloadContainingCrlf() throws IOException {
      byte[] payload = toBytes("a\r\nb");

      byte[] actual = serialize(new BulkString(payload));

      assertArrayEquals(toBytes("$4\r\na\r\nb\r\n"), actual);
    }

    @Test
    @DisplayName("length is measured in bytes for multibyte UTF-8")
    void writesUtf8BulkWithByteLength() throws IOException {
      byte[] payload = "Привет".getBytes(StandardCharsets.UTF_8); // 12 bytes

      byte[] actual = serialize(new BulkString(payload));

      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.write(toBytes("$12\r\n"));
      expected.write(payload);
      expected.write(toBytes("\r\n"));

      assertArrayEquals(expected.toByteArray(), actual);
    }

    @Test
    @DisplayName("RespSerializer writes multi-digit length correctly")
    void writesMultiDigitLength() throws IOException {
      byte[] payload = new byte[100];
      Arrays.fill(payload, (byte)'x');

      byte[] actual = serialize(new BulkString(payload));

      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.write(toBytes("$100\r\n"));
      expected.write(payload);
      expected.write(toBytes("\r\n"));

      assertArrayEquals(expected.toByteArray(), actual);
    }

    @Test
    @DisplayName("-1 represents null value when writing a RESP object.")
    void writesNullBulkAsDollarMinusOne() throws IOException {
      byte[] actual = serialize(RespNull.INSTANCE);
      assertArrayEquals(toBytes("$-1\r\n"), actual);
    }
  }

  @Nested
  class WriteArrayTest {
    @Test
    @DisplayName("RespSerializer writes empty array as *0 CRLF")
    void writesEmptyArray() throws IOException {
      byte[] actual = serialize(new RespArray(List.of()));

      assertArrayEquals(toBytes("*0\r\n"), actual);
    }

    @Test
    @DisplayName("RespSerializer writes single-element array")
    void writesSingleElementArray() throws IOException {
      RespArray arr = new RespArray(List.of(new SimpleString("OK")));

      byte[] actual = serialize(arr);

      assertArrayEquals(toBytes("*1\r\n+OK\r\n"), actual);
    }

    @Test
    @DisplayName("RespSerializer writes array with mixed element types")
    void writesMixedTypeArray() throws IOException {
      RespArray arr =
          new RespArray(List.of(new SimpleString("OK"), new RespInteger(42),
                                new BulkString(toBytes("hello")),
                                RespNull.INSTANCE, new RespError("ERR bad")));

      byte[] actual = serialize(arr);

      assertArrayEquals(toBytes("*5\r\n"
                                + "+OK\r\n"
                                + ":42\r\n"
                                + "$5\r\nhello\r\n"
                                + "$-1\r\n"
                                + "-ERR bad\r\n"),
                        actual);
    }

    @Test
    @DisplayName("RespSerializer writes nested arrays")
    void writesNestedArrays() throws IOException {
      RespArray inner =
          new RespArray(List.of(new RespInteger(1), new RespInteger(2)));
      RespArray outer =
          new RespArray(List.of(inner, new BulkString(toBytes("foo"))));

      byte[] actual = serialize(outer);

      assertArrayEquals(toBytes("*2\r\n"
                                + "*2\r\n:1\r\n:2\r\n"
                                + "$3\r\nfoo\r\n"),
                        actual);
    }
  }
}
