package is.petabytes.pestering.dataretrieval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.google.common.base.CharMatcher;
import com.rometools.rome.io.FeedException;

import is.petabytes.pestering.dataretrieval.RSSFeedRetriever;
import is.petabytes.pestering.pojo.Document;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DataRetriever {

	public List<Document> retrieveURLs(final List<String> urls) {

		final List<Document> result = new ArrayList<>();
		try {

			for (final String url : urls) {
				result.addAll(RSSFeedRetriever.read(url));
			}

		} catch (IllegalArgumentException | FeedException | IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	// WARC-Date: 2016-05-08T21:06:17Z^M

	public List<Document> retrieveDataFile(final Path path) {
		final List<Document> docs = new ArrayList<>();

		try (final Stream<String> filteredLines = Files.lines(path).onClose(() -> System.out.println("File closed"))
				.filter(s -> CharMatcher.ascii().matchesAllOf(s) && ((s.length() > 50 && s.length() < 1000) || s.contains("WARC-Date:")))) {

			final List<Date> currentDate = new ArrayList<>();
			filteredLines.forEachOrdered(s -> {
				if (s.contains("WARC-Date:")) {
					final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					Date date;
					try {
						date = parser.parse(s.substring(11, 31));
						System.out.println(date);
						currentDate.add(date);
					} catch (final ParseException e) {
						e.printStackTrace();
					}
				} else {

					if (currentDate.isEmpty()) {
						// Do nothing
					} else {
						final Document d = new Document();
						d.setDate(currentDate.get(currentDate.size() - 1));
						d.setBody(s);
						docs.add(d);
					}
				}
			});

		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		return docs;
	}
}
