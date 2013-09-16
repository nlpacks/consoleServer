package org.integration.roger;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ThreadFactory {
    private Logger log;
    private static ThreadFactory factory = new ThreadFactory();

	public void createThread(String message) {
        log = Logger.getLogger(getClass().getName());
        PropertyConfigurator.configure("./lib/log4j.properties");
        String[] sh=null;
        if (message.indexOf(";")>-1)
        	sh=message.split(";");
        else 
        	sh=new String[]{message};
        
        for (int i=0;i<sh.length;i++)
        {
            if (sh[i]!=null&&sh[i].trim().length()>0)
            	servicesContral(sh[i]);
            else
            {
        		if (log.isInfoEnabled())
        			log.info("skip illegal message [" + message + "]");
            }
        }
		
	}
	public static ThreadFactory getInstance() {
		return factory;
	}
	private void servicesContral(String command)
	{
		ServicesContral sc=new ServicesContral();
        if (command.startsWith("start"))  
	        sc.startService();
        if (command.startsWith("stop"))   
            sc.stopService();               
        if (command.startsWith("build")) {
			Server.getInstance().getServerStatus().createWorkThead(command);
		}
        if (command.startsWith("interrupt")) {
			Server.getInstance().getServerStatus().interruptWorkThead(command);
		}
 	}
}
