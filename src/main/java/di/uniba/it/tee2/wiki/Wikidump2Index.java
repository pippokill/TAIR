/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.wiki;

import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import di.uniba.it.tee2.TemporalEventExtraction;
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

    private TemporalEventExtraction tee;

    private static final String defaultEncoding = "ISO-8859-1";

    public int getMinTextLegth() {
        return minTextLegth;
    }

    public void setMinTextLegth(int minTextLegth) {
        this.minTextLegth = minTextLegth;
    }

    public void init(String lang, String timeIndexDir, String docIndexDir) throws Exception {
        tee = new TemporalEventExtraction();
        tee.init(lang, timeIndexDir, docIndexDir);
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
                if (!title.matches(notValidTitle)) {
                    if (parsedPage != null) {
                        String text = parsedPage.getText();
                        logger.log(Level.INFO, "Process doc {0}", title);
                        if (text.length() > this.minTextLegth) {
                            try {
                                tee.add(text, title, docID.toString());
                                docID++;
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, "Error to index page", ex);
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Wikidump2Index builder = new Wikidump2Index();
            builder.init(args[1], args[2], args[3]);
            if (args.length == 4) {
                builder.build(args[0], defaultEncoding);
            } else if (args.length > 4) {
                builder.build(args[0], args[4]);
            } else {
                throw new Exception("No valid arguments");
            }
        } catch (Exception ex) {
            Logger.getLogger(Wikidump2Index.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
