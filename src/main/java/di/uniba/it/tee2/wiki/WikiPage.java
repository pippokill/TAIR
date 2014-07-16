/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.wiki;


import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;

/**
 *
 * @author pierpaolo
 */
public class WikiPage {
    
    private String title;
    
    private String text;
    
    private ParsedPage parsedPage;

    public ParsedPage getParsedPage() {
        return parsedPage;
    }

    public void setParsedPage(ParsedPage parsedPage) {
        this.parsedPage = parsedPage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public WikiPage() {
    }

    
    
    
    
}
