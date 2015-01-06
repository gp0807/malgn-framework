package malgnsoft.util;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import malgnsoft.db.DataSet;
import java.io.Writer;
import malgnsoft.util.Malgn;

public class OpenApi {

	public String[] apiTypes = { 
		"naverbook=>네이버책",
		"naverimage=>네이버이미지",
		"navernews=>네이버뉴스",
		"naverkin=>네이버지식",
		"tomorrow=>내일검색",
		"archive=>국가기록원",
		"youtube=>You Tube" 
	};

/*
	//국가기록원 전용
	//기록물 유형
	public String[] archivesType1 = { "01=>일반기록물", "02=>시청각기록물", "03=>대통령기록물", "04=>총독부기록물", "05=>정부간행물", "06=>해외기록물", "07=>역사기록물", "08=>행정박물", "09=>민간기록물", "10=>영화필름", "11=>방송프로그램" };
	//doc_type 문자 기록물 형태 
	public String[] archivesTpype2 = { "1=>행정박물(관인류)", "2=>행정박물(상징기념물)", "3=>행정박물(사무집기류)", "4=>행정박물(기타)", "A=>일반문서류", "B=>도면류", "C=>사진,필름류", "D=>녹음,동영상류", "E=>카드류", "F=>대장류"", ""G=>국무회의록", "H=>지도", "I=>대통령전자문서", "M=>정부간행물", "O=>일반도서", "P=>총독부간행물" }; 
	*/

	URL url = null;
	InputStream is = null;
	Document xmlDocument = null;
	DocumentBuilderFactory factory = null;
	DocumentBuilder builder = null;

	Element root = null;
	NodeList items = null;

	String apiUrl = null;
	String apiName = null;
	String keyword = null;
	Hashtable<String, String> parameters = new Hashtable<String, String>();

	String dataField = "item";
	String[] dataElements = null;
	String[] reportElements = null;
	String dateFormat = null;
	String dateConvFormat = null;

	Vector<String> errors = new Vector<String>();

	public OpenApi() {} //api타입목록 사용시

	public OpenApi(String apiName, String keyword) throws Exception { //검색시
		this.apiName = apiName.toLowerCase();
		this.keyword = keyword;
		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (Exception e) {
			errors.add("API검색 초기화 실패. error - create builder from factory.");
			Malgn.errorLog("{OpenApi.OpenApi} " + e.getMessage(), e);
		}
	}

