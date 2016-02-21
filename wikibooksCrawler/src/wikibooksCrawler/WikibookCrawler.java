/**
 * 
 */
package wikibooksCrawler;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
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
		LinkedHashMap<String, String> topicsMap = new LinkedHashMap<String, String>();
		HashMap<String, LinkedHashMap<String, String>> pages = new HashMap<String, LinkedHashMap<String, String>>();
		String pageClassifier;
		getLinks(url, topicsMap);

		for(Map.Entry<String, String> entry:topicsMap.entrySet()){ 
			String link = entry.getValue();
			pageClassifier = getPageStart(link, pages);
			if (!pageClassifier.equals("noscript"))
				getText(link, pages, pageClassifier);
		}
		print("%d", pages.size());
		writeToFile(pages);

	 
/*		pageClassifier = getPageStart(url+"/Statements", pages);
		if (!pageClassifier.equals("noscript"))
			getText(url+"/Statements", pages, pageClassifier);
		print("%d %s", pages.size() , pages.toString());
*/	
	}
	
	private static void writeToFile(HashMap<String, LinkedHashMap<String, String>> hp){
		for (Map.Entry<String, LinkedHashMap<String,String>> page: hp.entrySet()){
			boolean dirCreated;
			String pageHeading = page.getKey();
			File dir = new File("/home/arun/Documents/Adaptive Web/assignment2/crawledPages/"+pageHeading);
			dirCreated = dir.mkdir();
			if (dirCreated){
				String filePath = dir.getAbsolutePath();
				for(Map.Entry<String, String> topic: page.getValue().entrySet()){
					String topicName = topic.getKey();
					File topicFile = new File(filePath+"/"+topicName);
					try {
						FileWriter fw = new FileWriter(topicFile);
						fw.write(topic.getValue());
						fw.flush();
						fw.close();
						print("File Written: %s", topicFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private static String getPageStart(String url,HashMap<String, LinkedHashMap<String, String>> hp) throws IOException {
		int index = url.lastIndexOf("/");
		String title = url.substring(index+1);
		boolean flag = true;
		boolean divFlag = false;
		print("Fetching pageStart: %s", title);
		Document doc = Jsoup.connect(url).get();
		String pageClassifier = null;
		Elements contents = doc.select("#mw-content-text > table");
		if (contents.size() == 0){
			divFlag = true;
			contents = doc.select("#mw-content-text > div > table");
		}
		LinkedHashMap<String, String> tempMap = new LinkedHashMap<String, String>();
		for(Element content : contents){
			StringBuilder sb = new StringBuilder();
			if (content.hasClass("metadata")){
				pageClassifier = "noscript";
				content = content.nextElementSibling();
				while(!content.tagName().equals("noscript")){
					sb.append(content.text()+"\n");
					content = content.nextElementSibling();
				}
				tempMap.put(title+"_introduction", sb.toString());
			}
			else if (content.hasClass("wikitable") && content.hasAttr("style") && flag){
				flag = false;
				Element temp = content.nextElementSibling();
				content = content.nextElementSibling();
				String text;
				if(divFlag){
					Element lastElement = temp.lastElementSibling();
					while(temp.hasText()){
						sb.append(temp.text()+"\n");
						if(temp != lastElement)
							temp = temp.nextElementSibling();
						else
							break;
					}
					pageClassifier = "h2";
					tempMap.put(title+"_introduction", sb.toString());
				}
				else{
					while(!temp.nextElementSibling().tagName().equals("h2") && !temp.nextElementSibling().tagName().equals("h3") && !temp.nextElementSibling().tagName().equals("noscript")){
						temp = temp.nextElementSibling();
					}
					pageClassifier  = temp.nextElementSibling().tagName();
					switch(pageClassifier){
						case "h2":{
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
							tempMap.put(title+"_introduction", sb.toString());
							break;
						}
						case "h3":{
							sb.append(content.text()+"\n");
							tempMap.put(title+"_introduction", sb.toString());
							break;
	
						}
						case "noscript":{
							while(!content.nextElementSibling().nextElementSibling().tagName().equals("noscript")){
								text = content.text();
								if(text.startsWith("Code section") || text.startsWith("Code listing")){
									int indexTemp = text.lastIndexOf(": ");
									text = text.substring(indexTemp+2);
								}
								sb.append(text+"\n");
								content = content.nextElementSibling();
								tempMap.put(title+"_introduction", sb.toString());
								break;
							}
						}
					}
				}
			}
		}
		hp.put(title, tempMap);
		return pageClassifier;
	}

	private static void getText(String url, HashMap<String, LinkedHashMap<String, String>> hp, String pageClassifier) throws IOException {
		int index = url.lastIndexOf("/");
		String title = url.substring(index+1);
		print("Fetching Text: %s", title);
		Document doc = Jsoup.connect(url).get();
		Elements contents = doc.select("#mw-content-text > p");
		Elements subHeadings = doc.select("#mw-content-text >" + pageClassifier);
		Elements codes = doc.getElementsByClass("mw-highlight");
		LinkedHashMap<String, String> tempMap = hp.get(title);
		for(Element subHeading: subHeadings){
			int len = subHeading.text().length();
			int pos;
			String subTopic = subHeading.text();
			pos = subHeading.text().indexOf("[edit]");
			if (pos != -1)
				subTopic = subHeading.text().substring(0, pos);
			else if(pos == -1){
				pos = subHeading.text().indexOf("Edit");
				if (pos != -1)
					subTopic = subHeading.text().substring(0, pos);
			}
			String text;
			StringBuilder sb = new StringBuilder();
			while(!subHeading.nextElementSibling().tagName().equals(pageClassifier) && subHeading.nextElementSibling().hasText()){
				text = subHeading.nextElementSibling().text();
				if(text.startsWith("Code section") || text.startsWith("Code listing")){
					int indexTemp = text.lastIndexOf(": ");
					text = text.substring(indexTemp+2);
				}
				sb.append(text+"\n");
				subHeading = subHeading.nextElementSibling();
			}
			if(subTopic.contains("/"))
				subTopic = subTopic.replaceAll("/", "_");
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

	private static void getLinks(String url, LinkedHashMap<String, String> hp) throws IOException {
		try {
			boolean flag = false;
			print("Fetching %s...", url);
			Document doc = Jsoup.connect(url).get();
			Elements topicList = doc.select("#mw-content-text > ul > li");
			for (Element topic : topicList) {
				Element temp = topic.select("a[href]").last();
				String title = temp.text();
				String link = temp.attr("abs:href");
				if (title.equals("Statements"))
					flag = true;
				if (title.equals("Links"))
					flag = false;
				if (hp.containsKey(title) == false && flag) {
					hp.put(title, link);
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
