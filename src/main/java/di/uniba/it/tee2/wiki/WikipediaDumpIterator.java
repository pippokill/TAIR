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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public WikipediaDumpIterator(File xmlFile) throws XMLStreamException, FileNotFoundException, CompressorException, IOException {
        MediaWikiParserFactory parserFactory = new MediaWikiParserFactory(WikiConstants.Language.english);
        parserFactory.setTemplateParserClass(FlushTemplates.class);
        parserFactory.setShowImageText(false);
        parserFactory.setShowMathTagContent(false);
        parser = parserFactory.createParser();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        if (xmlFile.getName().endsWith(".bz2")) {
            logger.log(Level.INFO, "Trying to open compress dumb...");
            BZip2CompressorInputStream compressIS = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(xmlFile)));
            xmlStreamReader = inputFactory.createXMLStreamReader(compressIS, "UTF-8");
        } else {
            logger.log(Level.INFO, "Trying to open plai text dumb...");
            xmlStreamReader = inputFactory.createXMLStreamReader(new BufferedInputStream(new FileInputStream(xmlFile)), "UTF-8");
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
            logger.log(Level.WARNING, "Error reading the XML stream...returning false", ex);
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
