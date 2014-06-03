package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.util.CharSequenceLexer;

/**
 * This pipe assumes instance data to be LineInfo[] after tokenization (via pipe TokenizeLineInfo)
 * 
 * @author ghuang
 *
 */
public class TrieLexiconMembership4LineInfo extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final String END_TRIE_TOKEN = "ENDTRIETOKEN";
	
	String m_featureName;
	boolean m_ignoreCase;
	Pattern m_lexerPattern;
	Pattern m_ignorePattern;
	HashMap m_trieLexicon;
	
	
	public TrieLexiconMembership4LineInfo(String featureName, File lexiconFile,
			boolean ignoreCase, Pattern lexerPattern, Pattern ignorePattern) 
	{
		m_featureName = featureName;
		m_ignoreCase = ignoreCase;
		m_lexerPattern = lexerPattern;
		m_ignorePattern = ignorePattern;

		try { 
			initializeTrieLexicon(lexiconFile);
		}
		catch (IOException e) {
			throw new IllegalStateException(e.toString());
		}
	}
	
	
	private void initializeTrieLexicon(File lexiconFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(lexiconFile));
		String line;

		m_trieLexicon = new HashMap();

		while ((line = reader.readLine()) != null) {
			if (m_ignorePattern != null && m_ignorePattern.matcher(line.trim()).matches())
				continue;

			CharSequenceLexer lexer = new CharSequenceLexer(line, m_lexerPattern);
			HashMap currentLevel = m_trieLexicon;

			while (lexer.hasNext()) {
				String token = (String) lexer.next();

				if (m_ignoreCase)
					token = token.toLowerCase();

				if (! currentLevel.containsKey(token))
					currentLevel.put(token, new HashMap());

				currentLevel = (HashMap) currentLevel.get(token);
			}

			currentLevel.put(END_TRIE_TOKEN, "");
		}

	}
	
	
	private boolean containsLexicon(LineInfo lineInfo)
	{
		assert(lineInfo.textTokens != null);

		int startIdx = 0;
		String[] textTokens = lineInfo.textTokens;
		
		while (startIdx < textTokens.length) {
			HashMap currentLevel = m_trieLexicon;

			for (int i = startIdx; i < textTokens.length; i++) {
				String token = textTokens[i];

				if (m_ignoreCase) 
					token = token.toLowerCase();

				currentLevel = (HashMap) currentLevel.get(token);

				if (currentLevel == null) {
					startIdx++;
					break;
				}
				else if (currentLevel.containsKey(END_TRIE_TOKEN))
					return true;
				else if (i == textTokens.length - 1) { 
					startIdx++;
					break;
				}
			}
		}

		return false;
	}
	
	
	public Instance pipe(Instance carrier)
	{
		LineInfo[] lineInfos = (LineInfo[]) carrier.getData();
		
		for (int i = 0; i < lineInfos.length; i++) {
			if (containsLexicon(lineInfos[i]))
				lineInfos[i].presentFeatures.add(m_featureName);
		}
		
		return carrier;
	}

}
