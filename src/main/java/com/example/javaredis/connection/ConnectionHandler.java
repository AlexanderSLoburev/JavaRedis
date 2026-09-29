package com.example.javaredis.connection;

import com.example.javaredis.protocol.ProtocolException;
import com.example.javaredis.protocol.commands.CommandDispatcher;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.RespParser;
import com.example.javaredis.protocol.resp.RespSerializer;
import com.example.javaredis.protocol.resp.values.RespValue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class ConnectionHandler {
  private final CommandDispatcher dispatcher;
  private final RespSerializer serializer;

  private RespValue dispatchSafely(RespArray request) {
    try {
      return dispatcher.dispatch(request);
    } catch (RuntimeException e) {
      return new RespError("ERR internal error: " + e.getMessage());
    }
  }

  public ConnectionHandler(CommandDispatcher dispatcher,
                           RespSerializer serializer) {
    this.dispatcher = dispatcher;
    this.serializer = serializer;
  }

  public void run(Socket socket) throws IOException {
    InputStream in = new BufferedInputStream(socket.getInputStream());
    OutputStream out = new BufferedOutputStream(socket.getOutputStream());
    RespParser parser = new RespParser(in);

    try {
      while (true) {
        RespValue request = parser.readNext();
        if (request == null) {
          return;
        }

        if (!(request instanceof RespArray array)) {
          throw new ProtocolException("expected array, got " +
                                      request.getClass().getSimpleName());
        }

        serializer.write(out, dispatchSafely(array));
        out.flush();
      }
    } catch (ProtocolException e) {
      serializer.write(out,
                       new RespError("ERR protocol error:" + e.getMessage()));
      out.flush();
    }
  }
}
