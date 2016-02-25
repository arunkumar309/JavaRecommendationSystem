package wikibooksCrawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class oracleCrawler {
	
	static LinkedHashMap<String, String> startLinksMap = new LinkedHashMap<String,String>();
	static LinkedHashMap<String, String> innerLinksMap = new LinkedHashMap<String,String>();
	static String startURL = "https://docs.oracle.com/javase/tutorial/";
	public static int i = 1;


	public oracleCrawler() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		crawlURL(startURL, startLinksMap);
		for(Map.Entry<String, String> tempMap: startLinksMap.entrySet())
			crawlInnerURLS(tempMap.getValue(), innerLinksMap);
		print("%d", innerLinksMap.size());
	}
	private static void crawlURL(String url, LinkedHashMap<String, String> hp) throws IOException {
		try {
			boolean flag = false;
			print("Fetching %s...", url);
			Document doc = Jsoup.connect(url).get();
			Elements topicList = doc.select("#TutBody > ul > li");
			for (Element topic : topicList) {
				Element temp = topic.select("a[href]").last();
				String title = temp.text();
				String link = temp.attr("abs:href");
				hp.put(title, link);
			}
		} catch (NullPointerException e) {
			print("%s", "Exception caught");
		}
	}
	
	private static void writeToFile(String title, String con) throws IOException{
		File dir = new File("oracle");
		title = dir.getAbsolutePath()+"/"+title;
		print("%s", title);
		FileWriter fw = new FileWriter(new File(title));
		fw.write(con);
		fw.flush();
		fw.close();
		
	}
	
	private static void crawlInnerURLS(String url, LinkedHashMap<String, String> hp) throws IOException{
		Document doc = Jsoup.connect(url).get();
		Elements content = doc.select("#PageContent");
		Elements urls = content.select("a[href]");
		String title = null;
		for(Element urlTemp: urls){
			String toAddURL = urlTemp.attr("abs:href");
			title = urlTemp.text();
			hp.put(title, toAddURL);
		}
		for(Element con: content){
			Element last = con.lastElementSibling();
			while(con.nextElementSibling() != null && con.hasText()){
				String text = con.text();
				print("%s", text);
				writeToFile(Integer.toString(i), text);
				i++;
				con = con.nextElementSibling();
			}
		}
	}
	private static void print(String msg, Object... args) {
		System.out.println(String.format(msg, args));
	}


}
