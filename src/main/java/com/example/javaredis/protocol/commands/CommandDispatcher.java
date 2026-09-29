package com.example.javaredis.protocol.commands;

import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.ProtocolException;
import com.example.javaredis.protocol.commands.CommandRegistry.CommandSpec;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespArray;
import com.example.javaredis.protocol.resp.values.RespError;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public final class CommandDispatcher {
  private final CommandRegistry registry;
  private final ExecutionContext context;

  private BulkString toBulk(RespValue respValue) {
    return switch (respValue) {
      case BulkString bulkString -> bulkString;
      case SimpleString simpleString ->
        new BulkString(simpleString.value().getBytes(StandardCharsets.UTF_8));
      default ->
        throw new ProtocolException("Expected a string, got " +
                                    respValue.getClass());
    };
  }

  public CommandDispatcher(CommandRegistry registry, ExecutionContext context) {
    Objects.requireNonNull(registry);
    Objects.requireNonNull(context);
    this.registry = registry;
    this.context = context;
  }

  public RespValue dispatch(RespArray request) {
    List<RespValue> respValues = request.elements();

    if (respValues.isEmpty()) {
      return RespError.emptyCommand();
    }

    String commandName = toBulk(respValues.get(0)).asText();

    List<BulkString> args =
        respValues.stream().skip(1).map(this::toBulk).toList();

    Optional<CommandSpec> commandSpec = registry.lookup(commandName);

    if (commandSpec.isEmpty()) {
      return RespError.unknownCommand(commandName);
    }

    if (!commandSpec.get().arityMatches(args.size())) {
      return RespError.wrongArgCount(commandName);
    }

    return commandSpec.get().command().execute(context, args);
  }
}
