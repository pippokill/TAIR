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

package di.uniba.it.tee2.wiki;

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants;
import de.tudarmstadt.ukp.wikipedia.parser.ParsedPage;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.FlushTemplates;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParser;
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 *
 * @author Piero Molino
 */
public class WikipediaDumpIterator implements Iterator<WikiPage> {

    private final XMLStreamReader xmlStreamReader;
    private MediaWikiParser parser;
    private static final Logger logger = Logger.getLogger(WikipediaDumpIterator.class.getName());

    public WikipediaDumpIterator(File xmlFile, String encoding) throws XMLStreamException, FileNotFoundException, CompressorException, IOException {
        MediaWikiParserFactory parserFactory = new MediaWikiParserFactory(WikiConstants.Language.english);
        parserFactory.setTemplateParserClass(FlushTemplates.class);
        parserFactory.setShowImageText(false);
        parserFactory.setShowMathTagContent(false);
        parser = parserFactory.createParser();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        if (xmlFile.getName().endsWith(".bz2")) {
            logger.log(Level.INFO, "Trying to open Wikipedia compress dump (bzip2)...");
            BZip2CompressorInputStream compressIS = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(xmlFile)));
            xmlStreamReader = inputFactory.createXMLStreamReader(compressIS, encoding);
        } else if (xmlFile.getName().endsWith(".gz")) {
            logger.log(Level.INFO, "Trying to open Wikipedia compress dump (gzip)...");
            GZIPInputStream compressIS = new GZIPInputStream(new BufferedInputStream(new FileInputStream(xmlFile)));
            xmlStreamReader = inputFactory.createXMLStreamReader(compressIS, encoding);
        } else {
            logger.log(Level.INFO, "Trying to open Wikipedia plain text dump...");
            xmlStreamReader = inputFactory.createXMLStreamReader(new BufferedInputStream(new FileInputStream(xmlFile)), encoding);
        }
    }

    @Override
    public boolean hasNext() {
        boolean foundNext = false;
        try {
            while (!foundNext && xmlStreamReader.hasNext()) {
                int eventCode = xmlStreamReader.next();
                switch (eventCode) {
                    case XMLStreamConstants.START_ELEMENT:
                        if (xmlStreamReader.getLocalName().equals("page")) {
                            foundNext = true;
                        }
                        break;
                }
            }
        } catch (XMLStreamException ex) {
            logger.log(Level.WARNING, "Error reading the XML stream...return false", ex);
        }
        return foundNext;
    }

    @Override
    public WikiPage next() {
        WikiPage page = new WikiPage();
        try {
            StringBuilder wikimediaText = new StringBuilder();
            StringBuilder title = new StringBuilder();
            boolean finishedParsingArticle = false;
            char lastElement = 'n';
            while (!finishedParsingArticle && xmlStreamReader.hasNext()) {
                int eventCode = xmlStreamReader.next();
                switch (eventCode) {
                    case XMLStreamConstants.START_ELEMENT:
                        switch (xmlStreamReader.getLocalName()) {
                            case "title":
                                lastElement = 't';
                                break;
                            case "text":
                                lastElement = 'c';
                                break;
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        switch (xmlStreamReader.getLocalName()) {
                            case "page":
                                finishedParsingArticle = true;
                                break;
                            case "title":
                                lastElement = 'n';
                                break;
                            case "text":
                                lastElement = 'n';
                                break;
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (lastElement == 't') {
                            title.append(xmlStreamReader.getText());
                        } else if (lastElement == 'c') {
                            wikimediaText.append(xmlStreamReader.getText());
                        }
                        break;
                }
            }

            page.setTitle(title.toString());
            try {
                ParsedPage parsedPage = parser.parse(wikimediaText.toString());
                page.setParsedPage(parsedPage);
            } catch (Exception ex) {
                Logger.getLogger(WikipediaDumpIterator.class.getName()).log(Level.WARNING, "Error to parse page: " + page.getTitle(), ex);
            }
        } catch (XMLStreamException ex) {
            Logger.getLogger(WikipediaDumpIterator.class.getName()).log(Level.WARNING, "Error reading XML stream", ex);
        }
        return page;
    }

    @Override
    public void remove() {
    }

    public void close() {
        parser = null;
        try {
            xmlStreamReader.close();
        } catch (XMLStreamException ex) {
            Logger.getLogger(WikipediaDumpIterator.class.getName()).log(Level.SEVERE, "Error reading XML stream", ex);
        }
    }
}
