/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.api;

import di.uniba.it.tee2.extraction.TemporalExtractor;
import di.uniba.it.tee2.search.TemporalEventSearch;
import java.io.IOException;

/**
 *
 * @author pierpaolo
 */
public class SearchServiceWrapper {

    private TemporalExtractor te;

    private TemporalEventSearch search;
    
    private static SearchServiceWrapper instance;

    private SearchServiceWrapper(String language, String maindir) throws IOException {
        te = new TemporalExtractor(language);
        te.init();
        search = new TemporalEventSearch(maindir, te);
        search.init();
    }
    
    public static synchronized SearchServiceWrapper getInstance(String language, String maindir) throws IOException {
        if (instance==null) {
            instance=new SearchServiceWrapper(language, maindir);
        }
        return instance;
    }

    public TemporalEventSearch getSearch() {
        return search;
    }

}
