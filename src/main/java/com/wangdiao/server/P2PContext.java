package com.wangdiao.server;

import com.wangdiao.model.ControlCmd;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangdiao
 */
@Slf4j
public class P2PContext {

    /**
     * key: 代理端的port
     */
    private Map<Integer, PeerContext> servers = new ConcurrentHashMap<>();
    /**
     * key 注册端的port, value 代理端的port
     */
    private Map<Integer, Integer> peerMap = new ConcurrentHashMap<>();

    public void applyCmd(ControlCmd controlCmd, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        if (controlCmd.getOp() == ControlCmd.OP.START) {
            this.addItem(controlCmd, bossGroup, workerGroup);
        } else if (controlCmd.getOp() == ControlCmd.OP.STOP) {
            this.removeItem(controlCmd);
        }
    }

    public void addItem(ControlCmd controlCmd, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        if (servers.containsKey(controlCmd.getProxyPort()) || peerMap.containsKey(controlCmd.getProxyPort())
                || servers.containsKey(controlCmd.getRegisterPort()) || peerMap.containsKey(controlCmd.getRegisterPort())) {
            throw new IllegalArgumentException(String.format("端口%d或%d已被注册", controlCmd.getProxyPort(), controlCmd.getRegisterPort()));
        }
        PeerContext item = new PeerContext(controlCmd);
        servers.put(controlCmd.getProxyPort(), item);
        peerMap.put(controlCmd.getRegisterPort(), controlCmd.getProxyPort());
        item.start(bossGroup, workerGroup);
    }

    public void removeItem(ControlCmd in) {
        PeerContext item = servers.get(in.getProxyPort());
        if (item != null) {
            item.stop();
            servers.remove(in.getProxyPort());
            peerMap.remove(in.getRegisterPort());
        }
    }
}
