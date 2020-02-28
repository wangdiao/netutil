package com.wangdiao.server;

import com.wangdiao.model.ControlCmd;
import com.wangdiao.model.TransferData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangdiao
 */
@Slf4j
public class PeerContext {
    private ChannelHandlerContext registerChannelContext;
    private Map<SocketAddress, ChannelHandlerContext> clientChannelContexts = new HashMap<>();
    private RegisterServer registerServer;
    private ProxyServer proxyServer;

    public PeerContext(ControlCmd controlCmd) {
        this.registerServer = new RegisterServer(controlCmd.getRegisterPort());
        this.proxyServer = new ProxyServer(controlCmd.getProxyPort());
    }

    public void setRegisterChannelContext(ChannelHandlerContext registerChannelContext) {
        this.registerChannelContext = registerChannelContext;
    }

    public void addClientChannelContext(ChannelHandlerContext ctx) {
        this.clientChannelContexts.put(ctx.channel().remoteAddress(), ctx);
    }

    public void removeClientChannelContext(ChannelHandlerContext ctx) {
        this.clientChannelContexts.remove(ctx.channel().remoteAddress());
    }

    public void start(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        registerServer.start(this, bossGroup, workerGroup);
        proxyServer.start(this, bossGroup, workerGroup);
    }

    public void writeToClient(TransferData data) {
        ChannelHandlerContext ctx = clientChannelContexts.get(data.getAddress());
        switch (data.getOperate()) {
            case TransferData.ACTIVE:
                log.info("active {}", data);
                break;
            case TransferData.INACTIVE:
                log.info("inactive {}", data);
                clientChannelContexts.remove(data.getAddress());
                ctx.close();
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
        }
    }

    public void stop() {
        registerServer.stop();
        proxyServer.stop();
    }
}
