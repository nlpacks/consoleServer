package org.integration.roger;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class DB {
    private static Logger log=null;
    public DB() {
        if (log == null) {
            log = Logger.getLogger(this.getClass().getName());
            PropertyConfigurator.configure("./lib/log4j.properties");
        }
    }
    public static Connection getConnection() {
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("./lib/config.properties"));
            String driver = prop.getProperty("driver").trim();
            String url = prop.getProperty("url").trim();
            String username = prop.getProperty("username").trim();
            String password = prop.getProperty("password").trim();
            Class.forName(driver);
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            log.error("get JDBC Connection " + e,e);
        }
        return null;
    }

}
