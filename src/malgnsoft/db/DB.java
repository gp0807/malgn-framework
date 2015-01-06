package malgnsoft.db;

import java.io.File;
import java.util.Hashtable;
import java.util.Enumeration;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import org.apache.commons.dbcp.BasicDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.Writer;

import malgnsoft.db.DataSet;
import malgnsoft.db.RecordSet;
import malgnsoft.util.Malgn;
import malgnsoft.util.Config;
import malgnsoft.util.SimpleParser;

public class DB {

	private static Hashtable<String, DataSource> dsTable = new Hashtable<String, DataSource>();
	private static Hashtable<String, String> dbTypes = new Hashtable<String, String>();

	private Connection _conn = null;
	private Statement _stmt = null;
	private PreparedStatement _pstmt = null;
	private int newId = 0;
	private int timeout = 0;

	private Writer out = null;
	private boolean debug = false;

	private String jndi = Config.getJndi();
	private String was = Config.getWas();

	private int errCnt = 0;
	public String errMsg = null;
	public String query = null;

	public DB() {

	}

	public DB(String jndi) {
		this.jndi = jndi;
	}

	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	public void setDebug() {
		this.debug = true;
		this.out = null;
	}
	public void setError(String msg) {
		this.errMsg = msg;
		if(debug == true) {
			try {
				if(null != out) 
					out.write("<script>try { parent.document.getElementById('sysfrm').width = '100%'; parent.document.getElementById('sysfrm').height = 700; } catch(e) {}</script><hr>" + msg + "<hr>");
				else Malgn.errorLog(msg);
			} catch(Exception e) {}
		}
	}

	public void setTimeout(int second) {
		this.timeout = second;
	}

	public String getQuery() {
		return this.query;
	}

	public String getError() {
		return this.errMsg;
	}

	public static DataSource getDataSource(String jndi) {
		return dsTable.get(jndi);
	}

	public Connection getConnection() throws Exception {
		if(dsTable == null) {
			dsTable = new Hashtable<String, DataSource>();
		}

		Connection conn = null;
		DataSource ds = dsTable.get(jndi);

		if(ds == null) {
			DataSet rs = Config.getDatabase();
			while(rs.next()) {
				if(jndi.equals(rs.getString("jndi-name"))) {
					BasicDataSource bds = null;
					try {
						bds = new BasicDataSource();            
						bds.setDriverClassName(rs.getString("driver"));
						bds.setUrl(rs.getString("url"));
						bds.setUsername(rs.getString("user"));
						bds.setPassword(rs.getString("password"));
						bds.setMaxActive(rs.getInt("max-active"));
						bds.setMinIdle(rs.getInt("min-idle"));
						bds.setMaxWait(rs.getInt("max-wait-time"));
						bds.setPoolPreparedStatements(false);

						/*
						if(rs.getString("url").indexOf("mysql") != -1) {
							bds.setValidationQuery("select 1");
							bds.setTestOnBorrow(false);
							bds.setTimeBetweenEvictionRunsMillis(1800000);
							bds.setTestWhileIdle(true);
						}
						*/

						conn = bds.getConnection();
						dsTable.put(jndi, (DataSource)bds);

						return conn;
					} catch(Exception e) {
						if(bds != null) bds.close();
						setError(e.getMessage());
						Malgn.errorLog("{DB.getConnection} " + e.getMessage(), e);
					}
					break;
				}
			}

			Context ctx = null;
			try {
				ctx = new InitialContext();
				if("resin".equals(was) || "tomcat".equals(was)) {
					try { ds = (DataSource)ctx.lookup("java:comp/env/" + jndi); }
					catch(Exception e) { 
						Malgn.errorLog("{DB.lookup} " + e.getMessage(), e);
						ds = (DataSource)ctx.lookup(jndi); 
					}
				} else {
					try { ds = (DataSource)ctx.lookup(jndi); }
					catch(Exception e) {
						Malgn.errorLog("{DB.lookup} " + e.getMessage(), e);
						ds = (DataSource)ctx.lookup("java:comp/env/" + jndi);
					}
				}
				conn = ds.getConnection();
				dsTable.put(jndi, ds);

				return conn;
			} catch(Exception e) {
				setError(e.getMessage());
				Malgn.errorLog("{DB.getConnection} " + e.getMessage(), e);
			} finally {
				if(ctx != null) try { ctx.close(); } catch(Exception e) {}
			}
		} else {
			try {
				conn = ds.getConnection();
			} catch(Exception e) {
				setError(e.getMessage());
				Malgn.errorLog("{DB.getConnection} " + e.getMessage(), e);
			}
		}

		return conn;
	}

	public String getDBType() throws Exception {
		String dbType = dbTypes.get(this.jndi);
		if(dbType == null) {
			Connection conn = this.getConnection();
			if(conn == null) return null;
			try {
				String connURL = conn.getMetaData().getURL();
				dbType = connURL.split("\\:")[1];
				if(connURL.indexOf("jdbc:sqlserver") != -1 || connURL.indexOf("jdbc:sqljdbc") != -1) dbType = "mssql";
				dbTypes.put(this.jndi, dbType);
			} catch(Exception e) {
				setError(e.getMessage());
				Malgn.errorLog("{DB.getDBType} " + e.getMessage(), e);
			} finally {
				if(conn != null) try { conn.close(); } catch(Exception e) {}
			}
		}
		return dbType;
	}

