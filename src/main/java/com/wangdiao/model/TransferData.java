package com.wangdiao.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * @author wangdiao
 */
@Data
@AllArgsConstructor
public class TransferData implements Serializable {
    private static final long serialVersionUID = -6372936830043264909L;

    public static final int READ = 1;
    public static final int ACTIVE = 2;
    public static final int INACTIVE = 3;
    private SocketAddress address;
    @ToString.Exclude
    private byte[] bytes;
    private int operate;
}
