package malgnsoft.util;

import java.util.*;
import java.text.SimpleDateFormat;
import java.net.URLEncoder;
import java.net.URLDecoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Auth {

	private HttpServletRequest request;
	private HttpServletResponse response;
	private HttpSession session;
	private static String encoding = Config.getEncoding();
	private static String secretId = Config.getSecretId();
	private Hashtable<String, String> data;

	public String keyName = "AUTHID";
	public String loginURL = "../member/login.jsp";
	public String domain = null;
	//public int validTime = 3600 * 24;
	public int validTime = -1;
	public int maxAge = -1;

	public Auth(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.data = new Hashtable<String, String>();
	}

	public Auth(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		this.request = request;
		this.response = response;
		this.session = session;
		this.data = new Hashtable<String, String>();
	}

	public void loginForm() throws Exception {
		this.loginForm(this.loginURL);
	}

	public void loginForm(String url) throws Exception {
		int port = request.getServerPort();
		String query = request.getQueryString();

		String uri = request.getScheme() + "://" + request.getServerName();
		if(port != 80 && port != 443) uri += ":" + port;
		uri += request.getRequestURI();
		if(query != null) uri += "?" +query;

		response.sendRedirect(url + (url.indexOf("?") == -1 ? "?" : "&") + "returl=" + URLEncoder.encode(uri, encoding));
	}

	public boolean isValid() throws Exception {
		String cookie = null;
		if(session == null) {
			Cookie[] cookies = request.getCookies();
			if(cookies !=null) {
				for(int i=0; i<cookies.length; i++) {
					if(cookies[i].getName().equals(keyName)) {
						cookie = cookies[i].getValue();
					}
				}
			}
		} else {
			cookie = (String)session.getAttribute(keyName);
		}
		if(cookie == null) return false;
		String[] arr = cookie.split("\\|");

		if(arr.length != 2) return false;
		if(!arr[0].equals(Malgn.md5(arr[1] + secretId))) return false;

		return getAuthInfo(arr[1]);
	}

	public int getInt(String name) {
		int ret = 0;
		try {
			ret = Integer.parseInt(data.get(name));
		} catch(Exception e) {
			Malgn.errorLog("{Auth.getInt} " + e.getMessage(), e);
		}
		return ret;
	}

	public String getString(String name) {
		return data.get(name);
	}

	public void put(String name, String value) {
		data.put(name, value);
	}

	public void put(String name, int i) {
		put(name, "" + i);
	}

	public void setAuthInfo() throws Exception {

		Enumeration e = data.keys();
		String key = null;
		String value = null;
		StringBuffer sb = new StringBuffer();

		while(e.hasMoreElements()) {
			key = (String)e.nextElement();
			value = (String)data.get(key);
			sb.append(key + "=" + value + "|");
		}
		String info = Base64.encode(sb.toString() + System.currentTimeMillis());
		String md5 = Malgn.md5(info + secretId);

		if(session == null) {
			Cookie cookie = new Cookie(keyName, md5 + "|" + info);
		 	if(-1 != maxAge) cookie.setMaxAge(maxAge);
			cookie.setPath("/");
			if(domain != null) cookie.setDomain(domain);
			response.addCookie(cookie);
		} else {
			session.setAttribute(keyName, md5 + "|" + info);
		}
	}

	public boolean getAuthInfo(String info) throws Exception {
		try {
			String[] arr = Base64.decode(info).split("\\|");
			for(int i=0; i<arr.length; i++) {
				String[] arr2 = arr[i].split("\\=");
				if(arr2.length == 2) data.put(arr2[0], arr2[1]);
			}
			if(validTime == -1) return true;
			if(System.currentTimeMillis() <= (Long.parseLong(arr[arr.length - 1]) + validTime * 1000)) return true;
		} catch(Exception e) { 
			Malgn.errorLog("{Auth.getAuthInfo} " + e.getMessage(), e);
		}
		return false;
	}

	public void delAuthInfo() {
		if(session == null) {
			Cookie cookie = new Cookie(keyName, "");
			cookie.setMaxAge(0);
			cookie.setPath("/");
			if(domain != null) cookie.setDomain(domain);
			response.addCookie(cookie);
		} else {
			session.removeAttribute(keyName);
		}
	}

}