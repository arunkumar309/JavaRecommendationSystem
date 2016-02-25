package wikibooksCrawler;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.tartarus.snowball.ext.PorterStemmer;
import wikibooksCrawler.stopWords;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
/**
 * Lucene Demo: basic similarity based content indexing 
 * @author Sharonpova
 * Current sample files fragments of wikibooks and stackoverflow. 
 */


public class SimpleLuceneIndexing {
	
	static stopWords stopObj = new stopWords();
	public static String stopwords[] = stopObj.stopwords;
	public static HashSet<String> stopSet = new HashSet<String>(Arrays.asList(stopwords));
	
	private static void indexDirectory(IndexWriter writer, File dir) throws IOException {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory()) {
				indexDirectory(writer, f); // recurse
			} else {
				// call indexFile to add the title of the txt file to your index (you can also index html)
				indexFile(writer, f);
			}
		}
	}
	private static void indexFile(IndexWriter writer, File f) throws IOException {
		System.out.println("Indexing " + f.getName());
		Document doc = new Document();
		doc.add(new TextField("filename", f.getPath(), TextField.Store.YES));
		
		
		//open each file to index the content
		try{
			
				FileInputStream is = new FileInputStream(f);
		        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		        StringBuffer stringBuffer = new StringBuffer();
		        String line = null;
		        while((line = reader.readLine())!=null){
		          stringBuffer.append(line).append("\n");
		        }
		        reader.close();
				doc.add(new TextField("contents", stringBuffer.toString(), TextField.Store.YES));
	

		}catch (Exception e) {
            
			System.out.println("something wrong with indexing content of the files");
        }    
		
          
        
		writer.addDocument(doc);
		
	}	
	
	public static String applyPorterStemmer(String input) throws IOException {
		StringBuffer sb = new StringBuffer();
	    PorterStemmer stemmer = new PorterStemmer();
		String[] words= input.split("\\s");
		for(String word: words){
			if(notStopWord(word)){
		        stemmer.setCurrent(word.toLowerCase());
		        stemmer.stem();
		        sb.append(stemmer.getCurrent()+ " ");
			}
		}
		return sb.toString();
    }
	
	private static boolean notStopWord(String word){
		return !(stopSet.contains(word));
	}
	
	private static void writeToFile(StringBuilder sb, String fileName) throws IOException{
		FileWriter fw = new FileWriter(fileName);
		fw.write(sb.toString());
		fw.flush();
		fw.close();
	}
	
	private static void extractContents(int postNo, String querystr, ScoreDoc[] hits, IndexSearcher searcher) throws IOException{
		String outputLoc = "./Recommendations/";
		int i;
		StringBuilder sb = new StringBuilder();
		sb.append(querystr+"\n####---####\n");
		for(i=0;i<hits.length;i++){
			 int docId = hits[i].doc;
			 Document d;
			 d = searcher.doc(docId);
			 String fileName = d.get("filename");
			 fileName = fileName.replace("Stemmed", "");
			 String outFileContents = new String(Files.readAllBytes(Paths.get(fileName)));
			 if(outFileContents.startsWith("Code") || outFileContents.startsWith("Test") || outFileContents.startsWith("COM_DATA") || outFileContents.startsWith("ComServer"))
				 outFileContents = "<pre><code>" + outFileContents + "</code></pre>";
			 sb.append(outFileContents + "##--##\n");
		}
		writeToFile(sb, outputLoc+postNo+"/");

	}
	
	 public static void main(String[] args) throws IOException, ParseException {
		 
		File dataDir = new File("./StemmedCrawledPages/");
		// Check whether the directory to be indexed exists
		if (!dataDir.exists() || !dataDir.isDirectory()) {
			throw new IOException(
					dataDir + " does not exist or is not a directory");
		}
		Directory indexDir = new RAMDirectory();
		
		// Specify the analyzer for tokenizing text.
		StandardAnalyzer analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(indexDir, config);
		
		// call indexDirectory to add to your index
		// the names of the txt files in dataDir
		indexDirectory(writer, dataDir);
		writer.close();
		 
		//Query string!
		int postNo = 1;
		String querystr;
		String inputContents = new String(Files.readAllBytes(Paths.get("./inputText.txt")));
		String[] inputContentArray = inputContents.split("##--##");
		for(String inputContent: inputContentArray){
			querystr = inputContent;
			String queryStrStemmed = applyPorterStemmer(querystr);
			queryStrStemmed = "contents:"+queryStrStemmed;
			/*//This is going to be your selected posts.
			Scanner console = new Scanner(System.in);
			String querystr = "contents:"+console.nextLine();
			System.out.println(querystr);
			*/
			
			Query q = new QueryParser( "contents", analyzer).parse(queryStrStemmed);
			int hitsPerPage = 10;
			IndexReader reader = null;
			 
			
			 
			 TopScoreDocCollector collector = null;
			 IndexSearcher searcher = null;
			 reader = DirectoryReader.open(indexDir);
			 searcher = new IndexSearcher(reader);
			 collector = TopScoreDocCollector.create(hitsPerPage);
			 searcher.search(q, collector);
			 
			 
			 
			 ScoreDoc[] hits = collector.topDocs().scoreDocs;
			 System.out.println("Found " + hits.length + " hits.");
			 System.out.println();
			 
			 for (int i = 0; i < hits.length; ++i) {
				 int docId = hits[i].doc;
				 Document d;
				 d = searcher.doc(docId);
				 System.out.println((i + 1) + ". " + d.get("filename"));
			 }
			 extractContents(postNo, querystr, hits, searcher);
			 postNo++;
			 reader.close();
		}
	 }

}