package com.wangdiao.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * @author wangdiao
 */
public class DiscoverData implements Serializable {
    private static final long serialVersionUID = -855649439655705207L;
    private CharSequence name;
    private InetSocketAddress socketAddress;

    public DiscoverData() {
    }

    public DiscoverData(CharSequence name, InetSocketAddress socketAddress) {
        this.name = name;
        this.socketAddress = socketAddress;
    }

    public void write(ByteBuf buf) {
        buf.writeInt(ByteBufUtil.utf8Bytes(name));
        buf.writeCharSequence(name, StandardCharsets.UTF_8);
        byte[] address = socketAddress.getAddress().getAddress();
        buf.writeInt(address.length);
        buf.writeBytes(address);
        buf.writeInt(socketAddress.getPort());
    }

    public void read(ByteBuf buf) throws UnknownHostException {
        this.name = buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8);
        int port = buf.readInt();
        byte[] address = new byte[buf.readInt()];
        buf.readBytes(address);
        InetAddress inetAddress = InetAddress.getByAddress(address);
        this.socketAddress = new InetSocketAddress(inetAddress, port);
    }

}
