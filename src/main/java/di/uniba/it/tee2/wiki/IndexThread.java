/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.wiki;

import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import di.uniba.it.tee2.TemporalEventIndexing;
import static di.uniba.it.tee2.wiki.Wikidump2IndexMT.notValidTitle;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class IndexThread extends Thread {

    private static final Logger logger = Logger.getLogger(IndexThread.class.getName());

    private final int minTextLegth;

    private final TemporalEventIndexing tee;

    public IndexThread(TemporalEventIndexing tee, int minTextLegth) {
        this.tee = tee;
        this.minTextLegth = minTextLegth;
    }

    @Override
    public void run() {
        boolean run = true;
        while (run) {
            try {
                WikiPage wikiPage = Wikidump2IndexMT.pages.take();
                if (!wikiPage.getTitle().equals("***POISON_PAGE***")) {
                    ParsedPage parsedPage = wikiPage.getParsedPage();
                    if (parsedPage != null) {
                        String text = parsedPage.getText();
                        logger.log(Level.INFO, "Process doc {0}", wikiPage.getTitle());
                        if (text.length() > this.minTextLegth) {
                            try {
                                int docid = Wikidump2IndexMT.incrementDoc();
                                tee.add(text, wikiPage.getTitle(), String.valueOf(docid));
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, "Error to index page (skip page) " + wikiPage.getTitle(), ex);
                            }
                        }
                    }
                } else {
                    run = false;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(IndexThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
