package com.wangdiao.server;

import com.wangdiao.model.DiscoverData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangdiao
 */
@Slf4j
public class UdpDiscoverServerHandle extends ChannelInboundHandlerAdapter {

    public static final int OP_REG = 1;
    public static final int OP_QUERY = 2;
    private Map<CharSequence, InetSocketAddress> registers = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("read {}", msg);
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            ByteBuf content = packet.content();
            CharSequence name = content.readCharSequence(content.readInt(), StandardCharsets.UTF_8);
            int op = content.readInt();
            InetSocketAddress sender = packet.sender();
            DiscoverData discoverData;
            if (op == OP_REG) {
                registers.put(name, sender);
                discoverData = new DiscoverData(name, sender);
            } else {
                InetSocketAddress socketAddress = registers.get(name);
                discoverData = new DiscoverData(name, socketAddress);
            }
            ByteBuf buffer = ctx.alloc().buffer();
            discoverData.write(buffer);
            DatagramPacket data = new DatagramPacket(buffer, sender);
            ctx.writeAndFlush(data);
        } finally {
            ReferenceCountUtil.release(packet);
        }
    }

}
