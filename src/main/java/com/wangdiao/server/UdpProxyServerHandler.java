package com.wangdiao.server;

import com.wangdiao.model.TransferData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class UdpProxyServerHandler extends ChannelInboundHandlerAdapter {
    private UdpPeerContextHandler udpPeerContextHandler;

    public UdpProxyServerHandler(UdpPeerContextHandler udpPeerContextHandler) {
        this.udpPeerContextHandler = udpPeerContextHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        udpPeerContextHandler.addClientChannelContext(ctx);
        TransferData data = new TransferData(ctx.channel().remoteAddress(), null, TransferData.ACTIVE);
        udpPeerContextHandler.writeToRegister(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        udpPeerContextHandler.removeClientChannelContext(ctx);
        TransferData data = new TransferData(ctx.channel().remoteAddress(), null, TransferData.INACTIVE);
        udpPeerContextHandler.writeToRegister(data);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        try {
            byte[] bytes = ByteBufUtil.getBytes(in);
            TransferData data = new TransferData(ctx.channel().remoteAddress(), bytes, TransferData.READ);
            udpPeerContextHandler.writeToRegister(data);
        } finally {
            ReferenceCountUtil.release(in);
        }
    }
}
