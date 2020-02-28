package com.wangdiao.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UdpDiscoverServerHandle extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("read {}", msg);
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            int i = packet.content().readInt();
            ByteBuf buffer = ctx.alloc().buffer(4);
            buffer.writeInt(i + 1);
            DatagramPacket data = new DatagramPacket(buffer, packet.sender());
            ctx.writeAndFlush(data);
        } finally {
            ReferenceCountUtil.release(packet);
        }
    }

}
