package com.wangdiao.server;

import com.wangdiao.client.UdpQueryClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UdpProxyServer implements Runnable {
    private int port;
    private String name;
    private String discoverHost;
    private int discoverPort;

    public UdpProxyServer(int port, String name, String discoverHost, int discoverPort) {
        this.port = port;
        this.name = name;
        this.discoverHost = discoverHost;
        this.discoverPort = discoverPort;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        UdpPeerContextHandler udpPeerContextHandler = new UdpPeerContextHandler();
        UdpQueryClient udpQueryClient = new UdpQueryClient(name, discoverHost, discoverPort, udpPeerContextHandler);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ObjectEncoder(),
                                    new UdpProxyServerHandler(udpPeerContextHandler));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            bossGroup.execute(udpQueryClient);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("run failed", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        UdpProxyServer udpProxyServer = new UdpProxyServer(10080, "test1", "localhost", 9998);
        udpProxyServer.run();
    }
}