	public void close() {
		if(_stmt != null) try { _stmt.close(); } catch(Exception e) {} finally { _stmt = null; }
		if(_pstmt != null) try { _pstmt.close(); } catch(Exception e) {} finally { _pstmt = null; }
		if(_conn != null) try { _conn.close(); } catch(Exception e) {} finally { _conn = null; }
	}

	public void begin() throws Exception {
		if(_conn == null) _conn = this.getConnection();
		_conn.setAutoCommit(false);
		setError("Start Transaction");
	}

	public boolean commit() throws SQLException {
		boolean flag = false;
		if(_conn != null) {
			if(this.errCnt == 0) { 
				_conn.commit(); 
				flag = true; 
				setError("commit");
			}
			else {
				_conn.rollback();
				setError("rollback");
			}
			this.close();
			setError("End Transaction");
		} else {
			setError("Tranaction connection is null");
		}
		return flag;
	}

	public void rollback() throws SQLException {
		if(_conn != null) _conn.rollback();
	}

	public RecordSet selectLimit(String sql, int limit) throws Exception {
		String dbType = getDBType();
		sql = sql.trim();
		if("oracle".equals(dbType)) {
			sql = "SELECT * FROM (" + sql + ") WHERE rownum  <= " + limit;
		} else if("mssql".equals(dbType)) {
			sql = sql.replaceAll("(?i)^(SELECT)", "SELECT TOP(" + limit + ")");
		} else if("db2".equals(dbType)) {
			sql += " FETCH FIRST " + limit + " ROWS ONLY";
		} else {
			sql += " LIMIT " + limit;
		}
		return query(sql);
	}
	public RecordSet selectRandom(String sql, int limit) throws Exception {
		String dbType = getDBType();
		sql = sql.trim();
		if("oracle".equals(dbType)) {
			sql = "SELECT * FROM (" + sql + " ORDER BY dbms_random.value) WHERE rownum  <= " + limit;
		} else if("mssql".equals(dbType)) {
			sql = sql.replaceAll("(?i)^(SELECT)", "SELECT TOP(" + limit + ")") + " ORDER BY NEWID()";
		} else if("db2".equals(dbType)) {
			sql = sql.replaceAll("(?i)^(SELECT)", "SELECT RAND() as IDXX, ") + " ORDER BY IDXX FETCH FIRST " + limit + " ROWS ONLY";
		} else {
			sql += " ORDER BY RAND() LIMIT " + limit; 
		}
		return query(sql);
	}

	public RecordSet query(String query) throws Exception {
		if(_conn != null) return new RecordSet(executeQuery(query));
		Connection conn = getConnection();
		if(conn == null) return new RecordSet(null);

		this.query = query;
		ResultSet rs = null;
		Statement stmt = null;
		RecordSet records = null;

		try {
			setError(query);
			stmt = conn.createStatement();
			if(timeout > 0) stmt.setQueryTimeout(timeout);
			rs = stmt.executeQuery(query);
			records = new RecordSet(rs);
		} catch(Exception e) {
			setError(e.getMessage());
			Malgn.errorLog("{DB.query} " + query + " => " + e.getMessage(), e);
		} finally {
			if(rs != null) try { rs.close(); } catch(Exception e) {}
			if(stmt != null) try { stmt.close(); } catch(Exception e) {}
			if(conn != null) try { conn.close(); } catch(Exception e) {}
		}

		if(records == null) records = new RecordSet(null);
		return records;
	}

	public int execute(String query) throws Exception {
		if(_conn != null) return executeUpdate(query);
		Connection conn = getConnection();
		if(conn == null) return -1;

		this.query = query;
		Statement stmt = null;
		int ret = -1;
		try {
			setError(query);
			stmt = conn.createStatement();
			if(timeout > 0) stmt.setQueryTimeout(timeout);
			ret = stmt.executeUpdate(query);
		} catch(Exception e) {
			setError(e.getMessage());
			Malgn.errorLog("{DB.execute} " + query + " => " + e.getMessage(), e);
		} finally {
			if(stmt != null) try { stmt.close(); } catch(Exception e) {}
			if(conn != null) try { conn.close(); } catch(Exception e) {}
		}

		return ret;
	}

