/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.text;

/**
 *
 * @author pierpaolo
 */
public class TxtDoc {
    
    private String title;
    
    private String text;
    
    private String filename;

    public TxtDoc(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public TxtDoc(String title, String text, String filename) {
        this.title = title;
        this.text = text;
        this.filename = filename;
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    
    
}
