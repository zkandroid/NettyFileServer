package Lestudio.fileServer;

import io.netty.handler.codec.http.multipart.DiskFileUpload;
import server.FileHttpServer;
import util.MyFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	FileHttpServer fileServer = MyFactory.context.getBean("FileHttpServer", FileHttpServer.class);
    	fileServer.start();
    	System.out.println("FileHttpServer is start");
    }
}
