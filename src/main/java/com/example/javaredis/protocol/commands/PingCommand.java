package com.example.javaredis.protocol.commands;

import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespValue;
import com.example.javaredis.protocol.resp.values.SimpleString;
import java.util.List;

public final class PingCommand implements Command {
  @Override
  public RespValue execute(ExecutionContext ctx, List<BulkString> args) {
    if (args.isEmpty()) {
      return SimpleString.PONG;
    }
    return new BulkString(args.get(0).value());
  }
}
