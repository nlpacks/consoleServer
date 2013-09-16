/*
 * Created on 2006-3-6
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.integration.roger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * @author liujiping
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ContralServices {

    public static void main(String[] args) {
        SocketChannel channel = null;
        Logger log = Logger.getLogger(ContralServices.class.getName());
        PropertyConfigurator.configure("./lib/log4j.properties");
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
            log.error(" get server host errror:" +e,e);
        } catch (IOException e) {
            log.error(" I/O stream error:" + e,e);
        }
        
		
        try {
        	CharBuffer buf = CharBuffer.wrap(args[0]);
        	ByteBuffer encoding = null;
        	Charset utfCharset = Charset.forName("UTF-8");
        	CharsetEncoder encoder = utfCharset.newEncoder();
            encoding=encoder.encode(buf);
            channel.write(encoding);
            if (log.isInfoEnabled())
                log.info("send an message ["+args[0]+"] to Server.");
            buf.clear();
            encoding.clear();
            channel.close();
        } catch (IOException e) {
            log.error(" buffer I/O error:" + e,e);
        }
    }
}