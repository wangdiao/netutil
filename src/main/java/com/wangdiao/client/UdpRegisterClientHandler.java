package com.wangdiao.client;

import com.wangdiao.common.AppConstants;
import com.wangdiao.model.DiscoverData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Slf4j
@ChannelHandler.Sharable
public class UdpRegisterClientHandler extends ChannelInboundHandlerAdapter {
    private String name;
    private volatile int status = AppConstants.UDP_CLIENT_STATUS_INIT;

    public UdpRegisterClientHandler(String name) {
        this.name = name;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive {}", ctx);
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeInt(AppConstants.OP_REG);
        buffer.writeInt(ByteBufUtil.utf8Bytes(name));
        buffer.writeCharSequence(name, StandardCharsets.UTF_8);
        DatagramPacket datagramPacket = new DatagramPacket(buffer, new InetSocketAddress("localhost", 9998));
        ctx.writeAndFlush(datagramPacket);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (status == AppConstants.UDP_CLIENT_STATUS_INIT) {
            DatagramPacket packet = (DatagramPacket) msg;
            try {
                ByteBuf content = packet.content();
                DiscoverData discoverData = new DiscoverData();
                discoverData.read(content);
                log.info("init register success. data={}", discoverData);
            } finally {
                ReferenceCountUtil.release(packet);
            }
            status = AppConstants.UDP_CLIENT_STATUS_CREATED;
        } else if (status == AppConstants.UDP_CLIENT_STATUS_CREATED) {
            super.channelRead(ctx, msg);
        }

    }
}
