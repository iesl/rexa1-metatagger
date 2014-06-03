/* Copyright (C) 2004 Univ. of Massachusetts Amherst, Computer Science Dept.
 * Based on MD5Hash.java from Nutch
 * author: mikem
 * */

/* Copyright (c) 2003 The Nutch Organization.  All rights reserved.   */
/* Use subject to the conditions in http://www.nutch.org/LICENSE.txt. */

package org.rexo.util;

import org.rexo.exceptions.InitializationException;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SHA1Hash {
	public static final int SHA1_LEN = 20;
	private static final MessageDigest DIGESTER;

	static {
		try {
			DIGESTER = MessageDigest.getInstance( "SHA1" );
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException( e );
		}
	}

	private byte digest[];

	/**
	 * Constructs an SHA1Hash.
	 */
	public SHA1Hash() {
		digest = new byte[SHA1_LEN];
	}

	public static SHA1Hash createFromHexString(String hexString) {
		return new SHA1Hash( hexString );
	}

	/**
	 * Constructs an SHA1Hash from a hex string.
	 */
	public SHA1Hash(String hex) {
		setDigest( hex );
	}

	/**
	 * Constructs an SHA1Hash with a specified value.
	 */
	public SHA1Hash(byte digest[]) {
		if (digest.length != SHA1_LEN) {
			throw new IllegalArgumentException( "Wrong length: " + digest.length );
		}
		this.digest = digest;
	}

	/**
	 * Copy the contents of another instance into this instance.
	 */
	public void set(SHA1Hash that) {
		System.arraycopy( that.digest, 0, digest, 0, SHA1_LEN );
	}

	/**
	 * Returns the digest bytes.
	 */
	public byte[] getDigest() {
		return digest;
	}

	/**
	 * Construct a hash value for a byte array.
	 */
	public static SHA1Hash digest(byte[] data) {
		return digest( data, 0, data.length );
	}

	/**
	 * Construct a hash value for a byte array.
	 */
	public static SHA1Hash digest(byte[] data, int start, int len) {
		byte digest[];
		synchronized (DIGESTER) {
			DIGESTER.update( data, start, len );
			digest = DIGESTER.digest();
		}
		return new SHA1Hash( digest );
	}

	/**
	 * Construct a hash value for a File object.
	 */
	public static SHA1Hash digest(File file) throws IOException {
		byte digest[];
		InputStream in = new BufferedInputStream( new FileInputStream( file ) );
		synchronized (DIGESTER) {
			byte data[] = new byte[8192];
			int len = in.read( data );
			while (len > 0) {
				DIGESTER.update( data, 0, len );
				len = in.read( data );
			}
			digest = DIGESTER.digest();
		}
		in.close();
		return new SHA1Hash( digest );
	}

	/**
	 * Construct a hash value for a String.
	 */
	public static SHA1Hash digest(String string) {
		return digest( string.getBytes() );
	}

	/**
	 * Returns true iff <code>o</code> is an SHA1Hash whose digest contains the same values.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof SHA1Hash)) {
			return false;
		}
		SHA1Hash other = (SHA1Hash)o;
		return Arrays.equals( digest, other.digest );
	}

	/**
	 * Returns a hash code value for this object.
	 */
	public int hashCode() {
		return (digest[0] | (digest[1] << 8) | (digest[2] << 16) | (digest[3] << 24)) ^
			   (digest[4] | (digest[5] << 8) | (digest[6] << 16) | (digest[7] << 24)) ^
			   (digest[8] | (digest[9] << 8) | (digest[10] << 16) | (digest[11] << 24)) ^
			   (digest[12] | (digest[13] << 8) | (digest[14] << 16) | (digest[15] << 24)) ^
			   (digest[16] | (digest[17] << 8) | (digest[18] << 16) | (digest[19] << 24));
	}

	/**
	 * Returns a string representation of this object.
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer( SHA1_LEN * 2 );
		for (int i = 0; i < SHA1_LEN; i++) {
			HexUtils.append( buffer, digest[i] );
		}
		return buffer.toString();
	}

	/**
	 * Sets the digest value from a hex string.
	 */
	public void setDigest(String hex) {
		if (hex.length() != SHA1_LEN * 2) {
			throw new IllegalArgumentException( "Wrong length: " + hex.length() );
		}
		byte digest[] = new byte[SHA1_LEN];
		for (int i = 0; i < SHA1_LEN; i++) {
			int j = i << 1;
			digest[i] = HexUtils.toByte( hex, j );
		}
		this.digest = digest;
	}
}
