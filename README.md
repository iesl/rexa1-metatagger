#### Rexa metatagger: scientific paper header and reference extraction.

![Alt text](./docs/img/pdf-and-meta.png)

#### Compiling

ant clean compile

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

