package com.wangdiao.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpMessageHandle extends ChannelInboundHandlerAdapter {
    private UdpContext udpContext;

    public UdpMessageHandle(UdpContext udpContext) {
        this.udpContext = udpContext;
    }

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
                    udpContext.createConnect(ctx, udpPacket, buf -> {
                        //TODO 处理收到的消息
                    });
                    return;
                case ACK:
                    udpContext.ack(udpPacket);
                    return;

            }
        }
        udpContext.receive(udpPacket);
    }

}
