package malgnsoft.util;

import java.util.*;
import java.io.*;
import malgnsoft.db.DataSet;
import net.sf.jxls.transformer.XLSTransformer;
import javax.servlet.http.HttpServletResponse;

public class ExcelTemplate {

    private Map<String, Object> beans = new HashMap<String, Object>();

    public ExcelTemplate() {

    }

    public void setVar(String name, Object value) {
        beans.put(name, value);
    }

    public void setVar(String name, DataSet value) {
        if(value != null) {
            value.first();
            if(value.next()) beans.put(name, value.getRow());
        }
    }

    public void setLoop(String name, DataSet value) {
        if(value != null) {
            value.first();
            List<Object> list = new ArrayList<Object>();
            while(value.next()) {
                list.add(value.getRow());
            }
            beans.put(name, list);
        }
    }

    public void transfer(String src, String target) throws Exception {
        XLSTransformer transformer = new XLSTransformer();
        transformer.transformXLS(src, beans, target);
    }

	public void transfer(HttpServletResponse response, String src, String filename) throws Exception {
		String target = Config.getDataDir() + "/excel/" + new Date().getTime() + ".xls";
		File f = new File(target);
		if(!f.getParentFile().isDirectory()) {
			f.getParentFile().mkdirs();
		}

        XLSTransformer transformer = new XLSTransformer();
        transformer.transformXLS(src, beans, target);

		if(f.exists()){
			response.setContentType("application/vnd.ms-excel");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + new String(filename.getBytes("KSC5601"),"8859_1") + "\"");

			BufferedInputStream fin = null;
			BufferedOutputStream outs = null;
			try {
				byte[] bbuf = new byte[2048];
				fin = new BufferedInputStream(new FileInputStream(f));
				outs = new BufferedOutputStream(response.getOutputStream());

				int read = 0;
				while ((read = fin.read(bbuf)) != -1){
					outs.write(bbuf, 0, read);
				}
			} catch(Exception e) {
			} finally {
				if(null != outs) outs.close();
				if(null != fin) fin.close();
			}
			f.delete();
		}
	}
}
