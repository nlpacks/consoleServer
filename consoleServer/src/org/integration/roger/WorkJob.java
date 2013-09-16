/*
 * Created on 2006-12-8
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.integration.roger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author liujiping
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class WorkJob extends Thread {
	private Logger log;
	private static byte[] lock = new byte[0]; // make sure all thread must
												// synchronized with it
	private String group;
	private String host;
	private String command;

	public WorkJob(ThreadGroup tg, String command) {
		super(tg, command);
		String[] arr = command.split("/");
		this.group = arr[1];
		this.command = command;
		log = Logger.getLogger(getClass().getName());
		PropertyConfigurator.configure("./lib/log4j.properties");
	}

	public void work() {
		Properties config = new Properties();
		try {
			config.load(new FileInputStream("./lib/config.properties"));
			host = config.getProperty("listenhost");
		} catch (FileNotFoundException e) {
			log.error("config file not fount: " + e, e);
		} catch (IOException e) {
			log.error("read config file: " + e, e);
		}

		Properties prop = getWaitJob();
		// update task state to a temp state(104)
		if (prop.getProperty("id") == null) {
			if (log.isInfoEnabled()) {
				log.info("current thread didn't found any more job to do and exit. ");
			}
			return;
		}

		// 将当前线程的id与具体任务在数据库中的id关联起来，为中止某个正在执行的线程准备判断依据
		Server.getInstance().getServerStatus()
				.setThreadIdMapTaskId(getId(), prop.getProperty("id"));

		setName(prop.getProperty("id"));
		TaskJob tj = new TaskJob();
		int seq = Integer.parseInt(prop.getProperty("id"));
		String script = config.getProperty("basedir") + File.separator
				+ "scripts" + File.separator + this.group + File.separator
				+ prop.getProperty("name");
		script = getAsOSPath(script, "sh");

		File xmlfile_tmp = new File(script);
		if (!xmlfile_tmp.exists()) {
			log.info("[task " + prop.getProperty("id") + "] buildfile : "
					+ script + "] does not exists, skip this job !");
			tj.updateTaskState(6, seq);
			return;
		}
		if (log.isInfoEnabled()) {
			log.info("current build job [task " + prop.getProperty("id")
					+ "] and thread id [" + getId() + "] project [" + script
					+ "] with arguments [" + prop.getProperty("args") + "] ");
		}
		String logfile = config.getProperty("basedir") + File.separator
				+ "logs" + File.separator + this.group + File.separator
				+ prop.getProperty("logfile");
		logfile = getAsOSPath(logfile, "txt");
		File t=new File(logfile);
		try {
			t.getParentFile().mkdirs();
			t.createNewFile();
		} catch (IOException ex) {
			log.error("create logfile error : " + ex, ex);
		}

		int action = 0;
		boolean interrupted = false;
		try {
			action = execute(script, prop.getProperty("args"), logfile);
		} catch (IOException e) {
			log.error("script file I/O : " + e, e);
		} catch (InterruptedException e) {
			log.error("current task [" + prop.getProperty("id")
					+ "] and thread [" + getId() + "] has been interrupted.", e);
			interrupted = true;
		}
		//
		// // call ant to build project
		//
		// boolean action = callAnt(xmlfile,logfile);
		String result = "";

		if (interrupted) {
			tj.updateTaskState(8, seq);
			result = "interrupted";
		}
		// update task complete state : success(5) or failure(6)
		else if (action == 0) {
			tj.updateTaskState(5, seq);
			result = "success";
		} else {
			tj.updateTaskState(6, seq);
			result = "failure";
		}
		if (log.isInfoEnabled())
			log.info("current build project [" + script + "] build result :"+ result);

		// send result to commit user by email
		SendMail mail = new SendMail();
		// set mail subject and content
		
		String subject = prop.getProperty("name") + " of " + prop.getProperty("groups") + " build " + result;

		String sw_version=null;
		if (prop.getProperty("args").toLowerCase().indexOf("-sw_version=")>-1)
		{
			int index=prop.getProperty("args").toLowerCase().indexOf("-sw_version=");
			sw_version=prop.getProperty("args").substring(index);
			String[] arr=sw_version.split(" ");
			String[] tarr=arr[0].split("=");
			sw_version=tarr[1];
		}
		boolean mailrs = false;
		try {
			mailrs = mail
					.send(subject,
							mailContent(config.getProperty("listenhost"), prop,
									result,sw_version), prop.getProperty("commituser"));
		} catch (UnknownHostException e) {
			log.error("send email fail:" + e, e);
		}
		if (log.isInfoEnabled())
			log.info("send an mail to user: " + prop.getProperty("commituser")
					+ " : " + mailrs);
		// ok, this job is over
	}

	private String mailContent(String host, Properties prop, String result,String swReversion) {
		String content = "<html><body><br>Project: "+ prop.getProperty("name")+ " build ";
		if (result.equals("success"))
			content=content+result;
		else 
			content=content+"<font color=\"red\"><b>"+result+"</b></font>";
		
		content=content + "<br>with arguments :<br><hr> "
				+ prop.getProperty("args")
				+ "<hr><br>logfile: \\\\"
				+ host
				+ "\\logs\\"
				+ this.group
				+ "\\"
				+ prop.getProperty("logfile");
		if (result.equals("success"))
			content=content+"<br>target file : \\\\" + host	+ "\\smp-res\\" + prop.getProperty("name") + "\\";
		if (swReversion!=null&&swReversion.trim().length()>0)
			content=content+swReversion.trim()+".zip</body></html>";
		
		return content;
	}

	public Properties getWaitJob() {
		synchronized (lock) {
			Properties prop = new Properties();
			String sql = "select t.id,p.name,g.name as groups,t.logfile,t.commituser "
					+ "from servers s , projects p, tasks t, groups g "
					+ "where g.name=? and t.state=? and s.id=g.serverid and g.id=p.groupid and s.host=? "
					+ "and t.projectid=p.id and p.state=? and g.state=? and t.ftype=? and s.state=? "
					+ "order by p.pri,t.id";
			int id = 0;

			TaskJob tj = new TaskJob();
			Connection con = DB.getConnection();
			PreparedStatement prep = null;
			ResultSet res = null;
			try {
				prep = con.prepareStatement(sql,
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				prep.setFetchSize(1);
				prep.setString(1, this.group);
				prep.setInt(2, 4);
				prep.setString(3, host);
				prep.setInt(4, 1);
				prep.setInt(5, 1);
				prep.setString(6, "build");
				prep.setInt(7, 1);

				res = prep.executeQuery();

				if (res.first()) {
					prop.setProperty("name", res.getString("name"));
					id = res.getInt("id");
					prop.setProperty("id", String.valueOf(id));
					prop.setProperty("logfile", res.getString("logfile"));
					prop.setProperty("commituser", res.getString("commituser"));
					prop.setProperty("groups", res.getString("groups"));
					String args = tj.getTaskArgs(id);
					prop.setProperty("args", args == null ? "" : args.trim());
				}
			} catch (SQLException e) {
				log.error("get a record from db " + e, e);
			} finally {
				try {
					res.close();
					prep.close();
					con.close();
				} catch (SQLException e) {
					log.error("close Database Connection Error: " + e, e);
				}
			}
			// update the task as a temporary state, then different thread can't
			// found the same task
			tj.updateTaskState(104, id);
			return prop;
		}
	}

	private int execute(String script, String parameters, String logfile)
			throws IOException, InterruptedException {

		String command = "";
		String os = System.getenv("OS");
		if (os != null && os.toLowerCase().indexOf("windows") > -1) // windows
			command = "cmd /c " + script;
		else
			command = "/bin/sh " + script;
		if (parameters != null && parameters.trim().length() > 0)
			command = command + " " + parameters;

		Execute ec = new Execute();
		return ec.execute(command, new File(logfile));
	}

	private String getAsOSPath(String script, String suffix) {
		String os = System.getenv("OS");
		if (os != null && os.toLowerCase().indexOf("windows") > -1) // windows
		{
			script = script.replace('\\', '/');
			script = script.replaceAll("//", "/");
			script = script.replace('/', '\\');
			if (suffix.equals("sh"))
				script = script + ".bat";
		} else {
			script = script.replace('\\', '/');
			script = script.replaceAll("//", "/");
			if (suffix.equals("sh"))
				script = script + ".sh";
		}
		return script;
	}

	public void run() {
		ServerStatus statue = Server.getInstance().getServerStatus();
		int count = statue.getWaitingWork(this.command);

		log.info("current thread id [" + getId()
				+ "] found total waiting job count :" + String.valueOf(count));
		// when there has many task in queue waiting for ,the current thread can
		// deal with all waiting task with only one thread
		while (count > 0) {
			log.info("work queue has [" + String.valueOf(count)
					+ "] jobs are waiting.");
			if (statue.isStop() == true) {
				break;
			}
			work();
			synchronized (lock) {
				Server.getInstance().getServerStatus()
						.decreaseWaitingWork(command);
				Server.getInstance().getServerStatus()
						.setCurrentTheads(count - 1, command);
			}

			// maybe there has many other thread are running, the total thread
			// count maybe reset by other threads
			// so must get the thread account from Server instance
			count = statue.getWaitingWork(this.command);
		}
		if (log.isInfoEnabled()) {
			log.info("work are done and thread id [" + getId() + "] exit.");
		}
	}

}
