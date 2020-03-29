package com.wangdiao.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * @author wangdiao
 */
public class UdpClientContext {
    private UdpConnectContextChannel udpConnectContextChannel;

    public UdpClientContext(UdpConnectContextChannel udpConnectContextChannel) {
        this.udpConnectContextChannel = udpConnectContextChannel;
    }

    private static final ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("udp-context-pool-%d").build());

    public static UdpClientContext createConnectChannel(ChannelHandlerContext ctx, InetSocketAddress recipient,
                                                        ContextListener listener) {
        UdpConnectContextChannel udpConnectContextChannel = UdpConnectContextChannel.createClient(ctx, executorService, recipient, listener);
        return new UdpClientContext(udpConnectContextChannel);
    }

    public void passiveCloseChannel(UdpHeader udpHeader) throws InterruptedException {
        udpConnectContextChannel.passiveCloseChannel(udpHeader.getPacketNumber());
    }

    public void closeChannel() {
        udpConnectContextChannel.closeChannel();
    }

    public void ack(UdpPacket udpPacket) {
        udpConnectContextChannel.ack(udpPacket);
    }

    public void receive(UdpPacket udpPacket) throws InterruptedException {
        udpConnectContextChannel.receive(udpPacket);
    }

    public CompletableFuture<UdpHeader> send(UdpPacket udpPacket) {
        return udpConnectContextChannel.send(udpPacket);
    }
}
