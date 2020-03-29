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

/**
 * @author wangdiao
 */
public class UdpRegisterClient {

    private String name;

    public UdpRegisterClient(String name) {
        this.name = name;
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
                            ch.pipeline().addLast(new UdpRegisterClientHandler(name), new UdpServerMessageHandle(udpContext));
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
        UdpRegisterClient client = new UdpRegisterClient("test1");
        client.run();
    }
}
