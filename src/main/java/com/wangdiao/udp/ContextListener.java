package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;

import java.util.EventListener;

public interface ContextListener extends EventListener {
    default void onActive() {
    }

    default void onRead(ByteBuf buf) {
    }

    default void onClose() {
    }
}
