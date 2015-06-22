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

import di.uniba.it.tee2.analyzer.EnglishNoStemAnalyzer;
import di.uniba.it.tee2.analyzer.ItalianNoStemAnalyzer;
import di.uniba.it.tee2.extraction.TemporalExtractor;
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
import org.apache.lucene.analysis.Analyzer;
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

    private final Analyzer analyzer;

    private int snipSize = 128;

    private final String language;

    public TemporalEventSearch(String mainDir, TemporalExtractor tempExtractor) {
        this.mainDir = mainDir;
        this.tempExtractor = tempExtractor;
        this.language = tempExtractor.getLanguage();
        switch (language) {
            case "italian":
                analyzer = new ItalianNoStemAnalyzer(Version.LUCENE_48);
                break;
            case "english":
                analyzer = new EnglishNoStemAnalyzer(Version.LUCENE_48);
                break;
            default:
                analyzer = new StandardAnalyzer(Version.LUCENE_48);
                break;
        }
    }

    public void init() throws IOException {
        DirectoryReader timeReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/time")));
        DirectoryReader docReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/doc")));
        DirectoryReader repoReader = DirectoryReader.open(FSDirectory.open(new File(mainDir + "/repo")));
        doc_searcher = new IndexSearcher(docReader);
        time_searcher = new IndexSearcher(timeReader);
        repo_searcher = new IndexSearcher(repoReader);
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
        QueryParser contentParser = new QueryParser(Version.LUCENE_48, "content", analyzer);
        QueryParser titleParser = new QueryParser(Version.LUCENE_48, "title", analyzer);
        QueryParser contextParser = new QueryParser(Version.LUCENE_48, "context", analyzer);
        QueryParser timeParser = new QueryParser(Version.LUCENE_48, "time", analyzer);
        String timeQueryString = null;
        if (timeRange.length() > 0) {
            timeQueryString = normalizeTimeQuery(timeRange);
        }

        Query contentQuery = null;
        Query titleQuery = null;
        Query contextQuery = null;
        if (query.length() > 0) {
            titleQuery = titleParser.parse(query);
            contentQuery = contentParser.parse(query);
            contextQuery = contextParser.parse(query);
        }

        Query timeConstraint = null;
        if (timeQueryString != null && timeQueryString.length() > 0) {
            timeConstraint = timeParser.parse(timeQueryString);
        }

        //BooleanQuery idQuery = new BooleanQuery();
        BooleanQuery docQuery = new BooleanQuery();
        if (titleQuery != null) {
            docQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
        }
        if (contentQuery != null) {
            docQuery.add(contentQuery, BooleanClause.Occur.SHOULD);
        }
        Map<String, Float> docScoreMap = new HashMap<>();
        if (titleQuery != null || contentQuery != null) {
            Logger.getLogger(TemporalEventSearch.class.getName()).log(Level.INFO, "Doc query: {0}", docQuery.toString());
            TopDocs topDocs = doc_searcher.search(contentQuery, 1000);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                String docid = doc_searcher.doc(sd.doc).get("id");
                docScoreMap.put(docid, sd.score + 1);
                //idQuery.add(new TermQuery(new Term("id", docid)), BooleanClause.Occur.SHOULD);
            }
        }

        BooleanQuery timeQuery = new BooleanQuery();
        if (timeConstraint != null) {
            timeQuery.add(timeConstraint, BooleanClause.Occur.MUST);
        }
        if (contextQuery != null) {
            timeQuery.add(contextQuery, BooleanClause.Occur.MUST);
        }
        /*if (timeConstraint != null || contextQuery != null) {
         timeQuery.add(idQuery, BooleanClause.Occur.MUST);
         }*/
        Logger.getLogger(TemporalEventSearch.class.getName()).log(Level.INFO, "Time query: {0}", timeQuery.toString());
        TopDocs timeDocs = time_searcher.search(timeQuery, 1000);
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc sd : timeDocs.scoreDocs) {
            Document timedoc = time_searcher.doc(sd.doc);
            String docId = timedoc.get("id");
            Document document = getDocument(docId);
            if (document != null && document.get("content") != null) {
                SearchResult sr = new SearchResult(sd.doc, docId);
                sr.setStartOffset(timedoc.getField("offset_start").numericValue().intValue());
                sr.setEndOffset(timedoc.getField("offset_end").numericValue().intValue());
                String snip = createSnippet(document.get("content"), sr.getStartOffset(), sr.getEndOffset());
                sr.setSnip(snip);
                sr.setTitle(document.get("title"));
                Float score = docScoreMap.get(docId);
                if (score != null) {
                    sr.setScore(sd.score * score);
                    results.add(sr);
                } else {
                    sr.setScore(sd.score);
                    results.add(sr);
                }
            } else {
                logger.log(Level.WARNING, "No text for doc: {0}", docId);
            }
        }
        Collections.sort(results);
        if (results.size() > maxResults) {
            return results.subList(0, maxResults);
        } else {
            return results;
        }
    }

    public List<SearchResult> search(String query, String timeRange, int maxResults) throws Exception {
        QueryParser contentParser = new QueryParser(Version.LUCENE_48, "content", analyzer);
        QueryParser titleParser = new QueryParser(Version.LUCENE_48, "title", analyzer);
        QueryParser contextParser = new QueryParser(Version.LUCENE_48, "context", analyzer);
        QueryParser timeParser = new QueryParser(Version.LUCENE_48, "time", analyzer);
        Query contentQuery = null;
        Query titleQuery = null;
        Query contextQuery = null;
        if (query.length() > 0) {
            titleQuery = titleParser.parse(query);
            contentQuery = contentParser.parse(query);
            contextQuery = contextParser.parse(query);
        }

        Query timeConstraint = null;
        if (timeRange != null && timeRange.length() > 0) {
            timeConstraint = timeParser.parse(timeRange);
        }

        //BooleanQuery idQuery = new BooleanQuery();
        BooleanQuery docQuery = new BooleanQuery();
        if (titleQuery != null) {
            docQuery.add(titleQuery, BooleanClause.Occur.SHOULD);
        }
        if (contentQuery != null) {
            docQuery.add(contentQuery, BooleanClause.Occur.SHOULD);
        }
        Map<String, Float> docScoreMap = new HashMap<>();
        if (titleQuery != null || contentQuery != null) {
            Logger.getLogger(TemporalEventSearch.class.getName()).log(Level.INFO, "Doc query: {0}", docQuery.toString());
            TopDocs topDocs = doc_searcher.search(contentQuery, 1000);
            for (ScoreDoc sd : topDocs.scoreDocs) {
                String docid = doc_searcher.doc(sd.doc).get("id");
                docScoreMap.put(docid, sd.score + 1);
                //idQuery.add(new TermQuery(new Term("id", docid)), BooleanClause.Occur.SHOULD);
            }
        }

        BooleanQuery timeQuery = new BooleanQuery();
        if (timeConstraint != null) {
            timeQuery.add(timeConstraint, BooleanClause.Occur.MUST);
        }
        if (contextQuery != null) {
            timeQuery.add(contextQuery, BooleanClause.Occur.MUST);
        }
        /*if (timeConstraint != null || contextQuery != null) {
         timeQuery.add(idQuery, BooleanClause.Occur.MUST);
         }*/
        Logger.getLogger(TemporalEventSearch.class.getName()).log(Level.INFO, "Time query: {0}", timeQuery.toString());
        TopDocs timeDocs = time_searcher.search(timeQuery, 1000);
        List<SearchResult> results = new ArrayList<>();
        for (ScoreDoc sd : timeDocs.scoreDocs) {
            Document timedoc = time_searcher.doc(sd.doc);
            String docId = timedoc.get("id");
            Document document = getDocument(docId);
            if (document != null && document.get("content") != null) {
                SearchResult sr = new SearchResult(sd.doc, docId);
                sr.setStartOffset(timedoc.getField("offset_start").numericValue().intValue());
                sr.setEndOffset(timedoc.getField("offset_end").numericValue().intValue());
                String snip = createSnippet(document.get("content"), sr.getStartOffset(), sr.getEndOffset());
                sr.setSnip(snip);
                sr.setTitle(document.get("title"));
                Float score = docScoreMap.get(docId);
                if (score != null) {
                    sr.setScore(sd.score * score);
                    results.add(sr);
                } else {
                    sr.setScore(sd.score);
                    results.add(sr);
                }
            } else {
                logger.log(Level.WARNING, "No text for doc: {0}", docId);
            }
        }
        Collections.sort(results);
        if (results.size() > maxResults) {
            return results.subList(0, maxResults);
        } else {
            return results;
        }
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

    /**
     * @param id
     * @return
     * @throws java.io.IOException
     *
     */
    public Document getDocument(String id) throws IOException {
        Query query = new TermQuery(new Term("id", id));
        TopDocs hits = repo_searcher.search(query, 1);
        int docId = hits.scoreDocs[0].doc;
        return repo_searcher.doc(docId);
    }

    public int getSnipSize() {
        return snipSize;
    }

    public void setSnipSize(int snipSize) {
        this.snipSize = snipSize;
    }

}
