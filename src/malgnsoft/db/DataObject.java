package malgnsoft.db;

import java.util.Date;
import java.util.Random;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.io.Writer;

import malgnsoft.db.RecordSet;
import malgnsoft.db.DB;
import malgnsoft.util.Malgn;
import malgnsoft.util.Config;

public class DataObject {

	public int limit = 1000;
	public String PK = "id";
	public String dbType = "oracle";
	public String fields = "*";
	public String table = "";
	public String orderby = null;
	public String groupby = null;
	public String join = "";
	public String jndi = Config.getJndi();
	public String seqTable = Config.get("seqTable");
	public String sql = "";
	public String id = "-1";
	public int newId = 0;
	public int seq = 0;
	public Hashtable<String, Object> record = new Hashtable<String, Object>();
	public Hashtable<String, String> func = new Hashtable<String, String>();
	public String errMsg = null;
	private Writer out = null;
	private boolean debug = false;
	public String useSeq = Config.get("useSeq");
	private QueryLog qlog = null;
	private DB db = null;

	public DataObject() {
		
	}

	public DataObject(String table) {
		this.table = table;
	}

	public void setDebug() {
		debug = true;
		this.out = null;
	}
	public void setDebug(Writer out) {
		debug = true;
		this.out = out;
	}
	public void d(Writer out) { setDebug(out); }
	public void d() { setDebug(); }

	protected void setError(String msg) {
		this.errMsg = msg;
		try {
			if(debug == true) {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Malgn.errorLog(msg);
			}
		} catch(Exception ex) {}
	}

	public void setQueryLog(QueryLog qlog) {
		this.qlog = qlog;
	}

	public void setFields(String f) {
		this.fields = f;
	}

	public void setTable(String tb) {
		this.table = tb;
	}

	public void setOrderBy(String sort) {
		this.orderby = sort;
	}

	public void addJoin(String tb, String type, String cond) {
		this.join += " " + type + " JOIN " + tb + " ON " + cond;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public String getQuery() {
		return this.sql;
	}

	public void item(String name, Object obj) {
		if(obj == null) {
			record.put(name, "");
	    } else if(obj instanceof String) {
			record.put(name, Malgn.replace((String)obj, "`", "'"));
		} else if(obj instanceof Date) {
			record.put(name, new java.sql.Timestamp(((Date)obj).getTime()));
		} else {
			record.put(name, obj);
		}
	}

	public void item(String name, int obj) {
		record.put(name, new Integer(obj));
	}

	public void item(String name, long obj) {
		record.put(name, new Long(obj));
	}

	public void item(String name, double obj) {
		record.put(name, new Double(obj));
	}

	public void item(Hashtable obj) {
		this.item(obj, "");
	}

	public void item(String name, String value, String fc) {
		record.put(name, Malgn.replace(value, "`", "'"));
		func.put(name, fc);
	}


	public void item(Hashtable obj, String exceptions) {
		Enumeration e = obj.keys();
		String[] arr = exceptions.split(",");
		while(e.hasMoreElements()) {
			String key = ((String)e.nextElement()).trim();
			if(Malgn.inArray(key, arr)) continue;
			this.item(key, null != obj.get(key) ? obj.get(key) : "");
		}
	}

	public void clear() {
		record.clear();
	}

	public RecordSet get(int i) {
		this.id = "" + i;
		return find(this.PK + " = " + i);
	}

	public RecordSet get(String id) {
		this.id = id;
		return find(this.PK + " = '" + id + "'");
	}

	public int getOneInt(String query) {
		String str = getOne(query);
		if(str.matches("^-?[0-9]+$")) {
			return Integer.parseInt(str);
		}
		return 0;
	}

	public String getOne(String query) {
		DataSet info = this.selectLimit(query, 1);
		if(info.next()) {
			Enumeration e = info.getRow().keys();
			while(e.hasMoreElements()) {
				String key = (String)e.nextElement();
				if(key.length() > 0 && "_".equals(key.substring(0, 1))) continue;
				return info.getString(key);
			}
		}
		return "";
	}

	public RecordSet find(String where) {
		return find(where, this.fields, this.orderby);
	}

	public RecordSet find(String where, String fields) {
		return find(where, fields, this.orderby);
	}

	public RecordSet find(String where, String fields, int limit) {
		return find(where, fields, this.orderby, limit);
	}

	public RecordSet find(String where, String fields, String sort) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		if(sort != null && !"".equals(sort)) sql = sql + " ORDER BY " + sort;
		return query(sql);
	}
	
