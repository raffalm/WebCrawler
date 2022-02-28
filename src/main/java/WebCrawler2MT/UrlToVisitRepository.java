package WebCrawler2MT;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UrlToVisitRepository extends JpaRepository<UrlToVisit,Long>{
	List<UrlToVisit> findFirst100ByVisited(boolean visited);
	List<UrlToVisit> findByUrlHashCode (int hash);
	 long countByVisited (boolean visited);
}
