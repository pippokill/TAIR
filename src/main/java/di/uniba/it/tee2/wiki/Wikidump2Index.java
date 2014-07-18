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
import di.uniba.it.tee2.TemporalEventIndexing;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.CompressorException;

/**
 *
 * @author pierpaolo
 */
public class Wikidump2Index {

    private static final String notValidTitle = "^[A-Za-z\\s_-]+:.*$";

    private int minTextLegth = 4000;

    private static final Logger logger = Logger.getLogger(Wikidump2Index.class.getName());

    private TemporalEventIndexing tee;

    private static final String defaultEncoding = "ISO-8859-1";

    public int getMinTextLegth() {
        return minTextLegth;
    }

    public void setMinTextLegth(int minTextLegth) {
        this.minTextLegth = minTextLegth;
    }

    public void init(String lang, String mainDir) throws Exception {
        tee = new TemporalEventIndexing();
        tee.init(lang, mainDir);
    }

    public void build(String xmlDumpFilename, String encoding) throws Exception {
        build(new File(xmlDumpFilename), encoding);
    }

    private void build(File xmlDump, String encoding) throws Exception {
        try {
            WikipediaDumpIterator wikiIterator = new WikipediaDumpIterator(xmlDump, encoding);
            int counter = 0;
            Integer docID = 0;
            while (wikiIterator.hasNext()) {
                WikiPage wikiPage = wikiIterator.next();
                ParsedPage parsedPage = wikiPage.getParsedPage();
                String title = wikiPage.getTitle();
                byte[] bytes = title.getBytes("ISO-8859-1");
                title = new String(bytes);
                if (!title.matches(notValidTitle)) {
                    if (parsedPage != null) {
                        String text = parsedPage.getText();
                        logger.log(Level.INFO, "Process doc {0}", title);
                        if (text.length() > this.minTextLegth) {
                            try {
                                tee.add(text, title, docID.toString());
                                docID++;
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, "Error to index page (skip page) " + title, ex);
                            }
                        }
                    }
                    counter++;
                }
                if (docID == 100) {
                    break;
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

    /**
     * language xml_dump output_dir encoding
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Wikidump2Index builder = new Wikidump2Index();
            builder.init(args[0],args[2]);
            if (args.length == 3) {
                builder.build(args[1], defaultEncoding);
            } else if (args.length > 3) {
                builder.build(args[1], args[3]);
            } else {
                throw new Exception("No valid arguments");
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidump2Index.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
