package com.example.javaredis.protocol.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.ProtocolException;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespInteger;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CommandDispatcherTest {
  private static RespArray request(String... parts) {
    List<RespValue> values = Arrays.stream(parts)
                                 .map(p
                                      -> (RespValue) new BulkString(
                                          p.getBytes(StandardCharsets.UTF_8)))
                                 .toList();
    return new RespArray(values);
  }

  private static byte[] toBytes(String str) {
    return str.getBytes(StandardCharsets.ISO_8859_1);
  }

  @Test
  @DisplayName("Dispatch of empty array returns empty command error")
  void dispatchOfEmptyArrayReturnsEmptyCommandError() {
    CommandDispatcher dispatcher =
        new CommandDispatcher(new CommandRegistry(), new ExecutionContext());

    RespValue result = dispatcher.dispatch(new RespArray(List.of()));

    assertEquals(RespError.emptyCommand(), result);
  }

  @Test
  @DisplayName("Dispatch of a command not in the registry returns an unknown "
               + "command error")
  void
  dispatchCommandNotInRegistryReturnsUnknownCommandError() {
    CommandDispatcher dispatcher =
        new CommandDispatcher(new CommandRegistry(), new ExecutionContext());

    RespValue result = dispatcher.dispatch(request("NOSUCH"));

    assertEquals(RespError.unknownCommand("NOSUCH"), result);
  }

  @Test
  @DisplayName("dispatch accepts SimpleString as command name")
  void dispatchAcceptsSimpleStringAsCommandName() {
    CommandRegistry registry = new CommandRegistry();
    registry.register(
        "PING", (ctx, args) -> new SimpleString("PONG"), arity -> arity == 0);

    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    RespArray req = new RespArray(List.of(new SimpleString("PING")));

    RespValue result = dispatcher.dispatch(req);

    assertEquals(new SimpleString("PONG"), result);
  }

  @Test
  @DisplayName("dispatch accepts SimpleString as argument")
  void dispatchAcceptsSimpleStringAsArgument() {
    List<BulkString> captured = new ArrayList<>();
    CommandRegistry registry = new CommandRegistry();
    registry.register("ECHO", (ctx, args) -> {
      captured.addAll(args);
      return args.get(0);
    }, arity -> arity == 1);

    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    RespArray req = new RespArray(
        List.of(new BulkString(toBytes("ECHO")), new SimpleString("hello")));

    RespValue result = dispatcher.dispatch(req);

    assertEquals(new BulkString(toBytes("hello")), result);
    assertEquals(1, captured.size());
    assertArrayEquals(toBytes("hello"), captured.get(0).value());
  }

  @Test
  @DisplayName("dispatch accepts an all-SimpleString request")
  void dispatchAcceptsAllSimpleStringRequest() {
    CommandRegistry registry = new CommandRegistry();
    registry.register("ECHO", (ctx, args) -> args.get(0), arity -> arity == 1);

    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    RespArray req = new RespArray(
        List.of(new SimpleString("ECHO"), new SimpleString("hello")));

    RespValue result = dispatcher.dispatch(req);

    assertEquals(new BulkString(toBytes("hello")), result);
  }

  @Test
  @DisplayName("SimpleString is converted with UTF-8, not platform charset")
  void simpleStringConvertedWithUtf8() {
    CommandRegistry registry = new CommandRegistry();
    registry.register("ECHO", (ctx, args) -> args.get(0), arity -> arity == 1);

    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    RespArray req = new RespArray(
        List.of(new SimpleString("ECHO"), new SimpleString("Привет")));

    RespValue result = dispatcher.dispatch(req);

    assertArrayEquals("Привет".getBytes(StandardCharsets.UTF_8),
                      ((BulkString)result).value());
  }

  @Test
  @DisplayName("dispatch does not accept any RespValue except BulkString and "
               + "SimpleString as a command name")
  void
  dispatchDoesNotAcceptAnyRespValueExceptBulkStringOrSimpleStringAsCommandName() {
    CommandRegistry registry = new CommandRegistry();
    registry.register("PING", new PingCommand(),
                      arity -> arity == 0 || arity == 1);
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());

    RespArray req = new RespArray(List.of(new RespInteger(42)));

    assertThrows(ProtocolException.class, () -> dispatcher.dispatch(req));
  }

  @Test
  @DisplayName("dispatch does not accept any RespValue except BulkString and "
               + "SimpleString as a command argument")
  void
  dispatchDoesNotAcceptAnyRespValueExceptBulkStringOrSimpleStringAsCommandArg() {
    CommandRegistry registry = new CommandRegistry();
    registry.register("ECHO", new EchoCommand(), arity -> arity == 1);
    CommandDispatcher dispatcher =
        new CommandDispatcher(registry, new ExecutionContext());
    RespArray req =
        new RespArray(List.of(new SimpleString("PING"), new RespInteger(42)));

    assertThrows(ProtocolException.class, () -> dispatcher.dispatch(req));
  }

  @Nested
  class PingTest {

    @Test
    @DisplayName(
        "Dispatch of no arguments to the ping command succeeds and responds")
    void
    dispatchNoArgumentsToPingCommandSucceedsAndResponds() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("PING", new PingCommand(),
                        arity -> arity == 0 || arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("PING"));

      assertEquals(SimpleString.PONG, result);
    }

    @Test
    @DisplayName(
        "Dispatch of one argument to the ping command succeeds and responds")
    void
    dispatchOfOneArgumentToPingCommandSucceedsAndResponds() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("PING", new PingCommand(),
                        arity -> arity == 0 || arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("PING", "One"));

      assertEquals(new BulkString("One".getBytes(StandardCharsets.UTF_8)),
                   result);
    }

    @Test
    @DisplayName("Dispatch of one multibyte UTF-8 argument to the ping "
                 + "command succeeds and responds")
    void
    dispatchOfOneUtf8ArgumentToPingCommandSucceedsAndResponds() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("PING", new PingCommand(),
                        arity -> arity == 0 || arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("PING", "Привет"));

      assertEquals(new BulkString("Привет".getBytes(StandardCharsets.UTF_8)),
                   result);
    }

    @Test
    @DisplayName(
        "Dispatch of a ping command with wrong arity returns an wrong argument "
        + "count error")
    void
    dispatchPingCommandWithWrongArityReturnsWrongArgumentCountError() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("PING", new PingCommand(),
                        arity -> arity == 0 || arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("PING", "One", "Two"));

      assertEquals(RespError.wrongArgCount("PING"), result);
    }
  }

  @Nested
  class EchoTest {

    @Test
    @DisplayName(
        "Dispatch of one argument to the echo command succeeds and responds")
    void
    dispatchOfOneArgumentToEchoCommandSucceedsAndResponds() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("ECHO", new PingCommand(), arity -> arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("ECHO", "One"));

      assertEquals(new BulkString("One".getBytes(StandardCharsets.UTF_8)),
                   result);
    }

    @Test
    @DisplayName("Dispatch of one multibyte UTF-8 argument to the echo "
                 + "command succeeds and responds")
    void
    dispatchOfOneUtf8ArgumentToEchoCommandSucceedsAndResponds() {
      CommandRegistry registry = new CommandRegistry();
      registry.register("ECHO", new PingCommand(),
                        arity -> arity == 0 || arity == 1);
      ExecutionContext context = new ExecutionContext();
      CommandDispatcher dispatcher = new CommandDispatcher(registry, context);

      RespValue result = dispatcher.dispatch(request("ECHO", "Привет"));

      assertEquals(new BulkString("Привет".getBytes(StandardCharsets.UTF_8)),
                   result);
    }
  }
}
