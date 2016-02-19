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
	public static void main(String[] args) throws IOException{
		String url = "https://en.wikibooks.org/wiki/Java_Programming";
		LinkedHashMap<String, HashMap<String,String>> topicsMap = new LinkedHashMap<String,HashMap<String,String>>();
		getLinks(url, topicsMap);
/*
		for(Map.Entry<String, HashMap<String,String>> entry: topicsMap.entrySet()){
			String title = entry.getKey();
			HashMap<String, String> tempMap = entry.getValue();
			print("%s %s", title, tempMap.toString());
			if (tempMap.containsKey(title) == false)
				getLinks(tempMap.get("link"), topicsMap);
		}
*/
		print("%d %s", topicsMap.size() , topicsMap.toString());
    }
	private static void getLinks(String url, LinkedHashMap<String, HashMap<String,String>> hp) throws IOException{
		try{
			print("Fetching %s...", url);
			Document doc = Jsoup.connect(url).get();
	        Elements topicList = doc.select("#mw-content-text > ul > li");
	        for(Element topic: topicList){
	        	Element temp = topic.select("a[href]").last();
	        	String title = temp.text();
	        	String link = temp.attr("abs:href");
	        	if (hp.containsKey(title) == false){
		        	hp.put(title, new HashMap<String, String>());
		        	HashMap<String, String> tempMap = hp.get(title);
		        	tempMap.put("title", title);
		        	tempMap.put("link",link);
	        	}
		    }
		}
		catch(NullPointerException e){
			print("%s", "Exception caught" );
		}
	}
    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
	}
