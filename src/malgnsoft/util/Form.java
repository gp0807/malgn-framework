package malgnsoft.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import javax.servlet.http.Cookie;

import malgnsoft.util.MultipartRequest;
import malgnsoft.util.Malgn;
import malgnsoft.db.DataSet;

public class Form {

	public String name = "form1";
	public Vector<String[]> elements = new Vector<String[]>();
	public Hashtable<String, String> data = new Hashtable<String, String>();
	public String errMsg = null;
	public String dataDir = null;
	public String uploadDir = null;
	public int maxPostSize = Config.getInt("maxPostSize") * 1024 * 1024;
	public String encoding = Config.getEncoding();

	private static Hashtable<String, String> options = new Hashtable<String, String>();
	private static String saveDir = Config.getDataDir() + "/tmp";

	private MultipartRequest mrequest = null;	
	private HttpServletRequest request;
	private Writer out = null;
	private boolean debug = false;
	private Hashtable<String, String> files = new Hashtable<String, String>();
	private String allowScript = null;
	private String allowHtml = null;
	private String allowIframe = null;
	private String allowLink = null;
	private String allowObject = null;
	private String denyHtml = Config.get("denyHtml");
	private String[] denyExt = {"jsp", "php", "asp", "aspx", "html", "htm", "exe", "sh"}; 

	static {
		options.put("email", "^[a-z0-9A-Z\\_\\.\\-]+@([a-z0-9A-Z\\.\\-]+)\\.([a-zA-Z]+)$");
		options.put("userid", "^([a-z0-9\\_\\.\\-]{4,20})$");
		options.put("url", "^(http:\\/\\/)(.+)");
		options.put("number", "^-?[\\,0-9]+$");
		options.put("domain", "^([a-z0-9]+)([a-z0-9\\.\\-]+)\\.([a-z]{2,4})$");
		options.put("engonly", "^([a-zA-Z]+)$");
		options.put("phone", "^([0-9]{2,4}-[0-9]{3,4}-[0-9]{4})$");
		options.put("jumin", "^([0-9]{6}-?[0-9]{7})$");
	}

	public Form() {
		if(maxPostSize == 0) maxPostSize = 1024 * 1024 * 1024;
	}

	public Form(String name) {
		if(maxPostSize == 0) maxPostSize = 1024 * 1024 * 1024;
		this.name = name;
	}
	
	public Form(String name, HttpServletRequest request) throws Exception {
		this(name);
		if(maxPostSize == 0) maxPostSize = 1024 * 1024 * 1024;
		setRequest(request);
	}

