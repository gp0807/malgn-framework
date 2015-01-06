package malgnsoft.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class MalgnServlet extends GenericServlet implements java.io.Serializable {

	protected String docRoot = Config.getDocRoot();
	protected String webUrl = Config.getWebUrl();
	protected String jndi = Config.getJndi();
	protected String tplRoot = Config.getTplRoot();
	protected String dataDir = Config.getDataDir();

	protected HttpServletRequest request;
	protected HttpServletResponse response;
    protected PrintWriter out;

	protected Malgn m;
	protected Form f;
	protected Page p;
	protected Auth auth;

	protected boolean isLogin = false;

    public MalgnServlet() { }
    
    protected void index() throws Exception { }
  
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            request = (HttpServletRequest) req;
            response = (HttpServletResponse) res;

			request.setCharacterEncoding(Config.getEncoding());
			response.setContentType("text/html; charset=" + Config.getEncoding());

			out = response.getWriter();
			m = new Malgn(request, response, out);

			f = new Form("form1");
			f.setRequest(request);

			p = new Page(tplRoot);
			p.setRequest(request, response);
			p.setWriter(out);

	        index();
        } catch (Exception e) {
			e.printStackTrace(System.out);
        }
    }
}
