package malgnsoft.db;

import java.io.Reader;
import java.util.*;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

import java.io.Writer;
import malgnsoft.db.DataSet;
import malgnsoft.util.Malgn;

public class SqlMap {

	private static final int DEFAULT_TIMEOUT = 1000;
	private static SqlMapClient sqlMap = null;

	private Writer out = null;
	private boolean debug = false;

	public String errMsg = null;
	public String query = null;

	public SqlMap() {
		if(sqlMap == null) reload();
	}

	public void reload() {
		try {
			String resource = "../SqlMapConfig.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			sqlMap = SqlMapClientBuilder.buildSqlMapClient(reader);
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.reload} " + ex.getMessage(), ex);
		}
	}

	public DataSet select(String id) {
		return select(id, new HashMap());
	}

	public DataSet select(String id, HashMap pk) {
		DataSet rs = new DataSet();
		try {
			List results = (List)sqlMap.queryForList(id, pk);
			for (int i = 0; i < results.size(); i++) {
				rs.addRow();
				HashMap result = (HashMap)results.get(i);
				Iterator iter = result.keySet().iterator();
				while(iter.hasNext()) {
					String key = ((String)iter.next()).toLowerCase();
					rs.put(key, result.get(key));
				}
			}
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.select} " + ex.getMessage(), ex);
		}
		return rs;
	}

	public DataSet select(String id, String pk) {
		DataSet rs = new DataSet();
		try {
			List results = (List)sqlMap.queryForList(id, pk);
			for (int i = 0; i < results.size(); i++) {
				rs.addRow();
				HashMap result = (HashMap)results.get(i);
				Iterator iter = result.keySet().iterator();
				while(iter.hasNext()) {
					String key = ((String)iter.next()).toLowerCase();
					rs.put(key, result.get(key));
				}
			}
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.select} " + ex.getMessage(), ex);
		}
		return rs;
	}

	public boolean insert(String id, HashMap pk) {
		boolean ret = false;
		try {
			Object r = sqlMap.insert(id, pk);
			if(r != null) ret = true;
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.insert} " + ex.getMessage(), ex);
		}
		return ret;
	}

	public boolean update(String id, HashMap pk) {
		boolean ret = false;
		try {
			int r = sqlMap.update(id, pk);
			if(r >= 0) ret = true;
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.update} " + ex.getMessage(), ex);
		}
		return ret;
	}

	public boolean delete(String id, String pk) {
		boolean ret = false;
		try {
			int r = sqlMap.delete(id, pk);
			if(r >= 0) ret = true;
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.delete} " + ex.getMessage(), ex);
		}
		return ret;
	}

	public boolean delete(String id, HashMap pk) {
		boolean ret = false;
		try {
			int r = sqlMap.delete(id, pk);
			if(r >= 0) ret = true;
		} catch(Exception ex) {
			setError(ex.getMessage());
			Malgn.errorLog("{SqlMap.delete} " + ex.getMessage(), ex);
		}
		return ret;
	}

	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	public void setError(String msg) {
		this.errMsg = msg;
		if(debug == true) {
			try {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Malgn.errorLog(msg);
			} catch(Exception e) {}
		}
	}

	public String getQuery() {
		return this.query;
	}

	public String getError() {
		return this.errMsg;
	}


}
