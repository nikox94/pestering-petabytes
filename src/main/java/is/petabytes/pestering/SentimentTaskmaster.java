package is.petabytes.pestering;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.protobuf.ByteString.Output;

import is.petabytes.pestering.dataretrieval.DataRetriever;
import is.petabytes.pestering.documentalnalysis.DocumentAnalyzer;
import is.petabytes.pestering.model.EntitySentiment;
import is.petabytes.pestering.model.GoogleCloudConnector;
import is.petabytes.pestering.pojo.Document;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SentimentTaskmaster {

	private static final String FILE = "/home/nikola/git/pestering-petabytes/data/2016 4/CC-MAIN-20160428161506-00000-ip-10-239-7-51.ec2.internal.warc.wet";
	private static final String PROGRESS_COUNTER = "/tmp/petabytes-progress.txt";

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

		final List<Document> docs = dataRetriever.retrieveDataFile(Paths.get(FILE));

		log.info("Starting mappers");

		final List<EntitySentiment> sentiments = new ArrayList<>();

		final int documentCount = docs.size();
		long progressCount = 0;
		for (final Document doc : docs) {

			final List<EntitySentiment> res = documentAnalyzer.analyze(doc.getBody(), companyNames);
			res.forEach(r -> r.setDate(doc.getDate()));
			log.info(res.toString());

			for (final EntitySentiment es : res) {
				googleCloudConnector.updateSentiment(es);
				log.info("UPLOADED : " + es);
			}

			log.info("PROGRESS " + (++progressCount) + "/" + documentCount + " PERCENT: "
					+ progressCount / (float) documentCount);

			try {
				Files.write(Paths.get(PROGRESS_COUNTER), Arrays.asList(String.valueOf(progressCount)),
						Charset.forName("UTF-8"));
			} catch (final IOException e) {
				e.printStackTrace();
			}
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
