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
 * A fast (circular) list of Objects. If new Objects are added after the maximal
 * size has been reached the oldest entries are overwritten.
 * 
 * @author gradl
 *
 */
public class ShiftListObject extends ShiftList<Object> {
	private Object[] mListBuffer;

	public ShiftListObject(int maxSize) {
		super(maxSize);
		mListBuffer = new Object[maxSize];
	}

	@Override
	public boolean add(Object e) {
		beforeAddItem();
		mListBuffer[mHeadIndex] = e;
		return true;
	}

	@Override
	public void add(int index, Object element) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean addAll(Collection<? extends Object> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Object> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		super.clear();
		Arrays.fill(mListBuffer, null);
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
	public Object get(int index) {
		return mListBuffer[getNormalizedIndex(index)];
	}

	public Object getHeadValue() {
		return mListBuffer[getHeadIndex()];
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Object> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<Object> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<Object> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object remove(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object set(int index, Object element) {
		if (index < getTailIndex())
			return element;
		mListBuffer[getNormalizedIndex(index)] = element;
		return element;
	}

	@Override
	public List<Object> subList(int fromIndex, int toIndex) {
		int size = toIndex - fromIndex;
		ArrayList<Object> list = new ArrayList<Object>(size);
		for (int i = fromIndex; i < toIndex; i++) {
			list.add(get(i));
		}
		return list;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public ShiftListMinMax<Object> getMinMax() {
		// TODO Auto-generated method stub
		return null;
	}

}
