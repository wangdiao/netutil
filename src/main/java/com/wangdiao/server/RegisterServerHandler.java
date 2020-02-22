package com.wangdiao.server;

import com.wangdiao.model.TransferData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles a server-side channel.
 *
 * @author wangdiao
 */
@Slf4j
public class RegisterServerHandler extends ChannelInboundHandlerAdapter {

    private PeerContext peerContext;

    public RegisterServerHandler(PeerContext peerContext) {
        this.peerContext = peerContext;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        peerContext.setRegisterChannelContext(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        peerContext.setRegisterChannelContext(null);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TransferData data = (TransferData) msg;
        peerContext.writeToClient(data);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

}