package malgnsoft.util;

import javax.servlet.*;
import java.io.File;
import java.util.Hashtable;
import java.util.Enumeration;
import malgnsoft.db.DataSet;
import malgnsoft.util.SimpleParser;
import javax.servlet.http.HttpServletRequest;

public class Config extends GenericServlet {
	
	public static boolean loaded = false;
	private static String path;
	private static String docRoot;
	private static String tplRoot;
	private static String dataDir;
	private static String logDir;
	private static String webUrl = "";
	private static String dataUrl = "/data";
	private static String jndi = "jdbc/malgn";
	private static String mailFrom = "webmaster@test.com";
	private static String mailHost = "127.0.0.1";
	private static String secretId = "malgn-23ywx-20x05-s7399";
	private static String was = "resin";
	private static String encoding = "UTF-8";
	private static Hashtable<String, String> data = new Hashtable<String, String>();

	public void init() throws ServletException {
		ServletContext sc = getServletContext();
		load(sc, null);
	}

	public void init(ServletConfig config) throws ServletException {
		String configPath = config.getInitParameter("configPath");
		ServletContext sc = config.getServletContext();
		load(sc, configPath);
	}

	public static void load(HttpServletRequest req) throws ServletException {
		load(req.getSession().getServletContext(), null);
	} 
	public static void load(HttpServletRequest req, String configPath) throws ServletException {
		load(req.getSession().getServletContext(), configPath);
	} 

	public static void load(ServletContext sc, String configPath) throws ServletException {
		if(configPath == null) configPath = "/WEB-INF/config.xml";
		docRoot = sc.getRealPath("/").replace('\\', '/');
		if(docRoot.endsWith("/")) docRoot = docRoot.substring(0, docRoot.length() - 1);
		tplRoot = sc.getRealPath("/html").replace('\\', '/');
		dataDir = sc.getRealPath("/data").replace('\\', '/');
		logDir = dataDir + "/log";
		path = sc.getRealPath(configPath);
		reload();
	}

	public static void reload() {
		try { 
			if((new File(path)).exists()) {

				SimpleParser sp = new SimpleParser(path);
				DataSet rs = sp.getDataSet("//config/env");
				if(rs.next()) {
					Enumeration e = rs.getRow().keys();
					while(e.hasMoreElements()) {
						String key = (String)e.nextElement();
						data.put(key, rs.getString(key));
					}
				}
				if(data.containsKey("docRoot")) docRoot = get("docRoot");
				if(data.containsKey("webUrl")) webUrl = get("webUrl");
				if(data.containsKey("tplRoot")) tplRoot = get("tplRoot");
				if(data.containsKey("dataDir")) dataDir = get("dataDir");
				if(data.containsKey("dataUrl")) dataUrl = get("dataUrl");
				if(data.containsKey("logDir")) logDir = get("logDir");
				if(data.containsKey("jndi")) jndi = get("jndi");
				if(data.containsKey("mailFrom")) mailFrom = get("mailFrom");
				if(data.containsKey("mailHost")) mailHost = get("mailHost");
				if(data.containsKey("was")) was = get("was");
				if(data.containsKey("encoding")) encoding = get("encoding");
				if(data.containsKey("secretId")) secretId = get("secretId");

				loaded = true;
			}
		} catch(Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	public static DataSet getDatabase() {
		try {
			SimpleParser sp = new SimpleParser(path);
			return sp.getDataSet("//config/database");
		} catch(Exception ex) {
			ex.printStackTrace(System.out);
			return new DataSet();
		}
	}

	public void service(ServletRequest req, ServletResponse res) throws ServletException {
	}

	public static String getSecretId() {
		return secretId;
	}

	public static String getDocRoot() {
		return docRoot;
	}

	public static String getWebUrl() {
		return webUrl;
	}

	public static String getTplRoot() {
		return tplRoot;
	}

	public static String getDataDir() {
		return dataDir;
	}
	
	public static String getDataUrl() {
		return dataUrl;
	}
	
	public static String getLogDir() {
		return logDir;
	}

	public static String getJndi() {
		return jndi;
	}

	public static String getMailFrom() {
		return mailFrom;
	}

	public static String getMailHost() {
		return mailHost;
	}

	public static String getWas() {
		return was;
	}

	public static String getEncoding() {
		return encoding;
	}

	public static void set(String key, String value) {
		data.put(key, value);
	}

	public static String get(String key) {
		return data.get(key);
	}
	public static int getInt(String key) {
		int ret = 0;
		try { ret = Integer.parseInt(data.get(key)); } catch(Exception e) { }
		return ret;
	}

}