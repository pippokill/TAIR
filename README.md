Extending and Information Retrieval System through Time Event Extraction (alpha version)
========================================================================================

General info
------------

This software implements an Information Retrieval system (TAIR) able to manage temporal information. The system allows temporal constraints in a classical keyword-based search.

Details about the algorithm are published in the following paper:

@inproceedings {dart2014ws,
	title            = "Extending an information retrieval system through time event extraction",
	year             = "2014",
	author           = "P Basile and A Caputo and G Semeraro and L Siciliani",
	booktitle        = "8th International Workshop on Information Filtering and Retrieval, DART 2014, Co-located with XIII AI*IA Symposium on Artificial Intelligence, AI*IA 2014",
	pages            = "36-47",
	publisher        = "CEUR Workshop Proceedings",
	volume           = "1314",
        ee               = "http://ceur-ws.org/Vol-1314/paper-04.pdf"
}

Please, cite our paper if you use our software.

Installation
------------
Install in your local maven repositery heideltime library. Go in the foder lib3rd/ and run the shell script install.sh

Compile our software running the maven command: mvn package.

Download the TreeTagger and its tagging scripts, installation scripts, as well as English, Italian parameter files into one directory from: http://www.ims.uni-stuttgart.de/projekte/corplex/TreeTagger/
      - mkdir treetagger 
      - cd treetagger
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tree-tagger-linux-3.2.tar.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tagger-scripts.tar.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/install-tagger.sh
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/english-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/italian-par-linux-3.2-utf8.bin.gz
Attention: If you do not use Linux, please download all TreeTagger files directly from http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/

Install the TreeTagger
      - sh install-tagger.sh

Open and edit the config.props file, set the *treeTaggerHome* parameter with the directorty where TreeTagger is installed.

Usage
-----
To run TAIR, you must execute the script run.sh in the main directory. 
The script requires the following parameters: *index_language* *index_dir* *shell_encoding (optional)*

To index a Wikipedia dump, you must execute the script index.sh in the main directory.
The script requires the following parameters: *language* *xml_dump_file* *output_dir* *n_thread* *dump_encoding (optional)*