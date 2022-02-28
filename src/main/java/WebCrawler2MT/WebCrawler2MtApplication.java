//TODO
//show/hide log currently visiting website
package WebCrawler2MT;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SpringBootApplication
public class WebCrawler2MtApplication {
	private final String blackListFileExtensions[] = { "pdf", "doc", "docx", "xls", "xlsx", "ppt", "zip", "rar", "7z",
			"tar", "gz", "avi", "mp4", "png", "jpeg", "jpg", "tif" };
	static UrlToVisitRepository urlRepository;
	private static UrlDataRepository urlDataRepository;
	private static final int CRAWLERS_COUNT = 5;
	private ThreadPoolExecutor executor;
	private static int	timeout=120000; //connection or reader timeout
	private static boolean showLinkProcessingProgress=false;
	private static boolean stopCrawlers=false;
	private int showLinkProcessingProgressInterval;
	private ConcurrentHashMap<Long, Boolean> urlProcessed = new ConcurrentHashMap<>();// pages currently being processed 
	private BlockingQueue<UrlToVisit> urlToVisitQueue = new LinkedBlockingQueue<>(150);//pages to visit
	private List<String> blackList = new ArrayList<>();
	private static boolean quitApp=false;
	private static boolean emptyUrlToVisit=false;
	


	class CrawlerRunnable implements Runnable {

		String name;
		UrlToVisit page;
		boolean poisonPill;

		public CrawlerRunnable(String name) {
			this.name = name;
			poisonPill=false;
		}

