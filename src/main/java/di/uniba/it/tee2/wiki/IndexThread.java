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
import di.uniba.it.tee2.index.TemporalEventIndexingTS;
import di.uniba.it.tee2.util.Counter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class IndexThread extends Thread {

    private static final Logger logger = Logger.getLogger(IndexThread.class.getName());

    private final int minTextLegth;

    private final TemporalEventIndexingTS tee;

    private final String language;

    public IndexThread(TemporalEventIndexingTS tee, String language, int minTextLegth) {
        this.tee = tee;
        this.minTextLegth = minTextLegth;
        this.language = language;
    }

    @Override
    public void run() {
        PageCleaner cleaner = PageCleanerWrapper.getInstance(language);
        boolean run = true;
        while (run) {
            try {
                WikiPage wikiPage = Wikidump2IndexMT.pages.take();
                if (!wikiPage.getTitle().equals("***POISON_PAGE***")) {
                    ParsedPage parsedPage = wikiPage.getParsedPage();
                    if (parsedPage != null) {
                        try {
                            String text = cleaner.clean(parsedPage.getText());
                            logger.log(Level.FINE, "Process doc {0}", wikiPage.getTitle());
                            if (text.length() > this.minTextLegth) {

                                int docid = Counter.increment();
                                tee.add(wikiPage.getTitle(), text, wikiPage.getTitle(), String.valueOf(docid), wikiPage.getWikiID(), wikiPage.getRevisionID());
                            }
                        } catch (Exception ex) {
                            logger.log(Level.SEVERE, "Error to index page (skip page) " + wikiPage.getTitle(), ex);
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