	public int execute(String query, Hashtable record) throws Exception {
		return execute(query, record, null);
	}
	public int execute(String query, Hashtable record, Hashtable func) throws Exception {
		if(_conn != null) {
			setCommand(query);
			if(record != null) {
                setError(record.toString());
                Enumeration keys = record.keys();
                int idx = 0;
                for(int k=1; keys.hasMoreElements(); k++) {
                    String key = (String)keys.nextElement();
                    if(null != func && func.containsKey(key)) {
                        if(((String)func.get(key)).indexOf("?") == -1) continue;
                    }
                    setParam(++idx, record.get(key));
                }
            }
			return execute();
		}
		Connection conn = this.getConnection();
		if(conn == null) return -1;

		this.query = query;
		PreparedStatement pstmt = null;
		int ret = -1;
		try {
			setError(query);
			boolean isInsertSql = query.trim().toUpperCase().startsWith("INSERT");

			if(isInsertSql && !"oracle".equals(getDBType())) {
				pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			} else {
				pstmt = conn.prepareStatement(query);
			}

			if(timeout > 0) pstmt.setQueryTimeout(timeout);
			if(record != null) {
				setError(record.toString());
				Enumeration keys = record.keys();
				int idx = 0;
				for(int k=1; keys.hasMoreElements(); k++) {
					String key = (String)keys.nextElement();
					if(null != func && func.containsKey(key)) {
						if(((String)func.get(key)).indexOf("?") == -1) continue;
					}
					pstmt.setObject(++idx, record.get(key));
				}
			}
			ret = pstmt.executeUpdate();
			if(ret == 1 && isInsertSql && !"oracle".equals(getDBType())) {
				ResultSet rs = pstmt.getGeneratedKeys();
				if (rs != null && rs.next()) {
					try { newId = rs.getInt(1); } catch(Exception e) {} finally { rs.close(); }
				}
			}
		} catch(Exception e) {
			setError(e.getMessage());
			Malgn.errorLog("{DB.execute} " + query + " => " + e.getMessage() + "\n" + record.toString(), e);
		} finally {
			if(pstmt != null) try { pstmt.close(); } catch(Exception e) {}
			if(conn != null) try { conn.close(); } catch(Exception e) {}
		}
		return ret;
	}

	public void setCommand(String cmd) throws Exception {
		if(_conn == null) _conn = this.getConnection();

		this.query = cmd;
		try {
			setError("setCommand: " + this.query);
			if(_pstmt != null) try { _pstmt.close(); } catch(Exception e) {} finally { _pstmt = null; }
			_pstmt = _conn.prepareStatement(this.query);
			if(timeout > 0) _pstmt.setQueryTimeout(timeout);
		} catch(Exception e) {
			setError(e.getMessage());
			Malgn.errorLog("{DB.setCommand} " + cmd + " => " + e.getMessage(), e);
		}
	}

	public void setParam(int i, Object param) throws Exception {
		_pstmt.setObject(i, param);
	}

	public void setParam(int i, String param) throws Exception {
		_pstmt.setString(i, param); 
	}

	public void setParam(int i, int param) throws Exception {
		_pstmt.setInt(i, param); 
	}

	public void setParam(int i, double param) throws Exception {
		_pstmt.setDouble(i, param); 
	}

	public void setParam(int i, long param) throws Exception {
		_pstmt.setLong(i, param); 
	}

	public RecordSet query() throws Exception {
		ResultSet rs = null;
		RecordSet records = null;
		try {
			rs = _pstmt.executeQuery();
			records = new RecordSet(rs);
		} catch(Exception e) {
			errCnt++;
			setError(e.getMessage());
			Malgn.errorLog("{DB.query} " + e.getMessage(), e);
		} finally {
			try { rs.close(); } catch(Exception e) {}
		}
		if(records == null) records = new RecordSet(null);
		return records;		
	}

	public int execute() throws Exception {
		int ret = -1;
		try {
			ret = _pstmt.executeUpdate();
			_pstmt.clearParameters();
		} catch(Exception e) {
			errCnt++;
			setError(e.getMessage());
			Malgn.errorLog("{DB.execute} " + e.getMessage(), e);
		}
		return ret;
	}

	public ResultSet executeQuery(String query) throws Exception {
		return executeQuery(query, timeout);
	}

	public ResultSet executeQuery(String query, int timeout) throws Exception {
		if(_conn == null) _conn = this.getConnection();

		this.query = query;
		ResultSet rs = null;
		try {
			setError(query);
			if(_stmt == null) {
				_stmt = _conn.createStatement();
				if(timeout > 0) _stmt.setQueryTimeout(timeout);
			}
			rs = _stmt.executeQuery(query);
		} catch(Exception e) {
			errCnt++;
			setError(e.getMessage());
			Malgn.errorLog("{DB.executeQuery} " + query + " => " + e.getMessage(), e);
		}

		return rs;
	}

	public int executeUpdate(String query) throws Exception {
		return executeUpdate(query, timeout);
	}

	public int executeUpdate(String query, int timeout) throws Exception {
		if(_conn == null) _conn = this.getConnection();

		this.query = query;
		int ret = -1;
		try {
			setError(query);
			if(_stmt == null) {
				_stmt = _conn.createStatement();
				if(timeout > 0) _stmt.setQueryTimeout(timeout);
			}
			ret = _stmt.executeUpdate(query);
		} catch(Exception e) {
			errCnt++;
			setError(e.getMessage());
			Malgn.errorLog("{DB.executeUpdate} " + query + " => " + e.getMessage(), e);
		}

		return ret;
	}

	public int getInsertId() {
		return newId;
	}
}