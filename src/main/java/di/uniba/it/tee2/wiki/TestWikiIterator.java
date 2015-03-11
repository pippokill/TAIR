/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.wiki;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.compress.compressors.CompressorException;

/**
 *
 * @author pierpaolo
 */
public class TestWikiIterator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            WikipediaDumpIterator it=new WikipediaDumpIterator(new File(args[0]), "UTF-8");
            while (it.hasNext()) {
                WikiPage page = it.next();
                System.out.println(page.getWikiID());
                System.out.println(page.getRevisionID());
                System.out.println(page.getTitle());
                System.out.println();
            }
        } catch (XMLStreamException | CompressorException | IOException ex) {
            Logger.getLogger(TestWikiIterator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
