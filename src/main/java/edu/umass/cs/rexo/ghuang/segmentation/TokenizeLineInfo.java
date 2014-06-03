package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;


/**
 * Input: instances with LineInfo[] in data field
 * Output: instances with LineInfo[] in data field, where each LineInfo's textTokens is set 
 * 
 * @author ghuang
 *
 */
public class TokenizeLineInfo extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;

	Pattern m_lexerPattern;
	
	
	public TokenizeLineInfo(Pattern lexerPattern)
	{
		m_lexerPattern = lexerPattern;
	}
	
	public Instance pipe(Instance carrier)
	{
		LineInfo[] lineInfos = (LineInfo[]) carrier.getData();
		ArrayList tokens = new ArrayList();

		for (int i = 0; i < lineInfos.length; i++) {
			CharSequenceLexer lexer = new CharSequenceLexer(lineInfos[i].text, m_lexerPattern);

			tokens.clear();

			while (lexer.hasNext()) {
				tokens.add(lexer.next());
			}

			lineInfos[i].textTokens = new String[tokens.size()];
			for (int j = 0; j < tokens.size(); j++)
				lineInfos[i].textTokens[j] = (String) tokens.get(j);
		}

		return carrier;
	}

}
