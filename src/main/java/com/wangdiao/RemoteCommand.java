package com.wangdiao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RemoteCommand {
    private Cmd cmd;
    private int port;

    public enum Cmd {
        NEW_PORT(1), CLOSE_PORT(2);

        Cmd(int value) {
            this.value = value;
        }

        private int value;

        public static Cmd valueOf(int value) {
            switch (value) {
                case 1:
                    return NEW_PORT;
                case 2:
                    return CLOSE_PORT;
                default:
                    return null;
            }
        }
    }
}
