/**
 * This file is part of the ShiftList distribution (https://github.com/mad-lab-fau/ShiftList).
 * Copyright (c) 2015-2020 Machine Learning and Data Analytics Lab, Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 * <p>
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fau.shiftlist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ShiftListBoolean extends ShiftList<Boolean> {

	private boolean[] mListBuffer;

	public ShiftListBoolean(int maxSize) {
		super(maxSize);
		mListBuffer = new boolean[maxSize];
	}

	/**
	 * Constructs a new ShiftListBoolean that wraps(!) the given boolean-list. It
	 * does not copy the array!
	 * 
	 * @param list boolean array that is wrapped with a ShiftListBoolean.
	 */
	public ShiftListBoolean(boolean[] list) {
		super(list.length);
		mListBuffer = list;
		mLifetimeSize = mListBuffer.length;
		mHeadIndex = (int) (mLifetimeSize - 1);
	}

	/**
	 * Copy constructor for generic (number) lists.
	 * 
	 * @param listToCopyFrom
	 */
	public ShiftListBoolean(List<? extends Number> listToCopyFrom) {
		this(listToCopyFrom.size());

		// check if the parameter is also a ShiftListDouble to allow fast copy
		ShiftList<?> list = null;
		if (listToCopyFrom instanceof ShiftList<?>) {
			list = (ShiftList<?>) listToCopyFrom;
			if (!(list instanceof ShiftListBoolean)) {
				list = null;
			}
		}

		if (list == null) {
			// no ShiftListDouble -> slow index-wise copy
			for (int i = 0; i < mListBuffer.length; i++) {
				mListBuffer[i] = (boolean) (listToCopyFrom.get(i).byteValue() == 1);
			}
		} else {
			// the parameter is also a ShiftListDouble
			ShiftListBoolean sl = (ShiftListBoolean) list;
			System.arraycopy(sl.mListBuffer, 0, mListBuffer, 0, sl.mListBuffer.length);
		}

		mLifetimeSize = mListBuffer.length;
		mHeadIndex = (int) (mLifetimeSize - 1);
	}

	@Override
	public boolean add(Boolean e) {
		beforeAddItem();
		mListBuffer[mHeadIndex] = e;
		return true;
	}

	@Override
	public void add(int index, Boolean element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Boolean> c) {
		for (Iterator<? extends Boolean> iterator = c.iterator(); iterator.hasNext();) {
			add((Boolean) iterator.next());
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Boolean> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		super.clear();
		Arrays.fill(mListBuffer, false);
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean get(int index) {
		return mListBuffer[getNormalizedIndex(index)];
	}

	public boolean getHeadValue() {
		return mListBuffer[getHeadIndex()];
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Boolean> iterator() {
		return new ShiftListBooleanIterator(this);
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<Boolean> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Boolean> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Boolean remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Boolean set(int index, Boolean element) {
		if (index < getTailIndex())
			return element;
		mListBuffer[getNormalizedIndex(index)] = element;
		return get(index);
	}

	@Override
	public List<Boolean> subList(int fromIndex, int toIndex) {
		int size = toIndex - fromIndex;
		ArrayList<Boolean> list = new ArrayList<Boolean>(size);
		for (int i = fromIndex; i < toIndex; i++) {
			list.add(get(i));
		}
		return list;
	}

	@Override
	public Object[] toArray() {
		Object[] list = new Double[getMaxSize()];
		for (int i = 0; i < list.length; i++) {
			list[i] = mListBuffer[i];
		}
		return list;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	public boolean[] getBuffer() {
		return mListBuffer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("[ ");
		for (int i = 0; i < mListBuffer.length; i++) {
			if (i > 0)
				str.append("; ");
			str.append(mListBuffer[i]);
		}
		str.append(" ]");
		return str.toString();
	}

	private class ShiftListBooleanIterator implements Iterator<Boolean> {
		private int mNextIndex = 0;
		private ShiftListBoolean mList;

		public ShiftListBooleanIterator(ShiftListBoolean list) {
			mList = list;
			mNextIndex = (int) mList.getTailIndex();
		}

		@Override
		public boolean hasNext() {
			return mNextIndex < mList.getLifetimeSize();
		}

		@Override
		public Boolean next() {
			return mList.get(mNextIndex++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public ShiftListMinMax<Boolean> getMinMax() {
		// TODO Auto-generated method stub
		return null;
	}

}
