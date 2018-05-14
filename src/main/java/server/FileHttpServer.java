package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * 类名：FileHttpServer
 * 描述：文件服务器的http
 * @version 0.0.1  
 * @author zk
 * @date 2018-4-17
 *
 */

public class FileHttpServer extends Thread {
	public void run() {
		try {
			startServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String address = "127.0.0.1";
	private int port = 8088;
	 private void startServer() throws Exception {
	        EventLoopGroup bossGroup = new NioEventLoopGroup();
	        EventLoopGroup workerGroup = new NioEventLoopGroup();
	        try {
	            ServerBootstrap b = new ServerBootstrap();
	            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
	                .childHandler(new ChannelInitializer<SocketChannel>() {
	                    @Override
	                    public void initChannel(SocketChannel ch) throws Exception {
	                        ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
	                    ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(1550000360));
	                	// server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
	                    ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
	                    // server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码
	                    
	                    ch.pipeline().addLast(new FileHttpServerHandler());
	                }
	            }).option(ChannelOption.SO_BACKLOG, 1024) 
	            .childOption(ChannelOption.SO_KEEPALIVE, true);
	        ChannelFuture f = b.bind(address,port).sync();
	        f.channel().closeFuture().sync();
	    } finally {
	        workerGroup.shutdownGracefully();
	        bossGroup.shutdownGracefully();
	    }
	 }
}
