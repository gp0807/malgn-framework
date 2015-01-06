package malgnsoft.util;

import java.util.*;
import malgnsoft.db.*;
import javax.servlet.http.HttpServletRequest;


public class Pager {

	private int pageNum = 1;
	private int totalNum = 0;
	private int listNum = 20;
	private int naviNum = 10;

	private HttpServletRequest _request;
	private String pageVar = "page";
	private String link;

	public int linkType = 0;

	public Pager() throws Exception {

	}

	public Pager(HttpServletRequest req) throws Exception {
		_request = req;
	}

	public void setPageVar(String str) {
		pageVar = str;
	}

	public void setPageNum(int num) {
		pageNum = num;
	}

	public void setTotalNum(int num) {
		totalNum = num;
	}

	public void setListNum(int num) {
		listNum = num;
	}

	public void setNaviNum(int num) {
		naviNum = num;
	}

	public void setLink(String link) {
		this.link = link;
	}	

	public int getPageNum() {
		return pageNum;
	}

	public int getLeftPage() {
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		if(firstPage > 1) return firstPage - 1;
		else return 0;
	}

	public int getRightPage() {
		int totalPage = (int)(java.lang.Math.ceil((double)totalNum / (double)listNum));
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		int lastPage = firstPage + naviNum - 1;
		if(lastPage < totalPage) return lastPage + 1;
		else return 0;
	}

