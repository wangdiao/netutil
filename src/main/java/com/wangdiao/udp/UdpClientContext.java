package com.wangdiao.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class UdpContext {
    public static final ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("udp-context-pool-%d").build());
    private Map<Long, UdpConnectContext> connectContextMap = new ConcurrentHashMap<>();
    private AtomicLong atomicConnectId = new AtomicLong();

    public void createConnect(ChannelHandlerContext ctx, InetSocketAddress sender, ContextListener listener) {
        long connectId = atomicConnectId.incrementAndGet();
        UdpConnectContext connectContext = new UdpConnectContext(connectId, ctx, executorService, listener);
        connectContextMap.put(connectId, connectContext);
        UdpPacket resPacket = connectContext.newUdpPacket(true, ControlMessageType.CREATE_RSP,
                null, sender);
        connectContext.send(resPacket, isSend -> connectContext.start());
    }

    public void closeConnect(long connectId, InetSocketAddress recipient) {
        UdpConnectContext connectContext = connectContextMap.get(connectId);
        UdpPacket udpPacket = connectContext.newUdpPacket(true, ControlMessageType.CLOSE, null, recipient);
        connectContext.send(udpPacket, x -> connectContext.close());
        connectContext.closing();
        connectContextMap.remove(connectId);
    }

    public void ack(UdpPacket udpPacket) {
        connectContextMap.get(udpPacket.getConnectId()).ack(udpPacket);
    }

    public void receive(UdpPacket udpPacket) {
        UdpConnectContext connectContext = connectContextMap.get(udpPacket.getConnectId());
        UdpPacket ackPacket = connectContext.newUdpPacket(true, ControlMessageType.ACK,
                null, udpPacket.getSender());
        connectContext.send(ackPacket, null);
        connectContext.receive(udpPacket);
    }

    public Future<Boolean> send(UdpPacket udpPacket, Consumer<Boolean> consumer) {
        UdpConnectContext connectContext = connectContextMap.get(udpPacket.getConnectId());
        return connectContext.send(udpPacket, consumer);
    }
}
