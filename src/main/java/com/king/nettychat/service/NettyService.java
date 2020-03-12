package com.king.nettychat.service;

import com.king.nettychat.handler.WebSockHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * netty服务类
 *
 * @author by jinc
 * @Classname NettyService
 * @Date 2020/3/12 3:44 下午
 */
@Slf4j
@Component
public class NettyService {

    @Value("${server.port}")
    private int port;

    @PostConstruct
    public void start() throws Exception{
        EventLoopGroup connectionEventLoopGroup = new NioEventLoopGroup();
        EventLoopGroup dataHandlerEventLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.option(ChannelOption.SO_BACKLOG,1024);
//            serverBootstrap.option(ChannelOption.SO_KEEPALIVE,true);
            serverBootstrap.group(connectionEventLoopGroup,dataHandlerEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(this.port)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            log.debug("{}已连接",ch.localAddress());
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws","WebSocket",true,65536*10));
                            ch.pipeline().addLast(new WebSockHandler());
                        }
                    });
            ChannelFuture cf = serverBootstrap.bind(port).sync();
            cf.channel().closeFuture().sync();
        } finally {
            connectionEventLoopGroup.shutdownGracefully().sync();
            dataHandlerEventLoopGroup.shutdownGracefully().sync();
        }
    }
}
