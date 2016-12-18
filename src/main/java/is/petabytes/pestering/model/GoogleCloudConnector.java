package is.petabytes.pestering.model;

import java.text.SimpleDateFormat;

import org.springframework.stereotype.Service;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DateTime;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleCloudConnector {
	private static final String COMPANY_SENTIMENT_TIMEPOINT_KIND = "CompanySentimentTimePointTest";
	private static final String COMPANY_NAME = "CompanyName";
	private static final String DATE = "Date";
	private static final String SENTIMENT = "Sentiment";
	private static final String SENTIMENT_SUM = "SentimentSum";
	private static final String SENTIMENT_COUNT = "SentimentCount";

	// DATE FORMATTER
	final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	final Datastore datastore;

	public GoogleCloudConnector() {
		datastore = DatastoreOptions.getDefaultInstance().getService();
	}

	public synchronized EntitySentiment retrieveAndUpdate() {
		return null;
	}

	public EntitySentiment updateSentiment(final EntitySentiment entitySentiment) {
		final Key taskKey = datastore.newKeyFactory().setKind(COMPANY_SENTIMENT_TIMEPOINT_KIND)
				.newKey(entitySentiment.getCompanyName().hashCode() + entitySentiment.getDate().getTime());

		// Prepares the new entity
		final Entity task = Entity.newBuilder(taskKey)
				.set(COMPANY_NAME, entitySentiment.getCompanyName())
				.set(DATE, DateTime.copyFrom(entitySentiment.getDate()))
				.set(SENTIMENT, entitySentiment.getSentiment())
				.set(SENTIMENT_SUM, entitySentiment.getSentimentSum())
				.set(SENTIMENT_COUNT, entitySentiment.getSentimentCount())
				.build();

		// Saves the entity
		datastore.put(task);

		System.out.printf("Saved %s: %s%n", task.getKey().getName(), task.getString(COMPANY_NAME));

		return entitySentiment;
	}

}