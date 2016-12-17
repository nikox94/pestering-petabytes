package is.petabytes.pestering;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import is.petabytes.pestering.dataretrieval.DataRetriever;
import is.petabytes.pestering.documentalnalysis.DocumentAnalyzer;
import is.petabytes.pestering.model.EntitySentiment;
import is.petabytes.pestering.model.GoogleCloudConnector;
import is.petabytes.pestering.pojo.Document;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SentimentTaskmaster {

	private final GoogleCloudConnector googleCloudConnector;

	private final DataRetriever dataRetriever;

	private final DocumentAnalyzer documentAnalyzer;

	@Autowired
	public SentimentTaskmaster(final GoogleCloudConnector googleCloudConnector, final DataRetriever dataRetriever,
			final DocumentAnalyzer documentAnalyzer) {
		this.googleCloudConnector = googleCloudConnector;
		this.dataRetriever = dataRetriever;
		this.documentAnalyzer = documentAnalyzer;
	}

	public void startApplication() {
		log.info("Reading configuration");

		final List<String> urls = new ArrayList<>();

		try (final InputStream file = new ClassPathResource("url-list.txt").getInputStream()) {

			final Scanner fileInput = new Scanner(file);

			// TODO: Rework with Java nio and Streams of lines
			while (fileInput.hasNextLine()) {
				urls.add(fileInput.nextLine());
			}

			fileInput.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final List<String> companyNames = new ArrayList<>();

		try (final InputStream file = new ClassPathResource("companies.txt").getInputStream()) {

			final Scanner fileInput = new Scanner(file);

			// TODO: Rework with Java nio and Streams of lines
			while (fileInput.hasNextLine()) {
				companyNames.add(fileInput.nextLine());
			}

			fileInput.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		log.info("Done reading configuration");

		log.info("Starting downloaders");

		final List<Document> docs = dataRetriever.retrieve(urls);

		log.info("Starting mappers");

		final List<EntitySentiment> sentiments = new ArrayList<>();
		for (final Document doc : docs) {
			final List<EntitySentiment> res = DocumentAnalyzer.analyze(doc.getTitle(), "", companyNames);
			res.forEach(r -> r.setDate(doc.getDate()));
			log.info(res.toString());
			sentiments.addAll(res);
		}

		log.info("Starting reduce and store workers");

		for (final EntitySentiment es : sentiments) {
			googleCloudConnector.updateSentiment(es);
		}
	}

	// initiateDownloader...
	public void download() {

	}

	public void map() {

	}

	public void reduce() {

	}
}
