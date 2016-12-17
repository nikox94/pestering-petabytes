package is.petabytes.pestering.dataretrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rometools.rome.io.FeedException;

import is.petabytes.pestering.dataretrieval.RSSFeedRetriever;
import is.petabytes.pestering.pojo.Document;

@Service
public class DataRetriever {

	public List<Document> retrieve(final List<String> urls) {

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
}