	public RecordSet find(String where, String fields, String sort, int limit) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		if(sort != null && !"".equals(sort)) sql = sql + " ORDER BY " + sort;
		return selectLimit(sql, limit);
	}

    public String getDBType() {
        try {
            if(db == null) db = new DB(this.jndi);
            dbType = db.getDBType();
        } catch(Exception e) {
            this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.getDBType} " + e.getMessage(), e);
        }       
        return dbType;
    }

	public RecordSet selectLimit(String sql, int limit) {
		RecordSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(this.jndi);
			if(debug == true) db.setDebug(out);
			rs = db.selectLimit(sql, limit);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}
			formatter(rs);

		} catch(Exception e) {
			this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.selectLimit} " + e.getMessage(), e);
		}
		return rs;
	}
	public RecordSet selectRandom(String sql, int limit) throws Exception {
		RecordSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(this.jndi);
			if(debug == true) db.setDebug(out);
			rs = db.selectRandom(sql, limit);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}
			formatter(rs);

		} catch(Exception e) {
			this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.selectRandom} " + e.getMessage(), e);
		}
		return rs;
	}
	
	public int findCount(String where) {
		RecordSet rs = find(where, " COUNT(*) AS count ");
		if(rs == null || !rs.next()) {
			return 0;
		} else {
			return rs.getInt("count");
		}
	}

	public String getInsertQuery() {
		int max = record.size();
		Enumeration keys = record.keys();
		StringBuffer sb = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();

		sb.append("INSERT INTO " + this.table + " (");
		for(int k=0; keys.hasMoreElements(); k++) {
			String key = (String)keys.nextElement();
			sb.append(key);
			if(k < (max - 1)) sb.append(",");

			if(func.containsKey(key)) { sb2.append(func.get(key)); } 
			else { sb2.append("?"); }
			if(k < (max - 1)) sb2.append(",");
		}
		sb.append(") VALUES (");
		sb2.append(")");
		return sb.toString() + sb2.toString();
	}

	public boolean insert() {
		boolean seqFlag = false;
		if("Y".equals(useSeq) && PK.equals("id") && !record.containsKey("id")) {
			item("id", getSequence());
			seqFlag = true;
		}
		int ret = execute(getInsertQuery(), record, func);
		if(qlog != null) qlog.log(this, "INSERT");
		if(seqFlag) record.remove("id");
		return ret > 0 ? true : false;
	}

	public boolean replace() {
		int max = record.size();
		Enumeration keys = record.keys();
		StringBuffer sb = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();

		sb.append("REPLACE INTO " + this.table + " (");
		for(int k=0; keys.hasMoreElements(); k++) {
			String key = (String)keys.nextElement();
			sb.append(key);
			if(func.containsKey(key)) { sb2.append(func.get(key)); } 
			else { sb2.append("?"); }
			if(k < (max - 1)) {
				sb.append(",");
				sb2.append(",");
			}
		}
		sb.append(") VALUES (");
		sb2.append(")");
		String sql = sb.toString() + sb2.toString();

		int ret = execute(sql, record, func);
		if(qlog != null) qlog.log(this, "REPLACE");
		return ret > 0 ? true : false;
	}

	public String getUpdateQuery() {
		return getUpdateQuery(this.PK + " = '" + id + "'");
	}
	public String getUpdateQuery(String where) {
		int max = record.size();
		Enumeration keys = record.keys();
		StringBuffer sb = new StringBuffer();

		sb.append("UPDATE " + this.table + " SET ");
		for(int k=0; keys.hasMoreElements(); k++) {
			String key = (String)keys.nextElement();
			if(func.containsKey(key)) {
				sb.append(key + "=" + func.get(key));
			} else {
				sb.append(key + "=?");
			}
			if(k < (max - 1)) sb.append(",");
		}
		sb.append(" WHERE " + where);
		return sb.toString();
	}

	public boolean update() {
		return update(this.PK + " = '" + id + "'");
	}
	public boolean update(String where) {
		int ret = execute(getUpdateQuery(where), record, func);
		if(qlog != null) qlog.log(this, "UPDATE");
		return ret > -1 ? true : false;
	}

	public boolean delete() {
		return delete(this.PK + " = '" + this.id + "'");
	}

	public boolean delete(int id) {
		return delete(this.PK + " = " + id);
	}

	public boolean delete(String where) {
		String sql = "DELETE FROM " + this.table + " WHERE " + where;

		int ret = execute(sql);
		return ret > -1 ? true : false;
	}

	public int getInsertId() {
		if(seq > 0) return seq;
		if(newId > 0) return newId;
		RecordSet rs = query("SELECT MAX("+ this.PK +") AS id FROM "+ table);
		if(rs != null && rs.next()) return rs.getInt("id");
		else return 0;
	}

	public int getSequence() {
		DB db = new DB(jndi);
		if(debug == true) db.setDebug(out);
		if(seqTable == null || "".equals(seqTable)) seqTable = "tb_sequence";
		try {
			long stime = System.currentTimeMillis();
			db.begin();
			RecordSet rs = db.query("SELECT seq FROM " + seqTable + " WHERE id = '" + table + "'");
			if(rs != null && rs.next()) {
				db.execute("UPDATE " + seqTable + " SET seq = seq + 1 WHERE id = '" + table + "'");
				seq = rs.getInt("seq") + 1;
			} else {
				db.execute("INSERT INTO " + seqTable + " (id, seq) VALUES ('" + table + "', 1)");
				seq = 1;
			}
			db.commit();
			long etime = System.currentTimeMillis();
			setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
		} catch(Exception e) {
			db.close();
		}
		return seq;
	}

	public String getSequence(String prefix, int length) {
		return prefix + Malgn.strrpad("" + getSequence(), length, "0");
	}

	public RecordSet query(String sql) {
		RecordSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(this.jndi);
			if(debug == true) db.setDebug(out);
			rs = db.query(sql);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}
			formatter(rs);

		} catch(Exception e) {
			this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.query} " + e.getMessage(), e);
		}
		return rs;
	}

	public RecordSet query(String sql, int limit) {
		return this.selectLimit(sql, limit);
	}

	public int execute(String sql) {
		int ret = -1;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(this.jndi);
			if(debug == true) db.setDebug(out);
			ret = db.execute(sql);
			newId = db.getInsertId();

			if(ret == -1) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}

			if(qlog != null) qlog.log(this, "EXECUTE");

		} catch(Exception e) {
			this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.execute} " + e.getMessage(), e);
		}
		return ret;
	}
	
	public int execute(String sql, Hashtable record) {
		return execute(sql, record, null);
	}
	public int execute(String sql, Hashtable record, Hashtable func) {
		int ret = 0;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(this.jndi);
			if(debug == true) db.setDebug(out);
			ret = db.execute(sql, record, func);
			newId = db.getInsertId();

			if(ret == -1) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}

		} catch(Exception e) {
			this.errMsg = db.errMsg;
			Malgn.errorLog("{DataObject.execute} " + e.getMessage(), e);
		}
		return ret;
	}

	public void startTrans() {
		try {
			if(db == null) db = new DB(jndi);
			if(debug == true) db.setDebug(out);
			db.begin();
		} catch(Exception e) {}
	}

	public void startTransWith(DataObject dao1) {
		startTransWith(dao1, null, null, null, null);
	}
	public void startTransWith(DataObject dao1, DataObject dao2) {
		startTransWith(dao1, dao2, null, null, null);		
	}
	public void startTransWith(DataObject dao1, DataObject dao2, DataObject dao3) {
		startTransWith(dao1, dao2, dao3, null, null);		
	}
	public void startTransWith(DataObject dao1, DataObject dao2, DataObject dao3, DataObject dao4) {
		startTransWith(dao1, dao2, dao3, dao4, null);		
	}
	public void startTransWith(DataObject dao1, DataObject dao2, DataObject dao3, DataObject dao4, DataObject dao5) {
		startTrans();
		if(dao1 != null) dao1.setDB(db);
		if(dao2 != null) dao2.setDB(db);
		if(dao3 != null) dao3.setDB(db);
		if(dao4 != null) dao4.setDB(db);
		if(dao5 != null) dao5.setDB(db);
	}

	public boolean endTrans() {
		boolean flag = false;
		try {
			if(db != null) flag = db.commit();
		} catch(Exception e) {}
		return flag;
	}

	public DB getDB() {
		if(db == null) db = new DB(jndi);
		return db;
	}

	public void setDB(DB d) {
		if(d != null) db = d;
	}

	public void connect(DataObject dao) {
		db = dao.getDB();
	}

	public String getErrMsg() {
		return this.errMsg;
	}

	public long getNextId() {
		return System.currentTimeMillis() * 1000 + (new Random()).nextInt(999);		
	}

	public String getNextId(String prefix) {
		return prefix + getNextId();
	}

	public void formatter(DataSet rs) {

	}

}