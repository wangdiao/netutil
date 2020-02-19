package com.wangdiao;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Discards any incoming data.
 */
public class RegisterServer {

    private int port;
    private List<ChannelHandlerContext> contexts = new ArrayList<>();
    private PassiveServer passiveServer;

    public RegisterServer(int port) {
        this.port = port;
    }

    public void setPassiveServer(PassiveServer passiveServer) {
        this.passiveServer = passiveServer;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RegisterServerHandler(contexts, passiveServer));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public List<ChannelHandlerContext> getContexts() {
        return contexts;
    }

    public static void main(String[] args) throws Exception {
        int port = 9999;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        new RegisterServer(port).run();
    }
}