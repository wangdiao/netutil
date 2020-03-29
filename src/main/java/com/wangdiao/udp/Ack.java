package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class Ack {
    public static final int MAX_COUNT = (1 << Byte.SIZE) - 1;

    public static Collection<Integer> getAckPackets(ByteBuf buf) {
        List<Integer> list = new ArrayList<>();
        while (buf.readableBytes() >= 4) {
            int start = buf.readUnsignedMedium();
            int count = buf.readUnsignedByte();
            IntStream.range(start, start + count).forEach(list::add);
        }
        return list;
    }


    public static List<Integer> fillUdpPacketBody(ByteBuf byteBuf, List<Integer> packetNumberList) {
        packetNumberList.sort(Integer::compareTo);
        assert byteBuf.writableBytes() >= 4;
        int start = -1;
        int count = 0;
        int num = 0;
        for (Integer pn : packetNumberList) {
            if (byteBuf.writableBytes() < 4) {
                break;
            }
            num++;
            if (start < 0) {
                start = pn;
                count = 1;
            } else if (pn.equals(start + count) && count < MAX_COUNT) {
                count++;
            } else {
                byteBuf.writeMedium(start);
                byteBuf.writeByte(count);
                start = -1;
                count = 0;
            }
        }
        if (start > 0) {
            byteBuf.writeMedium(start);
            byteBuf.writeByte(count);
        }
        return packetNumberList.subList(num, packetNumberList.size());
    }
}
