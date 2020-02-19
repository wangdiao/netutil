package com.wangdiao;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles a server-side channel.
 */
@Slf4j
public class PassiveServerHandler extends ChannelInboundHandlerAdapter {


    private List<ChannelHandlerContext> contexts;
    private RegisterServer registerServer;

    public PassiveServerHandler(List<ChannelHandlerContext> contexts, RegisterServer registerServer) {
        this.contexts = contexts;
        this.registerServer = registerServer;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        contexts.add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        contexts.remove(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            registerServer.getContexts().forEach(x -> x.writeAndFlush(in));
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}