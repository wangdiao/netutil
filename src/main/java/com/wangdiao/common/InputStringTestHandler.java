package com.wangdiao.common;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Random;

@Slf4j
public class InputStringTestHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        StringBuilder sb = new StringBuilder("test:").append(System.currentTimeMillis()).append("\n");
        Random random = new Random();
        byte[] bytes = new byte[1024];
        for (int i = 0; i < 10; i++) {
            random.nextBytes(bytes);
            sb.append(Base64.getEncoder().encodeToString(bytes)).append("\n");
        }
        ctx.writeAndFlush(sb.toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("read {}", msg);
    }
}
