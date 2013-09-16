package org.integration.roger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TaskJob {
	private Logger log;

	public TaskJob() {
		log = Logger.getLogger(getClass().getName());
		PropertyConfigurator.configure("./lib/log4j.properties");
	}

	public String getTaskArgs(int tid) {
		String args = "";
		Connection con = DB.getConnection();
		String sql = "SELECT distinct s.id,s.tasktkey,s.optionname,s.selectvalue,s.projectid "
				+ "FROM  selectoptions s, tasks t,options o,projectoptions p "
				+ "where  s.tasktkey=t.tkey and o.name=s.optionname and p.optionid=o.id and t.id=? "
				+ "order by p.projectid,t.tkey,p.sequence ;";
		PreparedStatement pre = null;
		ResultSet rs = null;
		try {
			pre = con.prepareStatement(sql);
			pre.setInt(1, tid);
			rs = pre.executeQuery();
			while (rs.next()) {
				String name = rs.getString("optionname");
				String value = rs.getString("selectvalue");
				args = args + " -" + name + "=" + value;
			}

		} catch (Exception e) {
			log.error("getTaskArgs  " + e, e);
		} finally {
			try {
				rs.close();
				pre.close();
				con.close();
			} catch (SQLException e) {
				log.error("close Database Connection Error: " + e, e);
			}
		}
		return args;
	}
	public void updateTaskState(int state, int taskID) {
		Connection con = DB.getConnection();
		String sql = "";
		PreparedStatement pre = null;
		try {
			Timestamp st = new Timestamp(Calendar.getInstance()
					.getTimeInMillis());
			if (state == 104)
				sql = "UPDATE tasks SET state = ? ,jobstarttime=? WHERE id = ?";
			else
				sql = "UPDATE tasks SET state = ? ,completetime=? WHERE id = ?";
			pre = con.prepareStatement(sql);
			pre.setInt(1, state);
			pre.setTimestamp(2, st);
			pre.setInt(3, taskID);
			pre.execute();
		} catch (Exception e) {
			log.error("update task to temp state  " + e, e);
		} finally {
			try {
				// pre.close();
				con.close();
			} catch (SQLException e) {
				log.error("close Database Connection Error: " + e, e);
			}
		}
	}
	 public void resetTaskState(String host)
	    {
	        String sql="update tasks set state=? " +
	        		"where state=? and projectid in (" +
	        		"select distinct p.id " +
	        		"from projects p, groups g, servers s " +
	        		"where p.state=? and g.state=? and s.state=? " +
	        		"and p.groupid=g.id and g.serverid=s.id and s.host=?) ;";
	        Connection con=DB.getConnection();
	        PreparedStatement prep = null;
	        try {        	
	            prep = con.prepareStatement(sql);
	            prep.setInt(1, 4);
	            prep.setInt(2, 104);
	            prep.setInt(3, 1);
	            prep.setInt(4, 1);
	            prep.setInt(5, 1);
	            prep.setString(6, host);
	            prep.executeUpdate();
	        } catch (SQLException e) {
	            log.error("init last running task state "+e,e);
	        }
	        finally
	        {
	            try
	            {
	                prep.close();
	                con.close(); 
	            }
	            catch(SQLException e)
	            {
	                log.error("close Database Connection Error: "+e,e);
	            }
	        }
	    }
		public List<String> getHistory(String host)
	    {
	    	resetTaskState(host);
	        ArrayList<String> list=new ArrayList<String>();
	        String sql="select t.ftype,g.name " +
	        		"from tasks t , projects p,groups g, servers s " +
	        		"where t.state=? and s.state=? and p.state=? and g.state=? " +
	        		"and t.projectid =p.id  and  p.groupid=g.id and g.serverid=s.id " +
	        		"and s.host=?;";
	        Connection con=DB.getConnection();
	        PreparedStatement prep = null;
	        ResultSet res = null;
	        try {        	
	            prep = con.prepareStatement(sql);
	            prep.setInt(1, 4);
	            prep.setInt(2,1);
	            prep.setInt(3,1);
	            prep.setInt(4,1);
	            prep.setString(5, host);
	            res=prep.executeQuery();
	            while (res.next())
	            {
	                String name=res.getString("name");
	                String ftype=res.getString("ftype");
	                list.add(ftype+"/"+name);
	            }
	        } catch (SQLException e) {
	            log.error("get history record from db "+e,e);
	        } 
	        finally
	        {
	            try
	            {
	                res.close();
	                prep.close();
	                con.close(); 
	            }
	            catch(SQLException e)
	            {
	                log.error("close Database Connection Error: "+e,e);
	            }
	        }
	        return list;
	    }

		public String transInterruptCommand2ActionCommand(String command) {
			String[] arr=command.split("/");
			int tid=Integer.parseInt(arr[1]);
			String str="";
		    String sql="select t.ftype,g.name " +
	        		"from tasks t , projects p,groups g " +
	        		"where t.id=? and p.state=? and g.state=? " +
	        		"and t.projectid =p.id and p.groupid=g.id ;";
	        Connection con=DB.getConnection();
	        PreparedStatement prep = null;
	        ResultSet res = null;
	        try {        	
	            prep = con.prepareStatement(sql);
	            prep.setInt(1, tid);
	            prep.setInt(2,1);
	            prep.setInt(3,1);
	            res=prep.executeQuery();
	            while (res.next())
	            {
	                String name=res.getString("name");
	                String ftype=res.getString("ftype");
	                str=ftype+"/"+name;
	            }
	        } catch (SQLException e) {
	            log.error("transInterruptCommand2ActionCommand fail "+e,e);
	        } 
	        finally
	        {
	            try
	            {
	                res.close();
	                prep.close();
	                con.close(); 
	            }
	            catch(SQLException e)
	            {
	                log.error("close Database Connection Error: "+e,e);
	            }
	        }
			return str;
		}
}
