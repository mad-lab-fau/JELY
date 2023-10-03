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

public class ShiftListMinMax<T> {

	private T mMin;

	public T getMin() {
		return mMin;
	}

	public T getMax() {
		return mMax;
	}

	public int getMinIndex() {
		return mMinIndex;
	}

	public int getMaxIndex() {
		return mMaxIndex;
	}

	public ShiftList<T> getList() {
		return mList;
	}

	private T mMax;
	private int mMinIndex;
	private int mMaxIndex;
	private ShiftList<T> mList;

	public ShiftListMinMax(ShiftList<T> list, T min, int minIndex, T max, int maxIndex) {
		mMin = min;
		mMax = max;
		mMinIndex = minIndex;
		mMaxIndex = maxIndex;
		mList = list;
	}

	public double getRange() {
		if (!(mMin instanceof Number)) {
			return 0;
		}
		double range = ((Number) mMax).doubleValue() - ((Number) mMin).doubleValue();
		return range;
	}
}
