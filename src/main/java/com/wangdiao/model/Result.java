package com.wangdiao.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author wangdiao
 */
@Data
public class Result implements Serializable {

    private static final long serialVersionUID = -4520152519624675352L;
    public int code;
    private String message;
}
