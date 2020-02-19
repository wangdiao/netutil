package com.wangdiao;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ContextNode {
    private ChannelHandlerContext channelHandlerContext;
    private boolean init;
}
