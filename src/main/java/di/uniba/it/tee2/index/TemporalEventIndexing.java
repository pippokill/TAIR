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
package di.uniba.it.tee2.index;

import di.uniba.it.tee2.extraction.TemporalExtractor;
import di.uniba.it.tee2.data.TaggedText;
import di.uniba.it.tee2.data.TimeEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class TemporalEventIndexing {

    private Analyzer analyzer;
    private TemporalExtractor tempExtractor;
    private FSDirectory time_index;
    private FSDirectory doc_index;
    private FSDirectory docrep_index;
    private IndexWriter time_writer;
    private IndexWriter doc_writer;
    private IndexWriter docrep_writer;
    private int contextSize = 256;
    private static final Logger logger = Logger.getLogger(TemporalEventIndexing.class.getName());

    /**
     * @param lang
     * @param mainDir
     * @throws IOException
     *
     */
    public void init(String lang, String mainDir) throws IOException {
        tempExtractor = new TemporalExtractor(lang);
        tempExtractor.init();
        time_index = FSDirectory.open(new File(mainDir + "/time"));
        doc_index = FSDirectory.open(new File(mainDir + "/doc"));
        docrep_index = FSDirectory.open(new File(mainDir + "/repo"));
        analyzer = new StandardAnalyzer(Version.LUCENE_48);
        IndexWriterConfig configTime = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configTime.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        time_writer = new IndexWriter(time_index, configTime);
        IndexWriterConfig configDoc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configDoc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        doc_writer = new IndexWriter(doc_index, configDoc);
        IndexWriterConfig configDocRep = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        configDoc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
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
     * @param title
     * @param content
     * @param fileName
     * @param docID
     */
    public void add(String title, String content, String fileName, String docID) throws Exception {
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
            docrep_doc.add(new StringField("title", title, Field.Store.YES));
            docrep_doc.add(new StoredField("content", tt.getText()));
            docrep_doc.add(new StringField("filename", fileName, Field.Store.YES));
            docrep_writer.addDocument(docrep_doc);

            //stores id and text (not tagged) in doc_index for search
            Document doc_doc = new Document();
            doc_doc.add(new StringField("id", docID, Field.Store.YES));
            doc_doc.add(new TextField("title", title, Field.Store.NO));
            doc_doc.add(new TextField("content", tt.getText(), Field.Store.NO));
            doc_writer.addDocument(doc_doc);

            logger.log(Level.FINE, "Found {0} temporal events", tt.getEvents().size());

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

}
