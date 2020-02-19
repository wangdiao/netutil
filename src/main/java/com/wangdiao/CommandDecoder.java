package com.wangdiao;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandDecoder extends ByteToMessageDecoder {
    private final AttributeKey<RemoteCommand> cmdKey = AttributeKey.valueOf("cmd");
    public static final Map<Integer, P2PContext> MAP = new ConcurrentHashMap<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public CommandDecoder(EventLoopGroup bossGroup, EventLoopGroup workerGroup, P2PServerHandler p2PServerHandler) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.p2PServerHandler = p2PServerHandler;
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        RemoteCommand command = channel.attr(cmdKey).get();
        if (command != null) {
            MAP.get(command.getPort()).getContext().remove(ctx);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Channel channel = ctx.channel();
        RemoteCommand command = channel.attr(cmdKey).get();
        if (command == null) {
            if (in.readableBytes() >= 8) {
                command = new RemoteCommand(RemoteCommand.Cmd.valueOf(in.readInt()), in.readInt());
                channel.attr(cmdKey).set(command);
                this.processCmd(ctx, command);
            } else {
                return;
            }
        }
        out.add(in);
    }

    private void processCmd(ChannelHandlerContext ctx, RemoteCommand remoteCommand) {
        if (remoteCommand.getCmd() == RemoteCommand.Cmd.NEW_PORT) {
            try {
                if (!MAP.containsKey(remoteCommand.getPort())) {
                    List<ChannelHandlerContext> list = new ArrayList<>();
                    list.add(ctx);
                    P2PContext p2PContext = new P2PContext(list, new ArrayList<>());
                    MAP.put(remoteCommand.getPort(), p2PContext);
                    P2PServerHandler p2PServerHandler = new P2PServerHandler(p2PContext.swap());
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(p2PServerHandler);
                                }
                            })
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // Bind and start to accept incoming connections.
                    ChannelFuture f = b.bind(remoteCommand.getPort()).sync();

                    // Wait until the server socket is closed.
                    // In this example, this does not happen, but you can do that to gracefully
                    // shut down your server.
                    f.channel().closeFuture().sync();
                } else {
                    MAP.get(remoteCommand.getPort()).getContext().add(ctx);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }
}