		@Override
		public void run() {
			log.info("started: "+this.name);
			while (!stopCrawlers && !poisonPill&&!Thread.currentThread().isInterrupted()) {
				try {
					page = urlToVisitQueue.take();
				} catch (InterruptedException e) {
				}
				if (page.getId()!=Long.MAX_VALUE) { //poison pill
					//add to currently processed pages
					if (urlProcessed.putIfAbsent(page.getId(), false) == null) {
						if (!urlOnBlackList(page.getUrl()) && !fileOnBlackList(page.getUrl(), blackListFileExtensions)) {
							log.info("\n[" + name + "]" + "\tVisiting page: " + page.getId()+':'+page.getUrl());
							processPage(page,this);
						}
						saveProcessedURL(page);
						if(urlProcessed.containsKey(page.getId()))
							urlProcessed.remove(page.getId());
					
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				} else
					poisonPill=true;
			}
			log.info("stopped: "+this.name);
		}
	}
	class UrlProvider implements Runnable {
		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				if (urlToVisitQueue.size() == 0) {
					int result=getUrlToVisitFromRepository(urlRepository);
					if(result==0)
						emptyUrlToVisit=true;
					else
						emptyUrlToVisit=false;

				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					
				}
					
			}
		}
	}
	class CrawlersService implements Runnable {

		@Override
		public void run() {
			
				int crawlerId;
				boolean printInfo=true;
				executor=(ThreadPoolExecutor)Executors.newFixedThreadPool(CRAWLERS_COUNT);
				for (crawlerId=1;crawlerId<=CRAWLERS_COUNT;++crawlerId) {
					executor.submit(new CrawlerRunnable("crawler Id."+(crawlerId)));
				}
				
				while (!Thread.currentThread().isInterrupted()) {
					//create new task
					if(!stopCrawlers && executor.getActiveCount()<CRAWLERS_COUNT) {
						executor.submit(new CrawlerRunnable("crawler Id."+(++crawlerId)));
						printInfo=true;
					}
					//all crawlers are stopped by user
					if(stopCrawlers && executor.getActiveCount()==0) {
						crawlerId=0;
						if(printInfo) {
							log.info("All crawlers are stopped");
							if(!quitApp)
								System.out.println("Type 's' to start crawlers or 'q' to quit application, then press ENTER");
							printInfo=false;
						}
					}
					//quit when empty UrlToVisit
					if (emptyUrlToVisit && quitApp) {
						int activeThreadCount=executor.getActiveCount();
						for (int i=0;i<activeThreadCount;++i) {
							UrlToVisit page=new UrlToVisit("");
							page.setPoison();
							//put poison pill
							try {
								urlToVisitQueue.put(page);
							} catch (InterruptedException e) {
								}
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						
					}
				}
			}
	}
	public boolean getShowLinkProcessingProgress() {
		return showLinkProcessingProgress;
	}
	public void setShowLinkProcessingProgress(boolean value) {
		showLinkProcessingProgress=value;
	}
	public void switchShowLinkProcessingProgress(boolean print) {
		if(showLinkProcessingProgress)
			setShowLinkProcessingProgress(false);
		else
			setShowLinkProcessingProgress(true);
		
		if(print) {
			if(getShowLinkProcessingProgress())
				System.out.println("Show link processing progress is Enabled \nType 'p' and press ENTER to Disable");
			else
				System.out.println("Show link processing progress is Disabled \nType 'p' and press ENTER to Enable");
			}
	}
	public boolean getStopCrawlers() {
		return stopCrawlers;
	}
	public void switchStopCrawlers(boolean print) {
		if(stopCrawlers)
			stopCrawlers=false;
		else
			stopCrawlers=true;
		if(print) {
			if(getStopCrawlers())
				System.out.println("Waiting for stop crawlers...");
			else
				System.out.println("Starting crawlers \nType 's' and press ENTER to Stop");
		}
	}
	private String urlToAbsoluteURL(String url, String urlCurrentPage) {

		if (url.startsWith("http://") || url.startsWith("https://")) {
			return url;
		}

		if (url.startsWith("/")) {
			String baseURL = getBaseURL(urlCurrentPage);
			if (baseURL != null) {
				return baseURL + url;
			}
		}
		return null;
	}
	private final static Pattern baseURLPattern = Pattern.compile("(((http://)|(https://))[^/]+)");
	private static String getBaseURL(String currentPageURL) {
		Matcher m = baseURLPattern.matcher(currentPageURL);
		if (m.find()) {
			String baseURL = m.group(1);
			return baseURL;
		}
		return null;
	}
	private synchronized void saveNewURL(String url) {
		
		List<UrlToVisit> urlList=urlRepository.findByUrlHashCode(url.hashCode());
		if(!urlList.isEmpty()) {
			for(UrlToVisit page:urlList) {
				if(page.getUrl().equals(url))
					return;
			}
		}
		UrlToVisit objUrl = new UrlToVisit(url);
		urlRepository.save(objUrl);
	}
	private void saveProcessedURL(UrlToVisit page) {
		if (!page.isVisited()) 
				page.setVisited();
		
			page.setTimestamp();
			urlRepository.save(page);
	
	}
	private void processLinks(UrlToVisit page, org.w3c.dom.Document pageDOC, CrawlerRunnable crawler) {
		
		XPath xPath = XPathFactory.newInstance().newXPath();

		//searching for links
		String xPathExpression = "//a[@href]/@href";
		try {
			NodeList resultNodeList = (NodeList) xPath.compile(xPathExpression).evaluate(pageDOC,
					XPathConstants.NODESET);

			int tempProgress=-1;
			int progress=0;

			for (int i = 0; i < resultNodeList.getLength(); ++i) {
				Node n = resultNodeList.item(i);
				String url = n.getTextContent();
				String absoluteURL = urlToAbsoluteURL(url, page.getUrl());
				
				if(absoluteURL!=null&& !urlOnBlackList(absoluteURL)) { 
					//check without blocking
					List<UrlToVisit> urlList=urlRepository.findByUrlHashCode(absoluteURL.hashCode());
					if(!urlList.isEmpty()) {
						
						for(UrlToVisit pageUrl:urlList) {
							if(!pageUrl.getUrl().equals(absoluteURL))
								saveNewURL(absoluteURL);
							else
								// number of finds
								pageUrl.incNumberOfFinds();
								urlRepository.save(pageUrl);
						}
					} else					
						saveNewURL(absoluteURL);
				}
				
				//progress
				if (showLinkProcessingProgress) {
					if(resultNodeList.getLength()!=0) {
						progress=(int)(((float)i/(float)resultNodeList.getLength())*100)+1;
						showLinkProcessingProgressInterval=(int)Math.ceil(1000.0/resultNodeList.getLength());
						//show
						if(progress%showLinkProcessingProgressInterval==0 && tempProgress!=progress) {
							tempProgress=progress;
							System.out.println("["+crawler.name+"]\tProcessed links: .."+progress+"% from "+resultNodeList.getLength()+" pcs");

						}
					}
				}
			}

		} catch (XPathExpressionException e) {
			log.error("Exception in XPath-links: " + e);
			//System.exit(-1);
		}
	}
	private void processData(UrlToVisit page, org.w3c.dom.Document pageDOC) {
		Optional<UrlData> optionalUrlData = urlDataRepository.findById(page.getId());
		UrlData urlData;
		if (optionalUrlData.isPresent()) {
			urlData = optionalUrlData.get();
		} else {
			urlData = new UrlData(page);
		}

		if (urlData.getTitle() == null) {
			XPath xPath = XPathFactory.newInstance().newXPath();
			String xPathExpression = "//head/title";
			try {
				String title = (String) xPath.compile(xPathExpression).evaluate(pageDOC, XPathConstants.STRING);
				if (title != null) {
					urlData.setTitle(title);
					page.setUrlData(urlData);
					//urlRepository.save(currentPageURL);
					//saveProcessedURL(page);
				}
			} catch (XPathExpressionException e) {
				log.error("Exception in XPath-title: " + e);
				//System.exit(-1);
			}
		}
		if (urlData.getDescription() == null) {
			XPath xPath = XPathFactory.newInstance().newXPath();
			String xPathExpression = "//head/meta[@name='description']/@content";
			try {
				String desc = (String) xPath.compile(xPathExpression).evaluate(pageDOC, XPathConstants.STRING);
				if (desc != null) {
					urlData.setDescription(desc);
					page.setUrlData(urlData);
					//urlRepository.save(currentPageURL);
					//saveProcessedURL(page);
				}
			} catch (XPathExpressionException e) {
				//System.err.println("Exception in XPath-title: " + e);
				log.error("Exception in XPath-description: " + e);
				//System.exit(-1);
			}
		}
	}
	private void processPage(UrlToVisit page, CrawlerRunnable crawler) {

		String htmlPage = Util.downloadHTMLPage(page.getUrl(), timeout);
		if (htmlPage == null) {
			log.info("I can't visit the page: " + page.getUrl());
			//System.out.println("The page cannot be visited: " + page.getUrl());
			saveProcessedURL(page);
			if(urlProcessed.containsKey(page.getId()))
				urlProcessed.remove(page.getId());
			
			return;
		}

		org.w3c.dom.Document doc = Util.doc2Doc(Util.htmlToDocument(htmlPage));
		if (doc == null) {
			return;
		}
		doc.getDocumentElement().normalize();
		processLinks(page, doc, crawler);
		processData(page, doc);
	}
	private void prepareBlackList() {
		blackList.add("facebook.com");
		blackList.add("twitter.com");
	}
	private boolean urlOnBlackList(String url) {
		for (String b : blackList) {
			if (url.contains(b)) {
				return true;
			}
		}
		return false;
	}
	private static boolean fileOnBlackList(String url, String[] fileExtensions) {

		for (String extension : fileExtensions) {
			Pattern fileUrlPattern = Pattern.compile(".+\\." + extension + "(?:\\?{1}.*)?$", Pattern.CASE_INSENSITIVE);
			Matcher m = fileUrlPattern.matcher(url);
			if (m.matches())
				return true;
		}
		return false;
	}
	private void startDeamonThreads() {
		Thread urlProvider = new Thread(new UrlProvider());
		urlProvider.setDaemon(true);
		urlProvider.start();

		Thread crawlersServiceThread = new Thread(new CrawlersService());
		crawlersServiceThread.setDaemon(true);
		crawlersServiceThread.start();
	}
	private int getUrlToVisitFromRepository(UrlToVisitRepository repository) {

		List<UrlToVisit> urlList = new ArrayList<>();
		try {
			urlList = repository.findFirst100ByVisited(false);
		} catch (RuntimeException e) {
			return -1;
		}
		if(urlList.isEmpty())
			return 0;
		else {
			for (UrlToVisit page : urlList) {
				try {
					if (!urlProcessed.containsKey(page.getId()))
						urlToVisitQueue.put(page);
				} catch (InterruptedException e) {
					}
			}
		}
		return 1;

	}
	private void printNumberOfLinksInRepository(UrlToVisitRepository repository) {
		long count=repository.count();
		long visited=repository.countByVisited(true);
		System.out.println("**********\nNumber of links in repository: " + count + "\nVisited: "
				+ visited + " | To visit: " + (count-visited)+"\n**********");
	}
	private String inputUrl(Scanner scanner) {
		
		System.out.println("There is no URL to visit in the database.");
		String url=new String();
		boolean urlOK=false;
		while(!urlOK) {
			System.out.println("Enter URL of the page to visit or type \'q\' to cancel and quit application: ");
			url=scanner.nextLine();
			if((url.compareTo("q")==0) || (url.compareTo("Q")==0)){
				url="";
				urlOK=true;
			} else {
				try {
					URL page = new URL(url);
					URLConnection conn = page.openConnection();
					conn.connect();
					urlOK=true;
				} catch (MalformedURLException e) {
					System.out.println("URL is not in a valid form");
				} catch (IOException e) {
					System.out.println("connection couldn't be established!");
				} 
			}
		}
		return url;
	}
	private static final Logger log  = LoggerFactory.getLogger(WebCrawler2MtApplication.class);
	public static void main(String[] args) {
		ConfigurableApplicationContext context=SpringApplication.run(WebCrawler2MtApplication.class, args);
		
		log.info("Shutting down...");
		try {
			System.exit(SpringApplication.exit(context,()->0));
		} catch (SecurityException e) {
			log.error(""+e);
		}
	}

	@Bean
	public CommandLineRunner runner(UrlToVisitRepository urlRepository, UrlDataRepository urlDataRepository) {
		
		return (args) -> {
			System.out.println("******************************\nWebCrawlerMT Application - list of commands: "
					+"\n******************************"
					+ "\n\'p\' - show/hide links processing progress"
					+ "\n\'s\' - start/stop crawlers"
					+ "\n\'n\' - print number of links in repository"
					+ "\n\'q\' - shutdown application"
					+ "\n******************************");
			Scanner scanner=new Scanner(System.in);
			char command='\u0000';//nul
			WebCrawler2MtApplication crawler = new WebCrawler2MtApplication();
			crawler.prepareBlackList();
			WebCrawler2MtApplication.urlRepository = urlRepository;
			WebCrawler2MtApplication.urlDataRepository = urlDataRepository;
			crawler.startDeamonThreads();
			try {
				Thread.currentThread().sleep(2000);//waiting for start deamon threads
			} catch (InterruptedException e) {
				
			}
			while(!quitApp) {
				if(emptyUrlToVisit) {
					String url=inputUrl(scanner);
					if(url.compareTo("")!=0)
						saveNewURL(url);
					else {
						quitApp=true;
					}
				}
				if(!quitApp && scanner.hasNext()) {
					try {
						command=scanner.next().charAt(0);
					} catch (IllegalStateException e) {
					
					}
				}
				if(command!='\u0000') {
					switch (command) {
					case 'p':;
					case 'P':
						crawler.switchShowLinkProcessingProgress(true);
						break;
					case 's':;
					case 'S':
						crawler.switchStopCrawlers(true);
						break;
					case 'n':;
					case 'N':
						printNumberOfLinksInRepository(urlRepository);
						break;
					case 'q':;
					case 'Q':
						quitApp=true;
						if(!crawler.getStopCrawlers())
							crawler.switchStopCrawlers(true);

					default:;
				
					}
					command='\u0000';
					scanner.nextLine();//cleaning					
				}
			}
			scanner.close();

			while(crawler.executor.getActiveCount()!=0) {
				try {
					Thread.currentThread().sleep(500);//waiting for stop tasks
				} catch (InterruptedException e) {
					
				}
				}
		};
	}
}

