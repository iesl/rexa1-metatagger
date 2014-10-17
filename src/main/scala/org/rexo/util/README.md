Analyze Filters
==========

The analyzer was designed to take the output from MetaTagger and report on how well a 
few of the filters are able to find things in the meta tagger data. 

Currently it will analyze results from two filters:

 * Author/Email/Institution Matching Filter
 * Citations In Context Filter
   
-----

####Author/Email/Institution Matching Filter

This filter requires labelled data to run. 

It is expecting the following columns in the CSV file handed to it: 

    Filename,applicable,AUTHOR_1,EMAIL_1,INSTITUTE_1,AUTHOR_2,EMAIL_2,INSTITUTE_2,AUTHOR_3,EMAIL_3,INSTITUTE_3,AUTHOR_4,EMAIL_4,INSTITUTE_4,AUTHOR_5,EMAIL_5,INSTITUTE_5,AUTHOR_6,EMAIL_6,INSTITUTE_6,AUTHOR_7,EMAIL_7,INSTITUTE_7

The applicable field is unused by the program. 
 
####Citations In Context Filter

This filter analyzer does not require labelled data to run.   It will ignore any cvs 
file handed in and just operate on all files located in the directory passed in.  It 
does, however, look for a meta file for each data file, named <pdf>.meta.xml.info, 
where pdf is the name of the file.   The info file contains the Citation Type that 
was found by the filter.

---

#### How to run the Analyzer

    Program Usage:
      FilterAnalyzer -d <directory> -r <csv results file> [-f <outfilename>]
             -a    CSV results file. One row per file - AuthorEmailTagging Filter uses this one.
             -c    CSV results file. One row per file - CitationTagginFilter uses this one.
             -d    directory where processed files are. Will only operate on *.meta.xml files
             -f    optional filename specifying where to print results to. Default is stdout


There is a script in meta-tagger/bin/

    runAnalyze -d "directory" 

The resulting analysis file is human readable.

It can also be view via CICWebAnalysis.php script located in meta-tagger/bin. Please see the php script itself for information
on how to run it. 