	//파라미터 추가/덮어쓰기
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	//파라미터 GET스트링 얻기
	private String getParameters() {
		String str = "";
		Enumeration e = parameters.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			String value = parameters.containsKey(key) ? parameters.get(key).toString() : "";
			str += "&" + key + "=" + value;
		}
		return str.length() > 1 ? str.substring(1) : "";
	}

	//XML 파싱
	private void parse() throws Exception {
		url = new URL(apiUrl);
		is = url.openStream();

		try { 
			xmlDocument = builder.parse(is); 
			root = xmlDocument.getDocumentElement();
			items = root.getElementsByTagName(dataField);
		}
		catch(Exception e) { 
			errors.add("API검색을 할수 없습니다. error - parseData from inputstream."); 
			Malgn.errorLog("{Generator.parse} " + e.getMessage(), e);
		}
		finally { if(is != null) is.close(); }

	}

	//결과를 데이타셋으로 반환
	public DataSet getDataSet() throws Exception {
		DataSet result = new DataSet();
		if(initialize()) {
			Locale loc = new Locale("ENGLISH");
			loc.setDefault(loc.US);
			parse();

			if(null != items) {
				for(int i=0; i<items.getLength(); i++) {
					result.addRow();
					NodeList subItems = items.item(i).getChildNodes();
					for(int j=0; j<subItems.getLength(); j++) {
						String[] dataElementKeys = Malgn.getKeys(dataElements);
						String nodeName = subItems.item(j).getNodeName();
						if(Malgn.inArray(nodeName, dataElementKeys)) {
							String key = Malgn.getItem(subItems.item(j).getNodeName().trim(), dataElements).toLowerCase();
							String value = null != subItems.item(j).getFirstChild() ? subItems.item(j).getFirstChild().getNodeValue() : "";
							result.put(key, value);

							if("media:thumbnail".equals(nodeName) || "media:player".equals(nodeName)) {
								result.put(key, ((Element)subItems.item(j)).getAttribute("url"));
							}
							if("pubdate".equals(key) && null != dateFormat) {
								result.put(key + "_conv", Malgn.getTimeString(dateConvFormat, Malgn.strToDate(dateFormat, value, loc)));
							}
						}
					}
					result.put("__i", i);
					result.put("__asc", i + 1);
				}
			}
		} else {
			errors.add("지정되지 않은 API. error - unknown api.");
		}
		return result;
	}

	//디버깅
	public void error(Writer out) throws Exception {
		out.write(!errors.isEmpty() ? Malgn.join("<hr><br>", errors.toArray()) : "");
		errors.clear();
	}



	/*
	 * api 정보 셋팅(사용가능한 것만)
	 */
	private boolean initialize() throws Exception {
		//네이버뉴스
		if("navernews".equals(apiName)) {
			parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7"); //서울디자인DB 네이버 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("target", "news");
			parameters.put("start", "1");
			parameters.put("display", "100");
			apiUrl = "http://openapi.naver.com/search?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "originallink", "link", "description", "pubDate" };
			reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" }; //검색리포트정보 미구현(필요없음..)

			dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z";
			dateConvFormat = "yyyy.MM.dd HH:mm";

			return true;
		} 
		//네이버책
		if("naverbook".equals(apiName)) {
			parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7"); //서울디자인DB 네이버 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("target", "book");
			parameters.put("start", "1");
			parameters.put("display", "100");
			apiUrl = "http://openapi.naver.com/search?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "originallink", "link", "image", "author", "price", "discount", "publisher", "pubdate", "isbn", "description" };
			reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };

			dateFormat = "yyyyMMdd";
			dateConvFormat = "yyyy.MM.dd";

			return true;
		} 
		//네이버지식
		if("naverkin".equals(apiName)) {
			parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7"); //서울디자인DB 네이버 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("target", "kin");
			parameters.put("start", "1");
			parameters.put("display", "100");
			apiUrl = "http://openapi.naver.com/search?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "link", "description" };
			reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };

			return true;
		} 
		//네이버이미지
		if("naverimage".equals(apiName)) {
			parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7"); //서울디자인DB 네이버 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("target", "image");
			parameters.put("start", "1");
			parameters.put("display", "100");
			apiUrl = "http://openapi.naver.com/search?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "link", "thumbnail", "sizeheight", "sizewidth" };
			reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };

			return true;
		} 
		//내일검색
		if("tomorrow".equals(apiName)) {
			parameters.put("apikey", "58A29064D80F4E055D88E39C719AB9445D0F160C"); //서울디자인DB 내일검색 인증키
			parameters.put("q", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("sort", "1");
			parameters.put("count", "100");
			apiUrl = "http://naeil.incruit.com/rss/search/?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "link", "description" };
			reportElements = new String[] { "" };
			
			return true;
		}
		//국가기록원나라검색
		if("archive".equals(apiName)) {
			parameters.put("key", "J0J9H2X6C4U7H2M9H2X1Z3X5W3X0Z5T0"); //서울디자인DB 국가기록원 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("sort", "1");
			parameters.put("online_reading", "Y");
			parameters.put("display", "100");
			apiUrl = "http://search.archives.go.kr/openapi/search.arc?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "link", "prod_name=>description" };
			reportElements = new String[] { "" };
			
			return true;
		}
		//YOU TUBE
		if("youtube".equals(apiName)) {
		//	parameters.put("key", "AI39si5vjDviDPuyyCknkXj7CdmRfWLrMqf-yyN0JsidFzKYlLI3vlTQLP24BFOnpFql91idKc7EKUIVKq0ajA4yRVX_lA3RDg"); //서울디자인DB youtube 인증키
			parameters.put("q", URLEncoder.encode(keyword, "utf-8"));
			apiUrl = "http://gdata.youtube.com/feeds/api/videos?" + getParameters();
			errors.add(apiUrl);
			dataField = "media:group";
			dataElements = new String[] { "media:title=>title", "media:player=>link", "media:description=>description", "media:thumbnail=>image" };
			reportElements = new String[] { "" };

			return true;
		}
		/*
		//네이버지도좌표(좌표만 가져옴)
		if("naverkin".equals(apiName)) {
			parameters.put("key", "9c79f18d0d8c2ce6d35318aa47a8b553"); //서울디자인DB 네이버 지도 인증키
			parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
			parameters.put("target", "kin");
			parameters.put("start", "1");
			parameters.put("display", "100");
			apiUrl = "http://openapi.naver.com/search?" + getParameters();
			errors.add(apiUrl);
			dataField = "item";
			dataElements = new String[] { "title", "link", "description" };
			reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };

			return true;
		} 
		*/

		return false;
	}


}
