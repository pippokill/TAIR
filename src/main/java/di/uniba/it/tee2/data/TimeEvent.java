/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.data;

import java.util.Date;

/**
 *
 * @author pierpaolo
 */
public class TimeEvent {

    private int startOffset;

    private int endOffset;

    private Date date;

    private String dateString;
    
    private String eventString;

    public TimeEvent(int startOffset, int endOffset, Date date) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.date = date;
    }

    public TimeEvent(int startOffset, int endOffset, String dateString) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.dateString = dateString;
    }

    public TimeEvent(int startOffset, int endOffset, Date date, String dateString) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.date = date;
        this.dateString = dateString;
    }

    public TimeEvent() {
    }

    public String getEventString() {
        return eventString;
    }

    public void setEventString(String eventString) {
        this.eventString = eventString;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

}
