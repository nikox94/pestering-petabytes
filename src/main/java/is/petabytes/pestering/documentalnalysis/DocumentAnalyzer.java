package is.petabytes.pestering.documentalnalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Entity;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import is.petabytes.pestering.model.EntitySentiment;
import is.petabytes.pestering.util.TimeLimitedCodeBlock;

@Service
public class DocumentAnalyzer {

	private final static double TITLE_ENTITY_SENTIMENT_WEIGHT = .5;
	private final static double CONTENT_ENTITY_SENTIMENT_WEIGHT = .5;

	private StanfordCoreNLP pipeline;

	public DocumentAnalyzer() {
		initNLP();
	}

	private void initNLP() {
		final Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment");
		props.setProperty("parse.maxtime", "4");
		props.setProperty("dcoref.maxtime", "4");
		pipeline = new StanfordCoreNLP(props);
	}

	private Set<String> listToSetIgnoreCase(final List<String> list) {
		final Set<String> set = new HashSet<>();
		for (final String s : list) {
			set.add(s.toLowerCase());
		}

		return set;
	}

	private HashMap<String, List<Double>> analyzeRaw(final String text, final List<String> _observedEntities) {
		final Annotation document = new Annotation(text);

		pipeline.annotate(document);

		// company -> list of sentiments in the sentences
		final HashMap<String, List<Double>> map = new HashMap<>();

		for (final CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {

			final Tree tree = sentence.get(SentimentAnnotatedTree.class);
			final String sentenceText = sentence.toString();

			// 0:very negative, 1:negative 2:neutral 3:positive and 4:very
			// positive
			final int sentimentClass = RNNCoreAnnotations.getPredictedClass(tree);
			final double sentimentScore = sentimentClass / 2.0 - 1;

			for (final String entity : _observedEntities) {
				if (sentenceText.contains(entity)) {
					if (!map.containsKey(entity)) {
						map.put(entity, new ArrayList<Double>());
					}

					map.get(entity).add(sentimentScore);
					// System.out.println(entity + ":" + sentimentScore);
				}
			}
		}

		return map;
	}

	private HashMap<String, Double> flattenListByAverage(final HashMap<String, List<Double>> map) {
		final HashMap<String, Double> flattened = new HashMap<String, Double>();

		for (final Entry<String, List<Double>> entry : map.entrySet()) {
			final List<Double> list = entry.getValue();

			Double average = 0.0;
			for (final Double x : list)
				average += x;
			average /= list.size();

			flattened.put(entry.getKey(), average);
		}

		return flattened;
	}

	private HashMap<String, Double> combineWeighted(final HashMap<String, Double> mapA,
			final HashMap<String, Double> mapB, final double coeffA, final double coeffB) {

		final HashMap<String, Double> combined = new HashMap<>();

		// add all element from A (including these in both A and B)
		for (final Entry<String, Double> entry : mapA.entrySet()) {
			final String key = entry.getKey();
			final Double value = entry.getValue();
			if (mapB.containsKey(key)) {
				combined.put(key, value * coeffA + mapB.get(key) * coeffB);
			} else {
				combined.put(key, value);
			}
		}

		// add element in B, but no in A
		for (final Entry<String, Double> entry : mapB.entrySet()) {
			final String key = entry.getKey();
			final Double value = entry.getValue();
			if (!mapA.containsKey(key)) {
				combined.put(key, value);
			}
		}

		return combined;
	}

	private List<EntitySentiment> mapToEntitySentimentList(final HashMap<String, Double> companySentimentMap) {
		final List<EntitySentiment> entitySentimentList = new ArrayList<>();

		for (final Entry<String, Double> entry : companySentimentMap.entrySet()) {
			entitySentimentList.add(new EntitySentiment(entry.getKey(), entry.getValue()));
		}

		return entitySentimentList;
	}

	/**
	 * Analyzes a single piece of text.
	 *
	 * @param text
	 * @param _observedEntities
	 * @return List of sentiments for the observed entities mention in the text.
	 */
	public List<EntitySentiment> analyze(final String text, final List<String> _observedEntities) {
		System.out.println("Analyzing: " + text);

		try {
			return TimeLimitedCodeBlock.runWithTimeout(new Callable<List<EntitySentiment>>() {

				@Override
				public List<EntitySentiment> call() throws Exception {
					return mapToEntitySentimentList(flattenListByAverage(analyzeRaw(text, _observedEntities)));
				}
			}, 5, TimeUnit.SECONDS);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		System.out.println("Timeout!!!");
		return new ArrayList<EntitySentiment>();
	}

	/**
	 * Analyzes title and content of a document.
	 *
	 * @param title
	 * @param content
	 * @param _observedEntities
	 * @return List of sentiments for the observed entities mention in the text.
	 */
	public List<EntitySentiment> analyze(final String title, final String content,
			final List<String> _observedEntities) {

		final HashMap<String, Double> titleResult = flattenListByAverage(analyzeRaw(title, _observedEntities));
		final HashMap<String, Double> contentResult = flattenListByAverage(analyzeRaw(content, _observedEntities));

		final HashMap<String, Double> companySentimentMap = combineWeighted(titleResult, contentResult,
				TITLE_ENTITY_SENTIMENT_WEIGHT, CONTENT_ENTITY_SENTIMENT_WEIGHT);

		final List<EntitySentiment> entitySentimentList = mapToEntitySentimentList(companySentimentMap);
		return entitySentimentList;
	}

	private Map<String, EntitySentiment> getMapOfObservedEnities(final List<Entity> entitiesList,
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
		final DocumentAnalyzer documentAnalyzer = new DocumentAnalyzer();
		documentAnalyzer.initNLP();

		// String url1 =
		// "http://www.economist.com/news/business/21711948-founder-inditex-has-become-worlds-second-richest-man-management-style-amancio";
		final String url = "http://www.independent.co.uk/life-style/fashion/how-zara-gets-a-coat-into-stores-in-just-25-days-a7460876.html";

		final String observed[] = new String[] { "Zara", "Apple", "Google", "Facebook", "H&M" };
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

		final String c2 = "There are no obvious candidates for the top job. Those closest to him are nearly as old as he is but much less revered. Swapo has to chose whether to go for a stop gap, like the current Lands Minister Hifikepunye Pohamba, who is the first minister to announce his intention of running for president, or find a younger, untested \"broom\". But the opposition are still not convinced that Mr Nujoma will retire. Ovamboland At his large office, overlooking fountains and flower beds at Windhoek's parliament, the leader of the opposition Congress of Democrats, Ben Ulenga, is planning optimistically for all eventualities. Mr Ulenga claims if Mr Nujoma stays in power, Namibians will be angry and turn against Swapo. And If he keeps his word, then Swapo will not be able to find anyone of similar stature - so either way the opposition will make inroads into Swapo's large parliamentary majority. But for Mr Ulenga to make it to the top, he will have to make a major impact in Ovamboland in the north which is Swapo's heartland and is by far the most densely populated region of Namibia. Land redistribution must be on the agreed willing buyer willing seller basis President Nujoma Although he is an Ovambo tribesman like President Nujoma, he did very poorly in Ovamboland at the last elections, but he now hopes to do much better with the president out of the way. Mugabe's admirer Swapo has been careful to secure the Ovambo vote by ploughing a large slice of development funding into the region, and the people there get more than their fair share of government positions. For the moment, Mr Nujoma's biggest headache is land reform. Huge tracks of land are still owned by a few white farmers and black Namibians are impatient at the slow pace of reform. White farmers say they are falling over backwards to please the government, but Mr Pahamba says that they are only handing over poor quality land. Meanwhile, the militant black farmer's union is threatening farm occupations similar to those in Zimbabwe. Guard dogs The union leaders also claim that, just like Zimbabwe, many white farms are ending up in the hands of government officials. President Nujoma - an admirer of Robert Mugabe - also points accusing fingers at whites, but he is adamant that Namibia is not Zimbabwe and that no-one will be allowed to break the law. Namibia remains amazingly unchanged after 14 years of independence. Land redistribution, he says, must be on the agreed willing seller, willing buyer basis. Namibia has remained amazingly unchanged after 14 years of independence. Windhoek is still a very white city. Shopping malls proliferate, supermarkets are well stocked, streets are immaculately clean. Everyone seems remarkably law abiding, and it is not just the security fences and guard dogs which are keeping people in order. Somehow, nobody wants to rock the boat, and if they do, they will have President Nujoma to contend with. E-mail this to a friend Printable version SEE ALSO: Deal ends Namibian land invasions 07 Nov 03 | Africa Surprise reshuffle in Namibia 27 Aug 02 | Africa Nujoma 'will not seek fourth term' 26 Nov 01 | Africa Nujoma's war on waste 09 Feb 01 | Africa Country profile: Namibia 07 Nov 03 | Country profiles Timeline: Namibia 07 Nov 03 | Country profiles RELATED INTERNET LINKS: Namibian Government The BBC is not responsible for the content of external internet sites TOP AFRICA STORIES Nigeria state oil firm 'insolvent' France to help Africa veterans Churches call for Sudan to split PRODUCTS AND SERVICES E-mail news Mobiles Alerts News feeds	Podcasts News Front Page | Africa | Americas | Asia-Pacific | Europe | Middle East | South Asia UK | Business | Entertainment | Science/Nature | Technology | Health Have Your Say | In Pictures | Week at a Glance | Country Profiles | In Depth | Programmes BBC Copyright NoticeMMIX Most Popular Now | 37,114 pages were read in the last minute.\"; Back to top ^^ Help Privacy and cookies policy News sources About the BBC Contact us";
		final String c3 = "The union leaders also claim that, just like Zimbabwe, many white farms are ending up in the hands of government officials. President Nujoma - an admirer of Robert Mugabe - also points accusing fingers at whites, but he is adamant that Namibia is not Zimbabwe and that no-one will be allowed to break the law. Namibia remains amazingly unchanged after 14 years of independence. Land redistribution, he says, must be on the agreed willing seller, willing buyer basis. Namibia has remained amazingly unchanged after 14 years of independence. Windhoek is still a very white city. Shopping malls proliferate, supermarkets are well stocked, streets are immaculately clean. Everyone seems remarkably law abiding, and it is not just the security fences and guard dogs which are keeping people in order. Somehow, nobody wants to rock the boat, and if they do, they will have President Nujoma to contend with.";
		// for(int i = 0; i < 5; i++) {

		final long start = System.currentTimeMillis();

		final List<EntitySentiment> res = documentAnalyzer.analyze(c3, observedList);
		for (final EntitySentiment es : res) {
			System.out.println(es.getEntity() + "=" + es.getSentiment());
		}

		final long end = System.currentTimeMillis();
		System.out.println(end - start);

	}

}
