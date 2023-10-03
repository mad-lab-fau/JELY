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

/**
 * A fast (circular) list of double values. If new doubles are added after the
 * maximal size has been reached the oldest entries are overwritten.
 * 
 * @author gradl
 *
 */
public class ShiftListDouble extends ShiftList<Double> {
	private double[] mListBuffer;

	public ShiftListDouble(int maxSize) {
		super(maxSize);
		mListBuffer = new double[maxSize];
	}

	/**
	 * Constructs a new ShiftListDouble that wraps(!) the given double-list. It does
	 * not copy the array!
	 * 
	 * @param list double array that is wrapped with a ShiftListDouble.
	 */
	public ShiftListDouble(double[] list) {
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
	public ShiftListDouble(List<? extends Number> listToCopyFrom) {
		this(listToCopyFrom.size());

		// check if the parameter is also a ShiftListDouble to allow fast copy
		ShiftList<?> list = null;
		if (listToCopyFrom instanceof ShiftList<?>) {
			list = (ShiftList<?>) listToCopyFrom;
			if (!(list instanceof ShiftListDouble)) {
				list = null;
			}
		}

		if (list == null) {
			// no ShiftListDouble -> slow index-wise copy
			for (int i = 0; i < mListBuffer.length; i++) {
				mListBuffer[i] = (double) listToCopyFrom.get(i);
			}
		} else {
			// the parameter is also a ShiftListDouble
			ShiftListDouble sl = (ShiftListDouble) list;
			System.arraycopy(sl.mListBuffer, 0, mListBuffer, 0, sl.mListBuffer.length);
		}

		mLifetimeSize = mListBuffer.length;
		mHeadIndex = (int) (mLifetimeSize - 1);
	}

	@Override
	public boolean add(Double e) {
		beforeAddItem();
		mListBuffer[mHeadIndex] = e;
		return true;
	}

	@Override
	public void add(int index, Double element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends Double> c) {
		for (Iterator<? extends Double> iterator = c.iterator(); iterator.hasNext();) {
			add((Double) iterator.next());
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Double> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		super.clear();
		Arrays.fill(mListBuffer, 0);
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
	public Double get(int index) {
		return mListBuffer[getNormalizedIndex(index)];
	}

	public double getHeadValue() {
		return mListBuffer[getHeadIndex()];
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Double> iterator() {
		return new ShiftListDoubleIterator(this);
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<Double> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<Double> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double remove(int index) {
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
	public Double set(int index, Double element) {
		if (index < getTailIndex())
			return element;
		mListBuffer[getNormalizedIndex(index)] = element;
		return element;
	}

	@Override
	public List<Double> subList(int fromIndex, int toIndex) {
		int size = toIndex - fromIndex;
		ArrayList<Double> list = new ArrayList<Double>(size);
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

	public double[] getBuffer() {
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

	private class ShiftListDoubleIterator implements Iterator<Double> {
		private int mNextIndex = 0;
		private ShiftListDouble mList;

		public ShiftListDoubleIterator(ShiftListDouble list) {
			mList = list;
			mNextIndex = (int) mList.getTailIndex();
		}

		@Override
		public boolean hasNext() {
			return mNextIndex < mList.getLifetimeSize();
		}

		@Override
		public Double next() {
			return mList.get(mNextIndex++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public ShiftListMinMax<Double> getMinMax() {
		double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
		int minIndex = 0, maxIndex = 0;

		double cur = 0;
		for (int i = 0; i < mListBuffer.length; i++) {
			cur = mListBuffer[i];
			if (cur > max) {
				max = cur;
				maxIndex = i;
			}
			if (cur < min) {
				min = cur;
				minIndex = i;
			}
		}

		return new ShiftListMinMax<Double>(this, new Double(min), minIndex, new Double(max), maxIndex);
	}

}
