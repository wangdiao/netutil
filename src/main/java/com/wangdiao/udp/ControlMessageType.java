package com.wangdiao.udp;

public enum ControlMessageType {
    ACK((byte) 0b0000000),
    CREATE((byte) 0b0000100),
    CLOSE((byte) 0b0000110),
    DATA((byte) 0b0000000);
    private byte data;

    ControlMessageType(byte data) {
        this.data = data;
    }

    public byte getData() {
        return data;
    }

    public static ControlMessageType valueOf(byte data) {
        for (ControlMessageType type : ControlMessageType.values()) {
            if (type.data == data) {
                return type;
            }
        }
        return null;
    }
}
