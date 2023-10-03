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

import java.util.List;

/**
 * Template class for a fast (circular) list, based on the List<?> interface.
 * 
 * @author gradl
 *
 * @param <T>
 */
public abstract class ShiftList<T> implements List<T> {
	protected int mMaxSize;
	protected long mLifetimeSize = 0;

	protected int mHeadIndex = -1;

	public ShiftList(int maxSize) {
		mMaxSize = maxSize;
	}

	public int getNormalizedIndex(int rIdx) {
		if (rIdx >= mMaxSize << 1) {
			return rIdx % mMaxSize;
		} else if (rIdx >= mMaxSize) {
			rIdx = rIdx - mMaxSize;
		} else if (rIdx < 0) {
			rIdx = mHeadIndex + rIdx;
			rIdx = rIdx % mMaxSize;
			// we expect negative mod to produce positive values
			if (rIdx < 0) {
				rIdx += mMaxSize;
			}
		}
		return rIdx;
	}

	/**
	 * Checks if the buffer position to the given index is still valid, or was
	 * already overwritten.
	 * 
	 * @param rIdx
	 * @return
	 */
	public boolean isIndexValid(int rIdx) {
		if (rIdx < size() - getMaxSize())
			return false;
		return true;
	}

	/**
	 * Needs to be called by descendent classes BEFORE a new item is added to the
	 * list.
	 */
	protected void beforeAddItem() {
		mLifetimeSize++;
		mHeadIndex++;
		if (mHeadIndex == mMaxSize)
			mHeadIndex = 0;
	}

	@Override
	public void clear() {
		mLifetimeSize = 0;
		mHeadIndex = -1;
	}

	@Override
	public boolean isEmpty() {
		return mLifetimeSize == 0;
	}

	@Override
	public int size() {
		return (int) mLifetimeSize;
	}

	public long getLifetimeSize() {
		return mLifetimeSize;
	}

	public int getShiftWindowSize() {
		return mMaxSize;
	}

	public int getMaxSize() {
		return mMaxSize;
	}

	/**
	 * 
	 * @return The number of currently used/filled entries. This is the actual
	 *         number of elements until all slots are filled, then it always returns
	 *         maxSize.
	 */
	public int getFilledSize() {
		if (mLifetimeSize <= mMaxSize)
			return (int) mLifetimeSize;
		return mMaxSize;
	}

	public int getHeadIndex() {
		return mHeadIndex;
	}

	public long getTailIndex() {
		long tail = mLifetimeSize - mMaxSize;
		if (tail < 0)
			tail = 0;
		return tail;
	}

	public abstract ShiftListMinMax<T> getMinMax();
}
