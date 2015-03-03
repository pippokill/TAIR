/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.util;

import di.uniba.it.tee2.extraction.TemporalExtractor;
import di.uniba.it.tee2.data.TaggedText;
import di.uniba.it.tee2.data.TimeEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class TestTemporalExtractor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            TemporalExtractor extractor = new TemporalExtractor("italian");
            extractor.init();
            TaggedText taggedText = extractor.process("Bedřich Smetana (Litomyšl, 2 marzo 1824 – Praga, 12 maggio 1884) è stato un compositore ceco.\n"
                    + "È conosciuto in particolare per il suo poema sinfonico Vltava (La Moldava in italiano), il secondo in un ciclo di sei che egli intitolò Má vlast (\"La mia patria\") (1874-1879), e per la sua opera La sposa venduta (1866), particolarmente ricca di motivi cechi.");
            List<TimeEvent> events = taggedText.getEvents();
            for (TimeEvent te : events) {
                System.out.println(te.toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(TestTemporalExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
