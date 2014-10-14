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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.CompressorException;

/**
 *
 * @author pierpaolo
 */
public class Wikidump2Text {

    //private static String encoding = "ISO-8859-1";
    private static String encoding = "UTF-8";

    //private static final String notValidTitle = "^[A-Za-z\\s_-]+:.*$";
    private static final String[] prefixItPage = new String[]{"Discussione:", "Utente:", "Discussioni utente:", "Wikipedia:", "Discussioni Wikipedia:",
        "File:", "Discussioni file:", "MediaWiki:", "Discussioni MediaWiki:", "Template:", "Discussioni template:", "Aiuto:", "Discussioni aiuto:",
        "Categoria:", "Discussioni categoria:", "Portale:", "Discussioni portale:", "Progetto:", "Discussioni progetto:"};

    private static boolean isSpecialNamespace(String[] prefix, String title) {
        for (String p : prefix) {
            if (title.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 1) {
            int counter = 0;
            try {
                if (args.length > 2) {
                    encoding = args[2];
                }
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(args[1])), "UTF-8"));
                WikipediaDumpIterator it = new WikipediaDumpIterator(new File(args[0]), encoding);
                while (it.hasNext()) {
                    WikiPage wikiPage = it.next();
                    ParsedPage parsedPage = wikiPage.getParsedPage();
                    if (parsedPage != null) {
                        String title = wikiPage.getTitle();
                        if (!isSpecialNamespace(prefixItPage, title)) {
                            if (parsedPage.getText() != null) {
                                writer.append(parsedPage.getText());
                                writer.newLine();
                                counter++;
                                if (counter % 10000 == 0) {
                                    System.out.println(counter);
                                    writer.flush();
                                }
                            }
                        }
                    }
                }
                writer.flush();
                writer.close();
            } catch (XMLStreamException | CompressorException | IOException ex) {
                Logger.getLogger(Wikidump2Text.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Indexed pages: " + counter);
        }
    }

}
