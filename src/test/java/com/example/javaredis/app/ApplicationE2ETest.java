package com.example.javaredis.app;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ApplicationE2ETest {
  private Process process;
  private int port;

  private static int findFreePort() throws IOException {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    }
  }

  private static void waitForPort(int port, Duration timeout)
      throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadline) {
      try (Socket ignored = new Socket("localhost", port)) {
        return;
      } catch (IOException e) {
        Thread.sleep(50);
      }
    }

    throw new IllegalStateException("server did not start on port " + port);
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    int b;

    while ((b = in.read()) != -1 && b != '\n') {
      buf.write(b);
    }

    return buf.toString(StandardCharsets.US_ASCII);
  }

  @BeforeEach
  void start() throws IOException, InterruptedException {
    port = findFreePort();
    ProcessBuilder pb = new ProcessBuilder(
        System.getProperty("java.home") + "/bin/java", "-cp",
        System.getProperty("java.class.path"),
        "com.example.javaredis.app.Application", Integer.toString(port));
    pb.redirectErrorStream(true);
    process = pb.start();
    waitForPort(port,
                Duration.ofSeconds(5)); // waiting for the server to come up
  }

  @AfterEach
  void stop() {
    process.destroy();
  }

  @Test
  @DisplayName("PING returns PONG over real TCP")
  void pingReturnsPong() throws IOException {
    try (Socket s = new Socket("localhost", port)) {
      OutputStream out = s.getOutputStream();
      InputStream in = s.getInputStream();

      out.write("*1\r\n$4\r\nPING\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();

      assertArrayEquals("+PONG\r\n".getBytes(StandardCharsets.US_ASCII),
                        in.readNBytes(7));
    }
  }

  @Test
  @DisplayName("ECHO returns the argument")
  void echoReturnsArgument() throws IOException {
    try (Socket s = new Socket("localhost", port)) {
      OutputStream out = s.getOutputStream();
      InputStream in = s.getInputStream();

      out.write("*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n".getBytes(
          StandardCharsets.US_ASCII));
      out.flush();

      assertArrayEquals("$5\r\nhello\r\n".getBytes(StandardCharsets.US_ASCII),
                        in.readNBytes(11));
    }
  }

  @Test
  @DisplayName("Unknown command returns error")
  void unknownCommandReturnsError() throws IOException {
    try (Socket s = new Socket("localhost", port)) {
      OutputStream out = s.getOutputStream();
      InputStream in = s.getInputStream();

      out.write("*1\r\n$7\r\nUNKNOWN\r\n".getBytes(StandardCharsets.US_ASCII));
      out.flush();

      String response = readLine(in);
      assertTrue(response.startsWith("-ERR unknown command"));
    }
  }
}