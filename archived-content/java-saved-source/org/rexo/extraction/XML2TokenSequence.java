package org.rexo.extraction;

import edu.umass.cs.mallet.base.extract.StringSpan;
import edu.umass.cs.mallet.base.extract.StringTokenization;
import edu.umass.cs.mallet.base.pipe.Input2CharSequence;
import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.pipe.SerialPipes;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XML2TokenSequence extends Pipe implements Serializable
{
	private static final String DEFAULT_SGML_REGEX = "<((/?[^> ]*)[^>]*)>";
  private String sgmlRegex = DEFAULT_SGML_REGEX;
	private Pattern sgmlPattern = Pattern.compile(sgmlRegex);
	private String lineStart = "LINE_START";
	private String lineEnd = "LINE_END";
	private String lineIn = "LINE_IN";
	private String fontProp = "FONT";


	private CharSequenceLexer lexer;
	private String lexerRegex;
	private String backgroundTag;
  private boolean processingRefs;

   public XML2TokenSequence (CharSequenceLexer lexer, String backgroundTag) {
     this (lexer, backgroundTag, false);
   }

  public XML2TokenSequence (CharSequenceLexer lexer, String backgroundTag, boolean processingRefs)
	{
		this.lexer = lexer;
		this.backgroundTag = backgroundTag;
		this.lexerRegex = lexer.getPattern();
    this.processingRefs = processingRefs;
	}

	public XML2TokenSequence (String regex, String backgroundTag)
	{
		this.lexer = new CharSequenceLexer (regex);
		this.backgroundTag = backgroundTag;
	}

	public XML2TokenSequence ()
	{
		this (new CharSequenceLexer(), "O", false);
	}


	/**
	   If processing references, each token is labeled with its parent (innermost) tag.
	   If processing headers, each token is labeled with the conjunction of all ancestor tags.
	*/
	public Instance pipe (Instance carrier)
	{
		CharSequence string = (CharSequence) carrier.getData();
		TokenSequence dataTokens = new StringTokenization (string);
		TokenSequence targetTokens = new TokenSequence ();
		ArrayList tagStack = new ArrayList();	   
		String tag = backgroundTag;
		Matcher m = sgmlPattern.matcher (string);

		int textStart = 0;
		int textEnd = 0;
		int nextStart = 0;
		boolean isLineStart = true;

		String nextFont = "";

		tagStack.add(backgroundTag);

		while (m.find ()) {
			String inner = m.group (1);
			String element = m.group (2);

			nextStart = m.end();
			textEnd = m.start();

			boolean lineEnd = false;
			if (element.equals ("position")) {
				lineEnd = true;
			} else if (element.equals ("doc")) {
				Pattern namePtn = Pattern.compile ("source=\"([^\"]*)\"");
				Matcher matcher = namePtn.matcher (inner);
				if (matcher.find ()) {
					String docName = matcher.group (1);
					carrier.setName (docName);
				}
			}

			int numToksAdded = tokenizeSpan (textStart, textEnd, string, dataTokens, targetTokens,
											 nextFont, isLineStart, lineEnd, tag);
			if (numToksAdded > 0) {
				isLineStart = false;
			}

			if (element.equals ("font")) {
				int startIndex = inner.indexOf("\"");
				int endIndex = inner.lastIndexOf("\"");
				nextFont = inner.substring(startIndex+1, endIndex);

			} else if (element.equals ("line") || element.equals ("/line") || element.equals ("/doc") || element.equals ("doc")
					   || element.endsWith ("-hlabeled")
					   || element.equals ("page") || element.equals ("phantom")) {
				
				//Ignore
				
			} else if (element.equals ("position")) {
				isLineStart = true;
					
			} else if (element.startsWith ("/")) {
				if (element.endsWith (tag)) {
					tagStack.remove(0);
					//if (processingRefs)
						tag = (String) tagStack.get(0);
						/*else
						  tag = tagStack.toString();*/
				}
			} else {
				tag = element.toLowerCase();
				tagStack.add(0, tag);
			}
			
			textStart = nextStart;
		}
		
		tokenizeSpan (textStart, string.length(), string,
					  dataTokens, targetTokens,
					  nextFont, false, true, tag);

		carrier.setData(dataTokens);
		carrier.setTarget(targetTokens);
		carrier.setSource(dataTokens);

		return carrier;
	}


  private int tokenizeSpan (int start, int end, CharSequence string, TokenSequence data, TokenSequence target,
                             String font, boolean isLineStart, boolean isLineEnd, String tag)
  {
    int numAdded = 0;
    if (end - start > 0) {
      lexer.setCharSequence (string.subSequence (start, end));
      while (lexer.hasNext()) {
        String tok = (String) lexer.next ();

        // this can happen b/c of whitespace between tags
        if (isWhitespace(tok)) continue;

        int tokStart = start + lexer.getStartOffset ();
        int tokEnd = start + lexer.getEndOffset ();
        Token token = new StringSpan (string, tokStart, tokEnd);

        // Set line start property
        if (isLineStart) {
          token.setProperty(lineStart, new Boolean(true));
          isLineStart = false;
        } else {
            token.setProperty(lineIn, new Boolean(true));
        }

        // Set font property
        if(!font.equals(""))
          token.setProperty(fontProp, font);

        data.add (token);

        // Add the target token
		target.add (new Token (tag));

        numAdded++;
      }

      if (isLineEnd) {
        if (data.size() > 0) {
          Token lasttok = data.getToken (data.size() - 1);
          lasttok.setProperty (lineIn, new Boolean (false));
          lasttok.setProperty (lineEnd, new Boolean (true));
        }
      }
    }

    return numAdded;

  }


  private boolean isWhitespace (String tok)
  {
    return Pattern.matches ("\\s*", tok);
  }


  public static void main (String[] args)
	{
		String punct = "\\n|\\S+";
		// String punct = "\\n|[\\p{Alpha}|\\p{Digit}|\\p{Punct}]+";

		try {
			Pipe p = new SerialPipes (new Pipe[] {
				new Input2CharSequence (),
				// new XML2TokenSequence(new CharSequenceLexer (Pattern.compile ("\\n|\\S+@\\S+|\\w+-\\w+|\\w\\.|\\+[A-Z]+\\+|\\p{Alpha}+|\\p{Digit}+|\\p{Punct}")), "O"),
				// new XML2TokenSequence (new CharSequenceLexer (Pattern.compile (".")), "O")
				
				new XML2TokenSequence (new CharSequenceLexer (Pattern.compile (punct)), "O", false)
				// new XML2TokenSequence ()
				});
			
			for (int i = 0; i < args.length; i++) {
				Instance carrier = new Instance (new File(args[i]), null, null, null, p);
				TokenSequence data = (TokenSequence) carrier.getData();
				TokenSequence target = (TokenSequence) carrier.getTarget();
				System.out.println ("===");
				System.out.println (args[i]);

				if(data.size() != target.size()) throw new UnsupportedOperationException();

				for (int j = 0; j < data.size(); j++){
					Token t = data.getToken(j);
					System.out.print(target.getToken(j).getText() + " " + t.getText()  + ": ");

					if(t.hasProperty("LINE_START")){
						System.out.print("LINE_START ");
					}
					if(t.hasProperty("LINE_END")){
						System.out.print("LINE_END ");
					}
					if(t.hasProperty("LINE_IN")){
						System.out.print("LINE_IN ");
					}
					if(t.hasProperty("FONT")){
						System.out.print("FONT=" + t.getProperty("FONT"));
					}

					System.out.println();
					// System.out.println (target.getToken(j).getText()+" "+data.getToken(j).getText());
				}
			}
		} catch (Exception e) {
			System.out.println (e);
			e.printStackTrace();
		}
	}


  public boolean isProcessingRefs ()
  {
    return processingRefs;
  }


  public void setProcessingRefs (boolean processingRefs)
  {
    this.processingRefs = processingRefs;
  }
	// Serialization
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 1;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);

		out.writeObject(sgmlRegex);
		out.writeObject(backgroundTag);
		out.writeObject(lexerRegex);

		out.writeObject(lineStart);
		out.writeObject(lineEnd);
		out.writeObject(lineIn);
		out.writeObject(fontProp);

    out.writeObject (backgroundTag);
    out.writeBoolean(processingRefs);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		sgmlRegex = (String)in.readObject();  // this will be thrown away; var should have been static
		backgroundTag = (String) in.readObject();
		lexerRegex = (String)in.readObject();
		lexer = new CharSequenceLexer(lexerRegex);

		lineStart = (String)in.readObject();
		lineEnd = (String)in.readObject();
		lineIn = (String) in.readObject();
		fontProp = (String)in.readObject();

    // Throw away old sgml Pattern
    sgmlRegex = DEFAULT_SGML_REGEX;
    sgmlPattern = Pattern.compile(sgmlRegex);

    if (version > 0) {
      backgroundTag = (String) in.readObject ();
      processingRefs = in.readBoolean ();
    }
  }
}
