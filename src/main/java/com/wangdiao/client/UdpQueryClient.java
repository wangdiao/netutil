package com.wangdiao.client;

import com.wangdiao.common.InputStringTestHandler;
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

/**
 * @author wangdiao
 */
public class UdpQueryClient {

    private String name;

    public UdpQueryClient(String name) {
        this.name = name;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final UdpClientMessageHandle udpClientMessageHandle = new UdpClientMessageHandle();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new UdpQueryClientHandler(name, udpClientMessageHandle), udpClientMessageHandle,
                                    new ObjectEncoder(), new ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                                    new InputStringTestHandler());
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
        UdpQueryClient client = new UdpQueryClient("test1");
        client.run();
    }
}
