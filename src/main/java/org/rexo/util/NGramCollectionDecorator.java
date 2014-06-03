package org.rexo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Generates a Collection containing bigrams of elements from an underlying collection. Can generate either all-pairs or contiguous-only bigrams. Author: saunders Created Sep 21, 2005 Copyright (C)
 * Univ. of Massachusetts Amherst, Computer Science Dept.
 * <p/>
 * ACS note: I am replacing this decorator with iterators, one for each of the types of combinations one might want to iterate over. I reimplemented one of the behaviors with
 * CombinationNoReplaceIterator, but have not implemented the 'sliding window' ngram behavior
 */
public class NGramCollectionDecorator implements Collection {

	public static class CombinationNoReplaceIterator implements Iterator {
		private int[] _indices = null;
		private Object[] _objects;
		private int _n;

		public CombinationNoReplaceIterator(Collection c, int n) {
			_objects = c.toArray();
			_n = n;
		}

		private void init(int i) {
			_indices[i] = i == 0 ? 0 : _indices[i - 1] + 1;
		}

		private boolean test(int i) {
			return _indices[i] < _objects.length;
		}

		private void mod(int i) {
			_indices[i]++;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			if (_indices == null) {
				_indices = new int[_n];
				for (int i = 0; i < _n; i++) {
					init( i );
				}
			}
			else {
				int i;
				for (i = _n; i > 0; i--) {
					mod( i - 1 );
					if (test( i - 1 )) {
						break;
					}
				}
				if (i > 0) {
					for (; i < _n; i++) {
						init( i );
					}
				}
			}
			boolean hasNext = true;
			for (int i = 0; i < _n; i++) {
				hasNext &= test( i );
			}
			return hasNext;
		}

		public Object next() {
			Object[] ngram = new Object[_n];
			for (int i = 0; i < _n; i++) {
				ngram[i] = _objects[_indices[i]];
			}
			return ngram;
		}
	}

	public static class SlidingWindowIterator implements Iterator {
		private Object[] _objects;
		private int _next;
		private int _nWindow;

		public SlidingWindowIterator(Collection c, int n) {
			_objects = c.toArray();
			_nWindow = n;
			_next = 0;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			return false;
		}

		public Object next() {
			return null;
		}
	}


	/**
	 * A Collection of Object[2] arrays.
	 */
	private Collection _bigrams;

	public NGramCollectionDecorator(Collection c) {
		this( c, false );
	}

	/**
	 * Constructs a NGramCollectionDecorator object.
	 *
	 * @param wantContiguousNGramsOnly if true, n-grams will be for contiguous elements only; otherwise pairwise n-grams will be generated
	 */
	public NGramCollectionDecorator(Collection c, boolean wantContiguousNGramsOnly) {
		if (wantContiguousNGramsOnly) {
			_bigrams = toContiguousList( c );
		}
		else {
			_bigrams = toPairwiseList( c );
		}
	}

	/**
	 * # of combinations
	 *
	 * @return size of pairwise collection
	 */
	public int size() {
		return _bigrams.size();
	}

	public boolean isEmpty() {
		return _bigrams.isEmpty();
	}

	private static List toPairwiseList(Collection c) {
		ArrayList pairs = new ArrayList();
		Object[] carray = c.toArray();
		int csize = c.size();
		for (int i = 0; i < csize; i++) {
			for (int j = i + 1; j < csize; j++) {
				pairs.add( new Object[]{carray[i], carray[j]} );
			}
		}
		return pairs;
	}

	private static List toContiguousList(Collection c) {
		ArrayList pairs = new ArrayList();
		Object[] carray = c.toArray();
		int csize = c.size();
		for (int i = 0; i < csize - 1; i++) {
			pairs.add( new Object[]{carray[i], carray[i + 1]} );
		}
		return pairs;
	}

	public Object[] toArray() {
		return _bigrams.toArray();
	}

	public Iterator iterator() {
		return _bigrams.iterator();
	}

	// Unsupported operations follow:

	public Object[] toArray(Object a[]) {
		throw new UnsupportedOperationException();
	}


	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

}
