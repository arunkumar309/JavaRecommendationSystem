/**
 * 
 */
package wikibooksCrawler;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * @author arun
 *
 */
public class WikibookCrawler {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String url = "https://en.wikibooks.org/wiki/Java_Programming";
		LinkedHashMap<String, HashMap<String, String>> topicsMap = new LinkedHashMap<String, HashMap<String, String>>();
		HashMap<String, LinkedHashMap<String, String>> pages = new HashMap<String, LinkedHashMap<String, String>>();
		getLinks(url, topicsMap);
		/*
		 * for(Map.Entry<String, HashMap<String,String>> entry:
		 * topicsMap.entrySet()){ String title = entry.getKey(); HashMap<String,
		 * String> tempMap = entry.getValue(); print("%s %s", title,
		 * tempMap.toString()); if (tempMap.containsKey(title) == false)
		 * getLinks(tempMap.get("link"), topicsMap); }
		 */
		getPageStart(url+"/Statements", pages);
		getText(url+"/Statements", pages);
		print("%d %s", pages.size() , pages.toString());
	}
	
	private static void getPageStart(String url,HashMap<String, LinkedHashMap<String, String>> hp) throws IOException {
		int index = url.lastIndexOf("/");
		String title = url.substring(index+1);
		Document doc = Jsoup.connect(url).get();
		Elements contents = doc.select("#mw-content-text > table");
		LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
		for(Element content : contents){
			StringBuilder sb = new StringBuilder();
			if (content.hasClass("wikitable")){
				content = content.nextElementSibling();
				String text;
				while(!content.nextElementSibling().tagName().equals("h2")){
					text = content.text();
					if(text.startsWith("Code section") || text.startsWith("Code listing")){
						int indexTemp = text.lastIndexOf(": ");
						text = text.substring(indexTemp+2);
					}
					sb.append(text+"\n");
					content = content.nextElementSibling();
				}
				sb.append(content.text()+"\n");
				tempMap.put("introduction", sb.toString());
			}
		}
		hp.put(title, tempMap);
	}

	private static void getText(String url, HashMap<String, LinkedHashMap<String, String>> hp) throws IOException {
		int index = url.lastIndexOf("/");
		String title = url.substring(index+1);
		Document doc = Jsoup.connect(url).get();
		Elements contents = doc.select("#mw-content-text > p");
		Elements subHeadings = doc.select("#mw-content-text > h2");
		Elements codes = doc.getElementsByClass("mw-highlight");
		LinkedHashMap<String, String> tempMap = hp.get(title);
		for(Element subHeading: subHeadings){
			int len = subHeading.text().length();
			String subTopic = subHeading.text().substring(0, len-6);
			String text;
			StringBuilder sb = new StringBuilder();
			while(!subHeading.nextElementSibling().tagName().equals("h2") && subHeading.nextElementSibling().hasText()){
				text = subHeading.nextElementSibling().text();
				if(text.startsWith("Code section") || text.startsWith("Code listing")){
					int indexTemp = text.lastIndexOf(": ");
					text = text.substring(indexTemp+2);
				}
				sb.append(text+"\n");
				subHeading = subHeading.nextElementSibling();
			}
			tempMap.put(subTopic, sb.toString());
		}
		
/*
		int i = 1;
		for (Element content : contents) {
			Elements codeTitle = content.nextElementSibling().getElementsByTag("b");
			String codeBlock = content.nextElementSibling().tagName();
			Elements code = content.nextElementSibling().getElementsByAttributeValueContaining("class", "mw-highligh");
			if (code.hasText()){
				print("%d\n################\n%s\n%s\n%s\n", i, content.text(), codeTitle.text(), code.text());
			}
			else if(codeBlock.equals("pre")){
				String codeBlockText = content.nextElementSibling().text();
				print("%d\n################\n%s\n%s\n", i, content.text(), codeBlockText);
			}
			else{
				print("%d\n################\n%s\n", i, content.text());
			}
			
			// print("%d\n______________________\n%s\n", i,content.text());
			i++;
		}
*/		
	}

	private static void getLinks(String url, LinkedHashMap<String, HashMap<String, String>> hp) throws IOException {
		try {
			print("Fetching %s...", url);
			Document doc = Jsoup.connect(url).get();
			Elements topicList = doc.select("#mw-content-text > ul > li");
			for (Element topic : topicList) {
				Element temp = topic.select("a[href]").last();
				String title = temp.text();
				String link = temp.attr("abs:href");
				if (hp.containsKey(title) == false) {
					hp.put(title, new HashMap<String, String>());
					HashMap<String, String> tempMap = hp.get(title);
					tempMap.put("title", title);
					tempMap.put("link", link);
				}
			}
		} catch (NullPointerException e) {
			print("%s", "Exception caught");
		}
	}

	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}

	private static String trim(String s, int width) {
		if (s.length() > width)
			return s.substring(0, width - 1) + ".";
		else
			return s;
	}
}
