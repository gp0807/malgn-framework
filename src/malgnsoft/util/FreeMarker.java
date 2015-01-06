package malgnsoft.util;

import freemarker.template.*;
import java.io.*;
import java.util.*;
import malgnsoft.db.*;

public class FreeMarker extends Page {

	private Configuration cfg = new Configuration();
	private freemarker.template.Template temp = null;

	public FreeMarker(String root) throws Exception {
		super(root);
		startTime = System.currentTimeMillis();

		cfg.setDirectoryForTemplateLoading(new File(root));
		cfg.setObjectWrapper(new DefaultObjectWrapper());
		cfg.setDefaultEncoding(Config.getEncoding());
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));  // FreeMarker 2.3.20 
	}

	public void display(Writer out) throws Exception {
		if(this.layout == null) temp = cfg.getTemplate(this.body);
		else {
			temp = cfg.getTemplate("/" + this.layout);
			this.setVar("BODY", "/" + this.body);
		}
		Map<String, Object> data = new HashMap<String, Object>(this.var);
		Enumeration e = this.loop.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			data.put(key, (DataSet)loop.get(key));
		}
		temp.process(data, out);
		if(pageInfo) {
			long endTime = System.currentTimeMillis();
			double exeTime = (double)(endTime - startTime) / 1000;
			out.write("\r\n<!-- LAYOUT : " + this.layout + " -->");
			out.write("\r\n<!-- BODY : " + this.body + " -->");
			out.write("\r\n<!-- EXECUTION TIME : " + exeTime + " Second -->");
		}
	}

	public String fetchAll() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		display(new OutputStreamWriter(bos));
		return bos.toString();
	}
}