package com.wangdiao.client;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

@Data
public class LocalContext {
    private SocketAddress socketAddress;
    private CompletableFuture<ChannelHandlerContext> channelHandlerContextFuture = new CompletableFuture<>();

    public LocalContext(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }
}
