package WebCrawler2MT;


import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="url")
public class UrlToVisit {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@Column(columnDefinition = "clob default null")
	private String url;
	@Column(name="hash")
	private int urlHashCode;
	@Column(columnDefinition="boolean default 0")
	private boolean visited;
	
	private LocalDateTime timestamp;
	
	@Column(name="finds")
	private Long numberOfFinds;
	
	@OneToOne(mappedBy="url", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private UrlData urlData;
	
	protected UrlToVisit() {
			}

	public UrlToVisit(String url) {
		this.url=url;
		this.urlHashCode=url.hashCode();
		this.visited=false;
		this.timestamp=LocalDateTime.now();
		this.numberOfFinds=Long.valueOf(1);
		
	}
	public Long getId() {
		return id;
	}
	public String getUrl() {
		return url;
	}
	public int getUrlHashCode() {
		return urlHashCode;
	}
	public void setVisited() {
		this.visited=true;
	}
	public boolean isVisited() {
		return visited;
	}
	public void setUrlData(UrlData urlData) {
		this.urlData=urlData;
	}
	public UrlData getUrlData() {
		return urlData;
	}
	public void setTimestamp () {
		this.timestamp=LocalDateTime.now();
	}
	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	public void setPoison() {
		this.id=Long.MAX_VALUE;
	}
	public void incNumberOfFinds() {
		numberOfFinds++;
	}
	public long getNumberOfFinds() {
		return numberOfFinds;
	}

}
