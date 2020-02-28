package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;

import java.util.EventListener;

public interface ContextListener extends EventListener {
    void onRead(ByteBuf buf);
}
