/**
 * 
 */
package wikibooksCrawler;

import org.jsoup.Jsoup;
import org.tartarus.snowball.ext.PorterStemmer;


import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import wikibooksCrawler.stopWords.*;

/**
 * @author arun
 *
 */
public class WikibookCrawler {

	/**
	 * @param args
	 */
	static stopWords stopObj = new stopWords();
	public static String[] stopwords = stopWords.stopwords;
	public static String[] javaKeyWords = stopWords.javaKeyWords;
	public static HashSet<String> stopSet = new HashSet<String>(Arrays.asList(stopwords));
	public static HashSet<String> keySet = new HashSet<String>(Arrays.asList(javaKeyWords));
	public static HashSet<String> unStemmedSet = new HashSet<String>();
	public static HashSet<String> stemmedSet = new HashSet<String>();

	public static void main(String[] args) throws IOException {
		String url = "https://en.wikibooks.org/wiki/Java_Programming";
		LinkedHashMap<String, String> topicsMap = new LinkedHashMap<String, String>();
		HashMap<String, LinkedHashMap<String, String>> pages = new HashMap<String, LinkedHashMap<String, String>>();
		HashMap<String, LinkedHashMap<String, String>> pagesCopy = new HashMap<String, LinkedHashMap<String, String>>();
		String pageClassifier;
		crawlURL(url, topicsMap);

		for(Map.Entry<String, String> entry:topicsMap.entrySet()){ 
			String link = entry.getValue();
			pageClassifier = getPageStart(link, pages);
			if (!pageClassifier.equals("noscript"))
				getText(link, pages, pageClassifier);
		}
		print("%d", pages.size());
		writeToFile(pages, false);
		stem(pages, pagesCopy);
		writeToFile(pagesCopy, true);

		
	/*	String[] tempPages = {
				"Reflection/Dynamic_Invocation",
				"Networking"};
		for(String page: tempPages){
			pageClassifier = getPageStart(url+"/"+page, pages);
			if (!pageClassifier.equals("noscript"))
				getText(url+"/"+page, pages, pageClassifier);
			}
		writeToFile(pages, false);
		stem(pages,pagesCopy);
		writeToFile(pagesCopy, true);
		print("%d", pages.size());
	*/

/*		pageClassifier = getPageStart(url+"/Statements", pages);
		if (!pageClassifier.equals("noscript"))
			getText(url+"/Statements", pages, pageClassifier);
		stem(pages,pagesCopy,topicsCopy);
		writeToFile(pagesCopy);
		print("%d %s", pages.size() , pagesCopy.toString());
*/	
	}
	
	private static boolean notStopWord(String word){
		return !(stopSet.contains(word) && !keySet.contains(word));
	}
	
	private static void stem(HashMap<String, LinkedHashMap<String, String>> hp, HashMap<String, LinkedHashMap<String, String>> pagesCopy){
		PorterStemmer stemmer = new PorterStemmer();
		for (Map.Entry<String, LinkedHashMap<String,String>> page: hp.entrySet()){
			LinkedHashMap<String, String> topicsCopy = new LinkedHashMap<String, String>();
			String pageHeading = page.getKey();
			print("Stemming page: %s", pageHeading);
			LinkedHashMap<String, String> innerMap = page.getValue();
			for(Map.Entry<String, String> innerContents: innerMap.entrySet()){
				StringBuilder sb = new StringBuilder();
				String topicHeading = innerContents.getKey();
				String[] words = innerContents.getValue().split("\\s");
				for(String word:words){
					if(notStopWord(word)){
						stemmer.setCurrent(word);
						if(stemmer.stem()){
							String temp = stemmer.getCurrent();
							sb.append(temp+" ");
						}
					}
				}
				topicsCopy.put(topicHeading, sb.toString());
			}
			pagesCopy.put(pageHeading, topicsCopy);
		}
	}
	
