package is.petabytes.pestering.model;

import java.util.Date;

import com.ibm.watson.developer_cloud.alchemy.v1.model.Sentiment;

public class EntitySentiment {
	
	private static final String SENTIMENT_POSITIVE = "positive";
	private static final String SENTIMENT_NEGATIVE = "negative";
	private static final String SENTIMENT_NEUTRAL = "neutral";
	
	private static double DEFAULT_NEGATIVE_SCORE = -.3;
	private static double DEFAULT_POSITIVE_SCORE = .3;
	private static double DEFAULT_NEUTRAL_SCORE = 0;
	
	
	private String entity;
	
	private double sentiment;
	
	private Date date;

	public EntitySentiment(String entity, Sentiment sentiment) {
		super();
		this.entity = entity;
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
	
	public EntitySentiment(String entity, double sentiment) {
		super();
		this.entity = entity;
		this.sentiment = sentiment;
		this.date = new Date();
	}
	
	public EntitySentiment(String entity, double sentiment, Date date) {
		super();
		this.entity = entity;
		this.sentiment = sentiment;
		this.date = date;
	}

	public String getEntity() {
		return entity;
	}

	public double getSentiment() {
		return sentiment;
	}

	public Date getDate() {
		return date;
	}	
}
