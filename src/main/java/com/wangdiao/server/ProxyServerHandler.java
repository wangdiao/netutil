package com.wangdiao.server;

import com.wangdiao.model.TransferData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author wangdiao
 */
public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private PeerContext peerContext;

    public ProxyServerHandler(PeerContext peerContext) {
        this.peerContext = peerContext;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        this.peerContext.addClientChannelContext(ctx);
        TransferData data = new TransferData(ctx.channel().remoteAddress(), null, TransferData.ACTIVE);
        peerContext.writeToRegister(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        peerContext.removeClientChannelContext(ctx);
        TransferData data = new TransferData(ctx.channel().remoteAddress(), null, TransferData.INACTIVE);
        peerContext.writeToRegister(data);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        try {
            byte[] bytes = ByteBufUtil.getBytes(in);
            TransferData data = new TransferData(ctx.channel().remoteAddress(), bytes, TransferData.READ);
            peerContext.writeToRegister(data);
        } finally {
            ReferenceCountUtil.release(in);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
