package malgnsoft.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import java.io.Writer;
import java.io.*;
import java.util.HashMap;
import java.net.URL;
import malgnsoft.db.*;
import malgnsoft.util.Malgn;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;


/**
 * <pre>
 * SimpleParser sp = new SimpleParser("/data/test.xml");
 * //SimpleParser sp = new SimpleParser("http://aaa.com/data/test.xml");
 * //sp.setDebug(out);
 * DataSet ds = sp.getDataSet("//rss/item");
 * m.p(ds);
 * </pre>
 */
public class SimpleParser {

	private Writer out = null;
	private boolean debug = false;
	private String path = null;
	private Document doc = null;

	public String errMsg = "";
	public String encoding = "UTF-8";

	public SimpleParser() {
	}

	public SimpleParser(String filepath) throws Exception {
		this.path = filepath;
	}

	public SimpleParser(String filepath, String encoding) throws Exception {
		this.path = filepath;
		this.encoding = encoding;
	}

	public void parse(String filepath, String encoding) throws Exception {
		InputStream is = null;
		try {
			if(filepath.indexOf("http") == 0) {
				URL url = new URL(filepath);
				is = url.openStream();
			} else if(filepath.indexOf("<?xml") == 0) {
				is = new ByteArrayInputStream(filepath.getBytes(encoding));
			} else {
				File f = new File(filepath);
				if(!f.exists()) {
					setError("File not found : " + filepath);
				} else {
					is = new FileInputStream(f);
				}
			}
			if(is != null) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
				doc = dBuilder.parse(new InputSource(in));
				doc.getDocumentElement().normalize();
			}
		} catch(Exception ex) {
			Malgn.errorLog("{SimpleParser.constructor} Path:" + filepath + " " + ex.getMessage(), ex);
			setError("Parser Error : " + ex.getMessage());			
		} finally {
			if(is != null) is.close();
		}
	}

	public void setDebug() {
		this.out = null;
		this.debug = true;
	}
	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}

	private void setError(String msg) {
		this.errMsg = msg;
		try {
			if(debug == true) {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Malgn.errorLog(msg);
			}
		} catch(Exception ex) {}
	}

	private NodeList getElements(Element elm, String[] nodeArr, int j) {
		if(j == nodeArr.length || "".equals(nodeArr[j])) return null;

		NodeList nodes = elm.getElementsByTagName(nodeArr[j]);
		if(nodes == null) return null;

		j++; if(j == nodeArr.length) return nodes;

		for(int i=0, max=nodes.getLength(); i<max; i++) {
			NodeList nodes2 = getElements((Element)nodes.item(i), nodeArr, j);
			if(nodes2 != null) return nodes2;
		}

		return null;
	}

	public DataSet getDataSet(String node) throws Exception {

		DataSet result = new DataSet();
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return result;

		try {
			String[] nodeArr = node.substring(2).split("/");

			if(!nodeArr[0].equals(doc.getDocumentElement().getTagName())) return result;

			NodeList nodes = null;
			if(nodeArr.length == 1) {
				nodes = doc.getElementsByTagName(nodeArr[0]);	
			} else {
				nodes = getElements(doc.getDocumentElement(), nodeArr, 1);
			}
			if(nodes == null) return result;

			for (int i=0, n=nodes.getLength(); i<n; i++) {
				result.addRow();
				NodeList xx = nodes.item(i).getChildNodes();
				if(xx != null) {
					for(int j=0, k=xx.getLength(); j<k; j++) {
						Node cnode = xx.item(j);
						if(cnode.getNodeType() == Node.ELEMENT_NODE) {
							Node child = cnode.getFirstChild();
							result.put(cnode.getNodeName(), child != null ? child.getNodeValue() : "");
						} else {
							result.put(cnode.getNodeName(), cnode.getNodeValue());					
						}
					}
				} else {
					if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {   
						Node child = nodes.item(i).getFirstChild();
						result.put(nodes.item(i).getNodeName(), child != null ? child.getNodeValue() : "");
					} else {
						result.put(nodes.item(i).getNodeName(), nodes.item(i).getNodeValue());					
					}
				}
			}
		} catch (Exception ex) {
			Malgn.errorLog("{SimpleParser.getDataSet} Node:" + node + " " + ex.getMessage(), ex);
			setError("XPath Error : " + ex.getMessage());
		}
		
		result.first();
		return result;
	}

	public String getAttribute(String xstr, String attr) throws Exception {
		String value = "";
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return value;
		try { 
			XPath xpath = XPathFactory.newInstance().newXPath();
			return getAttribute((Node)xpath.evaluate(xstr, doc, XPathConstants.NODE), attr);
		} catch(Exception ex) {
		}
		return value;
	}
	public String getAttribute(Node node, String attr) throws Exception {
		String value = "";
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return value;
		try { 
			value = node.getAttributes().getNamedItem(attr).getTextContent();
		} catch(Exception ex) {
		}
		return value;
	}

	public String getNodeValue(String xstr) throws Exception {
		String value = "";
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return value;
		try { 
			XPath xpath = XPathFactory.newInstance().newXPath();
			value = (String)xpath.evaluate(xstr, doc, XPathConstants.STRING);
		} catch(Exception ex) {
		}
		return value;
	}

	public NodeList getNodeList(String xstr) throws Exception {
		NodeList res = null;
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return res;
		try { 
			XPath xpath = XPathFactory.newInstance().newXPath();
			res = (NodeList)xpath.evaluate(xstr, doc, XPathConstants.NODESET);
		} catch(Exception ex) {
		}
		return res;
	}

	public Node getNode(String xstr) throws Exception {
		Node res = null;
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return res;
		try { 
			XPath xpath = XPathFactory.newInstance().newXPath();
			res = (Node)xpath.evaluate(xstr, doc, XPathConstants.NODE);
		} catch(Exception ex) {
		}
		return res;
	}

	public Document getDocument() throws Exception {
		if(doc == null) parse(this.path, this.encoding);
		if(doc == null) return null;
		return doc;
	}



}