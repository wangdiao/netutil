package com.wangdiao.udp;

import lombok.Data;

/**
 * @author wangdiao
 */
@Data
public class UdpHeader {
    private boolean control;
    private ControlMessageType type;
    private int packetNumber;
    private long connectId;
}
