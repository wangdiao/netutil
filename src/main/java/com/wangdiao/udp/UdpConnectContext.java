package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpConnectContext implements Runnable {
    private final long connectId;
    private final ChannelHandlerContext ctx;
    private Queue<UdpPacket> receivedPacket = new ArrayDeque<>();
    private Map<Integer, UdpPacket> packetMap = new ConcurrentHashMap<>();
    private BlockingQueue<Integer> sendQueue = new ArrayBlockingQueue<>(1000);
    private Map<Integer, UdpPacket> sendMap = new ConcurrentHashMap<>();
    private Map<Integer, CompletableFuture<UdpHeader>> sendFutureMap = new ConcurrentHashMap<>();
    private Queue<Integer> sendingQueue = new ArrayDeque<>();
    private int receivedPackageNumber;
    private long receiveTime = System.currentTimeMillis();
    private long sendTime = System.currentTimeMillis();
    private long sendAckTime = System.currentTimeMillis();
    private volatile int status = NEW;
    private volatile boolean running = false;
    public static final int NEW = 0;
    public static final int STARTED = 1;
    public static final int CLOSING = 2;
    public static final int CLOSED = 3;
    public final ExecutorService executorService;
    private ContextListener listener;
    private AtomicInteger packetNumber = new AtomicInteger();

    public UdpConnectContext(long connectId, ChannelHandlerContext ctx, ExecutorService executorService, ContextListener listener) {
        this.connectId = connectId;
        this.ctx = ctx;
        this.executorService = executorService;
        this.listener = listener;
    }

    public void receive(UdpPacket udpPacket) {
        this.sendAck(udpPacket);
        int nextPn = receivedPackageNumber + 1;
        if (udpPacket.getUdpHeader().getPacketNumber() == nextPn) {
            this.receive0(udpPacket);
        } else if (udpPacket.getUdpHeader().getPacketNumber() > nextPn) {
            this.packetMap.put(udpPacket.getUdpHeader().getPacketNumber(), udpPacket);
            receive();
        }
        //小于nextPn的情况直接忽略
    }

    public void sendAck(UdpPacket udpPacket) {
        UdpPacket ackPacket = newUdpPacket(true, ControlMessageType.ACK, null, udpPacket.getSender());
        this.sendDirect(ackPacket);
    }

    public CompletableFuture<UdpHeader> send(UdpPacket udpPacket) {
        if (!this.isClosing()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<UdpHeader> future = new CompletableFuture<>();
        sendQueue.offer(udpPacket.getUdpHeader().getPacketNumber());
        sendMap.put(udpPacket.getUdpHeader().getPacketNumber(), udpPacket);
        sendFutureMap.put(udpPacket.getUdpHeader().getPacketNumber(), future);
        this.fireStart();
        return future;
    }

    private void receive() {
        for (int i = receivedPackageNumber; ; i++) {
            if (packetMap.containsKey(i)) {
                UdpPacket udpPacket = packetMap.remove(i);
                this.receive0(udpPacket);
            } else {
                break;
            }
        }
        receiveTime = System.currentTimeMillis();
    }

    private void receive0(UdpPacket udpPacket) {
        receivedPacket.offer(udpPacket);
        receivedPackageNumber++;
        this.fireStart();
    }

    private boolean isActive() {
        long now = System.currentTimeMillis();
        if (now - receiveTime > 1000L && !packetMap.isEmpty()) {
            //出现长时间丢包未收到
            return false;
        }
        //出现长时间没有ACK
        return sendTime - sendAckTime <= 1000L;
    }

    public void closing() {
        if (this.status < CLOSING) {
            this.status = CLOSING;
        }
    }

    public void close() {
        this.status = CLOSED;
    }

    public void ack(UdpPacket udpPacket) {
        Collection<Integer> ackPackets = Ack.getAckPackets(udpPacket.getByteBuf());
        sendingQueue.removeAll(ackPackets);
        for (Integer i : ackPackets) {
            UdpPacket packet = sendMap.remove(i);
            CompletableFuture<UdpHeader> future = sendFutureMap.remove(i);
            future.complete(udpPacket.getUdpHeader());
            packet.release();
        }
        ReferenceCountUtil.release(udpPacket);
        this.sendAckTime = System.currentTimeMillis();
        this.fireStart();
    }

    public boolean isClose() {
        return this.status == CLOSED;
    }

    public boolean isClosing() {
        return this.status >= CLOSING;
    }

    public boolean startSend() {
        for (int i = 0; i < 100; i++) {
            Integer pn = sendQueue.poll();
            if (pn == null) {
                return true;
            }
            this.send0(pn);
            if (this.isClose()) {
                return true;
            }
        }
        return false;
    }

    public boolean startReSend() {
        if (!this.sendingQueue.isEmpty()) {
            for (int i = 0; i < 100; i++) {
                Integer pn = this.sendingQueue.poll();
                if (pn == null) {
                    return true;
                }
                this.send0(pn);
                if (this.isClose()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean startRead() {
        for (int i = 0; i < 100; i++) {
            UdpPacket udpPacket = receivedPacket.poll();
            if (udpPacket == null) {
                return true;
            }
            if (udpPacket.getUdpHeader().getType() == ControlMessageType.CLOSE) {
                this.closing();
            }
            this.listener.onRead(udpPacket.getByteBuf());
            if (this.isClose()) {
                return true;
            }
        }
        return false;
    }


    private void send0(Integer pn) {
        long sendTime = System.currentTimeMillis();
        UdpPacket udpPacket = sendMap.get(pn);
        if (udpPacket.getSendTime() > 0L && sendTime - udpPacket.getSendTime() < 20) {
            //重发的包未超时不再发送
            return;
        }
        this.sendDirect(udpPacket);
        this.sendingQueue.offer(pn);
        this.sendTime = sendTime;
    }

    private void sendDirect(UdpPacket udpPacket) {
        ByteBuf buffer = ctx.alloc().buffer();
        udpPacket.write(buffer);
        udpPacket.setSendTime(sendTime);
        DatagramPacket data = new DatagramPacket(buffer, udpPacket.getRecipient());
        this.ctx.writeAndFlush(data);
    }

    private void fireStart() {
        if (!this.isClose() && !this.running) {
            executorService.submit(this);
        }
    }


    @Override
    public void run() {
        boolean finish = false;
        try {
            this.running = true;
            //待发送队列
            finish = this.startSend();
            if (this.isClose()) {
                return;
            }
            //未ACK队列
            finish = finish && this.startReSend();
            if (this.isClose()) {
                return;
            }
            finish = finish && this.startRead();
            //检查中断的连接
            if (!this.isActive()) {
                this.close();
            }
        } catch (Throwable e) {
            log.error("run failed.", e);
        } finally {
            this.running = false;
            if (!finish) {
                this.fireStart();
            } else if (this.isClosing()) {
                this.close();
            }
        }
    }

    public int nextPacketNumber() {
        return packetNumber.incrementAndGet();
    }

    public void start() {
        this.status = STARTED;
    }

    public UdpPacket newUdpPacket(boolean control, ControlMessageType type, ByteBuf byteBuf, InetSocketAddress recipient) {
        int pn = this.nextPacketNumber();
        return new UdpPacket(control, type, pn, connectId, byteBuf, recipient);
    }
}
