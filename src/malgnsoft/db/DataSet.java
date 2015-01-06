package malgnsoft.db;

import java.util.*;
import java.io.Writer;
import java.math.*;
import malgnsoft.util.Malgn;
import malgnsoft.json.*;

public class DataSet extends Vector<Hashtable<String, Object>> {

	private Writer out = null;
	private boolean debug = false;
	private int idx = -1;
	public String[] columns;
	public int[] types;
	public int sortType = -1;
	
	public DataSet() {

	}

	public DataSet(DataSet ds) {
		ds.first();
		while(ds.next()) {
			this.addRow(ds.getRow());
		}
		this.first();
	}

	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	public boolean next() {
		if(this.size() <= (idx + 1)) return false;
		
		idx = idx + 1;
		return true;
	}

	public boolean move(int id) {
		if(id > -1 && id >= this.size()) return false;
		idx = id;
		return true;
	}

	public int getIndex() {
		return idx;
	}

	public int addRow() {
		this.addElement(new Hashtable<String, Object>());
		idx++;
		return idx;
	}

	public int addRow(Hashtable<String, Object> map) {
		if(map != null) {
			this.addElement(new Hashtable<String, Object>(map));
			idx++;
		}
		return idx;
	}

	public boolean updateRow(Hashtable data) {
		if(data == null) return false;
		if(idx > -1) {
			Hashtable map = this.get(idx);
			if(map == null) return false;
			
			Enumeration e = map.keys();
			while(e.hasMoreElements()) {
				String key = (String)(e.nextElement());
				if(data.containsKey(key)) {
					this.put(key, data.get(key));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean updateRow(int id, Hashtable data) {
		if(!move(id)) return false;
		return updateRow(data);
	}

	public void removeAll() {
		this.removeAllElements();
		idx = -1;
	}

	public boolean prev() {
		idx = idx - 1;
		if(idx < 0) {
			idx = 0;
			return false;
		} else {
			return true;
		}
	}

	public boolean first() {
		idx = -1;
		return true;
	}

	public boolean last() {
		idx = this.size() - 1;
		return true;
	}

	public void put(String name, int i) {
		this.put(name, new Integer(i));
	}

	public void put(String name, double d) {
		this.put(name, new Double(d));
	}

	public void put(String name, boolean b) {
		this.put(name, new Boolean(b));
	}

	public void put(String name, Object value) {
		if(value == null) value = "";
		this.get(idx).put(name, value);
	}

	public Object get(String name) {
		if(idx < 0) return null;
		Object ret = null;
		try {
			Hashtable map = this.get(idx);
			if(map != null && map.containsKey(name)) {
				ret = map.get(name);
			}
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.get} " + e.getMessage(), e);
		}

		return ret;
	}

	public String getString(String name) {
		if(idx < 0) return "";
		String ret = "";
		try {
			Hashtable map = this.get(idx);
			if(map != null && map.containsKey(name)) {
				ret = map.get(name).toString();
			}
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getString} " + e.getMessage(), e);
		}

		return ret;
	}
	
	public int getInt(String name) {
		int ret = 0;
		try {
			String val = getString(name).trim();
			if(val != null && !"".equals(val)) ret = Integer.parseInt(val);
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getInt} " + e.getMessage(), e);
		}

		return ret;
	}

	public long getLong(String name) {
		long ret = 0;
		try {
			String val = getString(name).trim();
			if(val != null && !"".equals(val)) ret = Long.parseLong(val);
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getLong} " + e.getMessage(), e);
		}

		return ret;
	}

	public double getDouble(String name) {
		double ret = 0.0;
		try {
			String val = getString(name).trim();
			if(val != null && !"".equals(val)) ret = Double.parseDouble(val);
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getDouble} " + e.getMessage(), e);
		}

