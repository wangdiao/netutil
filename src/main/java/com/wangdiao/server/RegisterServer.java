package com.wangdiao.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangdiao
 */
@Slf4j
public class RegisterServer {

    private int port;
    private ChannelFuture channelFuture;

    public RegisterServer(int port) {
        this.port = port;
    }

    public void start(PeerContext peerContext, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                                new RegisterServerHandler(peerContext));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        this.channelFuture = b.bind(port);
    }

    public void stop() {
        if (this.channelFuture != null) {
            this.channelFuture.addListeners(ChannelFutureListener.CLOSE);
        }
    }
}
