package util;

import org.springframework.context.support.ClassPathXmlApplicationContext;

//配置文件初始化（dubbo服务器的初始化）
public class MyFactory {
	public static ClassPathXmlApplicationContext context = null;
	//静态代码块是自动执行的
	static{
		context = new ClassPathXmlApplicationContext(new String[]{"applicationContext.xml"});
      context.start();//启动spring容器，
	}
}
