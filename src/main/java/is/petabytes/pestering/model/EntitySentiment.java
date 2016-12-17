package is.petabytes.pestering.model;

import java.util.Date;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntitySentiment {

	private static final String SENTIMENT_POSITIVE = "positive";
	private static final String SENTIMENT_NEGATIVE = "negative";
	private static final String SENTIMENT_NEUTRAL = "neutral";

	private static double DEFAULT_NEGATIVE_SCORE = -.3;
	private static double DEFAULT_POSITIVE_SCORE = .3;
	private static double DEFAULT_NEUTRAL_SCORE = .1;


	private String companyName;

	private double sentiment;

	private double sentimentSum;

	private int sentimentCount;

	private Date date;

	public EntitySentiment(final String entity, final Sentiment sentiment) {
		super();
		this.companyName = entity;
		if(sentiment.getScore() != null) {
			this.sentiment = sentiment.getScore();
		} else {

			switch(sentiment.getType()) {
			case NEGATIVE: this.sentiment = DEFAULT_NEGATIVE_SCORE; break;
			case POSITIVE: this.sentiment = DEFAULT_POSITIVE_SCORE; break;
			case NEUTRAL: this.sentiment = DEFAULT_NEUTRAL_SCORE; break;
			default: throw new IllegalStateException("no such sentiment type");
			}
		}
		this.date = new Date();
	}

	public EntitySentiment(final String entity, final double sentiment) {
		super();
		this.companyName = entity;
		this.sentiment = sentiment;
		this.date = new Date();
	}

	public EntitySentiment(final String entity, final double sentiment, final Date date) {
		super();
		this.companyName = entity;
		this.sentiment = sentiment;
		this.date = date;
	}

	public String getEntity() {
		return companyName;
	}

	public double getSentiment() {
		return sentiment;
	}

	public Date getDate() {
		return date;
	}
}
