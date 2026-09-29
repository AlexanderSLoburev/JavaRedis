package com.example.javaredis.connection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.commands.CommandDispatcher;
import com.example.javaredis.protocol.commands.CommandRegistry;
import com.example.javaredis.protocol.commands.PingCommand;
import com.example.javaredis.protocol.resp.RespSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class ConnectionHandlerTest {
  private ServerSocket server;
  private Thread serverThread;
  private volatile Throwable serverError;

  /**
   * Starts a ConnectionHandler on a ServerSocket and returns the client socket.
   */
  private Socket startServerWith(CommandDispatcher dispatcher,
                                 RespSerializer serializer) throws IOException {
    server = new ServerSocket(0);
    serverThread = new Thread(() -> {
      try (Socket accepted = server.accept()) {
        new ConnectionHandler(dispatcher, serializer).run(accepted);
      } catch (Throwable t) {
        serverError = t;
      }
    });

    serverThread.setDaemon(true);
    serverThread.start();

    return new Socket("localhost", server.getLocalPort());
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int b;
    while ((b = in.read()) != -1 && b != '\n') {
      buf.write(b);
    }
    return buf.toString(StandardCharsets.US_ASCII);
  }

  @Test
  @DisplayName("Writes response for a single valid request")
  void writesResponseForSingleRequest() throws IOException {
    CommandRegistry registry = new CommandRegistry();
    registry.register("PING", new PingCommand(),
                      arity -> arity == 0 || arity == 1);
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());
    RespSerializer serializer = new RespSerializer();

    try (Socket clientSocket = startServerWith(dispatcher, serializer)) {
      OutputStream out = clientSocket.getOutputStream();
      out.write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();

      byte[] response = clientSocket.getInputStream().readNBytes(7);
      assertArrayEquals("+PONG\r\n".getBytes(StandardCharsets.US_ASCII),
                        response);
    }
  }

  @Test
  @DisplayName("Writes responses for multiple sequential requests")
  void writesResponsesForMultipleRequests() throws IOException {
    CommandRegistry registry = new CommandRegistry();
    registry.register("PING", new PingCommand(),
                      arity -> arity == 0 || arity == 1);
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    try (Socket clientSocket =
             startServerWith(dispatcher, new RespSerializer())) {
      OutputStream out = clientSocket.getOutputStream();
      InputStream in = clientSocket.getInputStream();

      out.write("*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n".getBytes(
          StandardCharsets.US_ASCII));
      out.flush();

      byte[] first = in.readNBytes(7);
      byte[] second = in.readNBytes(7);

      assertArrayEquals("+PONG\r\n".getBytes(StandardCharsets.US_ASCII), first);
      assertArrayEquals("+PONG\r\n".getBytes(StandardCharsets.US_ASCII),
                        second);
    }
  }

  @Test
  @DisplayName("Returns quietly when client closes connection immediately")
  void returnsQuietlyOnImmediateEof() throws IOException, InterruptedException {
    CommandRegistry registry = new CommandRegistry();
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    try (Socket clientSocket =
             startServerWith(dispatcher, new RespSerializer())) {
      // Don't write anything—close the socket
    }

    // Waiting for the server thread to finish
    serverThread.join();
    assertNull(serverError);
  }

  @Test
  @DisplayName("Writes protocol error for non-array request")
  void writesProtocolErrorForNonArrayRequest() throws IOException {
    CommandRegistry registry = new CommandRegistry();
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    try (Socket client = startServerWith(dispatcher, new RespSerializer())) {
      OutputStream out = client.getOutputStream();
      InputStream in = client.getInputStream();

      // simple string instead of an array
      out.write("+HELLO\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();

      // Read the entire response until EOF (the server will close the
      // connection)
      byte[] response = in.readAllBytes();
      String text = new String(response, StandardCharsets.US_ASCII);

      assertTrue(text.startsWith("-ERR protocol error:"));
      assertTrue(text.contains("expected array"));
    }
  }

  @Test
  @DisplayName(
      "Dispatcher runtime error becomes internal error and loop continues")
  void
  dispatcherRuntimeErrorBecomesInternalErrorAndLoopContinues()
      throws IOException {
    CommandRegistry registry = new CommandRegistry();
    registry.register("BOOM", (ctx, args) -> {
      throw new IllegalStateException("kaboom");
    }, arity -> arity == 0);
    registry.register("PING", new PingCommand(),
                      arity -> arity == 0 || arity == 1);
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    try (Socket client = startServerWith(dispatcher, new RespSerializer())) {
      OutputStream out = client.getOutputStream();
      InputStream in = client.getInputStream();

      out.write("*1\r\n$4\r\nBOOM\r\n*1\r\n$4\r\nPING\r\n".getBytes(
          StandardCharsets.US_ASCII));
      out.flush();

      // the first response is an error
      String error = readLine(in);
      assertTrue(error.startsWith("-ERR internal error:"));
      assertTrue(error.contains("kaboom"));

      // the second response is normal, which means the cycle is alive
      byte[] pong = in.readNBytes(7);
      assertArrayEquals("+PONG\r\n".getBytes(StandardCharsets.US_ASCII), pong);
    }
  }

  @Test
  @DisplayName("Malformed RESP produces protocol error")
  void malformedRespProducesProtocolError() throws IOException {
    CommandRegistry registry = new CommandRegistry();
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    try (Socket client = startServerWith(dispatcher, new RespSerializer())) {
      OutputStream out = client.getOutputStream();
      out.write("*abc\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();

      String response = new String(client.getInputStream().readAllBytes(),
                                   StandardCharsets.US_ASCII);
      assertTrue(response.startsWith("-ERR protocol error:"));
    }
  }
}
