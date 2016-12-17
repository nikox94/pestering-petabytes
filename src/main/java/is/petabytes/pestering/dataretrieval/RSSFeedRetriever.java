package is.petabytes.pestering.dataretrieval;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

public class RSSFeedRetriever {

	
	public void read(String url) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
		System.out.println(feed.getTitle());
		List<SyndEntry> entries = feed.getEntries();
		
		for (SyndEntry entry : entries) {
			System.out.println(entry.getTitle());
			System.out.println(entry.getAuthor());
			System.out.println(entry.getLink());
			System.out.println(entry.getPublishedDate());
			System.out.println();
		}
	}
	
	public static void main(String[] args) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		String url = "http://feeds.marketwatch.com/marketwatch/StockstoWatch/";
		new RSSFeedRetriever().read(url);
	}
}
