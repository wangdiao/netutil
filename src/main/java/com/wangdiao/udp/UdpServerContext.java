package com.wangdiao.udp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author wangdiao
 */
public class UdpServerContext {
    public static final ExecutorService executorService = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("udp-server-context-pool-%d").build());

    private Map<Long, UdpConnectContextChannel> connectContextMap = new ConcurrentHashMap<>();
    private AtomicLong atomicConnectId = new AtomicLong();

    public void createConnectChannel(ChannelHandlerContext ctx, UdpPacket packet, ContextListener listener) {
        long connectId = atomicConnectId.incrementAndGet();
        UdpConnectContextChannel connectContext = new UdpConnectContextChannel(connectId, ctx, executorService, packet.getSender(), listener);
        connectContextMap.put(connectId, connectContext);
        connectContext.sendAckDirect(packet.getUdpHeader().getPacketNumber());
        connectContext.fireActive(packet.getCtx());
    }

    public void passiveCloseChannel(UdpHeader udpHeader) throws InterruptedException {
        connectContextMap.remove(udpHeader.getConnectId()).passiveCloseChannel(udpHeader.getPacketNumber());
    }

    public void closeChannel(long connectId) {
        connectContextMap.remove(connectId).closeChannel();
    }

    public void ack(UdpPacket udpPacket) {
        connectContextMap.get(udpPacket.getUdpHeader().getConnectId()).ack(udpPacket);
    }

    public void receive(UdpPacket udpPacket) throws InterruptedException {
        connectContextMap.get(udpPacket.getUdpHeader().getConnectId()).receive(udpPacket);
    }

    public CompletableFuture<Void> send(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        Long connectId = (Long) ctx.channel().attr(AttributeKey.newInstance("connectId")).get();
        return connectContextMap.get(connectId).send(ctx, byteBuf);
    }
}
