package org.integration.roger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ServerStatus {
	// cache all command
	private Hashtable<String, Vector<String>> cacheQueue;
	// record current thread which are running
	private Hashtable<String, String> currentThreadMap;
	private Hashtable<String, ThreadGroup> threadGroupMap;
	private Hashtable<String, String> threadIdMap;

	private Logger log;
	private boolean stop = true;
	private boolean start = false;

	public ServerStatus() {
		log = Logger.getLogger(getClass().getName());
		PropertyConfigurator.configure("./lib/log4j.properties");
		cacheQueue = new Hashtable<String, Vector<String>>();
		currentThreadMap = new Hashtable<String, String>();
		threadGroupMap = new Hashtable<String, ThreadGroup>();
		threadIdMap = new Hashtable<String, String>();
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean isStart() {
		return start;
	}

	public void setStart(boolean start) {
		this.start = start;
	}

	public int currentTheads(String command) {
		int count = 0;
		if (currentThreadMap.get(command) != null)
			count = Integer.parseInt(currentThreadMap.get(command));
		return count;
	}

	private int getMaxThread(String msg) {
		int max = 0;
		Properties config = new Properties();
		try {
			config.load(new FileInputStream("./lib/config.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] arr = msg.split("/");
		max = Integer.parseInt(config.getProperty("maxthread"));
		Object spec = config.getProperty(arr[0] + "_" + arr[1] + "_maxthread");
		if (spec != null) {
			if (spec.toString().trim().length() > 0)
				max = Integer.parseInt(spec.toString().trim());
		}
		return max;
	}

	public void setCurrentTheads(int currThreadCount, String command) {
		currentThreadMap.put(command, String.valueOf(currThreadCount));
	}

	public void createWorkThead(String command) {
		// cache all command first, and thread would get and to do it from queue
		cacheCommand(command);
		log.info("cache command [" + command
				+ "], current groups total thread [" + currentTheads(command)
				+ "] and max thread is [" + getMaxThread(command) + "].");

		if (isStart()) {
			if (currentTheads(command) < getMaxThread(command)) {
				setCurrentTheads(currentTheads(command) + 1, command);
				log.info("create new work thread [" + command
						+ "], current groups total thread ["
						+ currentTheads(command) + "] and max thread is ["
						+ getMaxThread(command) + "].");
				// use thread group to mark all thread
				ThreadGroup tg = getThreadGroup(command);
				Thread build = new WorkJob(tg, command);
				build.start();
				threadGroupMap.put(command, tg);
			}
		}
	}

	private void cacheCommand(String command) {
		Vector<String> vt = this.cacheQueue.get(command);
		if (vt == null)
			vt = new Vector<String>();
		vt.add(command);
		this.cacheQueue.put(command, vt);
	}

	public int getWaitingWork(String command) {
		Vector<String> vt = this.cacheQueue.get(command);
		if (vt == null)
			vt = new Vector<String>();
		return vt.size();
	}

	public void decreaseWaitingWork(String command) {
		Vector<String> vt = this.cacheQueue.get(command);
		if (vt == null)
			vt = new Vector<String>();
		if (vt.size() > 0)
			vt.remove(0);
		this.cacheQueue.put(command, vt);
	}

	public void clean() {
		cacheQueue.clear();
		currentThreadMap.clear();
		threadGroupMap.clear();
		threadIdMap.clear();
	}

	public ThreadGroup getThreadGroup(String command) {
		ThreadGroup tg = null;
		if (threadGroupMap.containsKey(command))
			tg = threadGroupMap.get(command);
		else
			tg = new ThreadGroup(command);
		return tg;
	}

	/**
	 * 在这个访问中的参数格式为interrupt/121这样的，
	 * 但是在ThreadGroupMap中的key确是 Build/C8680 这样格式的，
	 * 所以要获取真正的ThreadGroupMap,必须根据参数格式进行转换，
	 * @param command
	 */
	public void interruptWorkThead(String command) {
		TaskJob tj=new TaskJob();
		ThreadGroup tg = getThreadGroup(tj.transInterruptCommand2ActionCommand(command));
		Thread[] list = new Thread[tg.activeCount()];
		log.info("active thread in thread group [" + tg.activeCount() + "] ");
		int active = tg.enumerate(list);
		String[] arr = command.split("/");
		if (active <= list.length) {
			String taskthreadId = threadIdMap.get(arr[1]);
			boolean found = false;
			for (int i = 0; i < list.length; i++) {
				String threadId = String.valueOf(list[i].getId());
				log.info("thread id [" + threadId + "] in " + tg.getName());

				if (threadId.equals(taskthreadId)) {
					log.info("found the thread which would be interrupt ["
							+ list[i].getId() + "].");
					found = true;
					list[i].interrupt();
					break;
				}
			}
			if (!found)
				log.info("can't found the thread which should be interrupt ["
						+ command + "].");
		} else
			log.info("enumerate current active thread fail, active count ["
					+ String.valueOf(active) + "] is more than max thread ["
					+ String.valueOf(getMaxThread(command)) + "] count");

	}

	public void setThreadIdMapTaskId(long threadId, String taskId) {
		threadIdMap.put(taskId, String.valueOf(threadId));
	}
}
