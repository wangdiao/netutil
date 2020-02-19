package com.wangdiao;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles a server-side channel.
 */
@Slf4j
public class P2PServerHandler extends ChannelInboundHandlerAdapter {

    private P2PContext p2PContext;

    public P2PServerHandler(P2PContext p2PContext) {
        this.p2PContext = p2PContext;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        p2PContext.getContext().add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        p2PContext.getContext().remove(ctx);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            p2PContext.getPeerContext().forEach(x -> x.writeAndFlush(in));
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