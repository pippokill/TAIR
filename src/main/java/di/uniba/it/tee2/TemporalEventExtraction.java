package di.uniba.it.tee2;

import di.uniba.it.tee2.data.TaggedText;
import di.uniba.it.tee2.data.TimeEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class TemporalEventExtraction {

    private Analyzer analyzer;
    private TemporalExtractor tempExtractor;
    private FSDirectory time_index;
    private FSDirectory doc_index;
    private FSDirectory docrep_index;
    private IndexWriter time_writer;
    private IndexWriter doc_writer;
    private IndexWriter docrep_writer;
    private int contextSize = 256;
    private static final Logger logger = Logger.getLogger(TemporalEventExtraction.class.getName());

    /**
     * @param lang
     * @param timeDir
     * @param docDir
     * @throws IOException
     *
     */
    public void init(String lang, String timeDir, String docDir) throws IOException {
        tempExtractor = new TemporalExtractor(lang);
        tempExtractor.init();
        time_index = FSDirectory.open(new File(timeDir));
        doc_index = FSDirectory.open(new File(docDir));
        docrep_index = FSDirectory.open(new File(docDir + "_repo"));
        analyzer = new StandardAnalyzer(Version.LUCENE_48);
        IndexWriterConfig configTime = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configTime.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        time_writer = new IndexWriter(time_index, configTime);
        IndexWriterConfig configDoc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configDoc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        doc_writer = new IndexWriter(doc_index, configDoc);
        IndexWriterConfig configDocRep = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configDoc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        docrep_writer = new IndexWriter(docrep_index, configDocRep);

    }

    /**
     * @throws IOException
     *
     */
    public void close() throws IOException {
        //close writers
        time_writer.close();
        doc_writer.close();
        docrep_writer.close();
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    /**
     * Crea e memorizza un documento xml a partire dalla stringa fornita in
     * input dopo averla taggata usando HeidelTime.
     *
     * @param content
     * @param fileName
     * @param docID
     */
    public void add(String content, String fileName, String docID) throws Exception {
        TaggedText tt = null;
        try {
            tt = tempExtractor.process(content);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error to process doc " + docID + " (skip doc)", ex);
        }
        if (tt != null) {

            //stores id and text (not tagged) in docrep_index (document repository)
            Document docrep_doc = new Document();
            docrep_doc.add(new StringField("id", docID, Field.Store.YES));
            docrep_doc.add(new StringField("content", tt.getText(), Field.Store.YES));
            docrep_writer.addDocument(docrep_doc);

            //stores id and text (not tagged) in doc_index for search
            Document doc_doc = new Document();
            doc_doc.add(new StringField("id", docID, Field.Store.YES));
            doc_doc.add(new TextField("content", tt.getText(), Field.Store.NO));
            doc_writer.addDocument(doc_doc);

            for (TimeEvent event : tt.getEvents()) { //for each TIMEX3 store info time index
                //stores id, file name and text (TimeML tagged) in time_index
                Document time_doc = new Document();
                time_doc.add(new StringField("id", docID, Field.Store.YES));
                //time_doc.add(new StringField("file", fileName, Field.Store.YES));
                //time_doc.add(new TextField("content", tt.getTaggedText(), Field.Store.NO));
                /*FieldType ft = new FieldType();
                 ft.setStoreTermVectors(true);
                 ft.setTokenized(true);
                 ft.setStored(true);
                 ft.setIndexed(true);
                 ft.setStoreTermVectorPositions(true);
                 ft.setOmitNorms(false);*/

                time_doc.add(new StringField("time", event.getDateString(), Field.Store.YES));
                time_doc.add(new IntField("offset_start", event.getStartOffset(), Field.Store.YES));
                time_doc.add(new IntField("offset_end", event.getEndOffset(), Field.Store.YES));
                time_doc.add(new TextField("context", getTimeContext(tt.getText(), event.getStartOffset(), event.getEndOffset()), Field.Store.NO));
                time_writer.addDocument(time_doc);
            }
        }
    }

    public String getTimeContext(String content, int startOffset, int endOffset) {
        int start = Math.max(0, startOffset - contextSize);
        int end = Math.min(content.length(), endOffset + contextSize);
        String context = content.substring(start, end);
        int index = context.indexOf(" ");
        if (index >= 0 && index < context.length()) {
            context = context.substring(index + 1);
        }
        index = context.lastIndexOf(" ");
        if (index >= 0 && index < context.length()) {
            context = context.substring(0, index);
        }
        return context;
    }

    /**
     * @param docID
     * @throws IOException
     */
    public void remove(String docID) throws IOException {
        time_writer.deleteDocuments(new Term("id", docID));
        doc_writer.deleteDocuments(new Term("id", docID));
    }

    /**
     * @param keyword
     * @param timeRange
     * @param maxResults
     * @return
     * @throws java.lang.Exception
     *
     */
    public List<Document> search(String keyword, String timeRange, int maxResults) throws Exception {
        String timeQuery = normalizeTimeQuery(timeRange);
        keyword = keyword.replaceAll(" ", " AND ");
        QueryParser parser = new QueryParser(Version.LUCENE_48, "content", analyzer);
        Query queryKeyword = parser.parse(keyword);
        parser = new QueryParser(Version.LUCENE_48, "time", analyzer);
        Query queryTime = parser.parse(timeQuery);

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(queryKeyword, BooleanClause.Occur.MUST);
        booleanQuery.add(queryTime, BooleanClause.Occur.MUST);
        DirectoryReader reader = DirectoryReader.open(time_index);
        IndexSearcher time_searcher = new IndexSearcher(reader);
        logger.info(booleanQuery.toString());
        TopDocs hits = time_searcher.search(booleanQuery, maxResults);
        List<Document> results = new ArrayList<>();
        for (int i = 0; i < hits.totalHits; i++) {
            int docId = hits.scoreDocs[i].doc;
            Document d = time_searcher.doc(docId);
            results.add(d);
        }

        return results;
    }

    /**
     * @param id
     * @return
     * @throws java.io.IOException
     *
     */
    public String getDocumentText(String id) throws IOException {
        Query query = new TermQuery(new Term("id", id));
        DirectoryReader reader = DirectoryReader.open(doc_index);
        IndexSearcher id_searcher = new IndexSearcher(reader);
        TopDocs hits = id_searcher.search(query, 1);
        int docId = hits.scoreDocs[0].doc;
        Document d = id_searcher.doc(docId);
        String text = d.get("content");
        return text;
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

}
