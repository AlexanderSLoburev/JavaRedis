package com.example.javaredis.app;

import com.example.javaredis.connection.ConnectionHandler;
import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.commands.CommandDispatcher;
import com.example.javaredis.protocol.commands.CommandRegistry;
import com.example.javaredis.protocol.commands.EchoCommand;
import com.example.javaredis.protocol.commands.PingCommand;
import com.example.javaredis.protocol.resp.RespSerializer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Application {
  private static final int DEFAULT_PORT = 6379;

  private static void handle(Socket socket, CommandDispatcher dispatcher,
                             RespSerializer serializer) {
    try (socket) {
      new ConnectionHandler(dispatcher, serializer).run(socket);
    } catch (Exception e) {
      // ignore network errors
    }
  }

  private static void registerCommands(CommandRegistry registry) {
    registry.register("PING", new PingCommand(),
                      arity -> arity == 0 || arity == 1);
    registry.register("ECHO", new EchoCommand(), arity -> arity == 1);
  }

  public static void main(String[] args) throws IOException {
    final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    final ExecutionContext context = new ExecutionContext();
    final CommandRegistry registry = new CommandRegistry();

    registerCommands(registry);

    final CommandDispatcher dispatcher =
        new CommandDispatcher(registry, context);
    final RespSerializer serializer = new RespSerializer();

    try (ServerSocket server = new ServerSocket(port)) {
      while (true) {
        Socket socket = server.accept();
        Thread.ofVirtual()
            .name("conn-", 0)
            .start(() -> handle(socket, dispatcher, serializer));
      }
    }
  }
}