	public Form(HttpServletRequest request) throws Exception {
		this("form1");
		if(maxPostSize == 0) maxPostSize = 1024 * 1024 * 1024;
		setRequest(request);
	}

	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}
	public void denyHtml() {
		denyHtml = "Y";
	}
	
	public void denyExt(String[] arr) {
		denyExt = arr;
	}

	public void setError(String msg) {
		this.errMsg = msg;
		if(this.debug == true) {
			try {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Malgn.errorLog(msg);
			} catch(Exception e) {}
		}
	}

	public void setRequest(HttpServletRequest req) throws Exception {
		this.request = req;

		File tmp = new File(saveDir);
		if(!tmp.exists()) tmp.mkdirs();

		String key = null;
		String type = req.getContentType();
		if(type != null && type.toLowerCase().startsWith("multipart/form-data")) {
			mrequest = new MultipartRequest(req, saveDir, maxPostSize, encoding);
			Enumeration e = mrequest.getParameterNames();
			while(e.hasMoreElements()) {
				key = (String)e.nextElement();
				data.put(key, mrequest.getParameter(key));
			}
		} else {
			Enumeration e = req.getParameterNames();
			while(e.hasMoreElements()) {
				key = (String)e.nextElement();
				data.put(key, request.getParameter(key));
			}
		}
	}
	
	public void addElement(String name, String value, String attributes) {
		String[] element = new String[3];
		element[0] = name;
		element[1] = value;
		element[2] = attributes;
		elements.addElement(element);
		if(null != attributes && attributes.indexOf("allowscript:'Y'") != -1) {
			allowScript = (null == allowScript ? "" : allowScript) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowhtml:'Y'") != -1) {
			allowHtml = (null == allowHtml ? "" : allowHtml) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowiframe:'Y'") != -1) {
			allowIframe = (null == allowIframe ? "" : allowIframe) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowlink:'Y'") != -1) {
			allowLink = (null == allowLink ? "" : allowLink) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowobject:'Y'") != -1) {
			allowObject = (null == allowObject ? "" : allowObject) + "[" + name + "]";
		}
	}

	public void addElement(String name, int value, String attributes) {
		addElement(name, "" + value, attributes);
	}
	
	public void put(String name, String value) {
		data.put(name, value);
	}

	public void put(String name, int value) {
		data.put(name, "" + value);
	}

	public void put(String name, double value) {
		data.put(name, "" + value);
	}

	public void put(String name, boolean value) {
		data.put(name, "" + value);
	}

	public String get(String name) {
		return get(name, "");
	}

	public String get(String name, String str) {
		if(data.containsKey(name)) {
			return xss(name, data.get(name));
		} else {
			return str;
		}
	}

	public String glue(String delim, String names) {
		String[] vars = names.split(",");
		if(vars == null) return "";
		for(int i=0; i<vars.length; i++) {
			vars[i] = get(vars[i].trim());
		}
		return Malgn.join(delim, vars);
	}

	public String[] getArr(String name) {
		String[] arr;
		if(mrequest != null) arr = mrequest.getParameterValues(name);
		else arr = request.getParameterValues(name);
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				arr[i] = xss(name, arr[i]);
			}
		}
		return arr;
	}

	public DataSet getArrList(String[] names) {
	
		DataSet ret = new DataSet();
		String[] data = getArr(names[0]);

		if(data == null) return ret;
		
		Hashtable<String, String[]> map = new Hashtable<String, String[]>();
		map.put(names[0], data);

		for(int i=1; i<names.length; i++) {
			String[] arr = getArr(names[i]);
			if(arr != null && arr.length == data.length) {
				map.put(names[i], arr);
			}
		}

		for(int i=0; i<data.length; i++) {
			ret.addRow();
			for(int j=0; j<names.length; j++) {
				if(map.containsKey(names[j])) ret.put(names[j], map.get(names[j])[i]);
			}
		}
		ret.first();
		return ret;
	}

	public DataSet getArrList(String name) {
		String[] names = Malgn.replace(name, " ", "").split("\\,");
		return getArrList(names);
	}

	private String xss(String name, String value) {
		if(null == allowScript || allowScript.indexOf("[" + name + "]") == -1) {

			String tail = value.endsWith(">") ? ">" : "";
			String[] x1 = value.split(">");
			String res = "";
			for(int i=0; i<x1.length; i++) {
				String[] x2 = x1[i].split("<");
				for(int j=0; j<x2.length; j++) {
					if(j > 0) res += "<";
					if(j == x2.length - 1) {
						res += x2[j].replaceAll("(?i)(x-)?(vbscript|javascript|script|expression|eval|FSCommand|onAbort|onActivate|onAfterPrint|onAfterUpdate|onBeforeActivate|onBeforeCopy|onBeforeCut|onBeforeDeactivate|onBeforeEditFocus|onBeforePaste|onBeforePrint|onBeforeUnload|onBegin|onBlur|onBounce|onCellChange|onChange|onClick|onContextMenu|onControlSelect|onCopy|onCut|onDataAvailable|onDataSetChanged|onDataSetComplete|onDblClick|onDeactivate|onDrag|onDragEnd|onDragLeave|onDragEnter|onDragOver|onDragDrop|onDrop|onEnd|onError|onErrorUpdate|onFilterChange|onFinish|onFocus|onFocusIn|onFocusOut|onHelp|onKeyDown|onKeyPress|onKeyUp|onLayoutComplete|onLoad|onLoseCapture|onMediaComplete|onMediaError|onMouseDown|onMouseEnter|onMouseLeave|onMouseMove|onMouseOut|onMouseOver|onMouseUp|onMouseWheel|onMove|onMoveEnd|onMoveStart|onOutOfSync|onPaste|onPause|onProgress|onPropertyChange|onReadyStateChange|onRepeat|onReset|onResize|onResizeEnd|onResizeStart|onResume|onReverse|onRowsEnter|onRowExit|onRowDelete|onRowInserted|onScroll|onSeek|onSelect|onSelectionChange|onSelectStart|onStart|onStop|onSyncRestored|onSubmit|onTimeError|onTrackChange|onUnload|onURLFlip|seekSegmentTime)", "x-$2");
					} else {
						res += x2[j];
					}
				}
				if(i + 1 < x1.length) res += ">";
			}
			res += tail;
			value = res;

			/*
			value = value.replaceAll("(?i)<script[^>]*>", "[NOT SCRIPT]")
				.replaceAll("(?i)</script[^>]*>", "")
				.replaceAll("(?i)<[a-zA-Z]+ [^>]*javascript:[^>]+>", "[NOT SCRIPT]")
				.replaceAll("(?i)<[a-zA-Z]+ [^>]*vbscript:[^>]+>", "[NOT SCRIPT]")
				.replaceAll("(?i)<[a-zA-Z]+[^>]* on[^>]+>", "[NOT SCRIPT]")
				//.replaceAll("(?i)expression\\((.*?)\\)", "")
				//.replaceAll("(?i)eval\\((.*?)\\)", "")
				//.replaceAll("(?i)src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", "")
				//.replaceAll("(?i)src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", "")
				.replaceAll("&#[a-zA-Z0-9]{2,8}", "")
			;
			*/
		}
		if(null == allowIframe || allowIframe.indexOf("[" + name + "]") == -1) {
			value = value.replaceAll("(?i)<iframe[^>]*>", "").replaceAll("(?i)</iframe>", "");
		}
		if(null == allowLink || allowLink.indexOf("[" + name + "]") == -1) {
			value = value.replaceAll("(?i)<link[^>]*>", "");
		}
		if("Y".equals(denyHtml)) {
			if(null == allowObject || allowObject.indexOf("[" + name + "]") == -1) {
				value = value.replaceAll("(?i)<object[^>]*>", "")
					.replaceAll("(?i)</object>", "")
					.replaceAll("(?i)<param[^>]*>", "")
					.replaceAll("(?i)</param>", "")
					.replaceAll("(?i)<embed[^>]*>", "")
					.replaceAll("(?i)</embed>", "")
				;
			}
			if(null != allowHtml && allowHtml.indexOf("[" + name + "]") == -1) {
				value = Malgn.replace( Malgn.replace(value , "<", "&lt;") , ">", "&gt;");
			}
		}
		return value.replace('\'', '`');
	}

	public Hashtable getMap(String name) {
		Hashtable<String, String> map = new Hashtable<String, String>();
		int len = name.length();
		try {
			Enumeration e = data.keys();
			while(e.hasMoreElements()) {
				String key = (String)e.nextElement();
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key.substring(len), xss(key, data.get(key)));
				}
			}
		} catch(Exception ex) {
			Malgn.errorLog("{Form.getMap} " + ex.getMessage(), ex);
		}
		return map;
	}

	public int getInt(String name) {
		return getInt(name, 0);
	}

	public int getInt(String name, int i) {
		String str = get(name);
		if(str.matches("^-?[\\,0-9]+$")) return Integer.parseInt(Malgn.replace(str, ",", ""));
		else return i;
	}

	public long getLong(String name) {
		return getLong(name, 0);
	}

	public long getLong(String name, long i) {
		String str = get(name);
		if(str.matches("^-?[\\,0-9]+$")) return Long.parseLong(Malgn.replace(str, ",", ""));
		else return i;
	}

	public double getDouble(String name) {
		return getDouble(name, 0.0);
	}

	public double getDouble(String name, double i) {
		String str = get(name);
		if(str.matches("^-?[\\.\\,0-9]+$")) return Double.parseDouble(Malgn.replace(str, ",", ""));
		else return i;
	}
	
	public boolean validate() throws Exception {
		Iterator ie = elements.iterator();         //Vector의 요소의 리스트를 리턴
		while(ie.hasNext()) {
		    String[] element = (String[])ie.next();
		    if(isValid(element) == false) return false;
		}
		return true;
	}

	public File saveFile(String name) throws Exception {
		if(mrequest == null) return null;

		String orgname = mrequest.getOriginalFileName(name);
		if(orgname == null) return null;

		String filename = null;
		String path = null;
		File f = null;
		int i = 0;
		do {
			filename = (i > 0) ? "[" + i + "]" + orgname : orgname;
			path = null != uploadDir ? uploadDir + "/" + mrequest.getFilesystemName(name) : Malgn.getUploadPath(filename, dataDir);
			f = new File(path);
			i++;
		} while (f.exists());
		
		if(!f.getParentFile().isDirectory()) {
			f.getParentFile().mkdirs();
		}
		files.put(name, filename);
		return saveFile(name, path);
	}

	public File saveFile(String name, String path) throws Exception {
		if(mrequest == null) return null;

		File f = mrequest.getFile(name);
		if(f != null && f.exists()) {

			String orgname = mrequest.getOriginalFileName(name);
			String ext = Malgn.getFileExt(orgname).toLowerCase();
			if(denyExt != null && Malgn.inArray(ext, denyExt)) {
				f.delete();
				Malgn.errorLog("{Form.saveFile} attached file is denied : " + orgname);
				return null;
			}

			File target = new File(path);

			try {
				if(target.isDirectory()) {
					path += "/" + f.getName();	
					target = new File(path);
				}
				if(!target.getParentFile().isDirectory()) {
					target.getParentFile().mkdirs();
				}
			} catch(Exception ex) {
				Malgn.errorLog("{Form.saveFile} directory creation error - path:" + path, ex);
			}

			try {
				f.renameTo(target);
			} catch(Exception ex) {
				Malgn.errorLog("{Form.saveFile} file rename error", ex);
				f.delete();
			}

			if(target.exists()) return target;
			else return null;

		} else {
			return null;
		}
	}

	public File getFile(String name) {
		if(mrequest == null) return null;
		return mrequest.getFile(name);
	}

	public String getFileName(String name) {
		if(mrequest == null) return "";
		if(files.containsKey(name)) return files.get(name);
		else return mrequest.getOriginalFileName(name);
	}

	public String getFileType(String name) {
		if(mrequest == null) return "";
		return mrequest.getContentType(name);
	}

	public boolean isset(String name) {
		return data.containsKey(name);
	}
	
	private Hashtable<String, String> getAttributes(String str) {
		Hashtable<String, String> map = new Hashtable<String, String>();
		if(str != null && !"".equals(str)) {
			String[] arr = str.split("\\,");
			for(int i=0; i<arr.length; i++) {
				String[] arr2 = null;
				arr2 = arr[i].split("[=:]");
				if(arr2.length == 2) {
					map.put(arr2[0].trim().toUpperCase(), arr2[1].replace('\'', '\0').trim());
				}
			}
		}
		return map;
	}
	
	private boolean isValid(String[] element) throws Exception {
		String name = element[0];
		String value = get(name);
		Hashtable<String, String> attributes = getAttributes(element[2]);
		String nicname = attributes.get("HNAME");
		if(nicname == null) nicname = name;
	//	nicname = new String(nicname.getBytes("KSC5601"),"8859_1");
		
		if(attributes.containsKey("REQUIRED")) {
			if(mrequest !=  null && mrequest.getFile(name) != null) value = getFileName(name);
			if("".equals(value.trim())) {
				this.errMsg = "["+ nicname +"]항목은 필수항목입니다.";
				return false;
			}
		}
		
		if(!"".equals(value) && attributes.containsKey("MAXBYTE")) {
			int size = Integer.parseInt(attributes.get("MAXBYTE"));
			if(value.getBytes().length > size) {
				this.errMsg = "["+ nicname +"]항목의 최대길이는 "+ size +"자 입니다.";
				return false;
			}
		}

		if(!"".equals(value) && attributes.containsKey("MINBYTE")) {
			int size = Integer.parseInt(attributes.get("MINBYTE"));
			if(value.getBytes().length < size) {
				this.errMsg = "["+ nicname +"]항목의 최소길이는 "+ size +"자 입니다.";
				return false;
			}
		}
		if(attributes.containsKey("FIXBYTE")) {
			int size = Integer.parseInt(attributes.get("FIXBYTE"));
			if(value.getBytes().length != size) {
				this.errMsg = "["+ nicname +"]항목은 정확히 "+ size +"자이어야 합니다.";
				return false;
			}
		}
		
		if(!"".equals(value) && attributes.containsKey("MINSIZE")) {
			int size = Integer.parseInt(attributes.get("MINSIZE"));
			int v = Integer.parseInt(value);
			if(v < size) {
				this.errMsg = "["+ nicname +"]항목의 값은 "+ size +"이하이어야 합니다.";
				return false;
			}
		}

		if(!"".equals(value) && attributes.containsKey("MAXSIZE")) {
			int size = Integer.parseInt(attributes.get("MAXSIZE"));
			int v = Integer.parseInt(value);
			if(v > size) {
				this.errMsg = "["+ nicname +"]항목의 값은 "+ size +"이상이어야 합니다.";
				return false;
			}
		}
		

		if(attributes.containsKey("GLUE")) {
			String glue = attributes.get("GLUE");
			String delim = attributes.containsKey("DELIM") ? attributes.get("DELIM") : "";
			String[] arr = glue.split("\\|");
			for(int i=0; i<arr.length; i++) {
				if(!"".equals(get(arr[i].trim()))) value += delim + get(arr[i].trim());
			}
		}

		if(attributes.containsKey("OPTION") && !"".equals(value)) {
			String option = attributes.get("OPTION");
			if("number".equals(option)) {
				value = Malgn.replace(value, ",", "");
				data.put(name, value);
			}
			String re = options.get(option);
			if(re == null) re = option;
			Pattern pattern = Pattern.compile(re);
			Matcher match = pattern.matcher(value);
			if(match.find() == false) {
				this.errMsg = "["+ nicname +"]항목은 형식에 어긋납니다.";
				return false;
			}
		}

		if(attributes.containsKey("ALLOW")) {
			String filename = getFileName(name);
			String re = attributes.get("ALLOW");
			if(filename != null && !"".equals(filename) && !"".equals(re)) {
				Pattern pattern = Pattern.compile("(" + re.replace('\'', '|') + ")$");
				Matcher match = pattern.matcher(getFileName(name).toLowerCase());
				if(match.find() == false) {
					this.errMsg = "["+ nicname +"]항목은 업로드가 제한된 파일입니다.";
					return false;
				}
			}
		}

		if(attributes.containsKey("DENY")) {
			String filename = getFileName(name);
			String re = attributes.get("DENY");
			if(filename != null && !"".equals(filename) && !"".equals(re)) {
				Pattern pattern = Pattern.compile("(" + re.replace('\'', '|') + ")$");
				Matcher match = pattern.matcher(getFileName(name).toLowerCase());
				if(match.find() == true) {
					this.errMsg = "["+ nicname +"]항목은 업로드가 제한된 파일입니다.";
					return false;
				}
			}
		}

		return true;
	}
	
	public String getScript() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<script type='text/javascript'>\r\n");
		sb.append("//<![CDATA[\r\n");
		sb.append("function __setElement(el, v, a) { if(v) v = v.replace(/__&LT__/g, '<').replace(/__&GT__/g, '>'); if(typeof(el) != 'object' && typeof(el) != 'function') return; if(v != null) switch(el.type) { case 'text': case 'hidden': case 'password': case 'file': case 'email': el.value = v; break; case 'textarea': el.value = v; break; case 'checkbox': case 'radio': if(el.value == v) el.checked = true; else el.checked = false; break; case 'select-one': for(var i=0; i<el.options.length; i++) if(el.options[i].value == v) el.options[i].selected = true; break; default: for(var i=0; i<el.length; i++) if(el[i].value == v) el[i].checked = true; el = el[0]; break; } if(typeof(a) == 'object') { if(el.type != 'select-one' && el.length > 1) el = el[0]; for(i in a) el.setAttribute(i, a[i]); } }\r\n");
		sb.append("if(_f = document.forms['" + this.name + "']) {\r\n");

		Iterator ie = elements.iterator();
		while(ie.hasNext()) {
		    String[] element = null;
		    String value = null;
		    element = (String[])ie.next();
		    value = this.get(element[0], null);
		    if(value == null && element[1] != null) {
				value = element[1];
			}
		    sb.append("\t__setElement(_f['" + element[0] + "'], ");
			if("Y".equals(denyHtml) && null != allowHtml && allowHtml.indexOf("[" + element[0] + "]") == -1) {
				sb.append(value != null ? "'" + Malgn.replace(Malgn.replace(Malgn.replace(Malgn.replace(Malgn.addSlashes(value), "<", "__&LT__"), ">", "__&GT__"), "&lt;", "__&LT__"), "&gt;", "__&GT__") + "'" : "null");
			} else {
		    	sb.append(value != null ? "'" + Malgn.replace(Malgn.replace(Malgn.addSlashes(value), "<", "__&LT__"), ">", "__&GT__") + "'" : "null");
			}
		    sb.append(", {" + (element[2] != null ? element[2] : "") + "});\r\n");
		}
		
		sb.append("\tif(!_f.onsubmit) _f.onsubmit = function() { return validate(this); };\r\n");
		sb.append("}\r\n");
		sb.append("//]]>\r\n");
		sb.append("</script>");
	
		if(errMsg != null) sb.append("<script>alert('"+ errMsg +"')</script>");

		return sb.toString();
	}
}