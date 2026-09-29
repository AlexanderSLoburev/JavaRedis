package com.example.javaredis.protocol.commands;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

public final class CommandRegistry {

  public record CommandSpec(Command command, IntPredicate arityValidator) {
    public boolean arityMatches(int arity) {
      return arityValidator.test(arity);
    }
  }

  private final Map<String, CommandSpec> commands = new HashMap<>();

  public Optional<CommandSpec> lookup(String name) {
    return Optional.ofNullable(commands.get(name.toUpperCase(Locale.ROOT)));
  }

  public void register(String commandName, Command command,
                       IntPredicate arityChecker) {
    commands.put(commandName, new CommandSpec(command, arityChecker));
  }
}
