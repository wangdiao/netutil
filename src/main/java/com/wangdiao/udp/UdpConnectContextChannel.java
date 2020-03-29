package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpConnectContextChannel implements Runnable {
    private long connectId;
    private final ChannelHandlerContext ctx;
    private Queue<UdpPacket> receivedPacket = new ArrayDeque<>();
    private Map<Integer, UdpPacket> packetMap = new ConcurrentHashMap<>();
    private BlockingQueue<Integer> sendQueue = new ArrayBlockingQueue<>(4096);
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
    public static final int ACTIVE = 1;
    public static final int CLOSING = 2;
    public static final int CLOSED = 3;
    public final ExecutorService executorService;
    private ContextListener listener;
    @Getter
    private InetSocketAddress peerSocketAddress;
    private AtomicInteger packetNumber = new AtomicInteger();
    private BlockingQueue<Integer> ackQueue = new ArrayBlockingQueue<>(4096);

    public UdpConnectContextChannel(long connectId, ChannelHandlerContext ctx, ExecutorService executorService,
                                    InetSocketAddress peerSocketAddress, ContextListener listener) {
        this.connectId = connectId;
        this.ctx = ctx;
        this.executorService = executorService;
        this.peerSocketAddress = peerSocketAddress;
        this.listener = listener;
    }

    public static UdpConnectContextChannel createClient(ChannelHandlerContext ctx, ExecutorService executorService,
                                                        InetSocketAddress peerSocketAddress, ContextListener listener) {
        UdpPacket packet = new UdpPacket(true, ControlMessageType.CREATE, 0, 0, null, peerSocketAddress);
        sendDirect(ctx, packet);
        return new UdpConnectContextChannel(0L, ctx, executorService, peerSocketAddress, listener);
    }

    /**
     * 关闭连接
     */
    public void closeChannel() {
        UdpPacket udpPacket = this.newUdpPacket(true, ControlMessageType.CLOSE, null, peerSocketAddress);
        this.send(udpPacket).thenAccept(x -> this.close());
        this.closing();
    }

    public void receive(UdpPacket udpPacket) throws InterruptedException {
        this.sendAck(udpPacket.getUdpHeader().getPacketNumber());
        int nextPn = receivedPackageNumber + 1;
        if (udpPacket.getUdpHeader().getPacketNumber() == nextPn) {
            this.receive0(udpPacket);
        } else if (udpPacket.getUdpHeader().getPacketNumber() > nextPn) {
            this.packetMap.put(udpPacket.getUdpHeader().getPacketNumber(), udpPacket);
            receive();
        }
        //小于nextPn的情况直接忽略
    }

    public void sendAckDirect(int pn) {
        ByteBuf byteBuf = this.ctx.alloc().buffer();
        byteBuf.writeMedium(pn);
        byteBuf.writeByte(1);
        UdpPacket udpPacket = new UdpPacket(true, ControlMessageType.ACK, 0, connectId,
                byteBuf, peerSocketAddress);
        sendDirect(this.ctx, udpPacket);
    }

    public CompletableFuture<UdpHeader> send(UdpPacket udpPacket) {
        if (this.isClosing()) {
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
        this.listener.onClose();
    }

    public void ack(UdpPacket udpPacket) {
        Collection<Integer> ackPackets = Ack.getAckPackets(udpPacket.getByteBuf());
        if (this.status < ACTIVE) {
            if (ackPackets.contains(0)) {
                this.connectId = udpPacket.getUdpHeader().getConnectId();
                this.fireActive();
                ReferenceCountUtil.release(udpPacket);
                return;
            }
        }
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
            try {
                this.listener.onRead(udpPacket.getByteBuf().retain());
                if (this.isClose()) {
                    return true;
                }
            } finally {
                ReferenceCountUtil.release(udpPacket);
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
        sendDirect(this.ctx, udpPacket);
        udpPacket.setSendTime(sendTime);
        this.sendingQueue.offer(pn);
        this.sendTime = sendTime;
    }

    private static void sendDirect(ChannelHandlerContext ctx, UdpPacket udpPacket) {
        ByteBuf buffer = ctx.alloc().buffer();
        udpPacket.write(buffer);
        DatagramPacket data = new DatagramPacket(buffer, udpPacket.getRecipient());
        ctx.writeAndFlush(data);
    }

    private void fireStart() {
        if (!this.isClose() && !this.running) {
            executorService.submit(this);
        }
    }


    @Override
    public void run() {
        if (this.status < ACTIVE) {
            return;
        }
        boolean finish = false;
        try {
            this.running = true;
            finish = this.startSendAck();
            //待发送队列
            finish = finish && this.startSend();
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

    private boolean startSendAck() {
        if (ackQueue.isEmpty()) {
            return true;
        }
        List<Integer> list = new ArrayList<>();
        ackQueue.drainTo(list);
        while (!list.isEmpty()) {
            ByteBuf byteBuf = this.ctx.alloc().buffer(128, UdpPacket.MAX_PACKET);
            list = Ack.fillUdpPacketBody(byteBuf, list);
            UdpPacket udpPacket = new UdpPacket(true, ControlMessageType.ACK, 0, connectId, byteBuf, peerSocketAddress);
            sendDirect(this.ctx, udpPacket);
        }
        return false;
    }

    public int nextPacketNumber() {
        return packetNumber.incrementAndGet();
    }

    public UdpPacket newUdpPacket(boolean control, ControlMessageType type, ByteBuf byteBuf, InetSocketAddress recipient) {
        int pn = this.nextPacketNumber();
        return new UdpPacket(control, type, pn, connectId, byteBuf, recipient);
    }

    public void sendAck(int pn) throws InterruptedException {
        this.ackQueue.put(pn);
    }

    public void fireActive() {
        this.status = ACTIVE;
        this.fireStart();
        this.listener.onActive();
    }

    public void passiveCloseChannel(int pn) throws InterruptedException {
        this.sendAck(pn);
        this.closing();
    }
}
