package is.petabytes.pestering.documentalnalysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Entities;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Entity;

import is.petabytes.pestering.model.EntitySentiment;

@Service
public class DocumentAnalyzer {

	private final static double TITLE_ENTITY_SENTIMENT_WEIGHT = .5;
	private final static double CONTENT_ENTITY_SENTIMENT_WEIGHT = .5;

	private static void loadApiKey(final AlchemyLanguage service) throws IOException {
		final InputStream file = new ClassPathResource("api_key").getInputStream();

		final Scanner fileInput = new Scanner(file);
		final String apiKey = fileInput.next();

		System.out.println("Loading API KEY=" + apiKey);
		service.setApiKey(apiKey);

		fileInput.close();
	}

	private static Set<String> listToSetIgnoreCase(final List<String> list) {
		final Set<String> set = new HashSet<>();
		for (final String s : list) {
			set.add(s.toLowerCase());
		}

		return set;
	}

	public List<EntitySentiment> analyze(final String url, final List<String> _observedEntities) {
		final Set<String> observedEntities = listToSetIgnoreCase(_observedEntities);

		final AlchemyLanguage service = new AlchemyLanguage();

		try {
			loadApiKey(service);
		} catch (final IOException e) {
			e.printStackTrace();
			throw new IllegalStateException();
		}

		final Map<String, Object> params = new HashMap<String, Object>();
		params.put(AlchemyLanguage.SENTIMENT, "1");
		params.put(AlchemyLanguage.URL, url);
		final Entities entities = service.getEntities(params).execute();

		final List<Entity> entitiesList = entities.getEntities();

		final List<EntitySentiment> result = new ArrayList<>();

		for (final Entity e : entitiesList) {
			System.out.println(e);

			final String entityText = e.getText().trim().toLowerCase();
			if (observedEntities.contains(entityText)) {
				final Double sentiment = e.getSentiment().getScore();

				final EntitySentiment tuple = new EntitySentiment(entityText, sentiment);
				result.add(tuple);
			}
		}

		return result;
	}

	public static List<EntitySentiment> analyze(final String title, final String content,
			final List<String> _observedEntities) {
		final Set<String> observedEntities = listToSetIgnoreCase(_observedEntities);

		final AlchemyLanguage service = new AlchemyLanguage();

		try {
			loadApiKey(service);
		} catch (final IOException e) {
			e.printStackTrace();
			throw new IllegalStateException();
		}

		final Map<String, Object> params = new HashMap<String, Object>();
		params.put(AlchemyLanguage.SENTIMENT, "1");
		params.put(AlchemyLanguage.TEXT, title);

		final List<Entity> titleEntitiesList = service.getEntities(params).execute().getEntities();
		System.out.println("CONTENT");
		for (final Entity e : titleEntitiesList) {
			System.out.println(e);
		}

		final List<Entity> contentEntitiesList;
		if (content != null && content.length() > 5) {
			params.put(AlchemyLanguage.TEXT, content);
			contentEntitiesList = service.getEntities(params).execute().getEntities();
			System.out.println("CONTENT");
			for (final Entity e : contentEntitiesList) {
				System.out.println(e);
			}
		} else {
			contentEntitiesList = new ArrayList<>();
		}

		final Map<String, EntitySentiment> titleTuplesMap = getMapOfObservedEnities(titleEntitiesList,
				observedEntities);
		final Map<String, EntitySentiment> contentTuplesMap = getMapOfObservedEnities(contentEntitiesList,
				observedEntities);

		final List<EntitySentiment> result = new ArrayList<>();

		// add all entities from the title
		for (final String entity : titleTuplesMap.keySet()) {
			if (contentTuplesMap.containsKey(entity)) {
				final double titleSentiment = titleTuplesMap.get(entity).getSentiment();
				final double contentSentiment = contentTuplesMap.get(entity).getSentiment();

				final double aggregateSentiment = titleSentiment * TITLE_ENTITY_SENTIMENT_WEIGHT
						+ contentSentiment * CONTENT_ENTITY_SENTIMENT_WEIGHT;

				result.add(new EntitySentiment(entity, aggregateSentiment));
			} else {
				result.add(titleTuplesMap.get(entity));
			}
		}

		// add all entities from the content which are not in the title
		for (final String entity : contentTuplesMap.keySet()) {
			if (!titleTuplesMap.containsKey(entity)) {
				result.add(contentTuplesMap.get(entity));
			}
		}

		return result;
	}

	private static Map<String, EntitySentiment> getMapOfObservedEnities(final List<Entity> entitiesList,
			final Set<String> observedEntities) {
		final Map<String, EntitySentiment> entitySentiments = new HashMap<>();

		for (final Entity e : entitiesList) {
			final String entityText = e.getText().trim().toLowerCase();

			if (observedEntities.contains(entityText)) {
				final EntitySentiment tuple = new EntitySentiment(entityText, e.getSentiment());

				entitySentiments.put(entityText, tuple);
			}
		}

		return entitySentiments;
	}

	public static void strain() {
		// String url1 =
		// "http://www.economist.com/news/business/21711948-founder-inditex-has-become-worlds-second-richest-man-management-style-amancio";
		final String url = "http://www.independent.co.uk/life-style/fashion/how-zara-gets-a-coat-into-stores-in-just-25-days-a7460876.html";

		final String observed[] = new String[] { "Zara", "Apple", "Google", "Facebook" };
		final List<String> observedList = Arrays.asList(observed);

		// new DocumentAnalyzer().analyze(url, observedList);

		final String title = "How Zara gets a coat into stores in just 25 days";
		final String content = "In a world where the internet can deliver almost instant gratification,"
				+ " it is easy to forget how much hard graft goes into getting a garment from a designer’s"
				+ " brain onto shelves. Now Zara have given a glimpse into the reality of fast fashion by"
				+ " revealing how they can get a coat out in the shops in just 25 days. The Spanish retailer"
				+ " recently unveiled a black wrap coat with a high collar, complete with a metal fastening"
				+ " based on feedback that customers wanted “hardware this season”, the Wall Street Journal"
				+ " reported. The process started with Zara store managers feeding the thoughts of customers"
				+ " back to the firm's designers in Spain, who took five days to put together a prototype for"
				+ " the coat.  Over the next fortnight, a pattern maker, cutters and seamstresses produced over"
				+ " 8,000 coats, which took a further six days to label and tag. They were then sent to Barcelona"
				+ " airport and flown to John F Kennedy in New York, before spending another day on a truck to"
				+ " the store in the Big Apple.  Among Zara's parent company Inditex's secrets to speed are making"
				+ " 60 per cent of its garments in Spain and countries nearby, and having its creative team"
				+ " working at its headquarters. The Spanish retailer’s focus on fast fashion has also seen"
				+ " Inditex enjoy an 8 per cent boost in profits in the first quarter of 2016 - beating H&M"
				+ " – by improving its online shopping experience. ";

		new DocumentAnalyzer();
		final List<EntitySentiment> res = DocumentAnalyzer.analyze(title, content, observedList);
		for (final EntitySentiment es : res) {
			System.out.println(es.getEntity() + "=" + es.getSentiment());
		}

	}

}