	public String getPager() throws Exception {

		parseQuery();

		if(totalNum == 0) return "";
		int totalPage = (int)(java.lang.Math.ceil((double)totalNum / (double)listNum));
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		int lastPage = firstPage + naviNum - 1;
		if(totalPage < lastPage) {
			lastPage = totalPage;
		}

		StringBuffer sb = new StringBuffer();

		if(linkType == 9) {

			//첫 페이지
			sb.append("<button type=\"button\" class=\"btn\"" + (pageNum > 1 ? " onclick=\"location.href = '" + getPageLink(1) + "'\"" : "") + " title=\"First Page\" style=\"background:#fff\"><i class=\"icon-double-angle-left\"></i></button>");

			//이전 블럭 페이지
			sb.append("<button type=\"button\" class=\"btn\"" + (firstPage > 1 ? " onclick=\"location.href = '" + getPageLink(firstPage - 1) + "'\"" : "") + " title=\"Previous " + naviNum + " Pages\" style=\"background:#fff\"><i class=\"icon-angle-left\"></i></button>");

			for(int i = firstPage; i <= lastPage; i++) {
				sb.append("<button type=\"button\" class=\"btn" + (pageNum == i ? " current" : "") + "\"");
				if(pageNum != i) {
					sb.append(" onclick=\"location.href = '" + getPageLink(i) + "'\" style=\"background:#fff\"");
				} 
				sb.append(">" + i + "</button>");
			}

			//다음 블럭 페이지
			sb.append("<button type=\"button\" class=\"btn\"" + (lastPage < totalPage ? " onclick=\"location.href = '" + getPageLink(lastPage + 1) + "'\"" : "") + " title=\"Next " + naviNum + " Pages\" style=\"background:#fff\"><i class=\"icon-angle-right\"></i></button>");

			//마지막 페이지
			sb.append("<button type=\"button\" class=\"btn\"" + (pageNum < totalPage ? " onclick=\"location.href = '" + getPageLink(totalPage) + "'\"" : "") + " title=\"Last Page\" style=\"background:#fff\"><i class=\"icon-double-angle-right\"></i></button>");

		} else {

			String firstImg = "<div class='page_first_btn' title='First Page'><!----></div>";
			String prevImg = "<div class='page_prev_btn' title='Previous " + naviNum + " Pages'><!----></div>";
			String pImg = "<div class='page_p_btn' title='Previous Page'><!----></div>";
			String nImg = "<div class='page_n_btn' title='Next Page'><!----></div>";
			String nextImg = "<div class='page_next_btn' title='Next " + naviNum + " Pages'><!----></div>";
			String lastImg = "<div class='page_last_btn' title='Last Page'><!----></div>";
			String separator = "<div class='page_seperator'><!----></div>";
			
			sb.append("<style>");
			sb.append(".page_box { display:table; margin:0 auto; text-align:center; }");
			sb.append(".page_box ul { margin:0px; list-style:none; padding:0px; }");
			sb.append(".page_box li { margin:0px; padding:0px; display:inline-block;*zoom:1;*display:inline; }");
			sb.append(".page_box .page_margin { overflow:hidden !important; }");
			sb.append("</style>");
			sb.append("<div class='page_box'><ul>");

			//첫 페이지
			sb.append("<li>" + (pageNum > 1 ? "<a href='"+getPageLink(1)+"' class='on'>" + firstImg + "</a>" : firstImg) + "</li>");

			//이전 블럭 페이지
			sb.append("<li>" + (firstPage > 1 ? "<a href='"+getPageLink(firstPage-1)+"' class='on'>" + prevImg + "</a>" : prevImg) + "</li>");

			//이전 페이지
			sb.append("<li>" + (pageNum > 1 ? "<a href='"+getPageLink(pageNum-1)+"' class='on'>" + pImg + "</a>" : pImg) + "</li>");

			sb.append("<li class='page_margin'>&nbsp;</li>");
			if(linkType == 2) {
				sb.append("<li><span class='page_current'>" + pageNum + "</span><span class='page_seperator'>/</span><span class='page_total'>" + totalPage + "</span></li>");
			} else {
				for(int i = firstPage; i <= lastPage; i++) {
					sb.append("<li>");
					if(pageNum != i) {
						sb.append("<a href='"+getPageLink(i)+"'><div class='page_number_btn'>"+ i +"</div></a>");
					} else {
						sb.append("<div class='page_number_btn_on'>"+ i + "</div>");
					}
					sb.append("</li>");
					if(i < lastPage) sb.append("<li>" + separator + "</li>");
				}
			}
			sb.append("<li class='page_margin'>&nbsp;</li>");

			//다음 페이지
			sb.append("<li>" + (pageNum < totalPage ? "<a href='"+getPageLink(pageNum + 1)+"' class='on'>" + nImg + "</a>" : nImg) + "</li>");

			//다음 블럭 페이지
			sb.append("<li>" + (lastPage < totalPage ? "<a href='"+getPageLink(lastPage+1)+"' class='on'>" + nextImg + "</a>" : nextImg) + "</li>");

			//마지막 페이지
			sb.append("<li>" + (pageNum < totalPage ? "<a href='"+getPageLink(totalPage)+"' class='on'>" + lastImg + "</a>" : lastImg) + "</li>");

			sb.append("<li style='clear:both; margin:0px; padding:0px; height:0px; border-width:0px; overflow-x:hidden; overflow-y:hidden;'></li>");
			sb.append("</ul></div>");
		}

		return sb.toString();
	}
	public DataSet getPageData() throws Exception {

		parseQuery();

		if(totalNum == 0) return new DataSet();
		int totalPage = (int)(java.lang.Math.ceil((double)totalNum / (double)listNum));
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		int lastPage = firstPage + naviNum - 1;
		if(totalPage < lastPage) { lastPage = totalPage; }

		DataSet info = new DataSet();
		info.addRow();
		info.put("total_page", totalPage);
		info.put("current_page", pageNum);
		info.put("first_page", firstPage);
		info.put("last_page", lastPage);

		info.put("first_link", pageNum > 1 ? getPageLink(1) : "");
		info.put("prev_link", firstPage > 1 ? getPageLink(firstPage - 1) : "");
		info.put("p_link", pageNum > 1 ? getPageLink(pageNum - 1) : "");
		info.put("n_link", pageNum < totalPage ? getPageLink(pageNum + 1) : "");
		info.put("next_link", lastPage < totalPage ? getPageLink(lastPage + 1) : "");
		info.put("last_link", pageNum < totalPage ? getPageLink(totalPage) : "");

		info.put("first_title", "First page");
		info.put("prev_title", "Previous " + naviNum + " Pages");
		info.put("p_title", "Previous Page");
		info.put("n_title", "Next Page");
		info.put("next_title", "Next " + naviNum + " Pages");
		info.put("last_title", "Last page");

		DataSet pages = new DataSet();
		for(int i = firstPage; i <= lastPage; i++) {
			pages.addRow();
			pages.put("page_link", pageNum != i ? getPageLink(i) : "");
			pages.put("pageno", i);
		}
		pages.first();
		info.put(".pages", pages);

		return info;
	}

	private void parseQuery() throws Exception {
		link = _request.getRequestURI() + "?";
		String query = _request.getQueryString();
		if(query != null) {
			StringTokenizer token = new StringTokenizer(query, "&");
			String subtoken = null;
			String key = null;
			String value = null;
			StringBuffer sb = new StringBuffer();
			while(token.hasMoreTokens()) {
				int itmp;
				subtoken = token.nextToken();
				if((itmp = subtoken.indexOf("=")) != -1) {
					key = subtoken.substring(0,itmp);
					value = subtoken.substring(itmp+1);
					if(!key.equals(pageVar)) {
						sb.append(key + "=" + value + "&");
					}
				}
			}
			query = sb.toString();
		}
		if(!"".equals(query) && query != null) link = link + query;
	}

	private String getPageLink(int num) {
		if(this.linkType == 1) return "javascript:NaviPage("+ num +")";
		else return link + pageVar + "=" + num;
	}
}