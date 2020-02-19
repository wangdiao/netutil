package com.wangdiao;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Handles a server-side channel.
 */
@Slf4j
public class RegisterServerHandler extends ChannelInboundHandlerAdapter {

    private List<ChannelHandlerContext> contexts;
    private PassiveServer passiveServer;

    public RegisterServerHandler(List<ChannelHandlerContext> contexts, PassiveServer passiveServer) {
        this.contexts = contexts;
        this.passiveServer = passiveServer;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        contexts.add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        contexts.remove(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            passiveServer.getContexts().forEach(x -> x.writeAndFlush(in));
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