package com.wangdiao.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.ClassResolvers;

import java.util.List;

public class DatagramObjectDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final ClassResolver classResolver = ClassResolvers.weakCachingResolver(null);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

    }
}
