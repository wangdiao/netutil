package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class Ack {
    public static Collection<Integer> getAckPackets(ByteBuf buf) {
        List<Integer> list = new ArrayList<>();
        while (buf.readableBytes() >= 4) {
            int start = buf.readUnsignedMedium();
            int count = buf.readUnsignedByte();
            IntStream.range(start, start + count).forEach(list::add);
        }
        return list;
    }
}
