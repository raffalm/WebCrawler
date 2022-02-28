package WebCrawler2MT;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="url_data")
public class UrlData {
	@Id
	private Long id;
	@Column(columnDefinition = "clob default null")
	private String title;
	@Column(columnDefinition = "clob default null")
	private String description;

	@OneToOne(fetch=FetchType.LAZY)
	@MapsId
	private UrlToVisit url;

	protected UrlData() {
	}
	public UrlData(UrlToVisit url) {
		id=url.getId();
		this.url=url;
	}
	public Long getId() {
		return id;
	}
	public UrlToVisit getUrl() {
		return url;
	}
	public void setTitle(String title) {
		this.title=title;
	}
	public String getTitle() {
		return title;
	}
	public void setDescription(String description) {
		this.description=description;
	}
	public String getDescription() {
		return description;
	}
}