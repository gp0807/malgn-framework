package malgnsoft.util;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import java.io.Writer;
import malgnsoft.db.DataSet;

public class Template {
	
	protected String root = Config.getTplRoot();
	protected Hashtable<String, String> var = new Hashtable<String, String>();
	protected Hashtable<String, DataSet> loop = new Hashtable<String, DataSet>();
	protected Writer out = null;
	private PageContext pageContext = null;
	private HttpServletRequest request = null;
	private HttpServletResponse response = null;
	private boolean debug = false;
	private String encoding = Config.getEncoding();
	private Message message = null;

	public Template() {
	}

	public Template(String path) {
		setRoot(path);
	}

	public void setDebug() {
		this.out = null;
		debug = true;
	}

	public void setDebug(Writer out) {
		this.out = out;
		debug = true;
	}

	public void setRequest(HttpServletRequest request, HttpServletResponse response) {
		this.response = response;
		setRequest(request);
	}
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	public void setRequest(HttpServletRequest request) {
		this.request = request;
		Enumeration e = request.getParameterNames();
		while(e.hasMoreElements()) {
			String key = null;
			String value = null;
			key = (String)e.nextElement();
			value = request.getParameter(key);

			//xss
			String value2 = replace(value.toLowerCase(), " ", "");
			if(value2.indexOf("\"") != -1) continue;
			if(value2.indexOf("'") != -1) continue;
			if(value2.indexOf("/*") != -1) continue;
			if(value2.indexOf("*/") != -1) continue;
			if(value2.indexOf("script") != -1) continue;
			if(value2.indexOf("javascript") != -1) continue;
			if(value2.indexOf("expression(") != -1) continue;
			if(value2.indexOf("{") != -1) continue;
			if(value2.indexOf("}") != -1) continue;

			var.put(key, replace(replace(value, "<", "&lt;"), ">", "&gt;"));
		}
	}

	public void setPageContext(PageContext pc) {
		pageContext = pc;
	}
	
	public void setRoot(String path) {
		root = path + "/";
	}

	public void setLanguage(Language lang) {
		message = (Message)lang;
	}
	public void setMessage(Message msg) {
		message = msg;
	}

	public void setVar(String name, String value) {
		if(name == null) return;
		var.put(name, value == null ? "" : value);
	}

	public void setVar(String name, int value) {
		setVar(name, "" + value);
	}

	public void setVar(String name, long value) {
		setVar(name, "" + value);
	}
	
	public void setVar(String name, boolean value) {
		setVar(name, value == true ? "true" : "false");
	}

	public void setVar(Hashtable values) {
		if(values == null) return;
		Enumeration e = values.keys();
		while(e.hasMoreElements()) {
			String key = e.nextElement().toString();
			if(values.get(key) != null) {
				setVar(key, values.get(key).toString());
			}
		}
	}

	public void setVar(DataSet values) {
		if(values != null && values.size() > 0) {
			if(values.getIndex() == -1) values.next();
			this.setVar(values.getRow());
		}
	}

	public void setVar(String name, DataSet values) {
		if(values.getIndex() == -1) values.next();
		this.setVar(name, values.getRow());
	}
	
	public void setVar(String name, Hashtable values) {
		if(name == null || values == null) return;

		int sub = 0;
		Enumeration e = values.keys();
		while(e.hasMoreElements()) {
			String key = null;
			key = e.nextElement().toString();
			if(values.get(key) == null || key.length() == 0) continue;
			if(key.charAt(0) != '.') {
				setVar(name + "." + key, values.get(key).toString());
			} else {
				setLoop(key.substring(1), (DataSet)values.get(key));
			}
			/*
			if(values.get(key) instanceof DataSet) {
				setLoop(key.substring(1), (DataSet)values.get(key));
			} else {
				setVar(name + "." + key, values.get(key).toString());
			}
			*/
		}
	}

	public void setLoop(String name, DataSet rs) {
		if(rs != null && rs.size() > 0) {
			rs.first();
			loop.put(name, rs);
			setVar(name, true);
		} else {
			loop.put(name, new DataSet());
			setVar(name, false);
		}
	}

