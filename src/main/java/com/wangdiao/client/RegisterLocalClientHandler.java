package com.wangdiao.client;

import com.wangdiao.model.TransferData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * @author wangdiao
 */
public class RegisterLocalClientHandler extends ChannelInboundHandlerAdapter {
    private LocalContext localContext;
    private ChannelHandlerContext registerCtx;

    public RegisterLocalClientHandler(LocalContext localContext, ChannelHandlerContext registerCtx) {
        this.localContext = localContext;
        this.registerCtx = registerCtx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.localContext.getChannelHandlerContextFuture().complete(ctx);
        TransferData data = new TransferData(localContext.getSocketAddress(), null, TransferData.ACTIVE);
        registerCtx.writeAndFlush(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        TransferData data = new TransferData(localContext.getSocketAddress(), null, TransferData.INACTIVE);
        registerCtx.writeAndFlush(data);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        try {
            TransferData data = new TransferData(localContext.getSocketAddress(), ByteBufUtil.getBytes(in), TransferData.READ);
            registerCtx.writeAndFlush(data);
        } finally {
            ReferenceCountUtil.release(in);
        }
    }
}
