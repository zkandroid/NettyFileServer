package download;

import java.io.File;

import io.netty.channel.ChannelHandlerContext;



public class FileDown {
	
	public boolean downFile(ChannelHandlerContext ctx,String path,String uri) {
		File file = new File("F:\\QRCodePng\\test.png");
        if (file.isFile() && file.exists()) {//如果不是一个文件或者文件夹回写403
        	System.out.println("file:"+uri);
            return true;
        }else  {
        	return true;
		}
		
		
	}
}