	private static void writeToFile(HashMap<String, LinkedHashMap<String, String>> hp, boolean stemmed){
		String loc;
		if (stemmed)
			loc = "./StemmedCrawledPages/";
		else
			loc = "./CrawledPages/";
		for (Map.Entry<String, LinkedHashMap<String,String>> page: hp.entrySet()){
			boolean dirCreated;
			String pageHeading = page.getKey();
			File dir = new File(loc+pageHeading);
			dirCreated = dir.mkdirs();
			if (dirCreated){
				String filePath = dir.getAbsolutePath();
				for(Map.Entry<String, String> topic: page.getValue().entrySet()){
					String topicName = topic.getKey();
					File topicFile = new File(filePath+"/"+topicName);
					String toWrite = topic.getValue();
					try {
						FileWriter fw = new FileWriter(topicFile);
						if((stemmed) && (stemmedSet.add(toWrite.toLowerCase()))){
							fw.write(toWrite.toLowerCase());
						}
						else if((!stemmed) && (unStemmedSet.add(toWrite))){
							fw.write(toWrite);
						}
						else
							break;
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
		int index = url.lastIndexOf("Java_Programming/");
		String title = url.substring(index+17);
		if(title.contains("/"))
			title = title.replaceAll("/", "_");
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
			int i = 1;
			StringBuilder sb = new StringBuilder();
			if (content.hasClass("metadata")){
				pageClassifier = "noscript";
				content = content.nextElementSibling();
				while(!content.tagName().equals("noscript")){
					//sb.append(content.text()+"\n");
					tempMap.put(title+"_introduction_"+i, content.text() + "\n");
					content = content.nextElementSibling();
					i++;
				}
				//tempMap.put(title+"_introduction", sb.toString());
			}
			else if (content.hasClass("wikitable") && content.hasAttr("style") && flag){
				flag = false;
				Element temp = content.nextElementSibling();
				content = content.nextElementSibling();
				String text;
				if(divFlag){
					Element lastElement = temp.lastElementSibling();
					while(temp.hasText()){
						//sb.append(temp.text()+"\n");
						
						tempMap.put(title+"_introduction_"+i, content.text() + "\n");
						i++;
						if(temp != lastElement)
							temp = temp.nextElementSibling();
						else
							break;
					}
					pageClassifier = "h2";
					//tempMap.put(title+"_introduction", sb.toString());
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
								//sb.append(text+"\n");

								tempMap.put(title+"_introduction_"+i, content.text() + "\n");
								i++;
								
								content = content.nextElementSibling();
							}
							//sb.append(content.text()+"\n");
							//tempMap.put(title+"_introduction", sb.toString());

							tempMap.put(title+"_introduction_"+i, content.text() + "\n");

							break;
						}
						case "h3":{
/*							sb.append(content.text()+"\n");
							tempMap.put(title+"_introduction", sb.toString());
*/							
							tempMap.put(title+"_introduction_"+i, content.text() + "\n");
							i++;
							
							break;
	
						}
						case "noscript":{
							while((!content.nextElementSibling().nextElementSibling().tagName().equals("noscript")) || ((content.nextElementSibling().nextElementSibling().tagName().equals("table")) && ((content.nextElementSibling().nextElementSibling().hasClass("noprint") || (content.nextElementSibling().nextElementSibling().hasClass("notice")))))){
								text = content.text();
								if(text.startsWith("Code section") || text.startsWith("Code listing")){
									int indexTemp = text.lastIndexOf(": ");
									text = text.substring(indexTemp+2);
								}
								
								//sb.append(text+"\n");
								content = content.nextElementSibling();
							}

							//tempMap.put(title+"_introduction", sb.toString());
							
							tempMap.put(title+"_introduction_"+i, content.text() + "\n");
							i++;

							break;
						}
					}
				}
			}
		}
		hp.put(title, tempMap);
		return pageClassifier;
	}

	private static void getText(String url, HashMap<String, LinkedHashMap<String, String>> hp, String pageClassifier) throws IOException {
		int i = 1;
		int index = url.lastIndexOf("Java_Programming/");
		String title = url.substring(index+17);
		if(title.contains("/"))
			title = title.replaceAll("/", "_");
		print("Fetching Text: %s", title);
		Document doc = Jsoup.connect(url).get();
		//Elements contents = doc.select("#mw-content-text > p");
		Elements subHeadings = doc.select("#mw-content-text >" + pageClassifier);
		//Elements codes = doc.getElementsByClass("mw-highlight");
		LinkedHashMap<String, String> tempMap = hp.get(title);
		for(Element subHeading: subHeadings){
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
			if(subTopic.contains("/"))
				subTopic = subTopic.replaceAll("/", "_");
			String text = null;
			//StringBuilder sb = new StringBuilder();
			while(!subHeading.nextElementSibling().tagName().equals(pageClassifier) && subHeading.nextElementSibling().hasText() && !subHeading.nextElementSibling().hasClass("collapsible")){
				if(subHeading.hasClass("collapsible"))
					subHeading = subHeading.nextElementSibling();
				text = subHeading.nextElementSibling().text();
				if(text.startsWith("Code section") || text.startsWith("Code listing")){
					int indexTemp = text.lastIndexOf(": ");
					text = text.substring(indexTemp+2);
				}
				
				//tempMap.put(subTopic, sb.toString());
				
				tempMap.put(subTopic + "_" + i, text + "\n");
				i++;

				//sb.append(text+"\n");
				subHeading = subHeading.nextElementSibling();
			}

			//tempMap.put(subTopic, sb.toString());
			
		}
		
/* To extract text and code separately
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

	private static void crawlURL(String url, LinkedHashMap<String, String> hp) throws IOException {
		try {
			boolean flag = false;
			print("Fetching %s...", url);
			Document doc = Jsoup.connect(url).get();
			Elements topicList = doc.select("#mw-content-text > ul > li");
			fetchLinks(topicList, hp, flag);
			flag = true;
			topicList = doc.select("#mw-content-text > ul > li > ul > li");
			fetchLinks(topicList, hp, flag);
			topicList = doc.select("#mw-content-text > dl > dd > ul > li");
			fetchLinks(topicList, hp, flag);
			} catch (NullPointerException e) {
			print("%s", "Exception caught");
		}
	}

	private static void fetchLinks(Elements topicList, LinkedHashMap<String, String> hp, boolean flag){
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
	}
	
	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}
}
