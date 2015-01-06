package malgnsoft.util;

import java.io.Writer;
import java.io.File;
import java.util.Hashtable;
import malgnsoft.db.DataSet;
import malgnsoft.util.Malgn;

/**
 * <pre>
 * Cache cache = new Cache();
 * //cache.setDebug(out);
 * if(cache.print(out, "key")) return true;
 * ....
 * cache.savePrint("key", "data", out);
 * </pre>
 */
public class Cache {

	private int timeOut = 300; //second

	private Writer out = null;
	private boolean debug = false;

	public String errMsg = "";

	public Cache() { }

	public void setDebug() {
		this.out = null;
		this.debug = true;
	}
	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}

	private void setError(String msg) {
		this.errMsg = msg;
		try {
			if(debug == true) {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Malgn.errorLog(msg);
			}
		} catch(Exception e) {}
	}

	public void setTimeOut(int t) {
		this.timeOut = t;
	}

	public Object get(String key) {
		File f = new File(getCachePath(key));
		if(!f.exists()) {
			setError(key + " NOT EXISTS");
			return null;
		}
		if(System.currentTimeMillis() - f.lastModified() < (timeOut * 1000)) {
			return Malgn.unserialize(f);
		} else {
			if(debug) setError("Time:" + (System.currentTimeMillis() - f.lastModified()));
			return null;
		}
	}

	public String getString(String key) {
		return (String)get(key);
	}

	public Hashtable getMap(String key) {
		return (Hashtable)get(key);
	}

	public DataSet getDataSet(String key) {
		return (DataSet)get(key);
	}

	public boolean print(Writer out, String key) throws Exception {
		String data = getString(key);
		if(data != null) {
			out.write(data);
			return true;
		} else {
			return false;
		}
	}

	public void save(String key, Object data) {
		File dir = new File(Config.getDataDir() + "/cache");
		if(!dir.exists()) dir.mkdirs();

		Malgn.serialize(getCachePath(key), data);
	}

	public void savePrint(String key, Object data, Writer out) throws Exception {
		save(key, data);
		out.write(data.toString());
	}

	private String getCachePath(String key) {
		return Config.getDataDir() + "/cache/" + Malgn.md5(key);
	}
}