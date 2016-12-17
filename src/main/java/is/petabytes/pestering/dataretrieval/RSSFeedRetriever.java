package is.petabytes.pestering.dataretrieval;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import is.petabytes.pestering.pojo.Document;

public class RSSFeedRetriever {


	public static List<Document> read(final String url) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		final SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
		System.out.println(feed.getTitle());
		final List<SyndEntry> entries = feed.getEntries();
		final List<Document> parsedDocs = new ArrayList<>();

		for (final SyndEntry entry : entries) {
			System.out.println(entry.getTitle());
			System.out.println(entry.getAuthor());
			System.out.println(entry.getLink());
			System.out.println(entry.getPublishedDate());
			System.out.println();

			// TODO: Implement message bus and send document to message bus here
			final Document doc = new Document();
			doc.setTitle(entry.getTitle());
			doc.setDate(entry.getPublishedDate());
			parsedDocs.add(doc);
		}

		return parsedDocs;
	}

	public static void pain(final String[] args) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		final String url = "http://feeds.marketwatch.com/marketwatch/StockstoWatch/";
		RSSFeedRetriever.read(url);
	}
}
