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
package di.uniba.it.tee2.extraction;

import di.uniba.it.tee2.util.TEEUtils;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import di.uniba.it.tee2.data.TaggedText;
import di.uniba.it.tee2.data.TimeEvent;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author pierpaolo
 */
public class TemporalExtractor {

    private HeidelTimeStandalone heidelTagger;

    private static final Logger logger = Logger.getLogger(TemporalExtractor.class.getName());

    private final Language langObj;

    public TemporalExtractor(String language) {
        langObj = Language.getLanguageFromString(language);
    }

    public void init() {
        heidelTagger = new HeidelTimeStandalone(langObj, DocumentType.NARRATIVES, OutputType.TIMEML, "config.props");
    }

    public String getLanguage() {
        return langObj.getName();
    }

    public TaggedText process(String text) throws Exception {
        Date currentTime = Calendar.getInstance(TimeZone.getDefault()).getTime();
        TaggedText taggedText = new TaggedText();
        taggedText.setText(text);
        String timemlOutput = heidelTagger.process(text, currentTime);
        taggedText.setTaggedText(timemlOutput);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(timemlOutput)));

        StringBuilder sb = new StringBuilder();
        NodeList timemlNodes = doc.getElementsByTagName("TimeML");
        for (int i = 0; i < timemlNodes.getLength(); i++) {
            NodeList childs = timemlNodes.item(i).getChildNodes();
            for (int j = 0; j < childs.getLength(); j++) {
                Node child = childs.item(j);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    sb.append(child.getTextContent());
                } else if (child.getNodeName().equals("TIMEX3")) {
                    String timeText=child.getTextContent();
                    String timeValueString = child.getAttributes().getNamedItem("value").getNodeValue();
                    String normalizedTime=null;
                    try {
                        normalizedTime = TEEUtils.normalizeTime(timeValueString);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Error to normalize time: ", ex);
                    }
                    if (normalizedTime!=null) {
                        TimeEvent event=new TimeEvent(sb.length(), sb.length()+timeText.length(), normalizedTime);
                        event.setEventString(timeText);
                        taggedText.getEvents().add(event);
                    }
                    sb.append(timeText);
                }
                //VERBOSE
                //System.out.println(child.getNodeType() + "\t" + child.getNodeName() + "\t" + child.getTextContent());
                //System.out.println();
            }
        }
        taggedText.setText(sb.toString());
        return taggedText;
    }

    public void close() {
        heidelTagger = null;
        System.gc();
    }

}
