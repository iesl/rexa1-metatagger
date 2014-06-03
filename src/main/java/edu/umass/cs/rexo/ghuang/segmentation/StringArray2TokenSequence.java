package edu.umass.cs.rexo.ghuang.segmentation;

import java.io.Serializable;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import edu.umass.cs.mallet.base.types.Token;
import edu.umass.cs.mallet.base.types.TokenSequence;

/**
 * Converts String[][] data and String[] target fields of an Instance to TokenSequence's 
 * 
 * @author ghuang
 *
 */
public class StringArray2TokenSequence extends Pipe implements Serializable
{
	private static final long serialVersionUID = 1L;


	public StringArray2TokenSequence() 
	{
		super();
	}


	public Instance pipe(Instance carrier) 
	{
		String[][] data = (String[][]) carrier.getData();
		String[] target = (String[]) carrier.getTarget();
		Token[] dataTokens = new Token[data.length];
		Token[] targetTokens = new Token[target.length];

		assert(data.length == target.length);

		for (int i = 0; i < data.length; i++) {
			dataTokens[i] = new Token("");
			targetTokens[i] = new Token(target[i]);

			for (int j = 0; j < data[i].length; j++) {
				dataTokens[i].setFeatureValue(data[i][j], 1);
			}
		}

		TokenSequence dataTS = new TokenSequence(dataTokens);
		TokenSequence targetTS = new TokenSequence(targetTokens);

		carrier.setData(dataTS);
		carrier.setTarget(targetTS);
		
		return carrier;
	}
}