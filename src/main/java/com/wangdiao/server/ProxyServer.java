package com.wangdiao.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangdiao
 */
@Slf4j
public class ProxyServer {

    private int port;
    private ChannelFuture channelFuture;

    public ProxyServer(int port) {
        this.port = port;
    }

    public void start(PeerContext peerContext, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new ProxyServerHandler(peerContext));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        this.channelFuture = b.bind(port);
    }

    public void stop() {
        if (this.channelFuture != null) {
            this.channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
