package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.InetSocketAddress;

/**
 * @author wangdiao
 */
@ToString
@Getter
public class UdpPacket {
    private InetSocketAddress recipient;
    private InetSocketAddress sender;
    @Setter
    private long sendTime = 0L;
    private UdpHeader udpHeader;
    @ToString.Exclude
    private byte[] bytes;
    @Setter
    private ChannelHandlerContext ctx;
    public static final int MAX_PACKET = 1300;

    public UdpPacket(InetSocketAddress recipient, InetSocketAddress sender, ByteBuf byteBuf) {
        this.recipient = recipient;
        this.sender = sender;
        UdpHeader header = new UdpHeader();
        byte b = byteBuf.readByte();
        header.setControl((b & 0b10000000) == 0b00000000);
        header.setType(ControlMessageType.valueOf(b));
        header.setPacketNumber(byteBuf.readUnsignedMedium());
        header.setConnectId(byteBuf.readUnsignedInt());
        this.udpHeader = header;
        this.bytes = ByteBufUtil.getBytes(byteBuf);
        ReferenceCountUtil.release(byteBuf);
    }

    public UdpPacket(boolean control, ControlMessageType type,
                     int packetNumber, long connectId, byte[] bytes, InetSocketAddress recipient) {
        this.recipient = recipient;
        UdpHeader header = new UdpHeader();
        header.setControl(control);
        header.setType(type);
        header.setPacketNumber(packetNumber);
        header.setConnectId(connectId);
        this.udpHeader = header;
        this.bytes = bytes;
    }

    public void write(ByteBuf buf) {
        byte b = (byte) (this.udpHeader.isControl() ? 0b00000000 : 0b10000000);
        buf.writeByte(b | this.udpHeader.getType().getData());
        buf.writeMedium(this.udpHeader.getPacketNumber());
        buf.writeInt((int) this.udpHeader.getConnectId());
        buf.writeBytes(this.bytes);
    }
}
