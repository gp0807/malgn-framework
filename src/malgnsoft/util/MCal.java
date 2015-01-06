package malgnsoft.util;

import malgnsoft.util.Http;
import java.util.regex.*;
import java.util.*;

import malgnsoft.db.*;
import malgnsoft.util.Malgn;

public class MCal {
	public int yearRange = 5;

	public MCal() {
	}

	public MCal(int yearRange) {
		this.yearRange = yearRange;
	}

	public DataSet getYears() {
		return getYears(Malgn.parseInt(Malgn.getTimeString("yyyy")));
	}
	public DataSet getYears(String year) {
		if("".equals(year)) year = Malgn.getTimeString("yyyy");
		return getYears(Malgn.parseInt(year));
	}
	public DataSet getYears(int year) {
		DataSet list = new DataSet();
		for(int i=year-yearRange; i<year+yearRange; i++) {
			list.addRow();
			list.put("id", i);
			list.put("name", i);
		}
		list.first();
		return list;
	}
	public DataSet getMonths() {
		DataSet months = new DataSet();
		for(int i = 1; i <= 12; i++) {
			months.addRow();
			months.put("id", (i < 10 ? "0" : "") + i);
			months.put("name", (i < 10 ? "0" : "") + i);
			months.put("name2", i);
		}
		months.first();
		return months;
	}
	
	public DataSet getDays() {
		DataSet days = new DataSet();
		for(int i=1; i<=31; i++) {
			days.addRow();
			days.put("id", (i < 10 ? "0" : "") + i);
			days.put("name", (i < 10 ? "0" : "") + i);
			days.put("name2", i);
		}
		days.first();
		return days;
	}

	public DataSet getHours() {
		DataSet hours = new DataSet();
		for(int i=0; i<24; i++) {
			hours.addRow();
			hours.put("id", (i < 10 ? "0" : "") + i);
			hours.put("name", (i < 10 ? "0" : "") + i);
			hours.put("name2", i);
		}
		hours.first();
		return hours;
	}

	public DataSet getMinutes() {
		return getMinutes(1);
	}
	public DataSet getMinutes(int step) {
		DataSet minutes = new DataSet();
		for(int i=0; i<60; i+=step) {
			minutes.addRow();
			minutes.put("id", (i < 10 ? "0" : "") + i);
			minutes.put("name", (i < 10 ? "0" : "") + i);
			minutes.put("name2", i);
		}
		minutes.first();
		return minutes;
	}

	public int getWeekNum(String date, String format) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Malgn.strToDate(format, date));
		return calendar.get(calendar.DAY_OF_WEEK);
	}

	public int getWeekNum(String date) {
		return getWeekNum(date, "yyyyMMdd");
	}

	public Date getWeekFirstDate(String date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Malgn.strToDate("yyyyMMdd", date));
		return Malgn.addDate("D", -1 * calendar.get(calendar.DAY_OF_WEEK) + 1, calendar.getTime());
	}
	public Date getWeekLastDate(String date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Malgn.strToDate("yyyyMMdd", date));
		return Malgn.addDate("D", 7 - calendar.get(calendar.DAY_OF_WEEK), calendar.getTime());
	}
	public Date getMonthLastDate(String date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Malgn.strToDate("yyyyMMdd", date));
		return Malgn.strToDate(Malgn.getTimeString("yyyyMM", date) + calendar.getActualMaximum(calendar.DAY_OF_MONTH));
	}

	public DataSet getMonthDays(String date) {
		return getMonthDays(date, "yyyy-MM-dd");
	}
	public DataSet getMonthDays(String date, String format) {
		int ym = Integer.parseInt(Malgn.getTimeString("yyyyMM", date));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(Malgn.strToDate(format, date));
		Date startDate = Malgn.addDate("D", -1, getWeekFirstDate(Malgn.getTimeString("yyyyMM", date) + "01"));
		Date endDate = getWeekLastDate(Malgn.getTimeString("yyyyMM", date) + calendar.getActualMaximum(calendar.DAY_OF_MONTH));

		DataSet list = new DataSet(); int d = 0;
		while(true) {
			startDate = Malgn.addDate("D", 1, startDate);
			list.addRow();
			list.put("date", Malgn.getTimeString(format, startDate));
			if(Integer.parseInt(Malgn.getTimeString("yyyyMM", startDate)) < ym) list.put("type", "1");
			else if(Integer.parseInt(Malgn.getTimeString("yyyyMM", startDate)) == ym) list.put("type", "2");
			else if(Integer.parseInt(Malgn.getTimeString("yyyyMM", startDate)) > ym) list.put("type", "3");
			list.put("weeknum", (d % 7) + 1);
			list.put("__last", false);
			d++;

			if(Malgn.getTimeString(format, startDate).equals(Malgn.getTimeString(format, endDate))) break;
		}
		list.put("__last", true);
		list.first();
		return list;
	}
}