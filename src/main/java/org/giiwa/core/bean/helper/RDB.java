/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.core.bean.helper;

import java.io.File;
import java.sql.*;
import java.util.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Http;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.framework.web.Model;

/**
 * The {@code RDB} Class used to for RDS database layer operation.
 * 
 * @author joe
 *
 */
public class RDB {

  final private static Log log = LogFactory.getLog(RDB.class);

  /**
   * test is configured
   * 
   * @return boolean, true if configured
   */
  public static boolean isConfigured() {
    return getDataSource(Helper.DEFAULT) != null;
  }

  /**
   * drop all tables for the "db", the "db" name was configured in
   * "giiwa.properties", such as: db[ttt].url=....
   *
   * @param db
   *          the db
   * @return int of how many table was dropped
   */
  public static int dropAll(String db) {
    Connection c = null;
    PreparedStatement stat = null;
    ResultSet r = null;

    try {
      if (X.isEmpty(db)) {
        c = getConnection();
      } else {
        c = getConnection(db);
      }
      DatabaseMetaData dm = c.getMetaData();
      r = dm.getTables(null, null, "%", new String[] { "TABLE" });
      List<String> tables = new ArrayList<String>();
      while (r.next()) {
        tables.add(r.getString(3));
      }
      r.close();
      r = null;
      for (String t : tables) {
        stat = c.prepareStatement("drop table " + t);
        stat.executeUpdate();
        stat.close();
        stat = null;
      }

      return tables.size();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return 0;
  }

  /** The max active number. */
  private static int                          MAX_ACTIVE_NUMBER = 10;

  /** The max wait time. */
  private static int                          MAX_WAIT_TIME     = 10 * 1000;

  /** The dss. */
  private static Map<String, BasicDataSource> dss               = new TreeMap<String, BasicDataSource>();

  /** The conf. */
  private static Configuration                conf;

  /**
   * initialize the DB object from the "giiwa.properties"
   */
  public static synchronized void init() {
    conf = Config.getConf();
    if (conf == null)
      return;

    RDB.getDataSource(Helper.DEFAULT);

  }

  /**
   * Gets the driver.
   * 
   * Derby gives "Apache Derby" <br>
   * Microsoft SQL Server gives "Microsoft SQL Server" <br>
   * Oracle gives "Oracle" <br>
   * PostgreSQL gives "PostgreSQL"<br>
   * MySQL gives "MySQL"<br>
   * HSQLDB gives "HSQL Database Engine" <br>
   * DB2 gives "DB2/....". In my situation it gives "DB2/LINUXX8664" <br>
   * H2 gives "H2" <br>
   * 
   * @return the driver
   */
  public static String getDriver() {
    Connection c = null;
    try {
      c = getConnection();
      if (c != null) {
        String s = c.getMetaData().getDatabaseProductName().toLowerCase();
        String[] ss = X.split(s, "[ /]");

        return ss[0];
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      RDSHelper.inst.close(c);
    }
    return null;
  }

  /**
   * Gets the driver by the db name.
   *
   * @param name
   *          the name
   * @return the driver
   */
  public static String getDriver(String name) {
    Connection c = null;
    try {
      c = getConnection(name);
      if (c != null) {
        String s = c.getMetaData().getDatabaseProductName().toLowerCase();
        String[] ss = X.split(s, "[ /]");
        return ss[0];
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      RDSHelper.inst.close(c);
    }
    return null;
  }

  /**
   * Gets the connection.
   * 
   * @return the connection
   * @throws SQLException
   *           the sQL exception
   */
  public static Connection getConnection() throws SQLException {
    BasicDataSource ds = getDataSource(Helper.DEFAULT);
    if (ds != null) {
      Connection c = ds.getConnection();
      if (c != null) {
        c.setAutoCommit(true);
      }
      return c;
    }

    return null;
  }

  /**
   * Gets the connection by url, and create the datasource also if the name is
   * not empty
   *
   * @param name
   *          the dbname
   * @param url
   *          the url
   * @param username
   *          the user name
   * @param passwd
   *          the password
   * @return the connection by url
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnectionByUrl(String name, String url, String username, String passwd)
      throws SQLException {
    return getConnectionByUrl(name, url, username, passwd, 10);
  }

  /**
   * create the connection by the url, and create the Datasource for the name if
   * the name is not empty
   * 
   * @param name
   *          the datasource name, not create if it's empty
   * @param url
   *          the url
   * @param username
   *          the username
   * @param passwd
   *          the passwd
   * @param N
   *          the number of the datasource
   * @return the Connection
   * @throws SQLException
   *           throw SQLException
   */
  public static Connection getConnectionByUrl(String name, String url, String username, String passwd, int N)
      throws SQLException {

    BasicDataSource external = !X.isEmpty(name) ? dss.get(name) : null;
    String jar = null;
    if (external == null) {

      String D = _getDiver(url);

      log.debug("driver=" + D + ", url=" + url + ", user=" + username + ", password=" + passwd);

      if (!X.isEmpty(D)) {
        external = _get(D, username, passwd, url, N);

        if (!X.isEmpty(name)) {
          dss.put(name, external);
        }
      } else {
        throw new SQLException("unknown URL");
      }
    }

    try {

      Connection c = (external == null ? null : external.getConnection());
      // c.setAutoCommit(true);
      return c;

    } catch (SQLException e) {

      log.error(url, e);
      String error = e.getMessage();
      log.error("error=" + error + ", jar=" + jar);

      if (!X.isEmpty(error) && error.indexOf("Cannot load JDBC driver") >= 0) {
        if (!X.isEmpty(jar)) {
          // download the driver from giiwa
          int len = Http.owner.download("http://www.giiwa.org/archive/" + jar,
              new File(Model.GIIWA_HOME + "/giiwa/WEB-INF/lib/" + jar));
          if (len > 0) {
            // restart the giiwa
            System.exit(0);
          }
        } else {
          throw new SQLException(e.getMessage() + ", please download the jdbc driver, copy to {giiwa}/WEB-INF/lib");
        }
      } else {
        throw e;
      }
    }

    throw new SQLException("unknown driver, please download the jdbc driver, copy to {giiwa}/WEB-INF/lib");
  }

  /**
   * Gets the connection.
   * 
   * @param name
   *          the name
   * @return the connection
   * @throws SQLException
   *           the SQL exception
   */
  public static Connection getConnection(String name) throws SQLException {
    name = name.trim();
    BasicDataSource external = dss.get(name);
    if (external == null) {
      external = getDataSource(name);
    }

    Connection c = (external == null ? null : external.getConnection());
    if (c != null) {
      try {
        c.setAutoCommit(true);
      } catch (Exception e) {
        // possible not support
      }
    }
    return c;
  }

  public static BasicDataSource getDataSource(String name) {

    BasicDataSource external = dss.get(name);
    if (external == null && conf != null) {
      String url = conf.getString("db[" + name + "].url", null);
      String username = conf.getString("db[" + name + "].user", null);
      String passwd = conf.getString("db[" + name + "].passwd", null);

      if (!X.isEmpty(url)) {

        String D = _getDiver(url);

        int N = conf.getInt("db[" + name + "].conns", MAX_ACTIVE_NUMBER);

        external = _get(D, username, passwd, url, N);

        dss.put(name, external);
      }
    }

    return external;
  }

  private static BasicDataSource _get(String D, String username, String passwd, String url, int N) {
    BasicDataSource external = new BasicDataSource();
    external.setDriverClassName(D.trim());

    if (!X.isEmpty(username)) {
      external.setUsername(username.trim());
    }
    if (!X.isEmpty(passwd)) {
      external.setPassword(passwd.trim());
    }

    external.setUrl(url.trim());

    external.setMaxActive(N);
    external.setDefaultAutoCommit(true);
    external.setMaxIdle(N);
    external.setMaxWait(MAX_WAIT_TIME);
    external.setDefaultAutoCommit(true);
    external.setDefaultReadOnly(false);
    external.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    external.setValidationQuery(null);// VALIDATION_SQL);
    external.setPoolPreparedStatements(true);

    return external;
  }

  private static String _getDiver(String url) {
    if (url.startsWith("jdbc:hsqldb:")) {
      return "org.hsqldb.jdbc.JDBCDriver";
    } else if (url.startsWith("jdbc:derby:")) {
      return "org.apache.derby.jdbc.ClientDriver";
    } else if (url.startsWith("jdbc:firebirdsql:")) {
      return "org.firebirdsql.jdbc.FBDriver";
    } else if (url.startsWith("jdbc:sqlite:")) {
      return "org.sqlite.JDBC";
    } else if (url.startsWith("jdbc:h2:")) {
      return "org.giiwa.h2.jdbc.H2Driver";

    } else if (url.startsWith("jdbc:mongodb:")) {
      return "com.dbschema.MongoJdbcDriver";

    } else if (url.startsWith("jdbc:hive2:")) {
      return "org.apache.hive.jdbc.HiveDriver";

    } else if (url.startsWith("jdbc:postgresql:")) {
      return "org.postgresql.Driver";

    } else if (url.startsWith("jdbc:mysql:")) {
      return "com.mysql.jdbc.Driver";
    } else if (url.startsWith("jdbc:oracle:")) {
      return "oracle.jdbc.OracleDriver";
    } else if (url.startsWith("jdbc:db2:")) {
      return "com.ibm.db2.jcc.DB2Driver";
    } else if (url.startsWith("jdbc:informix-sqli:")) {
      return "com.informix.jdbc.IfxDriver";
    } else if (url.startsWith("jdbc:microsoft:sqlserver:")) {
      return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
    } else if (url.startsWith("jdbc:sqlserver:")) {
      return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    } else if (url.startsWith("jdbc:sybase:")) {
      return "net.sourceforge.jtds.jdbc.Driver";
    } else if (url.startsWith("jdbc:odbc:")) {
      return "sun.jdbc.odbc.JdbcOdbcDriver";
    }
    return null;
  }

}
