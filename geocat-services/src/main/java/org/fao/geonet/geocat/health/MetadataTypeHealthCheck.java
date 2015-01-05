package org.fao.geonet.geocat.health;

import com.yammer.metrics.core.HealthCheck;
import jeeves.monitor.HealthCheckFactory;
import jeeves.server.context.ServiceContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.fao.geonet.kernel.search.IndexAndTaxonomy;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.index.GeonetworkMultiReader;

/**
 * Checks to ensure that only iso19139.che metadata are in the database
 * <p/>
 * User: jeichar
 * Date: 3/26/12
 * Time: 9:01 AM
 */
public class MetadataTypeHealthCheck implements HealthCheckFactory {
    public HealthCheck create(final ServiceContext context) {
        return new HealthCheck("iso19139.che only Metadata") {
            @Override
            protected Result check() throws Exception {

                SearchManager searchMan = context.getBean(SearchManager.class);

                IndexAndTaxonomy indexAndTaxonomy = searchMan.getIndexReader(null, -1);
                GeonetworkMultiReader reader = indexAndTaxonomy.indexReader;

                try {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    BooleanQuery query = new BooleanQuery();
                    TermQuery schemaIsIso19139CHE = new TermQuery(new Term("_schema", "iso19139.che"));
                    TermQuery notHarvested = new TermQuery(new Term("_isHarvested", "n"));
                    query.add(new BooleanClause(notHarvested, Occur.MUST));
                    query.add(new BooleanClause(schemaIsIso19139CHE, Occur.MUST_NOT));
                    TopDocs hits = searcher.search(query, 1);
                    if (hits.totalHits > 0) {
                        return Result.unhealthy("Found " + hits.totalHits + " metadata that were not harvested but do not have iso19139" +
                                                ".che schema");
                    } else {
                        return Result.healthy();
                    }
                } catch (Throwable e) {
                    return Result.unhealthy(e);
                } finally {
                    searchMan.releaseIndexReader(indexAndTaxonomy);
                }
            }
        };
    }
}
