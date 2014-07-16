/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package di.uniba.it.tee2.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pierpaolo
 */
public class TaggedText {
    
    private String text;
    
    private String taggedText;
    
    private List<TimeEvent> events=new ArrayList<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TimeEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TimeEvent> events) {
        this.events = events;
    }

    public String getTaggedText() {
        return taggedText;
    }

    public void setTaggedText(String taggedText) {
        this.taggedText = taggedText;
    }
    
    
    
}
