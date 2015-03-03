/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.analyzer;

import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

/**
 *
 * @author pierpaolo
 */
public class EnglishNoStemAnalyzer extends StopwordAnalyzerBase {

    private final CharArraySet stemExclusionSet;

    /**
     * Returns an unmodifiable instance of the default stop words set.
     *
     * @return default stop words set.
     */
    public static CharArraySet getDefaultStopSet() {
        return DefaultSetHolder.DEFAULT_STOP_SET;
    }

    /**
     * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
     * class accesses the static final set the first time.;
     */
    private static class DefaultSetHolder {

        static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
    }

    /**
     * Builds an analyzer with the default stop words:
     * {@link #getDefaultStopSet}.
     */
    public EnglishNoStemAnalyzer(Version matchVersion) {
        this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion lucene compatibility version
     * @param stopwords a stopword set
     */
    public EnglishNoStemAnalyzer(Version matchVersion, CharArraySet stopwords) {
        this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop words. If a non-empty stem
     * exclusion set is provided this analyzer will add a
     * {@link SetKeywordMarkerFilter} before stemming.
     *
     * @param matchVersion lucene compatibility version
     * @param stopwords a stopword set
     * @param stemExclusionSet a set of terms not to be stemmed
     */
    public EnglishNoStemAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet) {
        super(matchVersion, stopwords);
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
                matchVersion, stemExclusionSet));
    }

    /**
     * Creates a
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} which
     * tokenizes all the text in the provided {@link Reader}.
     *
     * @return A
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} built
     * from an {@link StandardTokenizer} filtered with null     {@link StandardFilter}, {@link EnglishPossessiveFilter}, 
   *         {@link LowerCaseFilter}, {@link StopFilter}
     *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is provided and
     * {@link PorterStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
            Reader reader) {
        final Tokenizer source = new StandardTokenizer(matchVersion, reader);
        TokenStream result = new StandardFilter(matchVersion, source);
        // prior to this we get the classic behavior, standardfilter does it for us.
        if (matchVersion.onOrAfter(Version.LUCENE_31)) {
            result = new EnglishPossessiveFilter(matchVersion, result);
        }
        result = new LowerCaseFilter(matchVersion, result);
        result = new StopFilter(matchVersion, result, stopwords);
        return new TokenStreamComponents(source, result);
    }
}
