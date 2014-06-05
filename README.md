#### Rexa metatagger: scientific paper header and reference extraction.

Metatagger is a system which consumes the output of the Rexa's
[pstotext](https://github.com/iesl/rexa1-pstotext) tool, and produces an annotated version of the
text, and writes the results to an XML file. The system is structured a series of pipelined
components, each performing some task, such as layout analysis (e.g., header block, abstract,
body text), or finer-grained labelling, such as identifying reference fields.

The stablest and most mature of the pipeline components are the coarse segementation system, the
header field labeler, and the reference field labeler. Other components which are in various
states of development include an "acknowledgements" section labeller, grant number/granting
institution labelling, and citations-in-context identification (i.e., identifying the points in the
document where the reference markers appear).


![Alt text](./docs/img/pdf-and-meta-hdr.png)

Header fields include: 
   + title
   + authors, author, author-first, author-middle, author-last,
   + institution
   + address
   + email
   + abstract

![Alt text](./docs/img/pdf-and-meta-ref.png)

Reference fields include:
   + address
   + author, author-first, author-last, authors
   + conference
   + date
   + journal
   + note
   + pages
   + publisher
   + reference
   + ref-marker
   + series
   + title
   + volume
   + web

Each reference XML tag is given an id, such as `refID="p12x82.0y405.0"`, which uniquely identifies
that reference by page number and (x, y) page position in postscript coordinates.


#### Compiling

./sbt compile

#### Running

Basic usage: cat input-file-list | bin/runcrf

The wrapper script *runcrf* expects a list of filename inputs and output filenames, one per line,
like so:


```
/path/to/input/file.xml -> /path/to/output/file.output.xml
```

For example, the following command will create the necessary input files from the included samples:
```
17:16:29 > find ./pstotext-samples -type f  -name '*.pstotext.xml' -printf "%p -> %p.tagged.xml\n"
pstotext-samples/0036C69A8021179B87B8703EE912685CE9DF6606.pdf.pstotext.xml -> pstotext-samples/0036C69A8021179B87B8703EE912685CE9DF6606.pdf.pstotext.xml.tagged.xml
pstotext-samples/003E74C5D7BA741455E5F4659D9BBF4F7240BCF6.pdf.pstotext.xml -> pstotext-samples/003E74C5D7BA741455E5F4659D9BBF4F7240BCF6.pdf.pstotext.xml.tagged.xml
pstotext-samples/0035A6C7E94004CE1FF6FA8BE4A64B57D69C9791.ps.pstotext.xml -> pstotext-samples/0035A6C7E94004CE1FF6FA8BE4A64B57D69C9791.ps.pstotext.xml.tagged.xml
...
```
Running that same command and piping into runcrf will run the tagger:

```
find pstotext-samples -type f  -name '*.pstotext.xml' -printf "%p -> %p.whatever.xml\n" | bin/runcrf
```

