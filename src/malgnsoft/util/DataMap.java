package malgnsoft.util;

import java.util.Hashtable;
import java.util.Enumeration;

public class DataMap extends Hashtable<String, Object> {

	public DataMap() {
		super();
	}

	public boolean isset(String key) {
		return super.containsKey(key);
	}
	public String[] getKeys() {
		String[] arr = new String[size()];
		int i = 0; Enumeration e = super.keys(); 
		while(e.hasMoreElements()) arr[i++] = (String)e.nextElement();
		return arr;
	}

	public int put(String key, int value)			{ super.put(key, new Integer(value)); return value; }
	public long put(String key, long value)			{ super.put(key, new Long(value)); return value; }
	public double put(String key, double value)		{ super.put(key, new Double(value)); return value; }
	public boolean put(String key, boolean value)	{ super.put(key, new Boolean(value)); return value; }
	public Object put(String key, Object value)		{ if(value != null) super.put(key, value); return value; }

	public String s(String key)		{ return getString(key); }
	public int i(String key)		{ return getInt(key); }
	public long l(String key)		{ return getLong(key); }
	public double d(String key)		{ return getDouble(key); }
	public boolean b(String key)	{ return getBoolean(key); }

	public String getString(String key) {
		return isset(key) ? "" + super.get(key) : "";
	}
	public int getInt(String key) {
		int ret = 0;
		if(isset(key)) try { return Integer.parseInt(getString(key).trim()); } catch(Exception e) {}
		return ret;
	}
	public long getLong(String key) {
		long ret = 0;
		if(isset(key)) try { return Long.parseLong(getString(key).trim()); } catch(Exception e) {}
		return ret;
	}
	public double getDouble(String key) {
		double ret = 0.0;
		if(isset(key)) try { return Double.parseDouble(getString(key).trim()); } catch(Exception e) {}
		return ret;
	}
	public boolean getBoolean(String key) {
		boolean ret = false;
		if(isset(key)) {
			String val = getString(key).toUpperCase();
			if("Y".equals(val) || "1".equals(val) || "TRUE".equals(val)) ret = true;
		}
		return ret;
	}

}
