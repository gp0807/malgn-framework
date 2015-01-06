package malgnsoft.util;

import java.io.Writer;
import java.io.*;
import java.util.*;
import java.net.*;
import malgnsoft.util.Malgn;

/**
 * <pre>
 * Http http = new Http("http://aaa.com/data/test.jsp");
 * //http.setDebug(out);
 * http.setParam("var", "aaa");
 * http.send("GET", false);
 * </pre>
 */
public class Http {

	private Writer out = null;
	private boolean debug = false;
	private String url = null;
	private Hashtable<String, String> params = new Hashtable<String, String>();
	private String encoding = Config.getEncoding();
	private String method = "GET";
	private String data = null;

	public String errMsg = "";

	public Http() { }

	public Http(String path) {
		this.url = path;
	}

	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	private void setError(String msg) throws Exception {
		this.errMsg = msg;
		if(debug == true) {
			if(null != out) out.write("<hr>" + msg + "<hr>\n");
			else Malgn.errorLog(msg);
		}
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}

	public void setUrl(String path) {
		this.url = path;
	}

	public void setData(String d) {
		this.data = d;
	}

	public void setParam(String name, String value) {
		params.put(name, value);
	}

	public String send() throws Exception {
		return send(this.method);
	}

	public void send(String method, HttpListener listener) throws Exception {
		this.method = method;
		new HttpAsync(this, listener).start();
	}

	public String send(String method) throws Exception {
		StringBuffer buffer = new StringBuffer();
		String line;

		// Construct data
		if(data == null) {
			Enumeration e = params.keys();
			while(e.hasMoreElements()) {
				String name = (String)e.nextElement();
				if(data == null) data = URLEncoder.encode(name, encoding) + "=" + URLEncoder.encode(params.get(name), encoding);
				else data += "&" + URLEncoder.encode(name, encoding) + "=" + URLEncoder.encode(params.get(name), encoding);
			}
		}

		if("GET".equals(method) && !"".equals(data)) {
			if(url.indexOf("?") > 0) {
				this.url += "&" + data;	
			} else {
				this.url += "?" + data;
			}
		}

		setError(this.url);

		URL u = new URL(this.url);
		InputStream is;
		if("POST".equals(method)) {
			URLConnection conn = u.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), encoding);
			setError(data);
			wr.write(data);
			wr.flush();
			wr.close();

			is = conn.getInputStream();
		} else {
			is = u.openStream();
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
		int i = 1;
		while((line = in.readLine()) != null) {
			if(i > 1000) break;
			buffer.append(line + "\r\n");
			i++;
		}
		in.close();

		return buffer.toString();
	}

	public String getUrl() {
		return this.url;
	}

}

class HttpAsync extends Thread {

	private Http http = null;
	private HttpListener listener = null;
	private String result = null;

	public HttpAsync(Http h, HttpListener l) {
		http = h;
		listener = l;
	}

	public void run() {
		try {
			result = http.send();
			if(listener != null) listener.execute(result);
		} catch(Exception e) {
			Malgn.errorLog("{HttpAsync.run} " + e.getMessage(), e);
		}
	}
}