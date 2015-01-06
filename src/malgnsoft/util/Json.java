package malgnsoft.util;

import java.io.Writer;
import malgnsoft.json.*;
import java.util.*;

public class Json {

	private JSONObject data = new JSONObject();
	private JSONArray list = new JSONArray();
	private Vector<String> error = new Vector<String>();
	private Writer out;

	public Json(Writer out) {
		this.out = out;
	}
	public void setDebug() {
		data.put("_DEBUG_", true);
	}
	public void d() {
		setDebug();
	}
	public void put(Map<String, Object> map) {
		Iterator it = map.keySet().iterator();
		while(it.hasNext()) {
			String key = (String)it.next();
			data.put(key, map.get(key));
		}
	}
	public void put(List<Object> list) {
		this.list = new JSONArray(list);
	}
	public void put(String key, Object val) {
		data.put(key, val);
	}
	public void setError(String message) {
		error.add(message);
	}

	public void error(String message) throws Exception {
		JSONObject err = new JSONObject();
		err.put("_ERROR_", message);
		out.write(err.toString());
	}
	public void print() throws Exception {
		if(!error.isEmpty()) {
			if(error.size() == 1) {
				data.put("_ERROR_", error.get(0));
			} else {
				data.put("_ERROR_", error);
			}
		}
		out.write(data.toString());
	}
	public void printArray() throws Exception {
		out.write(list.toString());
	}
	public void clear() {
		data = new JSONObject();
		list = new JSONArray();
		error.clear();
	}
}