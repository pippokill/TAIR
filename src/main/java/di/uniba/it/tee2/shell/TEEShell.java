/**
 * Copyright (c) 2014, the Temporal Random Indexing AUTHORS.
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
package di.uniba.it.tee2.shell;

import di.uniba.it.tee2.TemporalExtractor;
import di.uniba.it.tee2.search.SearchResult;
import di.uniba.it.tee2.search.TemporalEventSearch;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Interactive command line tool to access a database using JDBC.
 *
 * @h2.resource
 */
public class TEEShell {

    private static final int HISTORY_COUNT = 20;
    private static final String PROMPT = "TEE2";
    private InputStream in = System.in;
    private BufferedReader reader;
    private final List<String> history = new ArrayList();
    public static final String VERSION = "2.00a";
    private static final String DEFAULT_CHARSET = "ISO-8859-1";
    private final String charset;
    static final Options options;

    static CommandLineParser cmdParser = new BasicParser();

    static {
        options = new Options();
        options.addOption("l", true, "language (italian, english)")
                .addOption("d", true, "the index directory")
                .addOption("e", true, "charset encoding (optional)");
    }
    /**
     * The output stream where this tool writes to.
     */
    public static PrintStream out = System.out;

    private TemporalExtractor te;

    private TemporalEventSearch search;

    /**
     * Sets the standard output stream.
     *
     * @param out the new standard output stream
     */
    public void setOut(PrintStream out) {
        TEEShell.out = out;
    }

    public TEEShell(String language, String maindir, String charset) throws IOException {
        te = new TemporalExtractor(language);
        te.init();
        search = new TemporalEventSearch(maindir, te);
        search.init();
        this.charset = charset;
    }

    public TEEShell(String language, String maindir) throws IOException {
        te = new TemporalExtractor(language);
        te.init();
        search = new TemporalEventSearch(maindir, te);
        search.init();
        charset = DEFAULT_CHARSET;
    }

    /**
     * language maindir encoding
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("l") && cmd.hasOption("d")) {
                TEEShell shell = new TEEShell(cmd.getOptionValue("l"), cmd.getOptionValue("d"), cmd.getOptionValue("e", DEFAULT_CHARSET));
                shell.promptLoop();
            } else {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("Run TEE2 console", options, true);
            }
        } catch (ParseException | IOException ex) {
            Logger.getLogger(TEEShell.class.getName()).log(Level.SEVERE, "General error", ex);
        }
    }

    public static void printMessageError(String errorMsg) {
        println("Error: " + errorMsg);
    }

    public static void printException(Exception ex) {
        printMessageError(ex.getMessage());
    }

    /**
     * Redirects the standard input. By default, System.in is used.
     *
     * @param in the input stream to use
     */
    public void setIn(InputStream in) {
        this.in = in;
    }

    /**
     * Redirects the standard input. By default, System.in is used.
     *
     * @param reader the input stream reader to use
     */
    public void setInReader(BufferedReader reader) {
        this.reader = reader;
    }

    private void showHelp() {
        println("Commands are case insensitive");
        println("help or ?      Display this help");
        println("history        Show the last 20 statements");
        println("quit or exit   Close and exit");
        println("");
    }

    private void promptLoop() {
        println("");
        println("Welcome to TEE2 Shell " + VERSION);
        println("Exit with !quit or !exit");

        if (reader == null) {
            try {
                reader = new BufferedReader(new InputStreamReader(in, charset));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TEEShell.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        while (true) {
            try {
                print(PROMPT + "> ");
                String line = readLine();
                if (line == null) {
                    break;
                }
                String cmd = line.trim();
                if (cmd.length() == 0) {
                    continue;
                }
                if ("!exit".equalsIgnoreCase(cmd) || "!quit".equalsIgnoreCase(cmd)) {
                    break;
                } else if (cmd.toLowerCase().equalsIgnoreCase("!help") || cmd.equalsIgnoreCase("!?")) {
                    showHelp();
                } else if ("!history".equals(cmd.toLowerCase())) {
                    for (int i = 0, size = history.size(); i < size; i++) {
                        String s = history.get(i);
                        s = s.replace('\n', ' ').replace('\r', ' ');
                        println("#" + (1 + i) + ": " + s);
                    }
                    if (history.size() > 0) {
                        println("To re-run a statement, type runh and the number and press and enter");
                    } else {
                        println("No history");
                    }
                } else if (cmd.toLowerCase().startsWith("!runh")) {
                    String[] split = cmd.split("\\s+");
                    if (split.length > 1) {
                        if (split[1].matches("[0-9]+")) {
                            int hi = Integer.parseInt(split[1]);
                            hi--;
                            if (hi < history.size()) {
                                executeSearch(history.get(hi));
                            } else {
                                printMessageError("No valid command number");
                            }
                        } else {
                            printMessageError("No valid command number");
                        }
                    } else {
                        printMessageError("runh syntax error");
                    }
                } else {
                    executeSearch(cmd);
                }
            } catch (Exception ex) {
                println("No managed exception sorry...");
                printException(ex);
                exit(1);
            }
        }
        exit(0);
    }

    private void executeSearch(String cmd) {
        try {
            //search
            List<SearchResult> res = null;
            int indexOf = cmd.indexOf("-temp");
            if (indexOf > -1) {
                String q = cmd.substring(0, indexOf).trim();
                String t = cmd.substring(indexOf + 5).trim();
                if (cmd.endsWith("-nl")) {
                    res = search.naturalSearch(q, t.substring(0, t.length() - 3), 10);
                } else {
                    res = search.search(q, t, 10);
                }
            } else {
                String q = cmd.trim();
                res = search.search(q, "", 10);
            }
            for (SearchResult r : res) {
                println(r.toString());
            }
        } catch (Exception ex) {
            printException(ex);
        }
        if (history.size() == HISTORY_COUNT) {
            history.remove(0);
        }
        history.add(cmd);
    }

    /**
     * Print the string without newline, and flush.
     *
     * @param s the string to print
     */
    public static void print(String s) {
        out.print(s);
        out.flush();
    }

    public static void println(String s) {
        out.println(s);
        out.flush();
    }

    private String readLine(String defaultValue) throws IOException {
        String s = readLine();
        return s.length() == 0 ? defaultValue : s;
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Aborted");
        }
        return line;
    }

    private void exit(int code) {
        try {
            search.close();
            te.close();
        } catch (Exception ex) {
            printException(ex);
            System.exit(1);
        }
        System.exit(code);
    }

}
