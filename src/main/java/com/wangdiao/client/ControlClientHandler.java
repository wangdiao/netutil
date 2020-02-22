package com.wangdiao.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author wangdiao
 */
@Slf4j
public class ControlClientHandler extends ChannelInboundHandlerAdapter {

    private Serializable msg;

    public ControlClientHandler(Serializable msg) {
        this.msg = msg;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(msg);
        log.info("write {}", msg);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.info("received {}", msg);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
