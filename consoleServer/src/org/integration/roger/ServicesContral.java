/*
 * Created on 2006-3-6
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.integration.roger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * @author liujiping
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ServicesContral {
	 private Logger log;
	 public ServicesContral()
	 {
	        log = Logger.getLogger(getClass().getName());
	        PropertyConfigurator.configure("./lib/log4j.properties");
	 }
    public static void main(String[] args) {
    	ServicesContral sc=new ServicesContral();
    	sc.contral(args[0]);
    }
    public void contral(String command)
    {
    	 SocketChannel channel = null;
         Properties config=new Properties();
         

         try {
         	config.load(new FileInputStream("./lib/config.properties"));
             int serverPort = Integer.parseInt(config.getProperty("listenport"));
             if (log.isDebugEnabled()) 
                 log.debug(" get server "+config.getProperty("listenhost")+" port [" + serverPort + "]");
             
             channel = SocketChannel.open();
             InetSocketAddress isa = new InetSocketAddress(config.getProperty("listenhost"),serverPort);            
             channel.connect(isa);
             channel.configureBlocking(false);
             channel.finishConnect();
         } catch (UnknownHostException e) {
             log.error("get server host errror:" +e,e);
         } catch (IOException e) {
             log.error("I/O stream error:" + e,e);
         }
         
 		
         try {
         	CharBuffer buf = CharBuffer.wrap(command);
         	ByteBuffer encoding = null;
         	Charset utfCharset = Charset.forName("UTF-8");
         	CharsetEncoder encoder = utfCharset.newEncoder();
             encoding=encoder.encode(buf);
             channel.write(encoding);
             if (log.isInfoEnabled())
                 log.info("send an message ["+command+"] to Server.");
             buf.clear();
             encoding.clear();
             channel.close();
         } catch (IOException e) {
             log.error("buffer I/O error:" + e,e);
         }
    }
    
   

   

    public void startService()
    {
        if (Server.getInstance().getServerStatus().isStop()==true)
        {
            if (log.isInfoEnabled())
                log.info("start services ....");
        }
        if (Server.getInstance().getServerStatus().isStart()==true)
        {
        	//make sure can call startService function only once
            if (log.isInfoEnabled())
                log.info("server has been started already, can't start again !");
            return;
        } 
        Server.getInstance().getServerStatus().setStop(false);

        Server.getInstance().getServerStatus().setStart(true);
        
    	Properties config = new Properties();
    	try {
			config.load(new FileInputStream("./lib/config.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	TaskJob tj=new TaskJob();
        List<String> list=tj.getHistory(config.getProperty("listenhost"));
        log.info("find total ["+list.size()+"] jobs in history task .");
        for (int i=0;i<list.size();i++)
        	ThreadFactory.getInstance().createThread(list.get(i).toString());
    }
    public void stopService()
    {
        if (Server.getInstance().getServerStatus().isStop()==false)
        {
            if (log.isInfoEnabled())
                log.info("server has stoped !");
        }
        if (Server.getInstance().getServerStatus().isStop()==true)
        {
        	//make sure can call stopService function only once
            if (log.isInfoEnabled())
                log.info("server has been stoped already, can't stop again !");
            return;
        } 
        Server.getInstance().getServerStatus().setStop(true);
        Server.getInstance().getServerStatus().setStart(false);
        //clear all requests when the server was stop, so the thread can't get any other requests
        Server.getInstance().getServerStatus().clean(); 
    }
}