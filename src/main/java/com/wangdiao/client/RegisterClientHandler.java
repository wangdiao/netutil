package com.wangdiao.client;

import com.wangdiao.model.TransferData;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangdiao
 */
@Slf4j
public class RegisterClientHandler extends ChannelInboundHandlerAdapter {

    private String localHost;
    private int localPort;
    private EventLoopGroup workerGroup;
    private ChannelHandlerContext registerCtx;
    private Map<SocketAddress, LocalContext> localCtxMap = new ConcurrentHashMap<>();

    public RegisterClientHandler(String localHost, int localPort, EventLoopGroup workerGroup) {
        this.localHost = localHost;
        this.localPort = localPort;
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.registerCtx = ctx;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TransferData data = (TransferData) msg;
        LocalContext localContext;
        switch (data.getOperate()) {
            case TransferData.ACTIVE:
                localCtxMap.computeIfAbsent(data.getAddress(), this::startLocalClient);
                break;
            case TransferData.INACTIVE:
                localContext = localCtxMap.remove(data.getAddress());
                if(localContext!=null) {
                    localContext.getChannelHandlerContextFuture().get().close();
                }
                break;
            case TransferData.READ:
                localContext = localCtxMap.get(data.getAddress());
                ChannelHandlerContext context = localContext.getChannelHandlerContextFuture().get();
                ByteBuf buffer = context.alloc().buffer();
                buffer.writeBytes(data.getBytes());
                context.writeAndFlush(buffer);
                break;
            default:
                //non
        }

    }

    private LocalContext startLocalClient(SocketAddress socketAddress) {
        LocalContext localContext = new LocalContext(socketAddress);
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new RegisterLocalClientHandler(localContext, registerCtx));
            }
        });
        // Start the client.
        b.connect(localHost, localPort);
        return localContext;
    }
}
