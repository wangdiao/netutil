package com.wangdiao.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.DefaultByteBufHolder;
import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;

/**
 * @author wangdiao
 */
@Getter
public class UdpPacket extends DefaultByteBufHolder {
    private InetSocketAddress recipient;
    private InetSocketAddress sender;
    @Setter
    private long sendTime = 0L;
    private UdpHeader udpHeader;
    private ByteBuf byteBuf;

    public UdpPacket(InetSocketAddress recipient, InetSocketAddress sender, ByteBuf byteBuf) {
        super(byteBuf);
        this.recipient = recipient;
        this.sender = sender;
        UdpHeader header = new UdpHeader();
        byte b = byteBuf.readByte();
        header.setControl((b & 0b10000000) == 0b00000000);
        header.setType(ControlMessageType.valueOf(b));
        header.setPacketNumber(byteBuf.readUnsignedMedium());
        header.setConnectId(byteBuf.readUnsignedInt());
        this.udpHeader = header;
        this.byteBuf = byteBuf;
    }

    public UdpPacket(boolean control, ControlMessageType type,
                     int packetNumber, long connectId, ByteBuf byteBuf, InetSocketAddress recipient) {
        super(byteBuf);
        this.recipient = recipient;
        UdpHeader header = new UdpHeader();
        header.setControl(control);
        header.setType(type);
        header.setPacketNumber(packetNumber);
        header.setConnectId(connectId);
        this.udpHeader = header;
        this.byteBuf = byteBuf;
    }

    public ByteBuf write(ByteBuf buf) {
        byte b = (byte) (this.udpHeader.isControl() ? 0b00000000 : 0b10000000);
        buf.writeByte(b | this.udpHeader.getType().getData());
        buf.writeMedium(this.udpHeader.getPacketNumber());
        buf.writeInt((int) this.udpHeader.getConnectId());
        buf.writeBytes(ByteBufUtil.getBytes(this.byteBuf));
        return buf;
    }
}
