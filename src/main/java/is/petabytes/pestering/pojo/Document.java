package is.petabytes.pestering.pojo;

import java.util.Date;

import lombok.Data;

@Data
public class Document {

	private String title;

	private String body;

	private DocumentType type;

	private Date date;
}
