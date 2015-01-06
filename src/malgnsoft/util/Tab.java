package malgnsoft.util;

import java.util.*;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import malgnsoft.util.Malgn;

public class Tab {

	private Hashtable<String, String> data = new Hashtable<String, String>();
	private Hashtable<String, String> param = new Hashtable<String, String>();
	private boolean debug = false;
	private Writer out = null;
	private HttpServletRequest request;

	public Tab(HttpServletRequest request) {
		this.request = request;
		if(null != request.getQueryString()) {
			String[] queries = request.getQueryString().split("\\&");

			for(int i=0; i<queries.length; i++) {
				String[] attributes = queries[i].split("\\=");
				if(attributes.length == 2) {
					setVar(attributes[0], attributes[1]);
				}
			}
		}
	}

	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	public void put(String name, String value) {
		this.data.put(name, value);
	}

	public void setVar(String name, long value) {
		this.setVar(name, "" + value);
	}

	public void setVar(String name, double value) {
		this.setVar(name, "" + value);
	}

	public void setVar(String name, String value) {
		if(value == null) value = "";
		param.put(name, value);
	}

	public String create(String id) throws Exception {
		if(id == null) return "";
		String[] arr = id.split(":");
		String key = arr[0];
		int j = arr.length > 1 ? Integer.parseInt(arr[1]) : 0;

		String str = "";
		for(int i=0; i<30; i++) {
			String tab = (String)data.get(key + ":" + i);
			if(tab != null) {
				String[] arr2 = tab.split(":");
				String name = arr2[0];
				String url = arr2.length > 1 ? arr2[1] : "";
				str += i == j ? "<li class='current'>" : "<li>";
				str += "<span><a href='"+ this.getUrl(url, key + ":" + i) +"'>"+ name +"</a></span></li>";
			} else {
				break;
			}
		}

		return str;
	}

	public String getUrl(String url, String tabIndex) {
		Enumeration e = param.keys();
		while(e.hasMoreElements()) {
			String key = e.nextElement().toString();
			url = Malgn.replace(url, "{" + key + "}", param.get(key).toString());
		}
		if(url.indexOf("?") != -1) url = url + "&tab=" + tabIndex;
		else url = url + "?tab=" + tabIndex;
		return url;
	}

	private void setError(String msg) throws Exception {
		if(debug == true) {
			if(null != out) out.write("<hr>" + msg + "<hr>\n");
			else Malgn.errorLog(msg);
		}
	}
}
