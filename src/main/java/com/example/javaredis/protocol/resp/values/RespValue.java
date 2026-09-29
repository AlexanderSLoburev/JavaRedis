package com.example.javaredis.protocol.resp.values;

public sealed interface RespValue permits BulkString, SimpleString, RespError,
    RespInteger, RespArray, RespNull {
}
