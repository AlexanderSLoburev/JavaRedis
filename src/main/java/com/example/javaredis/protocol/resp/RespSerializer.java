package com.example.javaredis.protocol.resp;

import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespInteger;
import com.example.javaredis.protocol.resp.values.RespNull;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class RespSerializer {
  private static final byte[] CRLF = {'\r', '\n'};

  private void writeArray(OutputStream out, RespArray arr) throws IOException {
    List<RespValue> values = arr.elements();
    out.write('*');
    out.write(
        Integer.toString(values.size()).getBytes(StandardCharsets.US_ASCII));
    out.write(CRLF);

    for (RespValue value : values) {
      write(out, value);
    }
  }

  private void writeBulk(OutputStream out, BulkString bulkString)
      throws IOException {
    byte[] value = bulkString.value();
    out.write('$');
    out.write(
        Integer.toString(value.length).getBytes(StandardCharsets.US_ASCII));
    out.write(CRLF);
    out.write(value);
    out.write(CRLF);
  }

  private void writeLine(OutputStream out, char marker, String payload)
      throws IOException {
    out.write(marker);
    out.write(payload.getBytes(StandardCharsets.US_ASCII));
    out.write(CRLF);
  }

  private void writeNull(OutputStream out) throws IOException {
    out.write('$');
    out.write('-');
    out.write('1');
    out.write(CRLF);
  }

  public void write(OutputStream out, RespValue value) throws IOException {
    switch (value) {
    case SimpleString s -> writeLine(out, '+', s.value());
    case BulkString b -> writeBulk(out, b);
    case RespError e -> writeLine(out, '-', e.message());
    case RespNull n -> writeNull(out);
    case RespInteger i -> writeLine(out, ':', Long.toString(i.value()));
    case RespArray a -> writeArray(out, a);
    }
  }
}
