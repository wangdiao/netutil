package com.wangdiao;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void utf8Test() {
        String a = "哈哈哈硕大的aaa( ⊙ o ⊙ )啊！";
        System.out.println(a.length());
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeInt(ByteBufUtil.utf8Bytes(a));
        buf.writeCharSequence(a, StandardCharsets.UTF_8);
        CharSequence charSequence = buf.readCharSequence(buf.readInt(), StandardCharsets.UTF_8);
        System.out.println(charSequence);
    }
}
