package WebCrawler2MT;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Util {
	static String downloadHTMLPage(String pageURLString, int timeout) {
		 
		//InputStream is = null;
		BufferedReader br=null;
		try {
			URL pageURL = new URL(pageURLString);
			URLConnection connection=pageURL.openConnection();
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			// connection to the server
			//is = pageURL.openStream();
 
			br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			StringBuilder sb = new StringBuilder();
 
			// read website content
			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
 
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
 
	static org.w3c.dom.Document htmlToDocument(String htmlPage) {
		try {
			Document jSoupDoc = Jsoup.parse(htmlPage);
			// System.out.println("jsoup doc: " + jSoupDoc.outerHtml());
			W3CDom w3cDom = new W3CDom();
			org.w3c.dom.Document doc = w3cDom.fromJsoup(jSoupDoc);
			return doc;
 
		} catch (Exception e) {
			return null;
		}
	}
 
	static void cleanEmptyNodes(Node node) {
		NodeList childs = node.getChildNodes();
		for (int n = childs.getLength() - 1; n >= 0; n--) {
			Node child = childs.item(n);
			short nodeType = child.getNodeType();
			if (nodeType == Node.ELEMENT_NODE) {
				cleanEmptyNodes(child);
			} else if (nodeType == Node.TEXT_NODE) {
				String trimmedNodeVal = child.getNodeValue().trim();
				if (trimmedNodeVal.length() == 0) {
					node.removeChild(child);
				} else {
					child.setNodeValue(trimmedNodeVal);
				}
			} else if (nodeType == Node.COMMENT_NODE) {
				node.removeChild(child);
			}
		}
	}
 
	static void printDocument(org.w3c.dom.Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
 
		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}
 
	// hack
	static org.w3c.dom.Document doc2Doc(org.w3c.dom.Document srcDoc) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			printDocument(srcDoc, bos);
 
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setValidating(false); // since the default value is false
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = dBuilder.parse(new ByteArrayInputStream(bos.toByteArray()));
			return doc;
		} catch (Exception e) {
			System.err.println("Exception: " + e);
		}
		return null;
 
	}
}
