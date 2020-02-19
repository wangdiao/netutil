package com.wangdiao;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hello world!
 */
@Slf4j
public class App {
    public static final RegisterServer registerServer = new RegisterServer(9999);
    public static final PassiveServer passiveServer = new PassiveServer(9998, registerServer);
    public static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws Exception {
        executorService.execute(() -> {
            try {
                registerServer.run();
            } catch (Exception e) {
                log.error("registerServer run failed.", e);
            }
        });
        passiveServer.run();
    }
}
