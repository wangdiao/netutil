package com.wangdiao.server;

import com.wangdiao.model.TransferData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangdiao
 */
@Slf4j
@ChannelHandler.Sharable
public class UdpPeerContextHandler extends ChannelInboundHandlerAdapter {
    /**
     * 到注册端的连接，UDP
     */
    private ChannelHandlerContext registerChannelContext;
    /**
     * 客户端到proxy的连接Map，为TCP连接
     */
    private Map<SocketAddress, ChannelHandlerContext> clientChannelContexts = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.registerChannelContext = ctx;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.registerChannelContext = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.writeToClient((TransferData) msg);
    }

    public void addClientChannelContext(ChannelHandlerContext ctx) {
        this.clientChannelContexts.put(ctx.channel().remoteAddress(), ctx);
    }

    public void removeClientChannelContext(ChannelHandlerContext ctx) {
        this.clientChannelContexts.remove(ctx.channel().remoteAddress());
    }

    public void writeToClient(TransferData data) {
        ChannelHandlerContext ctx = clientChannelContexts.get(data.getAddress());
        switch (data.getOperate()) {
            case TransferData.ACTIVE:
                log.info("active {}", data);
                break;
            case TransferData.INACTIVE:
                log.info("inactive {} ctx={}", data, ctx);
                clientChannelContexts.remove(data.getAddress());
                if (ctx != null) {
                    ctx.close();
                }
                break;
            case TransferData.READ:
                ByteBuf buffer = ctx.alloc().buffer();
                buffer.writeBytes(data.getBytes());
                ctx.writeAndFlush(buffer);
                break;
            default:
                //non
        }
    }

    public void writeToRegister(TransferData data) {
        if (registerChannelContext != null) {
            registerChannelContext.writeAndFlush(data);
        } else {
            log.error("writeToRegister but ctx is null. data={}", data);
        }
    }
}
