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

import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import di.uniba.it.tee2.index.TemporalEventIndexing;
import java.io.File;
import java.io.FileNotFoundException;
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
@Deprecated
public class Wikidump2Index {

    private static final String notValidTitle = "^[A-Za-z\\s_-]+:[A-Za-z].*$";

    private static int minTextLegth = 4000;

    private static final Logger logger = Logger.getLogger(Wikidump2Index.class.getName());

    private TemporalEventIndexing tee;

    private static String encoding = "UTF-8";

    public void init(String lang, String mainDir) throws Exception {
        tee = new TemporalEventIndexing();
        tee.init(lang, mainDir);
    }

    public void build(String xmlDumpFilename, String language) throws Exception {
        build(new File(xmlDumpFilename), language);
    }

    private void build(File xmlDump, String language) throws Exception {
        try {
            WikipediaDumpIterator wikiIterator = new WikipediaDumpIterator(xmlDump, encoding);
            PageCleaner cleaner = PageCleanerWrapper.getInstance(language);
            int counter = 0;
            Integer docID = 0;
            while (wikiIterator.hasNext()) {
                WikiPage wikiPage = wikiIterator.next();
                ParsedPage parsedPage = wikiPage.getParsedPage();
                String title = wikiPage.getTitle();
                if (!title.matches(notValidTitle)) {
                    if (parsedPage != null) {
                        String text = cleaner.clean(parsedPage.getText());
                        logger.log(Level.FINE, "Process doc {0}", title);
                        if (text.length() > this.minTextLegth) {
                            try {
                                tee.add(title, text, title, docID.toString());
                                docID++;
                                if (docID % 1000 == 0) {
                                    logger.log(Level.INFO, "Indexed pages: {0}", docID);
                                }
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, "Error to index page (skip page) " + title, ex);
                            }
                        }
                    }
                    counter++;
                }
            }

            logger.log(Level.INFO, "Extracted pages: {0}", counter);
            logger.log(Level.INFO, "Indexed pages: {0}", docID);
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
                .addOption("e", true, "charset encoding (optional, default UTF-8)");
    }

    /**
     * language xml_dump output_dir encoding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("l") && cmd.hasOption("d") && cmd.hasOption("o")) {
                encoding = cmd.getOptionValue("e", "UTF-8");
                minTextLegth=Integer.parseInt(cmd.getOptionValue("m","4000"));
                Wikidump2Index builder = new Wikidump2Index();
                builder.init(cmd.getOptionValue("l"), cmd.getOptionValue("o"));
                builder.build(cmd.getOptionValue("d"), cmd.getOptionValue("l"));
            } else {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("Index Wikipedia dump (single thread)", options, true);
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidump2Index.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
