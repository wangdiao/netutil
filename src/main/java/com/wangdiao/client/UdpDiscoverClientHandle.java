package com.wangdiao.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class UdpDiscoverClientHandle extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.info("channelRegistered {}", ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive {}", ctx);
        ByteBuf buffer = ctx.alloc().buffer(4);
        buffer.writeInt(1);
        DatagramPacket datagramPacket = new DatagramPacket(buffer, new InetSocketAddress("localhost", 9998));
        ctx.writeAndFlush(datagramPacket);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channelRead {}", msg);
        DatagramPacket packet = (DatagramPacket) msg;
        log.info("channelRead {}", packet.content().readInt());
        ReferenceCountUtil.release(packet);

    }
}
