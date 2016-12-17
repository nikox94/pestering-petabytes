package is.petabytes.pestering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import is.petabytes.pestering.SentimentTaskmaster;

@SpringBootApplication
public class SentimentAppApplication {

	public static void main(final String[] args) {
		final ApplicationContext ctx = SpringApplication.run(SentimentAppApplication.class, args);

		final SentimentTaskmaster taskmaster = (SentimentTaskmaster) ctx.getBean("sentimentTaskmaster");
		taskmaster.startApplication();

		System.exit(0);
	}
}
