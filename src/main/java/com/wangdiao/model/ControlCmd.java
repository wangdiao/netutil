package com.wangdiao.model;

import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;

/**
 * @author wangdiao
 */
@Data
public class ControlCmd implements Serializable {
    private static final long serialVersionUID = -5238115061926546058L;
    @NonNull
    private Integer registerPort;
    @NonNull
    private Integer proxyPort;
    @NonNull
    private OP op;

    public enum OP {
        START, STOP;
    }
}