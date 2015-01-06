package malgnsoft.util;

import java.io.File;
import jxl.*;
import malgnsoft.db.DataSet;
import malgnsoft.util.Malgn;

/**
 * <pre>
 * ExcelReader ex = new ExcelReader("/data/test.xls");
 * DataSet ds = ex.getDataSet();
 * </pre>
 */
public class ExcelReader {

	private Workbook workbook = null;
	private Sheet sheet = null;
	
	public ExcelReader(String path) throws Exception {
		File f = new File(path);
		if(f.exists()) {
			workbook = Workbook.getWorkbook(f);
			sheet = workbook.getSheet(0);
		}
	}

	public void setSheet(int i) throws Exception {
		if(workbook != null) {
			sheet = workbook.getSheet(i);
		}
	}

	public DataSet getDataSet() throws Exception {
		return getDataSet(0);
	}
	public DataSet getDataSet(int s) throws Exception {
		DataSet ds = new DataSet();
		if(sheet != null) {
			int x = sheet.getColumns();
			int y = sheet.getRows();

			for(int i=s; i<y; i++) {
				ds.addRow();
				int j = 0;
				for(j=0; j<x; j++) {
					ds.put("col" + j, this.getString(j, i));
				}
			}
			this.close();
		}
		ds.first();
		return ds;
	}

	public String getString(int x, int y) {
		String ret = "";
		if(sheet != null) {
			try {
				Cell cell = sheet.getCell(x, y); 
				ret = cell.getContents();
			} catch(Exception e) {
				Malgn.errorLog("{ExcelReader.getString} " + e.getMessage(), e);
			}
		}
		return ret;
	}

	public int getInt(int x, int y) {
		int ret = 0;
		try {
			ret = Integer.parseInt(this.getString(x, y));
		} catch(Exception e) {
			Malgn.errorLog("{ExcelReader.getInt} " + e.getMessage(), e);
		}
		return ret;
	}

	public long getLong(int x, int y) {
		long ret = 0;
		try {
			ret = Long.parseLong(this.getString(x, y));
		} catch(Exception e) {
			Malgn.errorLog("{ExcelReader.getLong} " + e.getMessage(), e);
		}
		return ret;
	}

	public double getDouble(int x, int y) {
		double ret = 0.0;
		try {
			ret = Double.parseDouble(this.getString(x, y));
		} catch(Exception e) {
			Malgn.errorLog("{ExcelReader.getDouble} " + e.getMessage(), e);
		}
		return ret;
	}

	public void close() {
		if(workbook != null) workbook.close();
	}
}