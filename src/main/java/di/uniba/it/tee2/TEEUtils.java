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
