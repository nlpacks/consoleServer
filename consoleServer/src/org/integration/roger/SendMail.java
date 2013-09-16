package org.integration.roger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.email.EmailTask;
import org.apache.tools.ant.taskdefs.email.Message;

public class SendMail {
	private Logger log=null;

	public SendMail() {
        log = Logger.getLogger(this.getClass().getName());
        PropertyConfigurator.configure("./lib/log4j.properties");
	}

	public boolean send(String subject, String content,String tolist) throws UnknownHostException {

		Properties config = new Properties();
		try {
			config.load(new FileInputStream("./lib/config.properties"));
		} catch (FileNotFoundException e) {
			log.error("config file not fount: " + e, e);
		} catch (IOException e) {
			log.error("read config file: " + e, e);
		}
		
		Project pj = new Project();
		pj.setName("sendResultMail");
		pj.init();

//		DefaultLogger consoleLogger = new DefaultLogger();
//		consoleLogger.setErrorPrintStream(System.out);
//		consoleLogger.setOutputPrintStream(System.out);
//		consoleLogger.setMessageOutputLevel(Project.MSG_VERBOSE);
//		pj.addBuildListener(consoleLogger);
		
		LogListener listener = new LogListener();
		pj.addBuildListener(listener);

		Target tg = new Target();
		tg.setName("mail");
		tg.setProject(pj);
		pj.addTarget(tg);

		EmailTask mail = new EmailTask();
		mail.setTaskName("mail");
		mail.setMailhost(config.getProperty("mail.host"));
		mail.setMailport(Integer.parseInt(config.getProperty("mail.port")));
		mail.setUser(config.getProperty("mail.username"));
		mail.setPassword(config.getProperty("mail.passwd"));
		mail.setFrom(InetAddress.getLocalHost().getHostName());
		mail.setToList(tolist);
		
		mail.setSubject(subject);

		Message msg = new Message();
		msg.setProject(pj);
		msg.setCharset("GB2312");
		msg.setMimeType("text/html");
		msg.addText(content);
		mail.addMessage(msg);
		EmailTask.Encoding encoding = new EmailTask.Encoding();
		encoding.setValue("mime");
		mail.setEncoding(encoding);
		mail.setProject(pj);
		tg.addTask(mail);

		Throwable ta = null;
		try {
			pj.executeTarget(tg.getName());
		} catch (Exception e) {
			ta=e;
			log.error("send mail exception: " + e,e);
		} finally {
			pj.fireBuildFinished(ta);
		}
		return listener.isSuccess();
	}
}
