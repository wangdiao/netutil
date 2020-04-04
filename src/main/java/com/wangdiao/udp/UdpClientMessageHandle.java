package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author wangdiao
 */
@Slf4j
@ChannelHandler.Sharable
public class UdpClientMessageHandle extends ChannelDuplexHandler {
    private UdpClientContext udpContext;
    @Setter
    private InetSocketAddress peerSocketAddress;
    public ContextListener contextListener = ContextListener.DEFAULT;

    public UdpClientMessageHandle() {
    }

    public UdpClientMessageHandle(InetSocketAddress peerSocketAddress) {
        this.peerSocketAddress = peerSocketAddress;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        assert peerSocketAddress != null;
        udpContext = UdpClientContext.createConnectChannel(ctx, peerSocketAddress, contextListener);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("read {}", msg);
        DatagramPacket packet = (DatagramPacket) msg;
        InetSocketAddress recipient = packet.recipient();
        InetSocketAddress sender = packet.sender();
        UdpPacket udpPacket = new UdpPacket(recipient, sender, packet.content());
        udpPacket.setCtx(ctx);
        UdpHeader udpHeader = udpPacket.getUdpHeader();
        if (udpHeader.isControl()) {
            switch (udpHeader.getType()) {
                //关闭包在udpContext里处理，因要保证之前的包已经全部收到
                case CREATE:
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


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        assert msg instanceof ByteBuf;
        ByteBuf buf = (ByteBuf) msg;
        udpContext.send(ctx, buf);
    }
}
