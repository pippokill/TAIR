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
package di.uniba.it.tee2.wiki;

import di.uniba.it.tee2.TemporalEventIndexingTS;
import di.uniba.it.tee2.util.Counter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.CompressorException;

/**
 *
 * @author pierpaolo
 */
public class Wikidump2IndexMT {

    public static final String notValidTitle = "^[A-Za-z\\s_-]+:[A-Z][a-z].*$";

    private int minTextLegth = 4000;

    private static final Logger logger = Logger.getLogger(Wikidump2IndexMT.class.getName());

    private TemporalEventIndexingTS tee;

    //public static final String defaultEncoding = "ISO-8859-1";
    private static final String defaultEncoding = "UTF-8";

    private int numberOfThreads = 4;

    public static BlockingQueue<WikiPage> pages = new ArrayBlockingQueue<>(1000);

    public int getMinTextLegth() {
        return minTextLegth;
    }

    public void setMinTextLegth(int minTextLegth) {
        this.minTextLegth = minTextLegth;
    }

    public void init(String lang, String mainDir, int nt) throws Exception {
        tee = new TemporalEventIndexingTS();
        tee.init(lang, mainDir);
        this.numberOfThreads = nt;
    }

    public void build(String xmlDumpFilename, String language) throws Exception {
        build(new File(xmlDumpFilename), language);
    }

    private void build(File xmlDump, String language) throws Exception {
        try {
            Counter.init();
            WikiPage poisonPage = new WikiPage();
            poisonPage.setTitle("***POISON_PAGE***");
            WikipediaDumpIterator wikiIterator = new WikipediaDumpIterator(xmlDump, defaultEncoding);
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                Thread thread = new IndexThread(tee, language, minTextLegth);
                threads.add(thread);
                thread.start();
            }
            int counter = 0;
            while (wikiIterator.hasNext()) {
                try {
                    WikiPage wikiPage = wikiIterator.next();
                    String title = wikiPage.getTitle();
                    if (!title.matches(notValidTitle)) {
                        wikiPage.setTitle(title);
                        Wikidump2IndexMT.pages.put(wikiPage);
                        counter++;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Wikidump2IndexMT.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            for (int i = 0; i < numberOfThreads; i++) {
                pages.put(poisonPage);
            }

            for (int i = 0; i < numberOfThreads; i++) {
                threads.get(i).join();
            }

            logger.log(Level.INFO, "Extracted pages: {0}", counter);
            logger.log(Level.INFO, "Indexed pages: {0}", Counter.get());
            wikiIterator.close();
            tee.close();

        } catch (XMLStreamException | FileNotFoundException | CompressorException ex) {
            logger.log(Level.SEVERE, "Error to build index", ex);
        }

    }

    /**
     * language xml_dump output_dir n_thread encoding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            if (args.length > 3) {
                Wikidump2IndexMT builder = new Wikidump2IndexMT();
                builder.init(args[0], args[2], Integer.parseInt(args[3]));
                builder.build(args[0], args[1]);
            } else {
                throw new Exception("No valid arguments");
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidump2IndexMT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
