/*
 * Created on 2006-12-8
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.integration.roger;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author liujiping
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class Server {
	private Logger log;
	private ServerSocketChannel server;
	private Selector selector;
	private int port;
	private String host;
	private static Server instance = new Server();
	private ServerStatus  serverStatus=null;

	private Server() {
		serverStatus=new ServerStatus();
		log = Logger.getLogger(getClass().getName());
		PropertyConfigurator.configure("./lib/log4j.properties");

		try {
			Properties config = new Properties();
			config.load(new FileInputStream("./lib/config.properties"));

			port = Integer.parseInt(config.getProperty("listenport"));
			host = config.getProperty("listenhost");
			server = ServerSocketChannel.open();
			server.configureBlocking(false);

			InetSocketAddress isa = new InetSocketAddress(host, port);
			server.socket().bind(isa);

			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);

		} catch (IOException e) {
			log.error("init Server Channel error:[" + String.valueOf(port)
					+ "]" + e);
		}
	}


	public void listen() {
		try {
			if (log.isInfoEnabled()) {
				server.socket().getInetAddress();
				log.info("Server " + host + " begin listening port ["
						+ String.valueOf(port) + "]");
			}

			ByteBuffer buf = ByteBuffer.allocate(768);
			while (selector.select() > 0) {
				Set<?> readyKeys = selector.selectedKeys();
				Iterator<?> it = readyKeys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();
					if (key.isAcceptable() == true) {
						SocketChannel socket = (SocketChannel) ((ServerSocketChannel) key
								.channel()).accept();
						socket.configureBlocking(false);
						socket.finishConnect();
						socket.register(selector, SelectionKey.OP_READ);
					}
					if (key.isReadable() == true) {
						SocketChannel socket = (SocketChannel) key.channel();

						buf.clear();
						try {
							socket.read(buf);
						} catch (IOException e) {
							log.error("read socket channel error:" + e, e);
						}
						buf.flip();
						Charset charset = Charset.forName("UTF-8");
						CharsetDecoder decoder = charset.newDecoder();
						CharBuffer charBuffer = decoder.decode(buf);
						String command = charBuffer.toString();
						if (log.isInfoEnabled())
							log.info("read an message ["
									+ command
									+ "] from client ["
									+ socket.socket().getInetAddress()
											.getHostName() + "]");
						dealWithMessage(command);
						buf.clear();
						key.cancel();
					}
				}
			}
			server.close();
		} catch (IOException e) {
			log.error("server listening Socket " + e, e);
		}
	}

	/**
	 * @param message
	 */
	private void dealWithMessage(String message) {
		ThreadFactory.getInstance().createThread(message);
	}
	
	public ServerStatus getServerStatus() {
		return serverStatus;
	}
	public static Server getInstance() {
		return instance;		
	}

	public static void main(String[] args) {
		Server.getInstance().listen();
	}
}
