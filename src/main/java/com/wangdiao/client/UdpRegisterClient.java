package com.wangdiao.client;

import com.wangdiao.udp.UdpServerContext;
import com.wangdiao.udp.UdpServerMessageHandle;
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

/**
 * @author wangdiao
 * UDP注册客户端
 */
public class UdpRegisterClient {

    private String name;
    private String discoverHost;
    private int discoverPort;
    private String localHost;
    private int localPort;

    public UdpRegisterClient(String name, String discoverHost, int discoverPort, String localHost, int localPort) {
        this.name = name;
        this.discoverHost = discoverHost;
        this.discoverPort = discoverPort;
        this.localHost = localHost;
        this.localPort = localPort;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        UdpServerContext udpContext = new UdpServerContext();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new UdpRegisterClientHandler(name, discoverHost, discoverPort), new UdpServerMessageHandle(udpContext),
                                    new ObjectEncoder(), new ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                                    new RegisterClientHandler(localHost, localPort, workerGroup));
                        }
                    });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(0).sync();

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        UdpRegisterClient client = new UdpRegisterClient("test1", "localhost", 9998, "localhost", 80);
        client.run();
    }
}
