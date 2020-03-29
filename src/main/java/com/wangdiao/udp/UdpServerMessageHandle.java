package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpServerMessageHandle extends ChannelInboundHandlerAdapter {
    private UdpServerContext udpContext;

    public UdpServerMessageHandle(UdpServerContext udpContext) {
        this.udpContext = udpContext;
    }

    public ContextListener contextListener = new ContextListener() {
        @Override
        public void onActive() {

        }

        @Override
        public void onRead(ByteBuf buf) {
            ReferenceCountUtil.release(buf);
        }
    };

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("read {}", msg);
        DatagramPacket packet = (DatagramPacket) msg;
        InetSocketAddress recipient = packet.recipient();
        InetSocketAddress sender = packet.sender();
        UdpPacket udpPacket = new UdpPacket(recipient, sender, packet.content());
        UdpHeader udpHeader = udpPacket.getUdpHeader();
        if (udpHeader.isControl()) {
            switch (udpHeader.getType()) {
                //关闭包在udpContext里处理，因要保证之前的包已经全部收到
                case CREATE:
                    udpContext.createConnectChannel(ctx, udpPacket, contextListener);
                    return;
                case CLOSE:
                    udpContext.passiveCloseChannel(udpPacket.getUdpHeader());
                    ReferenceCountUtil.release(udpContext);
                    return;
                case ACK:
                    udpContext.ack(udpPacket);
            }
        } else {
            udpContext.receive(udpPacket);
        }
    }


}