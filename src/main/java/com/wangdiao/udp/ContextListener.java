package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.EventListener;

public interface ContextListener extends EventListener {
    default void onActive(ChannelHandlerContext ctx) {
         ctx.fireChannelActive();
    }

    default void onRead(ChannelHandlerContext ctx, ByteBuf buf) {
        ctx.fireChannelRead(buf);
    }

    default void onClose(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
    }
}
