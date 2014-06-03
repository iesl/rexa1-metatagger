/**
 * Created on Jan 20, 2005
 * <p/>
 * Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * @author ghuang
 */

package org.rexo.extraction;

import edu.umass.cs.mallet.base.pipe.Pipe;
import edu.umass.cs.mallet.base.types.Instance;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.rexo.util.EnglishDictionary;

import java.io.*;

/**
 * Iterates over a pair of directories, one containing the source docs and one containing annotations for the source docs.
 */
public class FilePair2NewHtmlTokenization extends Pipe implements Serializable {
	static SAXBuilder saxBuilder = new SAXBuilder();  // XXX is this thread-safe?

	transient EnglishDictionary _dict;

	public FilePair2NewHtmlTokenization() {
		this( null );
	}

	/**
	 * Sets the pipe to use the given dictionary file to determine whether to remove hyphens that appear at the end of a line
	 */
	public FilePair2NewHtmlTokenization(File dictForDehyphenation) {

		if (dictForDehyphenation != null) {
			_dict = EnglishDictionary.create( dictForDehyphenation );
		}
		else {
			_dict = EnglishDictionary.createDefault();
		}
	}

	public Instance pipe(Instance carrier) {
		File[] pair = (File[])carrier.getData();
		File spanFile = pair[0];
		File docFile = pair[1];

		try {
			Document doc = saxBuilder.build( new FileInputStream( docFile ) );
			BufferedReader spanReader = new BufferedReader( new FileReader( spanFile ) );
			NewHtmlTokenization tokenization = new NewHtmlTokenization( doc, _dict, spanReader );

			spanReader.close();
			carrier.setData( tokenization );
		}
		catch (Exception e) {
			throw new IllegalStateException( e.toString() );
		}

		return carrier;
	}

	// Serialization 

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 1;

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt( CURRENT_SERIAL_VERSION );
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt();
		if ( version == 1 ) {
			in.readObject();
		}

	}

}
