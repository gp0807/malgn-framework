package malgnsoft.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Hashtable;
import malgnsoft.util.Malgn;

public class RecordSet extends DataSet {

	public boolean labelLower = true;
	private int columnCount = 0;
	public ResultSetMetaData meta;

	public RecordSet() { }

	public RecordSet(ResultSet rs) throws Exception {
		if(rs == null) return;
		setColumnData(rs);
		
		int j = 0;
		while(rs.next()) {
			this.addRow();
			for(int i = 1; i <= columnCount; i++) {
				try {
					if(meta.getColumnType(i) == java.sql.Types.CLOB) {
						this.put(columns[i-1], rs.getString(i));
					} else if(meta.getColumnType(i) == java.sql.Types.DATE) {
						this.put(columns[i-1], rs.getTimestamp(i));
					} else {
						this.put(columns[i-1], rs.getObject(i));
					}
				} catch(Exception e) {
					this.put(columns[i-1], "");
					Malgn.errorLog("{RecordSet.constructor} " + e.getMessage(), e);
				}
			}
			this.put("__first", j == 0);
			this.put("__ord", ++j);
			this.put("__last", false);
		}
		rs.close();
		if(j > 0) this.put("__last", true);

		this.first();
	}

	public DataSet getRow(ResultSet rs) throws Exception {
		if(rs == null) return null;
		if(columnCount == 0) { setColumnData(rs); }

		DataSet ret = new DataSet();
		ret.addRow();
		for(int i = 1; i <= columnCount; i++) {
			try {
				if(meta.getColumnType(i) == java.sql.Types.CLOB) {
					ret.put(columns[i-1], rs.getString(i));
				} else if(meta.getColumnType(i) == java.sql.Types.DATE) {
					ret.put(columns[i-1], rs.getTimestamp(i));
				} else {
					ret.put(columns[i-1], rs.getObject(i));
				}
			} catch(Exception e) {
				ret.put(columns[i-1], "");
			}
		}
		return ret;
	}

	public void setColumnData(ResultSet rs) throws Exception {
		meta = rs.getMetaData();
		columnCount = meta.getColumnCount();
		
		this.columns = new String[columnCount];
		for(int i = 0; i < columnCount; i++) {
			if(labelLower == true) {
				columns[i] = meta.getColumnLabel(i+1).toLowerCase();
			} else {
				columns[i] = meta.getColumnLabel(i+1);
			}
		}
	}
}