package com.example.javaredis.protocol.resp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.javaredis.protocol.ProtocolException;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespInteger;
import com.example.javaredis.protocol.resp.values.RespNull;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RespParserTest {
  private RespValue parse(String input) throws IOException {
    return new RespParser(
               new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)))
        .readNext();
  }

  @Nested
  class ParseRespValuesTest {
    @Test
    @DisplayName("RespParser parses empty bulk string ($0) followed by CRLF")
    void parsesEmptyBulkWithCrlf() throws IOException {
      RespValue value = parse("$0\r\n\r\n");
      assertEquals(new BulkString(new byte[0]), value);
    }

    @Test
    @DisplayName("RespParser parses simple string")
    void parsesSimpleString() throws IOException {
      assertEquals(new SimpleString("OK"), parse("+OK\r\n"));
    }

    @Test
    @DisplayName("RespParser parses error")
    void parsesError() throws IOException {
      assertEquals(new RespError("ERR bad"), parse("-ERR bad\r\n"));
    }

    @Test
    @DisplayName("RespParser parses integer")
    void parsesInteger() throws IOException {
      assertEquals(new RespInteger(42), parse(":42\r\n"));
    }

    @Test
    @DisplayName("RespParser parses bulk string")
    void parsesBulkString() throws IOException {
      assertEquals(new BulkString("hello".getBytes(StandardCharsets.UTF_8)),
                   parse("$5\r\nhello\r\n"));
    }

    @Test
    @DisplayName("RespParser parses bulk string with binary payload")
    void respParserBulkStringWithBinaryPayload() throws IOException {
      // 0xFF is a byte with the most significant bit, which would be an invalid
      // sequence in String/UTF-8;
      byte[] payload = {0x00, (byte)0xFF, 0x7F, '\r', '\n'};
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write(("$" + String.valueOf(payload.length) + "\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
      out.write(payload);
      out.write("\r\n".getBytes(StandardCharsets.US_ASCII));

      RespValue value =
          new RespParser(new ByteArrayInputStream(out.toByteArray()))
              .readNext();

      assertArrayEquals(payload, ((BulkString)value).value());
    }

    @Test
    @DisplayName("RespParser parses null bulk string")
    void parsesNullBulkString() throws IOException {
      assertSame(RespNull.INSTANCE, parse("$-1\r\n"));
    }

    @Test
    @DisplayName("RespParses parses array")
    void parsesArray() throws IOException {
      RespArray array = (RespArray)parse("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n");

      assertEquals(2, array.elements().size());
      assertEquals(new BulkString("foo".getBytes(StandardCharsets.UTF_8)),
                   array.elements().get(0));
      assertEquals(new BulkString("bar".getBytes(StandardCharsets.UTF_8)),
                   array.elements().get(1));
    }

    @Test
    @DisplayName("RespParses parses nested array")
    void parsesNestedArrays() throws IOException {
      RespArray outer =
          (RespArray)parse("*2\r\n*2\r\n:1\r\n:2\r\n$3\r\nfoo\r\n");
      RespArray inner = (RespArray)outer.elements().get(0);

      assertEquals(2, outer.elements().size());
      assertEquals(new RespInteger(1), inner.elements().get(0));
      assertEquals(new RespInteger(2), inner.elements().get(1));
      assertEquals(new BulkString("foo".getBytes(StandardCharsets.UTF_8)),
                   outer.elements().get(1));
    }

    @Test
    @DisplayName("RespParser handles EOF at the beginning of input")
    void handleEOFAtTheBeginningOfInput() throws IOException {
      RespParser parser = new RespParser(new ByteArrayInputStream(new byte[0]));

      assertNull(parser.readNext());
    }

    @Test
    @DisplayName("Bulk payload ending with CR is not confused with terminator")
    void bulkPayloadEndingWithCr() throws IOException {
      // payload is "ab\r" (3 bytes), then real CRLF
      RespValue value = parse("$3\r\nab\r\r\n");
      assertEquals(new BulkString(new byte[] {'a', 'b', '\r'}), value);
    }

    @Test
    @DisplayName("CRLF inside bulk payload is treated as data")
    void crlfInsideBulkPayload() throws IOException {
      // payload = "a\r\nb" (4 bytes), then real CRLF
      RespValue value = parse("$4\r\na\r\nb\r\n");
      assertEquals(new BulkString(new byte[] {'a', '\r', '\n', 'b'}), value);
    }

    @Test
    @DisplayName("RespParser is positioned right after CRLF for the next value")
    void parserStopsExactlyAfterCrlf() throws IOException {
      RespParser parser = new RespParser(new ByteArrayInputStream(
          "$5\r\nhello\r\n:42\r\n".getBytes(StandardCharsets.US_ASCII)));

      assertEquals(
          new BulkString("hello".getBytes(StandardCharsets.US_ASCII)),
          parser.readNext());
      assertEquals(new RespInteger(42), parser.readNext());
    }
  }

  @Nested
  class ProtocolExceptionTest {
    @Test
    @DisplayName("RespParser throws ProtocolException on unknown byte type")
    void protocolExceptionOnUnknownByteType() {
      assertThrows(ProtocolException.class, () -> parse("?oops\r\n"));
    }

    @Test
    @DisplayName(
        "RespParser throws ProtocolException when bulk payload is truncated")
    void
    protocolExceptionOnTruncatedBulkPayload() {
      assertThrows(ProtocolException.class, () -> parse("$5\r\naaa"));
    }

    @Test
    @DisplayName(
        "RespParser throws ProtocolException when bulk length is not a number")
    void
    protocolExceptionWhenLengthIsNotANumber() {
      assertThrows(ProtocolException.class, () -> parse("$aaa\r\n"));
    }

    @Test
    @DisplayName(
        "RespParser throws ProtocolException when the array size is too big")
    void
    protocolExceptionWhenArraySizeIsTooBig() {
      assertThrows(ProtocolException.class, () -> parse("*999999999\r\n"));
    }

    @Test
    @DisplayName("RespParser throws ProtocolException on arrays nested "
                 + "deeper than MAX_ARRAY_DEPTH")
    void
    protocolExceptionOnTooDeeplyNestedArrays() {
      String input = "*1\r\n".repeat(33) + ":0\r\n";

      assertThrows(ProtocolException.class, () -> parse(input));
    }

    @Test
    @DisplayName("RespParser accepts arrays nested exactly to MAX_ARRAY_DEPTH")
    void acceptsMaxDepthNesting() throws IOException {
      String input = "*1\r\n".repeat(32) + ":0\r\n";

      RespValue value = parse(input);

      // expand the nesting and check what is at the very bottom :0
      RespValue current = value;
      for (int i = 0; i < 32; i++) {
        assertInstanceOf(RespArray.class, current);
        RespArray arr = (RespArray)current;
        assertEquals(1, arr.elements().size());
        current = arr.elements().get(0);
      }
      assertEquals(new RespInteger(0), current);
    }

    @Test
    @DisplayName("RespParser throws ProtocolException on payload without CRLS")
    void protocolExceptionOnMissingCRLF() {
      assertThrows(ProtocolException.class, () -> parse("$5\rhello\r"));
    }

    @Test
    @DisplayName("RespParser throws ProtocolException on payload consisting "
                 + "solely of CRLS")
    void
    protocolExceptionOnEmptyMessageBody() {
      assertThrows(ProtocolException.class, () -> parse("\r\n"));
    }

    @Test
    @DisplayName("RespParser fails if stream ends right after bulk payload")
    void failsWhenEofAfterPayload() {
      assertThrows(ProtocolException.class, () -> parse("$5\r\nhello"));
    }

    @Test
    @DisplayName("RespParser fails if only CR follows bulk payload")
    void failsWhenOnlyCrFollowsPayload() {
      assertThrows(ProtocolException.class, () -> parse("$5\r\nhello\r"));
    }

    @Test
    @DisplayName("RespParser fails if bulk payload is not terminated by CR")
    void failsWhenFirstByteIsNotCr() {
      assertThrows(ProtocolException.class, () -> parse("$5\r\nhelloX\r\n"));
    }

    @Test
    @DisplayName("RespParser fails on empty bulk without trailing CRLF")
    void failsOnEmptyBulkWithoutCrlf() {
      assertThrows(ProtocolException.class, () -> parse("$0\r\n"));
    }

    @Test
    @DisplayName("RespParser fails on array with negative length")
    void failsOnArrayWithNegativeLength() {
      assertThrows(ProtocolException.class, () -> parse("*-1\r\n"));
    }

    @Test
    @DisplayName("RespParser fails on incorrect long value")
    void failsOnIncorrectLongValue() {
      assertThrows(ProtocolException.class, () -> parse(":1a\r\n"));
    }

    @Test
    @DisplayName(
        "RespParser fails when EOF occurs before the first element of an array")
    void
    failsWhenEofInsideArrayBeforeAnyElement() {
      assertThrows(ProtocolException.class, () -> parse("*2\r\n"));
    }

    @Test
    @DisplayName(
        "RespParser fails when EOF occurs after some elements of an array")
    void
    failsWhenEofInsideArrayAfterSomeElements() {
      assertThrows(ProtocolException.class, () -> parse("*3\r\n$3\r\nfoo\r\n"));
    }

    @Test
    @DisplayName("RespParser fails when EOF occurs inside a nested array")
    void failsWhenEofInsideNestedArray() {
      // outer array consists of 1 element, inner array of 2, but the second
      // element is missing
      assertThrows(ProtocolException.class,
                   () -> parse("*1\r\n*2\r\n$3\r\nfoo\r\n"));
    }

    @Test
    @DisplayName("RespParser fails on negative bulk length other than -1")
    void failsOnNegativeBulkLength() {
      assertThrows(ProtocolException.class, () -> parse("$-2\r\n"));
    }

    @Test
    @DisplayName("RespParser fails on bulk length greater than MAX_BULK_BYTES")
    void failsOnBulkLengthTooLarge() {
      long tooLarge = 512L * 1024 * 1024 + 1; // 536870913
      assertThrows(ProtocolException.class,
                   () -> parse("$" + tooLarge + "\r\n"));
    }

    @Test
    @DisplayName("RespParser fails when stream ends without a line terminator")
    void failsOnUnterminatedLine() {
      assertThrows(ProtocolException.class, () -> parse("+OK"));
    }
  }
}