package malgnsoft.util;

import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import malgnsoft.db.*;
import javax.servlet.http.HttpServletRequest;
import java.io.Writer;

public class FileListManager {
	public String dir;
	public File[] files;

	public String mode = "FILE";
	public DataSet conditions = new DataSet();
	public DataSet betweens = new DataSet();
	public String oname = "name";
	public String ord = "ASC";
	public Writer out;
	public HttpServletRequest request;

	public int total = 0;
	public int ln = 20;
	public int nn = 10;
	public int pg = 1;
	
	public FileListManager(String dir) {
		this.dir = dir;
	}
	public FileListManager() {
		this.dir = "";
	}

	public void setRequest(HttpServletRequest request) throws Exception {
		this.request = request;

		String page = request.getParameter("page");
		if(page == null || "".equals(page)) pg = 1;
		else if(page.matches("^[0-9]+$")) pg = Integer.parseInt(page);
		else pg = 1;
	}

	public void setMode(String mode) { //DIR or FILE or ALL
		this.mode = mode;	
	}

	public void setListNum(int ln) {
		this.ln = ln;
	}

	public void setNaviNum(int nn) {
		this.nn = nn;
	}

	public void addSearch(String field, String value, String type) {
		if(!"".equals(value)) {
			conditions.addRow();
			conditions.put("field", field);
			conditions.put("value", value);
			conditions.put("type", type);
		}
	}

	public void addBetween(String field, String svalue, String evalue) {
		betweens.addRow();
		betweens.put("field", field);
		betweens.put("svalue", svalue);
		betweens.put("evalue", evalue);
	}

	public void setOrderBy(String orderby) {
		String[] tmp = orderby.split("\\ ");
		if(tmp.length == 2) {
			this.oname = tmp[0].toLowerCase();	
			this.ord = tmp[1].toUpperCase();
		}
	}

	public String getPaging() throws Exception {
		Pager pager = new Pager(request);
		pager.setTotalNum(total);
		pager.setListNum(ln);
		pager.setNaviNum(nn);
		pager.setPageNum(pg);
		return pager.getPager();
	}

	public DataSet getPageData() throws Exception { 
		Pager pager = new Pager(request);
		pager.setTotalNum(total);
		pager.setListNum(ln);
		pager.setNaviNum(nn);
		pager.setPageNum(pg);

		return pager.getPageData();
	}

	public String getTotalString() {
		return "<span style=\"font-family:arial, dotum;font-weight:normal;\">Total : <font color=\"blue\">" + total + "</font> 건</span>";
	}

	public int getTotalNum() {
		return total;
	}

	public DataSet getDataSet() {
		DataSet list = new DataSet();
		if(new File(dir).exists()) {
			files = new File(dir).listFiles(new MyFilter());
			total = files.length;
			Arrays.sort(files, new MySort());

			for(int i=0, max=files.length; i<max; i++) {
				int s = (pg - 1) * ln;
				if(i >= s && i < s + ln) {
					list.addRow();
					list.put("name", files[i].getName());
					list.put("path", files[i].toString());
					list.put("length", new Long(files[i].length()));
					list.put("time", new Long(files[i].lastModified()));
					list.put("date", getTimeString("yyyyMMddHHmmss", new Date(new Long(files[i].lastModified()))));
					list.put("directory", files[i].isDirectory());
					int pos = list.s("path").lastIndexOf(".");
					list.put("ext", !files[i].isDirectory() ? list.s("path").substring(pos > -1 ? pos + 1 : list.s("path").length()).toLowerCase() : "");

					list.put("__asc", i + 1);
					list.put("__ord", total - list.i("__asc") + 1);
					list.put("ROW_CLASS", list.i("__asc") % 2 == 0 ? "even" : "odd");
				}
				if(i > s + ln) break;
			}
		}
		list.first();
		return list;
	}

	public String getTimeString(String sformat, Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(sdf == null || date == null) return "";
		return sdf.format(date);
	}


	private int depth = 0;
	private int id = 0;
	private DataSet result;
	private Hashtable<String, String> lmap;

	public DataSet getDirTree() throws Exception {
		return getDirTree(dir);
	}
	public DataSet getDirTree(String path) throws Exception {
		result = new DataSet(); 
		lmap = new Hashtable<String, String>();
		depth = 0; id = 0; 
		result = getDirTree(path, 0);
		result.first();
		return result;
	}
	private DataSet getDirTree(String path, int pid) throws Exception {
		File[] files = null;
		File file = new File(path);
		if(file.isDirectory()) {
			files = file.listFiles();
		} else {
			files = new File[1];
			files[0] = file;
		}
		if(null != files) {
			Arrays.sort(files);
			for(int i=0; i<files.length; i++) {
				boolean islink = false;
				if(files[i].isDirectory()) {
					if(files[i].getName().startsWith(".")) continue;
					if(!files[i].getAbsolutePath().equals(files[i].getCanonicalPath())) {
						islink = true;
						String key = files[i].getName();
						if(lmap.containsKey(key) && ((String)lmap.get(key)).equals(files[i].getCanonicalPath())) {
							continue;
						}
						lmap.put(key, files[i].getCanonicalPath());
					}
					result.addRow();
					result.put("id", ++id + "");
					result.put("parent_id", pid + "");
					result.put("path", files[i].toString() + "");
					result.put("name", files[i].getName() + "");
					result.put("date", getTimeString("yyyyMMddHHmmss", new Date(new Long(files[i].lastModified()))));
					result.put("time", files[i].lastModified() + "");
					result.put("depth", depth + "");
					depth++;
					getDirTree(files[i].toString(), id);
					depth--;
				}
			}
		}
		return result;
	}

