package server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.Charsets;

import download.FileDown;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.ReferenceCountUtil;
import util.MyFactory;

/**
 * 
 * @author zk
 * @create 2018-5-14
 */
public class FileHttpServerHandler extends ChannelInboundHandlerAdapter{
	
	
	
	private HttpHeaders headers;
	private HttpRequest request;
	private FullHttpResponse response;
	private FullHttpRequest fullRequest;
	private HttpPostRequestDecoder decoder;
	private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE);
    
    private static final String FAVICON_ICO = "/favicon.ico";
    private static final String SUCCESS = "success";
    private static final String ERROR = "error";
    private static final String CONNECTION_KEEP_ALIVE = "keep-alive";
    private static final String CONNECTION_CLOSE = "close";
    
    
    

    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
	

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
		System.out.println("channelRead");
		if(msg instanceof HttpRequest){
			try{
				request = (HttpRequest) msg;
				headers = request.headers();
				
				String uri = request.uri();
				System.out.println("http uri: " + uri);
				//去除浏览器"/favicon.ico"的干扰
				if(uri.equals(FAVICON_ICO)){
					return;
				}
				
				HttpMethod method = request.method();
				if(method.equals(HttpMethod.POST)){
					System.out.println("IS POST");
					
					//POST请求，由于你需要从消息体中获取数据，因此有必要把msg转换成FullHttpRequest
					//fullRequest = (FullHttpRequest) msg;
					
					//根据不同的 Content_Type 处理 body 数据
					dealWithContentType();
				}else if(method.equals(HttpMethod.GET)){
					MyFactory.context.getBean("FileDown",FileDown.class).downFile(ctx, "", uri);
				}
				else{
					//其他类型暂时在此不做处理，
				}
				
				writeResponse(ctx.channel(), HttpResponseStatus.OK, SUCCESS, false);
				
			}catch(Exception e){
				writeResponse(ctx.channel(), HttpResponseStatus.INTERNAL_SERVER_ERROR, ERROR, true);
				
			}finally{
				ReferenceCountUtil.release(msg);
			}
			
		}else{
			System.out.println(" NO msg instanceof HttpRequest");
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
	
	/**
	 * 简单处理常用几种 Content-Type 的 POST 内容（可自行扩展）
	 * @param headers
	 * @param content
	 * @throws Exception
	 */
	private void dealWithContentType() throws Exception{
		String contentType = getContentType();
		if(contentType.equals("multipart/form-data")){  //用于文件上传
			readHttpDataAllReceive();
			
		}else{
			System.out.println("Content-Type不是multipart/form-data格式");
		}
	}
	
	private void readHttpDataAllReceive() throws Exception{
		initPostRequestDecoder();
		try {
			List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
            for (InterfaceHttpData data : datas) {
                writeHttpData(data);
            }
        } catch (Exception e) {
        	//此处仅简单抛出异常至上一层捕获处理，可自定义处理
            throw new Exception(e);
        }
	}
	
	private void writeHttpData(InterfaceHttpData data) throws Exception{
		
    	if(data.getHttpDataType() == HttpDataType.FileUpload) {
    		FileUpload fileUpload = (FileUpload) data;
    		String fileName = fileUpload.getFilename();
    		System.out.println("fileName:"+fileName);
    		if(fileUpload.isCompleted()) {
    			//保存到磁盘
    			StringBuffer fileNameBuf = new StringBuffer(); 
    			fileNameBuf.append("F:\\QRCodePng\\").append(fileName);
    			fileUpload.renameTo(new File(fileNameBuf.toString()));
    		}
    	}
	}
	
	private void writeResponse(Channel channel, HttpResponseStatus status, String msg, boolean forceClose){
		ByteBuf byteBuf = Unpooled.wrappedBuffer(msg.getBytes());
		response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
		boolean close = isClose();
		if(!close && !forceClose){
			response.headers().add(org.apache.http.HttpHeaders.CONTENT_LENGTH, String.valueOf(byteBuf.readableBytes()));
		}
		ChannelFuture future = channel.write(response);
		if(close || forceClose){
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private String getContentType(){
		String typeStr = headers.get("Content-Type").toString();
		String[] list = typeStr.split(";");
		return list[0];
	}
	
	private void initPostRequestDecoder(){
		if (decoder != null) {  
            decoder.cleanFiles();  
            decoder = null;  
        }
		decoder = new HttpPostRequestDecoder(factory, request, Charsets.toCharset(CharEncoding.UTF_8));
	}
	
	private boolean isClose(){
		if(request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, CONNECTION_CLOSE, true) ||
				(request.protocolVersion().equals(HttpVersion.HTTP_1_0) && 
				!request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, CONNECTION_KEEP_ALIVE, true)))
			return true;
		return false;
	}

}