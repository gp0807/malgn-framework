package malgnsoft.util;

import java.util.Hashtable;
import java.util.Properties;
import java.io.*;

public class Language extends Message {

	public String dir = Config.get("langDir");

	public Language(String locale) {
		super(locale);
	}

	protected Properties getProperties() {
		Properties prop = null;
		try {
			if(propMap.containsKey(locale)) prop = propMap.get(locale);
			else {
				if(dir == null) dir = Config.getDocRoot() + "/WEB-INF/lang";
				File f = new File(dir + "/" + locale + ".properties");
				if(f.exists()) {
					prop = new Properties();
					FileInputStream fis = new FileInputStream(f);
					prop.load(fis);
					fis.close();
					propMap.put(locale, prop);
				} else {
					Malgn.errorLog("{Language.getProperties} File not found : " + f.toString());
				}
			}
		} catch(Exception e) {
			Malgn.errorLog("{Language.getProperties}", e);
		}
		return prop;
	}

	public String s(String key) throws Exception {
		return this.get(key);
	}

	public String s(String key, String[] param) throws Exception {
		return this.get(key, param);
	}

	public String[] translateArray(String[] arr) {
		return this.getArray(arr);
	}
}