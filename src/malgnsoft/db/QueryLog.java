package malgnsoft.db;

import java.util.*;
import malgnsoft.util.Malgn;

public class QueryLog extends DataObject {

	public String userId;
	public String userName;
	public String ipAddr;

	public QueryLog() {
		this.table = "tb_query_log";
	}

	public QueryLog(String table) {
		this.table = table;
	}

	public QueryLog(String userId, String userName, String ipAddr) {
		this.table = "tb_query_log";
		this.userId = userId;
		this.userName = userName;
		this.ipAddr = ipAddr;
	}

	public void log(DataObject dao, String type, String userId, String userName, String ipAddr) {
		this.item("target", dao.table);
		this.item("type", type);
		this.item("user_id", userId);
		this.item("user_name", userName);
		this.item("query", dao.getQuery());
		this.item("items", dao.record.toString());
		this.item("ip_addr", ipAddr);
		this.item("reg_date", Malgn.getTimeString("yyyyMMddHHmmss"));
		if(this.insert()) {}
	}

	public void log(DataObject dao, String type) {
		log(dao, type, userId, userName, ipAddr);
	}
}