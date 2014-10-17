Analyze Filters
==========

The analyzer was designed to take the output from MetaTagger and report on how well the 
Author Email Institution Matching and the Citation Tagging Filters performed. 
-----
 
#### How to run the Analzyer

    Program Usage:
      FilterAnalyzer -d <directory> -r <csv results file> [-f <outfilename>]
             -a    CSV results file. One row per file - AuthorEmailTagging Filter uses this one.
             -c    CSV results file. One row per file - CitationTagginFilter uses this one.
             -d    directory where processed files are. Will only operate on *.meta.xml files
             -f    optional filename specifying where to print results to. Default is stdout


There is a script in meta-tagger/bin/

    runAnalyze -d "directory" 

The resulting analysis file is human readable.

It can also be view via CICWebAnalysis.php script located in meta-tagger/bin. 

