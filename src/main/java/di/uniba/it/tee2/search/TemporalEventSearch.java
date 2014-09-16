/**
 * Copyright (c) 2014, the TEE2 AUTHORS.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the University of Bari nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * GNU GENERAL PUBLIC LICENSE - Version 3, 29 June 2007
 *
 */
package di.uniba.it.tee2.search;

import di.uniba.it.tee2.TemporalExtractor;
import di.uniba.it.tee2.data.TaggedText;
import di.uniba.it.tee2.data.TimeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author pierpaolo
 */
public class TemporalEventSearch {

    private IndexSearcher doc_searcher;

    private IndexSearcher time_searcher;

    private IndexSearcher repo_searcher;

    private final String mainDir;

    private final TemporalExtractor tempExtractor;

    private static final Logger logger = Logger.getLogger(TemporalEventSearch.class.getName());

    private StandardAnalyzer analyzer;

    private KeywordAnalyzer kwAnalyzer;

    private int snipSize = 128;

    public TemporalEventSearch(String mainDir, TemporalExtractor tempExtractor) {
        this.mainDir = mainDir;
        this.tempExtractor = tempExtractor;
    }

    public void init() throws IOException {
        DirectoryReader timeReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/time")));
        DirectoryReader docReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/doc")));
        DirectoryReader repoReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/repo")));
        doc_searcher = new IndexSearcher(docReader);
        time_searcher = new IndexSearcher(timeReader);
        repo_searcher = new IndexSearcher(repoReader);
        analyzer = new StandardAnalyzer(Version.LUCENE_48);
        kwAnalyzer = new KeywordAnalyzer();
    }

    public void close() throws IOException {
        doc_searcher.getIndexReader().close();
        time_searcher.getIndexReader().close();
        repo_searcher.getIndexReader().close();
    }

    /**
     * @param query
     * @param timeRange
     * @param maxResults
     * @return
     * @throws java.lang.Exception
     *
     */
    public List<SearchResult> naturalSearch(String query, String timeRange, int maxResults) throws Exception {
        String timeQueryString = null;
        if (timeRange.length() > 0) {
            timeQueryString = normalizeTimeQuery(timeRange);
        }
        //query = query.replaceAll("\\s+", " AND "); //no AND operator on keyword
        QueryParser contentParser = new QueryParser(Version.LUCENE_48, "content", analyzer);
        Query contentQuery = contentParser.parse(query);

        QueryParser timeParser = new QueryParser(Version.LUCENE_48, "time", kwAnalyzer);
        Query timeQuery = null;
        if (timeQueryString != null && timeQueryString.length() > 0) {
            timeQuery = timeParser.parse(timeQueryString);
        }

        TopDocs topDocs = doc_searcher.search(contentQuery, Integer.MAX_VALUE);
        Map<String, Float> docScoreMap = new HashMap<>();
        for (ScoreDoc sd : topDocs.scoreDocs) {
            docScoreMap.put(doc_searcher.doc(sd.doc).get("id"), sd.score);
        }
        BooleanQuery bq = new BooleanQuery();
        QueryParser contextParser = new QueryParser(Version.LUCENE_48, "context", analyzer);
        Query contextQuery = contextParser.parse(query);
        bq.add(contextQuery, BooleanClause.Occur.MUST);
        if (timeQuery != null) {
            bq.add(timeQuery, BooleanClause.Occur.MUST);
        }
        TopDocs timeDocs = time_searcher.search(bq, maxResults);
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc sd : timeDocs.scoreDocs) {
            Document timedoc = time_searcher.doc(sd.doc);
            String docId = timedoc.get("id");
            String text = getDocumentText(docId);
            if (text != null) {
                String snip = createSnippet(text, Integer.parseInt(timedoc.get("offset_start")), Integer.parseInt(timedoc.get("offset_end")));
                SearchResult sr = new SearchResult(sd.doc, docId);
                sr.setSnip(snip);
                Float score = docScoreMap.get(docId);
                if (score != null) {
                    sr.setScore(sd.score * score);
                } else {
                    sr.setScore(sd.score);
                }
                results.add(sr);

            } else {
                logger.log(Level.WARNING, "No text for doc: {0}", docId);
            }
        }
        Collections.sort(results);
        return results;
    }

    public List<SearchResult> search(String query, String timeRange, int maxResults) throws Exception {
        //query = query.replaceAll("\\s+", " AND "); //no AND operator on keyword
        QueryParser contentParser = new QueryParser(Version.LUCENE_48, "content", analyzer);
        Query contentQuery = null;
        if (query.length() > 0) {
            contentQuery = contentParser.parse(query);
        }

        QueryParser timeParser = new QueryParser(Version.LUCENE_48, "time", kwAnalyzer);
        Query timeQuery = null;
        if (timeRange.length() > 0) {
            timeQuery = timeParser.parse(timeRange);
        }

        Map<String, Float> docScoreMap = new HashMap<>();
        if (contentQuery != null) {
            TopDocs topDocs = doc_searcher.search(contentQuery, Integer.MAX_VALUE);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                docScoreMap.put(doc_searcher.doc(sd.doc).get("id"), sd.score);
            }
        }
        BooleanQuery bq = new BooleanQuery();
        QueryParser contextParser = new QueryParser(Version.LUCENE_48, "context", analyzer);
        if (query.length() > 0) {
            Query contextQuery = contextParser.parse(query);
            bq.add(contextQuery, BooleanClause.Occur.MUST);
        }
        if (timeQuery != null) {
            bq.add(timeQuery, BooleanClause.Occur.MUST);
        }
        TopDocs timeDocs = time_searcher.search(bq, maxResults);
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc sd : timeDocs.scoreDocs) {
            Document timedoc = time_searcher.doc(sd.doc);
            String docId = timedoc.get("id");
            String text = getDocumentText(docId);
            if (text != null) {
                String snip = createSnippet(text, Integer.parseInt(timedoc.get("offset_start")), Integer.parseInt(timedoc.get("offset_end")));
                SearchResult sr = new SearchResult(sd.doc, docId);
                sr.setSnip(snip);
                Float score = docScoreMap.get(docId);
                if (score != null) {
                    sr.setScore(sd.score * score);
                } else {
                    sr.setScore(sd.score);
                }
                results.add(sr);

            } else {
                logger.log(Level.WARNING, "No text for doc: {0}", docId);
            }
        }
        Collections.sort(results);
        return results;
    }

    private String createSnippet(String text, int startm, int end) {
        int s = Math.max(0, startm - snipSize);
        int e = Math.min(text.length(), end + snipSize);
        return text.substring(s, e);
    }

    /**
     * @param timeRange
     * @return
     */
    private String normalizeTimeQuery(String timeRange) throws Exception {
        TaggedText tt = tempExtractor.process(timeRange);
        for (TimeEvent event : tt.getEvents()) { //cicla su tutti  i tag timex3 presenti nel documento
            timeRange = timeRange.replace(event.getEventString(), event.getDateString());
        }
        return timeRange;
    }

    /**
     * @param id
     * @return
     * @throws java.io.IOException
     *
     */
    public String getDocumentText(String id) throws IOException {
        Query query = new TermQuery(new Term("id", id));
        TopDocs hits = repo_searcher.search(query, 1);
        int docId = hits.scoreDocs[0].doc;
        return repo_searcher.doc(docId).get("content");
    }

    public int getSnipSize() {
        return snipSize;
    }

    public void setSnipSize(int snipSize) {
        this.snipSize = snipSize;
    }

}
