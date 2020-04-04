package com.wangdiao.client;

import com.wangdiao.server.UdpPeerContextHandler;
import com.wangdiao.udp.UdpClientMessageHandle;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpQueryClient implements Runnable {

    private String name;
    private String discoverHost;
    private int discoverPort;
    private UdpPeerContextHandler udpPeerContextHandler;

    public UdpQueryClient(String name, String discoverHost, int discoverPort, UdpPeerContextHandler udpPeerContextHandler) {
        this.name = name;
        this.discoverHost = discoverHost;
        this.discoverPort = discoverPort;
        this.udpPeerContextHandler = udpPeerContextHandler;
    }

    public void run() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final UdpClientMessageHandle udpClientMessageHandle = new UdpClientMessageHandle();
        final UdpQueryClientHandler udpQueryClientHandler = new UdpQueryClientHandler(name, discoverHost, discoverPort, udpClientMessageHandle);

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(udpQueryClientHandler, udpClientMessageHandle,
                                    new ObjectEncoder(), new ObjectDecoder(ClassResolvers.weakCachingResolver(null)), udpPeerContextHandler);
                        }
                    });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(0).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("run failed.", e);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