		return ret;
	}

	public boolean getBoolean(String name) {
		boolean ret = false;
		try {
			String val = getString(name).trim();
			if(val != null) {
				val = val.toUpperCase();
				if("Y".equals(val) || "1".equals(val) || "TRUE".equals(val)) ret = true;
			}
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getDouble} " + e.getMessage(), e);
		}

		return ret;
	}

	public String s(String name) { return getString(name); }
	public int i(String name) { return getInt(name); }
	public long l(String name) { return getLong(name); }
	public double d(String name) { return getDouble(name); }
	public boolean b(String name) { return getBoolean(name); }

	public Date getDate(String name) {
		Date ret = null;
		try {
			ret = (Date)(this.get(idx).get(name));
		} catch(Exception e) {
			Malgn.errorLog("{DataSet.getDate} " + e.getMessage(), e);
		}

		return ret;
	}

	public Vector getRows() {
		return this;
	}

	public Hashtable<String, Object> getRow(int id) {
		if(move(id)) return getRow();
		else return null;
	}

	public Hashtable<String, Object> getRow() {
		if(idx > -1) {
			return new Hashtable<String, Object>(this.get(idx));
		} else {
			return null;
		}
	}

	public String[] getColumns() {
		return columns;
	}

	public String[] getKeys() {
		if(idx > -1) {
			Hashtable map = this.get(idx);
			if(map == null) return null;
			
			Enumeration e = map.keys();
			int i = 0;
			String keys = "";
			while(e.hasMoreElements()) {
				keys += "," + (String)(e.nextElement());
				i++;
			}
			if(i > 0) keys = keys.substring(1);
			return keys.split(",");
		} else {
			return null;
		}		
	}

	public boolean isColumn(String key) {
		if(columns != null) {
			return Malgn.inArray(key, columns);
		} else {
			return false;
		}
	}

	public boolean isKey(String key) {
		if(idx > -1) {
			Hashtable map = this.get(idx);
			if(map == null) return false;
			return map.containsKey(key);
		} else {
			return false;
		}	
	}

	public DataSet search(String key, String value, String op) {
		DataSet list = new DataSet();
		this.first();
		while(this.next()) {
			boolean flag = false;
			if("%".equals(op)) {
				flag = this.getString(key).indexOf(value) != -1;
			} else if("!%".equals(op)) {
				flag = this.getString(key).indexOf(value) == -1;
			} else if("!".equals(op)) {
				flag = !this.getString(key).equals(value);
			} else if("^".equals(op)) {
				flag = this.getString(key).matches(value);
			} else {
				flag = this.getString(key).equals(value);
			}
			if(flag) list.addRow(this.getRow());
		}
		this.first();
		list.first();
		return list;
	}
	public DataSet search(String key, String value) {
		return search(key, value, "=");
	}

	private String _key, _ord = "asc";
	public void sort(String key) {
		sort(key, "asc");
	}
	public void sort(String key, String ord) {
		if(key == null || "".equals(key)) return;
		this._key = key;
		this._ord = ord.toLowerCase();
		if(!"desc".equals(this._ord)) this._ord = "asc";
		Comparator<Object> LocalSort = new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				Hashtable h1 = (Hashtable)o1, h2 = (Hashtable)o2;
				Object value = h1.get(_key);
				if(null == value) return 0;

				String _type = "s";
				if(value instanceof Integer) _type = "i";
				else if(value instanceof Long) _type = "l";
				else if(value instanceof Double) _type = "d";
				else if(value instanceof BigDecimal) _type = "b";

				int op = "desc".equals(_ord) ? -1 : 1;
				if("i".equals(_type)) return op * ((Integer)h1.get(_key) < (Integer)h2.get(_key) ? -1 : 1);
				else if("l".equals(_type)) return op * ((Long)h1.get(_key) < (Long)h2.get(_key) ? -1 : 1);
				else if("d".equals(_type)) return op * ((Double)h1.get(_key) < (Double)h2.get(_key) ? -1 : 1);
				else if("b".equals(_type)) return op * (((BigDecimal)h1.get(_key)).longValueExact() < ((BigDecimal)h2.get(_key)).longValueExact() ? -1 : 1);
				else return op * ((h1.get(_key).toString()).compareTo(h2.get(_key).toString()) < 0 ? -1 : 1);
			}
		};
		Collections.sort(this, LocalSort);
	}

	public String serialize() {
		return new JSONArray(this).toString();
	}
	public void unserialize(String str) {
		JSONArray arr = new JSONArray(str);
		this.removeAll();
		for(int i=0; i<arr.length(); i++) {
			Hashtable<String, Object> map = new Hashtable<String, Object>();
			JSONObject obj = (JSONObject)arr.get(i);
			Iterator it = obj.keys();
			while(it.hasNext()) {
				String key = (String)it.next();
				map.put(key, obj.get(key));
			}
			this.addRow(map);
		}
	}

}