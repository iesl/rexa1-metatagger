#!/usr/bin/perl -w
use strict; 
use Getopt::Long qw(:config bundling no_ignore_case pass_through);
use POSIX;
use FindBin qw($Bin $RealBin);


#======================= [ Forward Decls ] =======================
sub runCmd($;$);


#======================= [ syntax ] =======================
sub syntax () {
  print <<EOS;
This script should be run from a root directory containing subdirectories for: 
pstotext
rexa-textmill
work
EOS
}


#======================= [  ] =======================
my %options=();
GetOptions(
	   "setup"       => \$options{setup},
	  );

{
  my $bindir = $RealBin;
  $bindir =~ s!/$!!;
  my $workroot = "work";

  -d 'rexa-textmill' or die "no rexa-textmill distribution available";
  -d 'work' or die "work directory doesn't exist"; 

  my $taskid = 1;
  $taskid = $ENV{"SGE_TASK_ID"} if ( defined( $ENV{"SGE_TASK_ID"} ) );

  # select/lock n sources from cms
  my $machine = `uname -n`;
  my $fetchCount = 50;
  chomp $machine;
  my $nodeid = "$machine.$taskid";
  my $nodeWorkDir = "$workroot/$nodeid";

  mkdir "$nodeWorkDir" or die "can't create dir $nodeWorkDir: $!" unless -d $nodeWorkDir;
  mkdir "$nodeWorkDir/pdfs" or die "can't create dir $nodeWorkDir/pdfs: $!" unless -d "$nodeWorkDir/pdfs";
  mkdir "$nodeWorkDir/crf-output" or die "can't create dir $nodeWorkDir/crf-output: $!" unless -d "$nodeWorkDir/crf-output";
    
  while ( 1 ) {
    # check for exit signal (database state?)
    ## Todo..

    # delete local files
    runCmd( "rm -r $nodeWorkDir/pdfs" );
    runCmd( "rm -r $nodeWorkDir/crf-output" );
    runCmd( "rm -r $nodeWorkDir/lockedfiles.txt" );

    runCmd( "./dist-cms/cms.pl --update-sources availability=crf.pending -Davailability=$nodeid --number $fetchCount");
    open my $LFH, "> $nodeWorkDir/lockedfiles.txt" or die "can't write file: $!";
    runCmd( "./dist-cms/cms.pl --select-sources availability=$nodeid", $LFH );
    close $LFH;

    # copy sources to local fs
    my $l;
    open my $FH, "< $nodeWorkDir/lockedfiles.txt" or die "can't read file: $!";
    while ($l = <$FH>) {
      chomp $l;
      next unless $l =~ /^[a-fA-F\d]{40}/;
      my $sha = substr( $l, 0, 40 );
      my $pre = substr( $sha, 0, 4 );
      my $fpath = join( '/', split( //, $pre ) );
      my $file = "$fpath/$sha";
      my $rsyncURL = "rsync://adam@"."vinci5.cs.umass.edu/text-output/$file.pstotext.xml";
      my $rsyncCmd = "rsync --port 1199 --archive --relative $rsyncURL $nodeWorkDir/pdfs/";
      runCmd( "$rsyncCmd" );
    }

    -d "$nodeWorkDir/pdfs" or die "no more files to process.. exiting";
      
    my $path = $nodeWorkDir;
      
    my $logfile = "$nodeWorkDir/crf.log.$nodeid";
      
    # count files
    runCmd( "(find $path/text -type f -name '*.document' | wc --lines)" );

    # unzip any gzipped files
    runCmd( "find $path/pdfs -type f -name '*.gz' -exec gunzip {} ';' " );

    ## build a list of files to process, in the form expected by crf MetaTagger
    runCmd( "find $path/pdfs -type f -name '*.xml' -printf '\%p -> \%p.crf.xml' > $nodeWorkDir/crfinput.txt" );

    runCmd( "cat $nodeWorkDir/crfinput.txt | ./rexa-textmill/bin/runcrf > $nodeWorkDir/crf.$nodeid.log" );
      
    # insert fulltext mentions into repository
    my $importMentionArgs = "-Dmention.type=fulltext.mention";
    runCmd( "find $path/crf-output -type f -name '*.crf.xml' -exec ./dist-cms/cms.pl --import-mention {} --url {} $importMentionArgs ';'" );
    runCmd( "find $path/crf-output -type f -name '*.crf.xml' -exec ./dist-cms/cms.pl --update-source {} -Davailability=pipeline.processing ';'" );
    runCmd( "./dist-cms/cms.pl --update-sources availability=$nodeid -Davailability=pstotext.error --number $fetchCount ';'" );
    # die "debug: only running one iteration...";

  }






  #======================= [  ] =======================
  sub runCmd($;$) {
    my ($cmd, $ohandle) = @_;
    print( "running $cmd\n" );
    open( CH, "$cmd |" );
    my $nl;
    while (($nl=<CH>)) {
      print $ohandle $nl if defined $ohandle;
      print $nl;
    }
    close CH;
  }

}

