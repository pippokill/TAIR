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
package di.uniba.it.tee2.text;

import di.uniba.it.tee2.wiki.*;
import di.uniba.it.tee2.index.TemporalEventIndexingTS;
import di.uniba.it.tee2.util.Counter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 *
 * @author pierpaolo
 */
public class TextDirIndex {

    private int minTextLegth = 4000;

    private static final Logger logger = Logger.getLogger(TextDirIndex.class.getName());

    private TemporalEventIndexingTS tee;

    private int numberOfThreads = 4;

    public static BlockingQueue<TxtDoc> pages = new ArrayBlockingQueue<>(1000);

    public int getMinTextLegth() {
        return minTextLegth;
    }

    public void setMinTextLegth(int minTextLegth) {
        this.minTextLegth = minTextLegth;
    }

    public void init(String lang, String mainDir, int nt) throws Exception {
        tee = new TemporalEventIndexingTS();
        tee.init(lang, mainDir);
        this.numberOfThreads = nt;
    }

    public void build(String xmlDumpFilename, String language) throws Exception {
        build(new File(xmlDumpFilename), language);
    }

    private void processFile(File inFile) throws IOException, InterruptedException {
        if (inFile.isDirectory()) {
            File[] files = inFile.listFiles();
            for (File file : files) {
                processFile(file);
            }
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(inFile));
            String title = "";
            StringBuilder content = new StringBuilder();
            if (reader.ready()) {
                title = reader.readLine();
            }
            while (reader.ready()) {
                content.append(reader.readLine()).append("\n");
            }
            pages.put(new TxtDoc(title, content.toString(), inFile.getName()));
        }
    }

    private void build(File startDir, String language) throws Exception {
        try {
            Counter.init();
            TxtDoc poisonPage = new TxtDoc("***POISON_PAGE***", "");
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                Thread thread = new IndexThread(tee, language, minTextLegth);
                threads.add(thread);
                thread.start();
            }
            processFile(startDir);

            for (int i = 0; i < numberOfThreads; i++) {
                pages.put(poisonPage);
            }

            for (int i = 0; i < numberOfThreads; i++) {
                threads.get(i).join();
            }

            logger.log(Level.INFO, "Indexed pages: {0}", Counter.get());
            tee.close();

        } catch (IOException | InterruptedException ex) {
            logger.log(Level.SEVERE, "Error to build index", ex);
        }

    }

    static final Options options;

    static CommandLineParser cmdParser = new BasicParser();

    static {
        options = new Options();
        options.addOption("l", true, "language (italian, english)")
                .addOption("i", true, "the input directory (files to index)")
                .addOption("o", true, "the output directory")
                .addOption("n", true, "number of threads (optional, default 2)");
    }

    /**
     * language_0 starting_dir_1 output_dir_2 n_thread_3
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("l") && cmd.hasOption("i") && cmd.hasOption("o")) {
                int nt = Integer.parseInt(cmd.getOptionValue("n", "2"));
                TextDirIndex builder = new TextDirIndex();
                builder.init(cmd.getOptionValue("l"), cmd.getOptionValue("o"), nt);
                builder.build(cmd.getOptionValue("l"), cmd.getOptionValue("i"));
            } else {
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp("Index a directory", options, true);
            }
        } catch (Exception ex) {
            Logger.getLogger(TextDirIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
