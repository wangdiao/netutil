package com.wangdiao;

import com.wangdiao.server.ControlServer;
import lombok.extern.slf4j.Slf4j;

/**
 * Hello world!
 */
@Slf4j
public class App {
    public static final ControlServer controlServer = new ControlServer(9999);

    public static void main(String[] args) throws Exception {
        controlServer.run();
    }
}
