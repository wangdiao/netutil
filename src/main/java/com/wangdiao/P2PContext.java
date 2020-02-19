package com.wangdiao;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class P2PContext {
    private List<ChannelHandlerContext> context;
    private List<ChannelHandlerContext> peerContext;

    public P2PContext swap() {
        return new P2PContext(peerContext, context);
    }
}
