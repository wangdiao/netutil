package com.wangdiao.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * @author wangdiao
 */
public class UdpClientContext {
    private UdpConnectContext connectContext;

    private static final ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("udp-context-pool-%d").build());

    public UdpClientContext(UdpConnectContext connectContext) {
        this.connectContext = connectContext;
    }

    public static CompletableFuture<UdpClientContext> createConnect(ChannelHandlerContext ctx, InetSocketAddress sender,
                                                                    ContextListener listener) {
        UdpConnectContext preConnectContext = new UdpConnectContext(0L, ctx, executorService, listener);
        UdpPacket resPacket = preConnectContext.newUdpPacket(true, ControlMessageType.CREATE,
                null, sender);
        CompletableFuture<UdpHeader> send = preConnectContext.send(resPacket);
        return send.thenApply(header -> {
            UdpConnectContext cc = new UdpConnectContext(header.getConnectId(), ctx, executorService, listener);
            return new UdpClientContext(cc);
        });
    }

    public void closeConnect(InetSocketAddress recipient) {
        UdpPacket udpPacket = connectContext.newUdpPacket(true, ControlMessageType.CLOSE, null, recipient);
        connectContext.send(udpPacket).thenAccept(x -> connectContext.close());
        connectContext.closing();
    }

    public void ack(UdpPacket udpPacket) {
        connectContext.ack(udpPacket);
    }

    public void receive(UdpPacket udpPacket) {
        UdpPacket ackPacket = connectContext.newUdpPacket(true, ControlMessageType.ACK,
                null, udpPacket.getSender());
        connectContext.send(ackPacket);
        connectContext.receive(udpPacket);
    }

    public CompletableFuture<UdpHeader> send(UdpPacket udpPacket) {
        return connectContext.send(udpPacket);
    }
}
