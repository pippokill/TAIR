/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.wiki;

/**
 *
 * @author pierpaolo
 */
public class PageCleanerWrapper {

    private static PageCleaner instance;

    public static synchronized PageCleaner getInstance(String language) {
        if (instance == null) {
            switch (language) {
                case "italian":
                    instance = new ItalianPageCleaner();
                    break;
                case "english":
                    instance = new EnglishPageCleaner();
                    break;
            }
        }
        return instance;
    }
}
