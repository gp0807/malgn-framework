package malgnsoft.util;

import java.util.*;
import java.io.*;

public class Message {

	protected static Hashtable<String, Properties> propMap = new Hashtable<String, Properties>();
	protected Properties properties;
	protected String dir = Config.get("msgDir");
	protected String locale;

	public Message() {
		setLocale("default");
	}

	public Message(String locale) {
		setLocale(locale);
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	protected Properties getProperties() {
		Properties prop = null;
		try {
			if(propMap.containsKey(locale)) prop = propMap.get(locale);
			else {
				if(dir == null) dir = Config.getDocRoot() + "/WEB-INF/message";
				File f = new File(dir + "/message" + ("default".equals(locale) ? "" : "_" + locale) + ".properties");
				if(f.exists()) {
					prop = new Properties();
					FileInputStream fis = new FileInputStream(f);
					prop.load(fis);
					fis.close();
					propMap.put(locale, prop);
				} else {
					Malgn.errorLog("{Message.getProperties} File not found : " + f.toString());
				}
			}
		} catch(Exception e) {
			Malgn.errorLog("{Message.getProperties}", e);
		}
		return prop;
	}

	public String get(String key) {
		String value = null;
		try {
			if(null == properties) {
				properties = getProperties();
				if(null == properties) return key;
			}
			value = properties.getProperty(new String(key.getBytes(Config.getEncoding()), "8859_1"));
			if(value != null) value = new String(value.getBytes("8859_1"), Config.getEncoding());
		} catch(Exception e) {}
		return value != null ? value : key;
	}

	public String get(String key, String[] param) {
		String value = this.get(key);
		if(param == null) return value;
		for(int i=0; i<param.length; i++) {
			String[] tmp = param[i].split("=>");
			String id = tmp[0].trim();
			String val = (tmp.length > 1 ? tmp[1] : tmp[0]).trim();
			value = Malgn.replace(value, "{{" + id + "}}", val);
		}
		return value;
	}

	public String[] getArray(String[] arr) {
		for(int i=0; i<arr.length; i++) {
			String[] tmp = arr[i].split("=>");
			try { arr[i] = tmp[0] + "=>" + this.get(tmp[1]); }
			catch(Exception e) {}
		}
		return arr;
	}

	public Map getMap(Map map) {
		Iterator it = map.keySet().iterator();
		while(it.hasNext()) {
			String key = (String)it.next();
			map.put(key, this.get((String)map.get(key)));
		}
		return map;
	}

	public void reload() {
		reload("default");
	}

	public void reload(String locale) {
		propMap.remove(locale);
	}

	public void reloadAll() {
		propMap.clear();
	}
}