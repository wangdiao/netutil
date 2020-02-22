package com.wangdiao.server;

import com.wangdiao.model.Result;
import com.wangdiao.model.ControlCmd;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangdiao
 */
@Slf4j
public class ControlServerHandler extends ChannelInboundHandlerAdapter {


    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private static P2PContext p2pContext = new P2PContext();

    public ControlServerHandler(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ControlCmd in = (ControlCmd) msg;
        Result result = new Result();
        result.setCode(0);
        try {
            p2pContext.applyCmd(in, bossGroup, workerGroup);
            result.setMessage("success " + in.toString());
        } catch (Throwable e) {
            log.error("addItem failed.", e);
            result.setMessage(e.getMessage());
        }
        ctx.writeAndFlush(result);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
