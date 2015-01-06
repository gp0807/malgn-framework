package malgnsoft.util;

import java.io.File;
import java.util.Date;
import java.io.Writer;
import malgnsoft.util.Template;
import malgnsoft.db.*;
import java.util.*;


public class Page extends Template {

	protected String layout;
	protected String body;
	protected long startTime;
	public boolean pageInfo = false;

	public Page() {
		super(Config.getTplRoot());
		startTime = System.currentTimeMillis();
	}
	public Page(String root) {
		super(root);
		startTime = System.currentTimeMillis();
	}

	public void setLayout(String layout) {
		if(layout == null) layout = null;
		else {
			this.layout = "layout/layout_" + layout.replace('.', '/') + ".html";
			File file = new File(this.root + "/" + this.layout);
			if(!file.exists()) {
				this.layout = "layout/layout_blank.html";
			}
		}
	}

	public void setBody(String body) {
		this.body = body.replace('.', '/') + ".html";
	}

	public void setWriter(Writer out) {
		this.out = out;
	}

	public void display() throws Exception {
		display(this.out);
	}
	public void display(Writer out) throws Exception {
		if(this.layout == null) this.print(out, this.body);
		else {
			this.setVar("BODY", this.body);
			this.print(out, this.layout);
		}

		if(pageInfo) {
			long endTime = System.currentTimeMillis();
			double exeTime = (double)(endTime - startTime) / 1000;
			out.write("\r\n<!-- LAYOUT : " + this.layout + " -->");
			out.write("\r\n<!-- BODY : " + this.body + " -->");
			out.write("\r\n<!-- EXECUTION TIME : " + exeTime + " Second -->");
		}
	}

	public String fetchAll() throws Exception {
		this.setVar("BODY", this.body);
		return fetch(null == this.layout ? this.body : this.layout);
	}
}