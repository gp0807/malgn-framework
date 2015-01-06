package malgnsoft.util;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.Writer;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import javax.naming.*;
import javax.naming.directory.*;

import malgnsoft.db.*;
import malgnsoft.json.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.swing.ImageIcon;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Malgn {

	public String secretId = "sdhflsdhflsdxxx";
	public String cookieDomain = null;
	public static String encoding = Config.getEncoding();
	public String mailFrom = Config.getMailFrom();
	public String mailHost = Config.getMailHost();
	public static int mailThreadNum = 0;
	public String dataDir = null;
	public String dataUrl = null;

	private HttpServletRequest request;
	private HttpServletResponse response;
	private HttpSession session;
	private Writer out;
	private Message message;

	public Malgn() {
	}
	public Malgn(HttpServletRequest request, HttpServletResponse response, Writer out) {
		this.request = request;
		this.response = response;
		this.out = out;
		this.session = request.getSession();
		try { if(!Config.loaded) Config.load(request); } catch(Exception e) {}
	}

	public void setMessage(Message msg) {
		this.message = msg;
	}

	public String qstr(String str) {
		return replace(str, "'", "''");
	}

	public String request(String name) {
		return request(name, "");
	}

	public String request(String name, String str) {
		String value = request.getParameter(name);
		if(value == null) {
			return str;
		} else {
			return replace(replace(value.replace('\'', '`'), "<", "&lt;"), ">", "&gt;");
		}
	}
	public String rs(String name) { return request(name, ""); }
	public String rs(String name, String str) { return request(name, str); }

	public int reqInt(String name) {
		return reqInt(name, 0);
	}

	public int reqInt(String name, int i) {
		String str = request(name, i + "");
		try {
			if(str.matches("^-?[\\,0-9]+$")) i = Integer.parseInt(replace(str, ",", ""));
		} catch(Exception e) { }
		return i;
	}
	public int ri(String name) { return reqInt(name, 0); }
	public int ri(String name, int i) { return reqInt(name, i); }


	public String reqSql(String name) {
		return replace(request(name, ""), "'", "''");
	}

	public String reqSql(String name, String str) {
		return replace(request(name, str), "'", "''");
	}

	public String[] reqArr(String name) {
		return request.getParameterValues(name);
	}

	public String reqEnum(String name, String[] arr) {
		if(arr == null) return null;
		String str = request(name);
		for(int i=0; i<arr.length; i++) {
			if(arr[i].equals(str)) return arr[i];
		}
		return arr[0];
	}

	public static int parseInt(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str);
		else return 0;
	}

	public static long parseLong(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Long.parseLong(str);
		else return 0;
	}

	public static double parseDouble(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str) * 1.0;
		else if(str != null && str.matches("^-?[0-9]+\\.[0-9]+$")) return Double.parseDouble(str);
		else return 0.0;
	}

	public Hashtable reqMap(String name) {
		Hashtable<String, String> map = new Hashtable<String, String>();
		int len = name.length();
		try {
			Enumeration e = request.getParameterNames();
			while(e.hasMoreElements()) {
				String key = (String)e.nextElement();
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key.substring(len), request.getParameter(key));
				}
			}
		} catch(Exception ex) {
			errorLog("{Malgn.reqMap} " + ex.getMessage());
		}
		return map;
	}

	public void redirect(String url) {
		try {
			response.sendRedirect(url);
		} catch(Exception e) {
			errorLog("{Malgn.redirect} " + e.getMessage());
			jsReplace(url);
		}
	}

	public boolean isPost() {
		if("POST".equals(request.getMethod())) {
			return true;
		} else {
			return false;
		}
	}

	public void jsAlert(String msg) {
		if(message != null) msg = message.get(msg);
		try {
			out.write("<script>alert('" + replace(msg, "\'", "\\\'") + "');</script>");
		} catch(Exception e) {
			errorLog("{Malgn.jsAlert} " + e.getMessage());
		}
	}

	public void jsError(String msg) {
		if(message != null) msg = message.get(msg);
		try {
			out.write("<script>alert('" + msg + "');history.go(-1)</script>");
		} catch(Exception e) {
			errorLog("{Malgn.jsError} " + e.getMessage());
		}
	}

	public void jsError(String msg, String target) {
		if(message != null) msg = message.get(msg);
		try {
			out.write("<script>alert('" + msg + "');" + target + ".location.href = " + target + ".location.href;</script>");
		} catch(Exception e) {
			errorLog("{Malgn.jsError} " + e.getMessage());
		}
	}

	public void js(String str) {
		try {
			out.write("<script type=\"text/javascript\">");
			out.write(str);
			out.write("</script>");
		} catch(Exception e) {
			errorLog("{Malgn.js} " + e.getMessage());
		}
	}

	public void jsErrClose(String msg) {
		jsErrClose(msg, null);
	}

	public void jsErrClose(String msg, String tgt) {
		if(message != null) msg = message.get(msg);
		try {
			if(tgt == null) tgt = "window";
			out.write("<script>alert('" + msg + "');" + tgt + ".close()</script>");
		} catch(Exception e) {
			errorLog("{Malgn.jsErrClose} " + e.getMessage());
		}
	}

	public void jsReplace(String url) {
		jsReplace(url, "window");
	}

	public void jsReplace(String url, String target) {
		try {
			out.write("<script>"+ target +".location.replace('" + url + "');</script>");
		} catch(Exception e) {
			errorLog("{Malgn.jsReplace} " + e.getMessage());
		}
	}

	// Get Cookie
	public String getCookie(String s) throws Exception {
		Cookie[] cookie = request.getCookies();
		if(cookie == null) return "";
		for(int i = 0; i < cookie.length; i++) {
			if(s.equals(cookie[i].getName())) {
				String value = URLDecoder.decode(cookie[i].getValue(), encoding);
				return value;
			}
		}
		return "";
	}

	// Set Cookie
	public void setCookie(String name, String value) throws Exception {
		Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

	public void setCookie(String name, String value, int time) throws Exception {
		Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		cookie.setMaxAge(time);
		response.addCookie(cookie);
	}

	// Delete Cookie
	public void delCookie(String name) {
		Cookie cookie = new Cookie(name, "");
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	// Get Session
	public String getSession(String s) {
		Object obj = session.getAttribute(s);
		if(obj == null) return "";
		return (String)obj;
	}

	// Set Session
	public void setSession(String name, String value) {
		session.setAttribute(name, value);
	}

	// Set Session
	public void setSession(String name, int value) {
		session.setAttribute(name, ""+value);
	}

	public static String getTimeString() {
		return getTimeString("yyyyMMddHHmmss");
		//return getTimeString("yyyy-MM-dd HH:mm:ss");
	}

	// Get DateTime String
	public static String getTimeString(String sformat) {
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		return sdf.format((new GregorianCalendar()).getTime());
	}

	// Get DateTime String
	public static String getTimeString(String sformat, Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(sdf == null || date == null) return "";
		return sdf.format(date);
	}

	// Get DateTime String
	public static String getTimeString(String sformat, String date) {
		Date d = strToDate(date.trim());
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(sdf == null || d == null) return "";
		return sdf.format(d);
	}

    // Get DateTime String
    public static String getTimeString(String sformat, Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(sdf == null || date == null) return "";
        return sdf.format(date);
    }

    // Get DateTime String
    public static String getTimeString(String sformat, String date, String timezone) {
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(sdf == null || d == null) return "";
        return sdf.format(d);
    }

	// Get DateTime String with locale
    public static String getTimeString(String sformat, Date date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(sdf == null || date == null) return "";
        return sdf.format(date);
    }
    public static String getTimeString(String sformat, String date, Locale locale) {
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(sdf == null || d == null) return "";
        return sdf.format(d);
    }
	public static String time() { return getTimeString(); }
	public static String time(String sformat) { return getTimeString(sformat); }
	public static String time(String sformat, Date date) { return getTimeString(sformat, date); }
	public static String time(String sformat, String date) { return getTimeString(sformat, date); }
	public static String time(String sformat, Date date, String tz) { return getTimeString(sformat, date, tz); }
	public static String time(String sformat, String date, String tz) { return getTimeString(sformat, date, tz); }
	public static String time(String sformat, String date, Locale locale) { return getTimeString(sformat, date, locale); }


	public static int diffDate(String type, String sdate, String edate) {
		int ret = 0;
		try {
			Date d1 = strToDate(sdate.trim());
			Date d2 = strToDate(edate.trim());

			long diff =	d2.getTime() - d1.getTime();
			type = type.toUpperCase();
			if("D".equals(type)) ret = (int)(diff / (long)(1000 * 3600 * 24));
			else if("H".equals(type)) ret = (int)(diff / (long)(1000 * 3600));
			else if("I".equals(type)) ret = (int)(diff / (long)(1000 * 60));
			else if("S".equals(type)) ret = (int)(diff / (long)1000);
			else if("Y".equals(type) || "M".equals(type)) {
				Calendar startCalendar = new GregorianCalendar();
				startCalendar.setTime(d1);
				Calendar endCalendar = new GregorianCalendar();
				endCalendar.setTime(d2);

				ret = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
				if("M".equals(type)) {
					ret = ret * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
				}
			}
		} catch(Exception e) {
			errorLog("{Malgn.diffDate} ", e);
		}
		return ret;
	}

	public static Date addDate(String type, int amount) {
		return addDate(type, amount, new Date());
	}

	public static Date addDate(String type, int amount, String d) {
		return addDate(type, amount, strToDate(d));
	}

	public static Date addDate(String type, int amount, Date d) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			type = type.toUpperCase();
			if("Y".equals(type)) cal.add(cal.YEAR, amount);
			else if("M".equals(type)) cal.add(cal.MONTH, amount);
			else if("W".equals(type)) cal.add(cal.WEEK_OF_YEAR, amount);
			else if("D".equals(type)) cal.add(cal.DAY_OF_MONTH, amount);
			else if("H".equals(type)) cal.add(cal.HOUR_OF_DAY, amount);
			else if("I".equals(type)) cal.add(cal.MINUTE, amount);
			else if("S".equals(type)) cal.add(cal.SECOND, amount);
			return cal.getTime();
		} catch(Exception e) {
			errorLog("{Malgn.addDate} ", e);
		}
		return null;
	}

	public static String addDate(String type, int amount, String d, String format) {
		return addDate(type, amount, strToDate(d), format);
	}

	public static String addDate(String type, int amount, Date d, String format) {
		return getTimeString(format, addDate(type, amount, d));
	}

	public static Date strToDate(String format, String source, Locale loc) {
		if(source == null || "".equals(source)) return null;

		SimpleDateFormat sdf = new SimpleDateFormat(format, loc);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Malgn.strToDate} ", e) ;
		}
		return d;
	}

	public static Date strToDate(String format, String source) {
		if(source == null || "".equals(source)) return null;

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Malgn.strToDate} ", e) ;
		}
		return d;
	}

	public static Date strToDate(String source) {
		if(source == null || "".equals(source)) return null;

		String format = "yyyyMMddHHmmss";
		if(source.matches("^[0-9]{8}$")) format = "yyyyMMdd";
		else if(source.matches("^[0-9]{14}$")) format = "yyyyMMddHHmmss";
		else if(source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) format = "yyyy-MM-dd";
		else if(source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$ [0-9]{2}:[0-9]{2}:[0-9]{2}")) format = "yyyy-MM-dd HH:mm:ss";

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Malgn.strToDate} ", e) ;
		}
		return d;
	}

	public static double getPercent(int cnt, int total) {
		if(total <= 0) return 0.0;
		return java.lang.Math.round(((double)cnt / (double)total) * 100);
	}

	public static String md5(String str) { return encrypt(str); }
	public static String sha1(String str) { return encrypt(str, "SHA-1"); }
	public static String sha256(String str) { return encrypt(str, "SHA-256"); }

	public static String encrypt(String str) { return encrypt(str, "MD5", encoding); }
	public static String encrypt(String str, String algorithm) { return encrypt(str, algorithm, encoding); }
	public static String encrypt(String str, String algorithm, String charset) {
		StringBuffer sb = new StringBuffer();
        try {
			MessageDigest di = MessageDigest.getInstance(algorithm.toUpperCase());
			di.update(new String(str).getBytes(charset));
			byte[] md5Code = di.digest();
			for (int i=0;i<md5Code.length;i++) {
				String md5Char = String.format("%02x", 0xff&(char)md5Code[i]);
				sb.append(md5Char);
			}
		} catch (Exception e) {
			errorLog("{Malgn.encrypt} " + e.getMessage());
		}
        return sb.toString();
    }

	public static String getFileExt(String filename) {
		int i = filename.lastIndexOf(".");
		if(i == -1) return "";
		else return filename.substring(i+1);
	}

	public String getUploadUrl(String filename) {
		return getUploadUrl(filename, dataUrl);
	}
	public static String getUploadUrl(String filename, String dataUrl) {
		if("".equals(filename)) return "noimg";
		if(dataUrl == null) dataUrl = Config.getDataUrl();
		String ext = getFileExt(filename);
		if("jsp".equals(ext.toLowerCase())) ext = "xxx";
		String md5name = getFileMD5(filename + "sdhflsdhflsdxxx") + "." + ext;
		return dataUrl + "/file/" + md5name;
	}

	public String getUploadPath(String filename) {
		return getUploadPath(filename, dataDir);
	}
	public static String getUploadPath(String filename, String dataDir) {
		if(dataDir == null) dataDir = Config.getDataDir();
		String ext = getFileExt(filename);
		if("jsp".equals(ext.toLowerCase())) ext = "xxx";
		String md5name = getFileMD5(filename + "sdhflsdhflsdxxx") + "." + ext;
		return dataDir + "/file/" + md5name;
	}
	public static String getFileMD5(String str) {
		StringBuffer buf = new StringBuffer();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] data = new byte[32];
			md.update(str.getBytes("KSC5601"), 0, str.length());
			data = md.digest();
			for (int i = 0; i < data.length; i++) {
				int halfbyte = (data[i] >>> 4) & 0x0F;
				int two_halfs = 0;
				do {
					if ((0 <= halfbyte) && (halfbyte <= 9))
						buf.append((char) ('0' + halfbyte));
					else
						buf.append((char) ('a' + (halfbyte - 10)));
					halfbyte = data[i] & 0x0F;
				} while(two_halfs++ < 1);
			}
		} catch (Exception e) {
			errorLog("{Malgn.getMD5} " + e.getMessage());
		}
		return buf.toString();
	}

	public String getQueryString(String exception) {
		String query = "";
		if(null != request.getQueryString()) {
			String[] exceptions = exception.replaceAll(" +", "").split("\\,");
			String[] queries = request.getQueryString().split("\\&");

			for(int i=0; i<queries.length; i++) {
				String que = replace(queries[i], new String[] { "<", ">", "'", "\"" }, new String[] { "&lt;", "&gt;", "&#39;", "&quot;" });
				String[] attributes = que.split("\\=");
				if(attributes.length > 0 && inArray(attributes[0], exceptions)) continue;
				query += "&" + que;
			}
		}
		return query.length() > 0 ? query.substring(1) : "";
	}
	public String getQueryString() {
		return getQueryString("");
	}
	public String qs(String exception) { return getQueryString(exception); }
	public String qs() { return getQueryString(""); }

	public String getThisURI() {
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		String thisuri = "";

		if(query == null) thisuri = uri;
		else thisuri = uri + "?" + query;

		return thisuri;
	}

	public void log(String msg) throws Exception {
		log("debug", msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg) throws Exception {
		log(prefix, msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg, String fmt) throws Exception {
    	String logDir = Config.getLogDir();
        try {
            if(logDir == null) logDir = "/tmp";
			File log = new File(logDir);
			if(!log.exists()) log.mkdirs();
			FileWriter logger = new FileWriter(logDir + "/" + prefix + "_" + getTimeString(fmt) + ".log", true);
			logger.write("["+getTimeString("yyyy-MM-dd HH:mm:ss")+"] "+request.getRemoteAddr()+" : "+getThisURI()+"\n"+msg+"\n");
			logger.close();
		} catch(Exception e) {
            e.printStackTrace(System.out);
		}
	}

    public static void errorLog(String msg) {
        errorLog(msg, null);
    }

    public static void errorLog(String msg, Exception ex) {
    	String logDir = Config.getLogDir();
        try {
            if(logDir == null) logDir = "/tmp";
			File log = new File(logDir);
			if(!log.exists()) log.mkdirs();
            if(ex != null) {
                StackTraceElement[] arr = ex.getStackTrace();
                for(int i=0; i<arr.length; i++) {
                    if(arr[i].getClassName().indexOf("_jsp") != -1)
                        msg = "at " + replace(replace(replace(arr[i].getClassName(), "__jsp", ".jsp"), "_jsp", ""), "._", "/")
                        + "\n" +  msg + " (" + ex.getMessage() + ")";
                }
            }
            FileWriter logger = new FileWriter(logDir + "/error_" + getTimeString("yyyyMMdd") + ".log", true);
            logger.write("["+getTimeString("yyyy-MM-dd HH:mm:ss")+"] "+ msg +"\n");
            logger.close();
        } catch(Exception e) {
            e.printStackTrace(System.out);
        }
    }

	public String getMX(String domain) throws Exception {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		DirContext ictx = new InitialDirContext(env);
		Attributes attrs = ictx.getAttributes(domain, new String[] { "MX" });
		Attribute attr = attrs.get("MX");

		if (( attr == null ) || ( attr.size() == 0 )) {
			attrs = ictx.getAttributes(domain, new String[] { "A" });
			attr = attrs.get("A");
			if( attr == null )
			throw new Exception( "No match for name '" + domain + "'" );
		}

		String x = (String)attr.get(0);
		String[] f = x.split(" ");
		if(f[1].endsWith(".")) f[1] = f[1].substring(0, (f[1].length() - 1));

		return f[1];
	}

	// Send Mail
	public void mail(String mailTo, String subject, String body) throws Exception {
		mail(mailTo, subject, body, null);
	}

	public void mail(String mailTo, String subject, String body, String filepath) throws Exception {
		try {
			if(mailHost == null) {
				String[] arr = mailTo.split("@");
				mailHost = getMX(replace(arr[1], ">", ""));
			}

			Properties props = new Properties();
			props.put("mail.smtp.host", mailHost);

			Session msgSession = Session.getDefaultInstance(props, null);

			MimeMessage msg = new MimeMessage(msgSession);
			InternetAddress ret = new InternetAddress(mailFrom);
			InternetAddress from = new InternetAddress(Config.getMailFrom());

			if(null != ret.getPersonal()) {
				ret.setPersonal(ret.getPersonal(), encoding);
				from.setPersonal(ret.getPersonal(), encoding);
			} else from.setPersonal(mailFrom.split("\\@")[0], encoding);

			InternetAddress to = new InternetAddress(mailTo);
			if(null != to.getPersonal()) to.setPersonal(to.getPersonal(), encoding);

			msg.setFrom(from);
			msg.setRecipient(MimeMessage.RecipientType.TO, to);
			msg.setSubject(subject, encoding);
			msg.setSentDate(new Date());
			msg.setReplyTo(new Address[] { ret });

			if(filepath == null) {
				msg.setContent(body, "text/html; charset=" + encoding);
			} else {
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setContent(body, "text/html; charset=" + encoding);
				MimeBodyPart mbp2 = new MimeBodyPart();

				FileDataSource fds = new FileDataSource(filepath);
				mbp2.setDataHandler(new DataHandler(fds));
				mbp2.setFileName(fds.getName());

				Multipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);
				mp.addBodyPart(mbp2);

				msg.setContent(mp);
			}

			Transport.send(msg);
		} catch(Exception ex) {
			errorLog("{Malgn.mail} " + ex.getMessage());
		}
	}

	// Get Unique ID
	public static String getUniqId() {
		String chars = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random r = new Random();
		char[] buf = new char[10];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = chars.charAt(r.nextInt(chars.length()));
		}
		return new String(buf);
	}

	public static String repeatString(String src, int repeat) {
		StringBuffer buf=new StringBuffer();
		for (int i=0; i<repeat; i++) {
			buf.append(src);
		}
		return buf.toString();
	}

	public static String cutString(String str, int len) throws Exception {
		return cutString(str, len, "...");
	}

	public static String cutString(String str, int len, String tail) throws Exception {
		try  {
			byte[] by = str.getBytes("KSC5601");
			if(by.length <= len) return str;
			int count = 0;
			for(int i = 0; i < len; i++) {
				if((by[i] & 0x80) == 0x80) count++;
			}
			if((by[len - 1] & 0x80) == 0x80 && (count % 2) == 1) len--;
			len = len - (int)(count / 2);
			return str.substring(0, len) + tail;
		} catch(Exception e) {
			errorLog("{Malgn.cutString} " + e.getMessage());
			return "";
		}
	}

	public static boolean inArray(String str, String[] array) {
		return inArray(str, join(",", array));
	}
	public static boolean inArray(int value, String values) {
		return inArray("" + value, values);
	}
	public static boolean inArray(String value, String values) {
		return ("," + replace(values, " ", "") + ",").indexOf("," + value + ",") != -1;
	}

	public static String join(String str, Object[] array) {
		if(str != null && array != null) {
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<array.length; i++) {
				sb.append(array[i].toString());
				if(i < (array.length - 1)) sb.append(str);
			}
			return sb.toString();
		}
		return "";
	}
	public static String join(String str, Hashtable map) {
		StringBuffer sb = new StringBuffer();
		Enumeration e = map.keys();
		int size = map.size(), i = 0;
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = map.get(key) != null ? map.get(key).toString() : "";

			sb.append(value);
			if(i < (size - 1)) sb.append(str);
			i++;
		}
		return sb.toString();
	}


	public static DataSet arr2loop(String[] arr) {
		return arr2loop(arr, false);
	}

	public static DataSet arr2loop(String[] arr, boolean empty) {
		DataSet result = new DataSet();
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				String[] tmp = arr[i].split("=>");
				String id = tmp[0].trim();
				String value = (tmp.length > 1 ? tmp[1] : (empty ? "" : tmp[0])).trim();
				result.addRow();
				result.put("id", id);
				result.put("value", value);
				result.put("name", value);
				result.put("__first", i == 0 ? "true" : "false");
				result.put("__last", i == arr.length - 1 ? "true" : "false");
				result.put("__idx", i + 1);
				result.put("__ord", arr.length - i);
			}
		}
		result.first();
		return result;
	}

	public static DataSet arr2loop(Hashtable map) {
		DataSet result = new DataSet();
		Enumeration e = map.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = map.get(key) != null ? map.get(key).toString() : "";

			result.addRow();
			result.put("id", key);
			result.put("value", value);
			result.put("name", value);
		}
		result.first();
		return result;
	}

	public static String getItem(int item, String[] arr) {
		return getItem(item + "", arr);
	}

	public static String getItem(String item, String[] arr) {
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				String[] tmp = arr[i].split("=>");
				String id = tmp[0].trim();
				String value = (tmp.length > 1 ? tmp[1] : tmp[0]).trim();
				if(id.equals(item)) return value;
			}
		}
		return "";
	}
	public String getValue(String item, String[] arr) {
		String value = getItem(item, arr);
		if(message != null) value = message.get(value);
		return value;
	}

	public static String getItem(int item, Hashtable map) {
		return getItem(item + "", map);
	}

	public static String getItem(String item, Hashtable map) {
		Enumeration e = map.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = map.get(key) != null ? map.get(key).toString() : "";
			if(key.equals(item)) return value;
		}
		return "";
	}
	public String getValue(String item, Hashtable map) {
		String value = getItem(item, map);
		if(message != null) value = message.get(value);
		return value;
	}

	public static String[] getKeys(Map map) {
		Iterator it = map.keySet().iterator();
		String[] data = new String[map.size()];
		for(int i=0; it.hasNext(); i++) {
			data[i] = (String)it.next();
		}
		return data;
	}

	public static String[] getKeys(String[] arr) {
		String[] data = new String[arr.length];
		for(int i=0; i<arr.length; i++) {
			String[] tmp = arr[i].split("=>");
			String id = tmp[0].trim();
			data[i] = id;
		}
		return data;
	}

	public boolean isMobile() {
		String agent = request.getHeader("user-agent");
		boolean isMobile = false;
		if(null != agent) {
			String[] mobileKeyWords = {
				"iPhone", "iPod", "iPad"
				, "BlackBerry", "Android", "Windows CE"
				, "LG", "MOT", "SAMSUNG", "SonyEricsson"
			};
			for(int i=0; i<mobileKeyWords.length; i++) {
				if(agent.indexOf(mobileKeyWords[i]) != -1) {
					isMobile = true;
					break;
				}
			}
		}
		return isMobile;
	}

	public static String getMimeType(String filename) {
		String ext = getFileExt(filename).toUpperCase();
		String mime = "application/octet-stream;";
		if(ext.equals("PDF")) {
			mime = "application/pdf";
		} else if(ext.equals("PPT") || ext.equals("PPTX")) {
			mime = "application/vnd.ms-powerpoint";
		} else if(ext.equals("DOC") || ext.equals("DOCX")) {
			mime = "application/msword";
		} else if(ext.equals("XLS") || ext.equals("XLSX")) {
			mime = "application/vnd.ms-excel";
		} else if(ext.equals("HWP")) {
			mime = "application/x-hwp";
		} else if(ext.equals("PNG")) {
			mime = "image/png";
		} else if(ext.equals("GIF")) {
			mime = "image/gif";
		} else if(ext.equals("JPG") || ext.equals("JPEG")) {
			mime = "image/jpeg";
		} else if(ext.equals("MP3")) {
			mime = "audio/mpeg";
		} else if(ext.equals("MP4")) {
			mime = "video/mp4";
		} else if(ext.equals("ZIP")) {
			mime = "application/zip";
		} else if(ext.equals("TXT")) {
			mime = "text/plain";
		} else if(ext.equals("AVI")) {
			mime = "video/x-msvideo";
		}
		return mime;
	}

	public void download(String path, String filename) throws Exception {

		File f = new File(path);
		if(f.exists()){

			try {
				String agent = request.getHeader("user-agent");

				if(agent.indexOf("MSIE") != -1) {
					filename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
				} else if(agent.indexOf("Firefox") != -1 || agent.indexOf("Safari") != -1) {
					if(!"jeus".equals(Config.getWas().toLowerCase())) {
						filename = new String(filename.getBytes("UTF-8"), "8859_1");
					}
				} else if(agent.indexOf("Chrome") != -1 || agent.indexOf("Opera") != -1) {
					StringBuffer sb = new StringBuffer();
					for (int i=0; i<filename.length(); i++) {
						char c = filename.charAt(i);
						if (c > '~') {
							sb.append(URLEncoder.encode("" + c, "UTF-8"));
						} else {
							sb.append(c);
						}
					}
					filename = sb.toString();
				} else {
					filename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
				}

				if(isMobile()) {
					response.setContentType( getMimeType(filename) );
				} else {
					response.setContentType( "application/octet-stream;" );
				}
				response.setContentLength( (int)f.length() );
				response.setHeader( "Content-Disposition", "attachment; filename=\"" + filename + "\"" );

				byte[] bbuf = new byte[2048];

				BufferedInputStream fin = new BufferedInputStream(new FileInputStream(f));
				BufferedOutputStream outs = new BufferedOutputStream(response.getOutputStream());

				int read = 0;
				while ((read = fin.read(bbuf)) != -1){
					outs.write(bbuf, 0, read);
				}

				outs.close();
				fin.close();

			} catch(Exception e) {
				errorLog("{Malgn.download} " + e.getMessage());
				response.setContentType("text/html");
				out.write("File Download Error : " + e.getMessage());
			}
		} else {
			response.setContentType("text/html");
			out.write("File Not Found : " + path);
		}

	}

	public static String readFile(String path) throws Exception {
		return readFile(path, encoding);
	}

	public static String readFile(String path, String encoding) throws Exception {
		File f = new File(path);
		if(f.exists()) {

			FileInputStream fin = new FileInputStream(f);
			Reader reader = new InputStreamReader(fin, encoding);
			BufferedReader br = new BufferedReader(reader);

			StringBuffer sb = new StringBuffer();
			int c = 0;
			while((c = br.read()) != -1) {
				sb.append((char)c);
			}
			br.close();
			reader.close();
			fin.close();

			return sb.toString();
		} else {
			return "";
		}
	}

	public static void copyFile(String source, String target) throws Exception {
		copyFile(new File(source), new File(target));
	}

	public static void copyFile(File source, File target) throws Exception {
		if(source.isDirectory()) {
			if(!target.isDirectory()){
				target.mkdirs();
			}
			String[] children  = source.list();
			for(int i=0; i<children.length; i++){
				copyFile(new File(source, children[i]),new File(target, children[i]));
			}
		} else {
			FileChannel inChannel = new FileInputStream(source).getChannel();
			FileChannel outChannel = new FileOutputStream(target).getChannel();
			try {
				// magic number for Windows, 64Mb - 32Kb
				int maxCount = (64 * 1024 * 1024) - (32 * 1024);
				long size = inChannel.size(), position = 0;
				while (position < size) {
					position += inChannel.transferTo(position, maxCount, outChannel);
				}
			} catch (IOException e) {
				errorLog("{Malgn.copyFile} " + e.getMessage());
				throw e;
			} finally {
				if (inChannel != null) inChannel.close();
				if (outChannel != null) outChannel.close();
			}
		}
	}

	public static void delFile(String path) throws Exception {
		File f = new File(path);
		path = replace(path, "../", "");
		if(!path.startsWith(Config.getDataDir())) {
			errorLog(path + " can not be deleted (only path in dataDir)");
			return;
		}
		if(f.exists()) {
			if(f.isDirectory()) {
				File[] files = f.listFiles();
				for(int i=0; i<files.length; i++) delFile(path + "/" + files[i].getName());
			}
			f.delete();
		} else {
			System.out.print(path + " is not found");
		}
	}

	public static void delFileRoot(String path) throws Exception {
		File f = new File(path);
		if(f.exists()) {
			if(f.isDirectory()) {
				File[] files = f.listFiles();
				for(int i=0; i<files.length; i++) delFile(path + "/" + files[i].getName());
			}
			f.delete();
		} else {
			System.out.print(path + " is not found");
		}
	}

	public static int getRandInt(int start, int count) {
		Random r = new Random();
		return start + r.nextInt(count);
	}

	public static int getUnixTime() {
		Date d = new Date();
		return (int)(d.getTime() / 1000);
	}

	public static int getUnixTime(String date) {
		Date d = strToDate(date);
		if(d == null) return 0;
		return (int)(d.getTime() / 1000);
	}

	public static String urlencode(String url) throws Exception {
		return URLEncoder.encode(url, encoding);
	}

	public static String urldecode(String url) throws Exception {
		return URLDecoder.decode(url, encoding);
	}

	public static String encode(String str) throws Exception {
		try { return replace(replace(Base64.encode(str), "=", "EQUAL"), "+", "PLUS"); }
		catch(Exception e) { return ""; }
	}
	public static String decode(String str) throws Exception {
		try { return Base64.decode(replace(replace(str, "PLUS", "+"), "EQUAL", "=")); }
		catch(Exception e) { return ""; }
	}

	public static Hashtable strToMap(String str) {
		return strToMap(str, "");
	}

	public static Hashtable strToMap(String str, String prefix) {
		Hashtable<String, String> h = new Hashtable<String, String>();
		if(str == null) return h;

		StringTokenizer token = new StringTokenizer(str, ",");
		while(token.hasMoreTokens()) {
			String subtoken = token.nextToken();
			int idx = subtoken.indexOf(":");
			if(idx != -1) {
				h.put(prefix + subtoken.substring(0, idx), replace(replace(subtoken.substring(idx + 1), "%3A", ":"), "%2C", ","));
			}
		}
		return h;
	}

	public static String mapToString(Hashtable values) {
		if(values == null) return "";
		StringBuffer sb = new StringBuffer();
		Enumeration e = values.keys();
		int i = 0;
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = values.get(key) != null ? replace(replace(values.get(key).toString(), ":", "%3A"), ",", "%2C") : "";
			sb.append("," + key + ":" + value);
			i++;
		}
		if(i > 0) return sb.toString().substring(1);
		else return "";
	}

	public static boolean serialize(String path, Object obj) {
		return serialize(new File(path), obj);
	}

	public static boolean serialize(File file, Object obj) {
		FileOutputStream f = null;
		ObjectOutput s = null;
		boolean flag = true;
		try {
		    if(!file.getParentFile().isDirectory()) {
		    	file.getParentFile().mkdirs();
		    }
			f = new FileOutputStream(file);
			s = new ObjectOutputStream(f);
			s.writeObject(obj);
			s.flush();
		} catch(Exception e) {
			errorLog("{Malgn.serialize} " + e.getMessage());
			e.printStackTrace(System.out);
			flag = false;
		} finally {
			if( s != null ) try { s.close(); } catch(Exception e) { e.printStackTrace(System.out); }
			if( f != null ) try { f.close(); } catch(Exception e) { e.printStackTrace(System.out); }
		}
		return flag;
	}

	public static Object unserialize(String path) {
		return unserialize(new File(path));
	}

	public static Object unserialize(File file) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Object obj = null;
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			obj = ois.readObject();
		} catch(Exception e) {
			errorLog("{Malgn.unserialize} " + e.getMessage());
			e.printStackTrace(System.out);
		} finally {
			if( ois != null ) try { ois.close(); } catch(Exception e) { e.printStackTrace(System.out); }
			if( fis != null ) try { fis.close(); } catch(Exception e) { e.printStackTrace(System.out); }
		}
		return obj;
	}

	public static String nl2br(String str) {
		//return replace(replace(str, "\r\n", "<br />"), "\n", "<br />");
		return replace(str, new String[] { "\r\n", "\r", "\n" }, "<br />");
	}

	public static String htmlToText(String str) {
		return nl2br(htmlentities(str));
	}
	public static String htt(String str) {
		return htmlentities(str);
	}
	public static String htmlentities(String src) {
		return replace(replace(replace(src, "&", "&amp;"), "<", "&lt;"), ">", "&gt;");
	}

    public static String stripTags(String str) {
        int offset = 0;
        int i = 0;
        int j = 0;
        int size = str.length();
        StringBuffer buf = new StringBuffer();
        synchronized(buf) {
            while((i = str.indexOf("<", offset)) != -1) {
                if((j = str.indexOf(">", offset)) != -1) {
                    buf.append(str.substring(offset, i));
                    offset = j + 1;
                } else {
                    break;
                }
            }
            buf.append(str.substring(offset));
            return replace(replace(replace(buf.toString(), "\t", ""), "\r", ""), "\n", "").trim();
        }
    }

	public static String strpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		String output = input;
		for(int i=0; i<gap; i++) {
			output += pad;
		}
		return output;
	}
	public static String strrpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		String output = "";
		for(int i=0; i<gap; i++) {
			output += pad;
		}
		return output + input;
	}

	public static String getFileSize(long size) {
		if(size >= 1024 * 1024 * 1024) {
			return (size / (1024 * 1024 * 1024)) + "GB";
		} else if(size >= 1024 * 1024) {
			return (size / (1024 * 1024)) + "MB";
		} else if(size >= 1024) {
			return (size / 1024) + "KB";
		} else {
			return size + "B";
		}
	}

	public static double round(double size, int i) {
		double sub = java.lang.Math.pow(10, i);
		return java.lang.Math.round(size * sub) / sub;
	}

	public static String numberFormat(int n) {
		DecimalFormat df = new DecimalFormat("#,###");
		return df.format(n);
	}

	public static String numberFormat(double n, int i) {
		String format = "#,##0";
		if(i > 0) {
			format += "." + strpad("", i, "0");
			n += Double.parseDouble("0." + strpad("", i, "0") + 1); //round fix
		}
		DecimalFormat df = new DecimalFormat(format);
		return df.format(n);
	}
	public static String nf(int n) { return numberFormat(n); }
	public static String nf(double n, int i) { return numberFormat(n, i); }

	public void p(Object obj) throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		if(obj != null) {
			if(obj instanceof RecordSet || obj instanceof DataSet) {
				out.write("<pre style='text-align:left;font-size:9pt;'>");
				out.write(replace(replace(replace(replace(obj.toString(), "{", "\r\n{\n\t"), ", ", ",\r\n\t["), "}", "\r\n}"), "=", "] => "));
				out.write("</pre>");
			} else  {
				out.write(obj.toString());
			}
		} else {
			out.write("NULL");
		}
		out.write("</div>");
	}

	public void p(Object[] obj) throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		if(obj != null) {
			for(int i=0; i<obj.length; i++) {
				if(i > 0) out.write(", ");
				out.write(obj[i].toString());
			}
		} else {
			out.write("NULL");
		}
		out.write("</div>");
	}

	public void p(int i) throws Exception {
		p("" + i);
	}

	public void p() throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		out.write("<pre style='text-align:left;font-size:9pt;'>");
		Enumeration e = request.getParameterNames();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			for(int i=0; i<request.getParameterValues(key).length; i++) {
				out.write("[" + key + "] => " + request.getParameterValues(key)[i] + "\r");
			}
		}
		out.write("</pre>");
		out.write("</div>");
	}

	public String getScriptDir() {
		return dirname(replace(request.getRealPath(request.getServletPath()), "\\", "/"));
	}

	public static String dirname(String path) {
		File f = new File(path);
		return f.getParent();
	}

	public static String[] split(String p, String str, int length) {
		String[] arr = str.split(p);
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			if(i < arr.length) {
				result[i] = arr[i];
			} else {
				result[i] = "";
			}
		}
		return result;
	}

	public static String[] split(String delim, String str) {
		ArrayList<String> list = new ArrayList<String>();
		int offset = 0;
		int len = delim.length();
		while(true) {
			int pos = str.indexOf(delim, offset);
			if(pos == -1) {
				list.add(str.substring(offset));
				break;
			} else {
				list.add(str.substring(offset, pos));
				offset = pos + len;
			}
		}
		return list.toArray(new String[list.size()]);
	}


	public static String addSlashes(String str) {
		return replace(replace(replace(replace(replace(str, "\"", "&quot;"), "\\", "\\\\"), "\"", "\\\""), "\'", "\\\'"), "\r\n", "\\r\\n");
	}

	public static String replace(String s, String sub, String with) {
		int c = 0;
		int i = s.indexOf(sub,c);
		if (i == -1) return s;

		StringBuffer buf = new StringBuffer(s.length() + with.length());

//		synchronized(buf) {
			do {
				buf.append(s.substring(c, i));
				buf.append(with);
				c = i + sub.length();
			} while((i = s.indexOf(sub, c)) != -1);
			if(c < s.length()) {
				buf.append(s.substring(c, s.length()));
			}
			return buf.toString();
//		}
	}
	public static String replace(String s, String[] sub, String[] with) {
		if(sub.length != with.length) return s;
		for(int i=0; i<sub.length; i++) {
			s = replace(s, sub[i], with[i]);
		}
		return s;
	}
	public static String replace(String s, String[] sub, String with) {
		for(int i=0; i<sub.length; i++) {
			s = replace(s, sub[i], with);
		}
		return s;
	}

	public boolean eq(String s1, String s2) {
		if(null == s1 || null == s2) return false;
		return s1.equals(s2);
	}

	public static long crc32(String str) throws Exception {
		byte bytes[] = str.getBytes(encoding);
		Checksum checksum = new CRC32();
		checksum.update(bytes,0,bytes.length);
		return checksum.getValue();
	}

	public HashMap<String, Object> jsonToMap(String str) throws Exception {
		return jsonToMap(new JSONObject(str));
	}
	public HashMap<String, Object> jsonToMap(JSONObject arr) throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		Iterator it = arr.keys();
		while(it.hasNext()) {
			String key = (String)it.next();
			if(arr.get(key) instanceof JSONObject) {
				map.put(key, jsonToMap((JSONObject)arr.get(key)));
			} else if(arr.get(key) instanceof JSONArray) {
				map.put(key, jsonToList((JSONArray)arr.get(key)));
			} else {
				map.put(key, arr.get(key));
			}
		}
		return map;
	}
	public ArrayList<Object> jsonToList(JSONArray arr) throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		for(int i=0; i<arr.length(); i++) {
			if(arr.get(i) instanceof JSONArray) {
				list.add(jsonToList((JSONArray)arr.get(i)));
			} else if(arr.get(i) instanceof JSONObject) {
				list.add(jsonToMap((JSONObject)arr.get(i)));
			} else {
				list.add(arr.get(i));
			}
		}
		return list;
	}

	public String getWebUrl() {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		String url = scheme + "://" + request.getServerName();
		if("https".equals(scheme)) {
			url += port != 443 ? ":" + port : "";
		} else {
			url += port != 80 ? ":" + port : "";
		}
		return url;
	}

	public void mailer(String mailTo, String subject, String body) throws Exception {
		mailer(mailTo, subject, body, null);
	}
	public void mailer(String mailTo, String subject, String body, String filepath) throws Exception {
		MailThread mt = new MailThread(this, mailTo, subject, body, filepath);
		boolean flag = false;
		for(int i=0; i<50; i++) {
			if(mailThreadNum >= 0 && mailThreadNum < 50) {
				mt.start();
				mailThreadNum++;
				flag = true;
				break;
			} else {
				mt.sleep(100);
			}
		}
		if(flag == false) {
			mail(mailTo, subject, body, filepath);
		}
	}

	public String msg(String key) {
		if(message != null) return message.get(key);
		else return key;
	}
	public String[] msgArray(String[] keys) {
		if(message != null) return message.getArray(keys);
		else return keys;
	}
}

class MailThread extends Thread {

	private Malgn m;
	private String mailTo;
	private String subject;
	private String body;
	private String filepath;

	public MailThread(Malgn m, String mailTo, String subject, String body, String filepath) {
		this.m = m;
		this.mailTo = mailTo;
		this.subject = subject;
		this.body = body;
		this.filepath = filepath;
	}

	public void run() {
		try {
			m.mail(mailTo, subject, body, filepath);
			m.mailThreadNum--;
			if(m.mailThreadNum < 0) m.mailThreadNum = 0;
		} catch(Exception e) {
			Malgn.errorLog("{MailThread.run} " + e.getMessage(), e);
		}
	}
}