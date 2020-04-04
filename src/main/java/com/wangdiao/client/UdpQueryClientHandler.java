package com.wangdiao.client;

import com.wangdiao.common.AppConstants;
import com.wangdiao.model.DiscoverData;
import com.wangdiao.udp.UdpClientMessageHandle;
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
public class UdpQueryClientHandler extends ChannelInboundHandlerAdapter {

    private final String name;
    private InetSocketAddress discoverSocketAddress;
    private final UdpClientMessageHandle udpClientMessageHandle;

    private volatile int status = AppConstants.UDP_CLIENT_STATUS_INIT;

    public UdpQueryClientHandler(String name, String discoverHost, int discoverPort, UdpClientMessageHandle udpClientMessageHandle) {
        this.name = name;
        discoverSocketAddress = new InetSocketAddress(discoverHost, discoverPort);
        this.udpClientMessageHandle = udpClientMessageHandle;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive {}", ctx);
        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeInt(AppConstants.OP_QUERY);
        buffer.writeInt(ByteBufUtil.utf8Bytes(name));
        buffer.writeCharSequence(name, StandardCharsets.UTF_8);
        DatagramPacket datagramPacket = new DatagramPacket(buffer, discoverSocketAddress);
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
                log.info("init query success. data={}", discoverData);
                udpClientMessageHandle.setPeerSocketAddress(discoverData.getSocketAddress());
                super.channelActive(ctx);
            } finally {
                ReferenceCountUtil.release(packet);
            }
            status = AppConstants.UDP_CLIENT_STATUS_CREATED;
        } else if (status == AppConstants.UDP_CLIENT_STATUS_CREATED) {
            super.channelRead(ctx, msg);
        }
    }
}
