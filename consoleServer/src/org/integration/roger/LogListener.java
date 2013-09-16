package org.integration.roger;

import java.util.Calendar;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

public class LogListener implements BuildListener {
    private boolean success=true;
    private Calendar starttime;

    public void buildFinished(BuildEvent arg0) {
        if (arg0.getException()==null)
            setSuccess(true);
        else setSuccess(false);
    }
    public void buildStarted(BuildEvent arg0) {}
    public void messageLogged(BuildEvent arg0) {}
    public void targetFinished(BuildEvent arg0) {
    	Calendar finishedtime=Calendar.getInstance();
    	int spendh=finishedtime.get(Calendar.HOUR_OF_DAY)-this.starttime.get(Calendar.HOUR_OF_DAY);
    	int spendm=finishedtime.get(Calendar.MINUTE)-this.starttime.get(Calendar.MINUTE);
    	int spends=finishedtime.get(Calendar.SECOND)-this.starttime.get(Calendar.SECOND);
    	if (spendm<0)
    	{
    		spendm=spendm+60;
    		spendh=spendh-1;
    	}
    	if (spends<0)
    	{
    		spends=spends+60;
    		spendm=spendm-1;
    	}
    	spendm=spendm+spendh*60;
    	arg0.getTarget().getProject().log(arg0.getTarget().getName()+" spend time: "+String.valueOf(spendm)+" minutes "+String.valueOf(spends)+" seconds");
    }
    public void targetStarted(BuildEvent arg0) {
    	this.starttime=Calendar.getInstance();
    }
    public void taskFinished(BuildEvent arg0) {}
    public void taskStarted(BuildEvent arg0) {}
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
