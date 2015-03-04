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

import di.uniba.it.tee2.index.TemporalEventIndexingTS;
import di.uniba.it.tee2.util.Counter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.compress.compressors.CompressorException;

/**
 *
 * @author pierpaolo
 */
public class Wikidump2IndexMT {

    public static final String notValidTitle = "^[A-Za-z\\s_-]+:[A-Za-z].*$";

    private static int minTextLegth = 4000;

    private static final Logger logger = Logger.getLogger(Wikidump2IndexMT.class.getName());

    private TemporalEventIndexingTS tee;

    //public static final String defaultEncoding = "ISO-8859-1";
    private static String encoding = "UTF-8";

    private int numberOfThreads = 4;

    private static int pageLimit = Integer.MAX_VALUE;

    public static BlockingQueue<WikiPage> pages = new ArrayBlockingQueue<>(1000);

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
            WikipediaDumpIterator wikiIterator = new WikipediaDumpIterator(xmlDump, encoding);
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                Thread thread = new IndexThread(tee, language, minTextLegth);
                threads.add(thread);
                thread.start();
            }
            int counter = 0;
            while (wikiIterator.hasNext() && pages.size() < pageLimit) {
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

    static final Options options;

    static CommandLineParser cmdParser = new BasicParser();

    static {
        options = new Options();
        options.addOption("l", true, "language (italian, english)")
                .addOption("d", true, "wikiepdia dump file")
                .addOption("o", true, "output index directory")
                .addOption("m", true, "min text length (optional, default 4000 characters)")
                .addOption("n", true, "number of threads (optional, default 2)")
                .addOption("e", true, "charset encoding (optional, default UTF-8)")
                .addOption("p", true, "limit indexed pages (optional, default no limit)");
    }

    /**
     * language xml_dump output_dir n_thread encoding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("l") && cmd.hasOption("d") && cmd.hasOption("o")) {
                encoding = cmd.getOptionValue("e", "UTF-8");
                minTextLegth = Integer.parseInt(cmd.getOptionValue("m", "4000"));
                if (cmd.hasOption("p")) {
                    pageLimit = Integer.parseInt(cmd.getOptionValue("p"));
                }
                int nt = Integer.parseInt(cmd.getOptionValue("n", "2"));
                Wikidump2IndexMT builder = new Wikidump2IndexMT();
                builder.init(cmd.getOptionValue("l"), cmd.getOptionValue("o"), nt);
                builder.build(cmd.getOptionValue("d"), cmd.getOptionValue("l"));
            } else {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("Index Wikipedia dump (multi threads)", options, true);
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidump2IndexMT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