	public String fetch(String filename) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		this.out = new OutputStreamWriter(bos);
		parseTag(readFile(filename));
		return bos.toString();
	}
	
	public void print(Writer out, String filename) throws Exception {
		this.out = out;
		parseTag(readFile(filename));
		clear();
	}
	
	public void print(String filename, Writer out) throws Exception {
		this.out = out;
		parseTag(readFile(filename));
		clear();
	}
	
	private void parseTag(String buffer) throws Exception {
		int pos = 0, offset = 0;
		while((pos = buffer.indexOf("<!--", offset)) != -1) {
			parseVar(buffer.substring(offset, pos));
			offset = pos + 4;
			
			String str = buffer.substring(offset, offset + 3);
			if( !str.equals(" IN") 
				&& !str.equals(" EX") 
				&& !str.equals(" LO")
				&& !str.equals(" IF")
				&& !str.equals(" NI")
				&& !str.equals("@de")
				&& !str.equals("@in")
				&& !str.equals("@ex")
				&& !str.equals("@lo")
				&& !str.equals("@if")
				&& !str.equals("@ni")
				&& !str.equals("/lo")
				&& !str.equals("/if")
				&& !str.equals("/ni")
			) { out.write("<!--"); continue; }

			int end = buffer.indexOf("-->", pos);
			if(end != -1) {
				offset = end + 3;
				String cmd = buffer.substring(pos + 4, end).trim();
				if(cmd.startsWith("INCLUDE ") || cmd.startsWith("@include(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					if(var.get(names[2]) != null) {
						parseTag(readFile(var.get(names[2]).toString()));
					} else {
						parseTag(readFile(names[2]));
					}
				} else if(cmd.startsWith("EXECUTE FILE '") || cmd.startsWith("@execute(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					if(null != pageContext) {
						pageContext.include(names[2]);
					} else if(null != request && null != response) {
						RequestDispatcher dispatcher = request.getRequestDispatcher(names[2]);
						dispatcher.include(request, response);
					}

				} else if(cmd.startsWith("LOOP START '") || cmd.startsWith("@loop(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					DataSet rs = (DataSet)loop.get(names[2]);
					String etag = !cmd.startsWith("@") ? "<!-- LOOP END '" + names[2] + "' -->" : "<!--/loop(" + names[2] + ")-->";
					int loop_end = buffer.indexOf(etag, offset);

					if(loop_end != -1) {
						if(rs != null) {
							rs.first();
							while(rs.next()) {
								setVar(names[2], (Hashtable)rs.getRow());
								parseTag(buffer.substring(end + 3, loop_end));
							}
						} else {
							setError("Loop Data is not exists, name is " + names[2]);
						}
						offset = loop_end + etag.length();
					} else {
						setError("Loop end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("IF START '") || cmd.startsWith("@if(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					String etag = !cmd.startsWith("@") ? "<!-- IF END '" + names[2] + "' -->" : "<!--/if(" + names[2] + ")-->";
					int if_end = buffer.indexOf(etag, offset);
					if(if_end != -1) {
						if(var.get(names[2]) == null 
							|| "false".equals(var.get(names[2])) 
							|| "".equals(var.get(names[2])) 
							|| (names[2].indexOf("_yn") != -1 && "N".equals(var.get(names[2])))
							|| (names[2].indexOf("is_") != -1 && "0".equals(var.get(names[2])))
						) {
							offset = if_end + etag.length();
						}
					} else {
						setError("If end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("IFNOT START '") || cmd.startsWith("@nif(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					String etag = !cmd.startsWith("@") ? "<!-- IFNOT END '" + names[2] + "' -->" : "<!--/nif(" + names[2] + ")-->";
					int if_end = buffer.indexOf(etag, offset);
					if(if_end != -1) {
						if(var.get(names[2]) != null 
							&& !"false".equals(var.get(names[2])) 
							&& !"".equals(var.get(names[2]))
							&& !(names[2].indexOf("_yn") != -1 && "N".equals(var.get(names[2])))
							&& !(names[2].indexOf("is_") != -1 && "0".equals(var.get(names[2])))
						) {
							offset = if_end + etag.length();
						}
					} else {
						setError("If end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("@debug(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					out.write("<div style='left:0px; top:0px; border:3px solid red; position:absolute; z-index:999999; width:50%; overflow:auto; height:600px; background:#fff'>");
					debug(var);

					Enumeration e = loop.keys();
					while(e.hasMoreElements()) {
						String key = (String)e.nextElement();
						out.write("<br><strong>" + key + "</strong> { <blockquote>");
						DataSet tmp = (DataSet)loop.get(key);
						tmp.first();
						while(tmp.next()) {
							debug(tmp.getRow());
							out.write("<hr>");
						}
						out.write("</blockquote> } ");
					}
					out.write("</div>");
				}
			} else {
				setError("Command end tag is not found");
				out.write("<!-- ");
			}
		}
		parseVar(buffer.substring(offset));
	}

	private void debug(Hashtable h) throws Exception {

		Enumeration e = h.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			out.write("<strong>" + key + "</strong> : " + h.get(key).toString() + "<br>");
		}
		
	}

	private String[] parseCmd(String buffer) {
		buffer = buffer.trim();
		String[] ret = new String[3];
		if(buffer.startsWith("@")) { 
			String[] arr1 = buffer.split("\\(");
			if(arr1.length != 2) return null;
			ret[0] = arr1[0].substring(1);
			ret[1] = ret[0].equals("name") ? "NAME" : "FILE";
			ret[2] = parseString(arr1[1].substring(0, arr1[1].length() - 1));
		} else {
			String[] arr1 = buffer.split(" ");
			if(arr1.length != 3) return null;
			ret[0] = arr1[0].toUpperCase();
			ret[1] = arr1[1].toUpperCase();
			ret[2] = parseString(arr1[2].substring(1, arr1[2].length() - 1));
		}
		return ret;
	}

	public String parseString(String buffer) {
        String arr1[] = buffer.split("\\}\\}");
        StringBuffer sb = new StringBuffer();

        for(int i=0; i<arr1.length; i++) {
            String arr2[] = arr1[i].split("\\{\\{");
			sb.append(arr2[0]);
            if(arr2.length == 2) {
                if(var.containsKey(arr2[1])) {
					sb.append(var.get(arr2[1]).toString());
				}
            }
        }
		return sb.toString();
    }

	private void parseVar(String buffer) throws Exception {
		int tail = 0, offset = buffer.length() - 2;
		if(offset >= 0 && buffer.substring(offset).equals("}}")) {
			buffer += " "; tail = 1;
		}
        String arr1[] = buffer.split("\\}\\}");
		if(arr1.length > 1) {
			for(int i=0, len=arr1.length - tail; i<len; i++) {
				String arr2[] = arr1[i].split("\\{\\{");
				out.write(arr2[0]);
				if(arr2.length == 2) {
					if(var.containsKey(arr2[1])) {
						out.write(var.get(arr2[1]).toString());
					} else if(message != null) {
						out.write(message.get(arr2[1]));
					}

				} else if(arr2.length > 2) {
					int max = arr2.length - 1;
					for(int j=1; j<max; j++) {
						out.write("{{" + arr2[j]);
					}
					if(var.containsKey(arr2[max])) {
						out.write(var.get(arr2[max]).toString());
					}
				} else if(i != (arr1.length - 1)) {
					out.write("}}");
				}
			}
		} else {
			out.write(buffer);
		}
		out.flush();
    }

    public String readFile(String filename) throws Exception {
        File f = new File(root + filename);
        if(!f.exists()) {
            f = new File(filename);
            if(!f.exists()) {
                setError("File not found!!, filename is " + root + filename);
                return "";
            }
        }

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
    }

	private void setError(String msg) throws Exception {
		if(debug == true) {
			if(null != out) out.write("<hr>" + msg + "<hr>\n");
			else Malgn.errorLog(msg);
		}
	}

	public void clear() {
		var.clear();
		loop.clear();
	}

	public String replace(String s, String sub, String with) {
		int c = 0;
		int i = s.indexOf(sub,c);
		if (i == -1) return s;

		StringBuffer buf = new StringBuffer(s.length() + with.length());

		synchronized(buf) {
			do {
				buf.append(s.substring(c, i));
				buf.append(with);
				c = i + sub.length();
			} while((i = s.indexOf(sub, c)) != -1);
			if(c < s.length()) {
				buf.append(s.substring(c, s.length()));
			}
			return buf.toString();
		}
	}
}