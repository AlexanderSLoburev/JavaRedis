package com.example.javaredis.protocol.commands;

import com.example.javaredis.context.ExecutionContext;
import com.example.javaredis.protocol.resp.values.BulkString;
import com.example.javaredis.protocol.resp.values.RespValue;
import java.util.List;

public class EchoCommand implements Command {
  @Override
  public RespValue execute(ExecutionContext ctx, List<BulkString> args) {
    return args.get(0);
  }
}