	/**
	 *
	 */
	class MySort implements Comparator<Object> {
		public int compare(Object o1, Object o2) {
			int oper = "ASC".equals(ord) ? 1 : -1;
			File f1 = (File)o1;
			File f2 = (File)o2;
			String p1 = "" + !f1.isDirectory();
			String p2 = "" + !f2.isDirectory();
			long ip1 = !f1.isDirectory() ? 0 : 10000000000000L;
			long ip2 = !f2.isDirectory() ? 0 : 10000000000000L;

			if("name".equals(oname)) {
				if ((p1 + f1.getName()).compareTo((p2 + f2.getName())) > 0) { return 1 * oper; }
				else if ((p1 + f1.getName()).compareTo((p2 + f2.getName())) < 0) { return -1 * oper; }
			} else if("path".equals(oname)) {
				if ((p1 + f1.toString()).compareTo((p2 + f2.toString())) > 0) { return 1 * oper; }
				else if ((p1 + f1.toString()).compareTo((p2 + f2.toString())) < 0) { return -1 * oper; }
			} else if("length".equals(oname)) {
				if (ip1 + f1.length() > ip2 + f2.length()) { return 1 * oper; }
				else if (ip1 + f1.length() < ip2 + f2.length()) { return -1 * oper; }
			} else if("date".equals(oname)) {
				if (ip1 + f1.lastModified() > ip2 + f2.lastModified()) { return 1 * oper; }
				else if (ip1 + f1.lastModified() < ip2 + f2.lastModified()) { return -1 * oper; }
			} else if("ext".equals(oname)) {
				String ext1 = f1.getName().substring(f1.getName().lastIndexOf(".") + 1).toLowerCase();
				String ext2 = f2.getName().substring(f2.getName().lastIndexOf(".") + 1).toLowerCase();
				if ((p1 + ext1).compareTo(p2 + ext2) > 0) { return 1 * oper; }
				else if ((p1 + ext1).compareTo(p2 + ext2) < 0) { return -1 * oper; }
			}
			return 0;
		}
	}


	/**
	 *
	 */
	class MyFilter implements FileFilter {
		public boolean accept(File file) {
			boolean flag = false;

			if("DIR".equals(mode)) flag = file.isDirectory();
			else if("FILE".equals(mode)) flag = !file.isDirectory();
			else flag = true;
			if(flag == false) return false;

			String filename = file.getName().toLowerCase();
			String filepath = file.toString().toLowerCase();
			long filelength = (long)file.length();
			long date = Long.parseLong(getTimeString("yyyyMMddHHmmss", new Date((long)file.lastModified())));

			betweens.first();
			while(betweens.next()) {
				String field = betweens.s("field");
				String svalue = betweens.s("svalue").toLowerCase();
				String evalue = betweens.s("evalue").toLowerCase();

				if("length".equals(field)) {
					flag = Long.parseLong(svalue) <= filelength && filelength <= Long.parseLong(evalue);
				} else if("date".equals(field)) {
					flag = Long.parseLong(svalue) <= date && date <= Long.parseLong(evalue);
				}
				if(flag == false) return false;
			}

			conditions.first();
			while(conditions.next()) {
				String field = conditions.s("field");
				String value = conditions.s("value").toLowerCase();
				String type = conditions.s("type");

				if("type".equals(field)) {
					String tmp[] = value.replace(" ", "").split("\\,");
					boolean iflag = false, mflag = false;
					for(int i=0; i<tmp.length; i++) {
						if("image".equals(tmp[i])) iflag = filename.matches("^.+\\.(jpg|gif|png|jpeg|bmp)$");
						else if("movie".equals(tmp[i])) mflag = filename.matches("^.+\\.(avi|asf|wav|wmv|wmp|ra|mov|mpeg|flv|swf|mp3|mp4|mkv)$");
					}
					flag = iflag || mflag;
				} else if("name".equals(field) || "path".equals(field)) {
					if("like".equals(type)) flag = filename.indexOf(value) != -1;
					else if("=".equals(type)) flag = filename == value;
				} else if("length".equals(field)) {
					if(">=".equals(type)) flag = filelength >= Long.parseLong(value);
					else if("<=".equals(type)) flag = filelength <= Long.parseLong(value);
					else if("=".equals(type)) flag = filelength == Long.parseLong(value);
				} else if("date".equals(field)) {
					if("like".equals(type)) flag = ("" + date).indexOf(value) != -1;
					else if(">=".equals(type)) flag = date >= Long.parseLong(value);
					else if("<=".equals(type)) flag = date <= Long.parseLong(value);
					else if("=".equals(type)) flag = date == Long.parseLong(value);
				} else if("ext".equals(type)) {
					flag = filename.endsWith(value);
				}
				if(flag == false) return false;
			}
			return flag;
		}
	}
}