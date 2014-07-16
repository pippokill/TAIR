/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.lucene.document.DateTools;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author pierpaolo
 */
public class TEEUtils {

    /**
     * @param oldDate
     * @return
     * @throws java.text.ParseException
     */
    public static String normalizeTime(String oldDate) throws Exception {
        Date now = new Date();
        if (oldDate.equals("PRESENT_REF")) {
            oldDate = new SimpleDateFormat("yyyy-MM-dd").format(now);
        } else if (oldDate.endsWith("FA")) {
            oldDate = oldDate.replace("FA", "09-23");
        } else if (oldDate.endsWith("WI")) {
            oldDate = oldDate.replace("WI", "12-21");
        } else if (oldDate.endsWith("SU")) {
            oldDate = oldDate.replace("SU", "06-20");
        } else if (oldDate.endsWith("SP")) {
            oldDate = oldDate.replace("SP", "03-20");
        } else if (oldDate.equals("PAST_REF") || (oldDate.endsWith("WE"))) {
            return "";
        } else if (oldDate.equals("FUTURE_REF")) {
            return new SimpleDateFormat("yyyyMMdd").format(now);
        }

        DateTime oldD = DateTime.parse(oldDate);

        DateTimeFormatter OldDFmt = ISODateTimeFormat.date();
        String str = OldDFmt.print(oldD);

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(str);
        String newDate = DateTools.dateToString(date, DateTools.Resolution.DAY);

        return newDate;
    }
    
 

}
