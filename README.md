Extending and Information Retrieval System through Time Event Extraction (alpha version)
========================================================================================

General info
------------

This software implements an Information Retrieval system (TAIR) able to manage temporal information. The system allows temporal constraints in a classical keyword-based search.

Details about the algorithm will be published in the following paper:

*Pierpaolo Basile, Annalina Caputo and Giovanni Semeraro, Lucia Siciliani*. **Extending and Information Retrieval System through Time Event Extraction**

**(NOTE citation details are not yet available)**

Usage
-----
To run TAIR, you must execute the script run.sh in the main directory. 
The script requires the following parameters: *index_language* *index_dir* *shell_encoding (optional)*

To index a Wikipedia dump, you must execute the script index.sh in the main directory.
The script requires the following parameters: *language* *xml_dump_file* *output_dir* *n_thread* *dump_encoding (optional)*