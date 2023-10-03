/**
 * This file is part of the JELY distribution (https://github.com/mad-lab-fau/JELY).
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
package src.main.java.jely.util;

/**
 * High performance ring buffer implementation.
 * <p>
 * It is an enforced policy of this implementation to never do any
 * reallocations. The number of items given in the constructor will be the
 * absolute maximum of items the list may hold during its entire lifetime.
 *
 * @author Stefan Gradl
 * @deprecated Use ShiftList instead.
 */
public abstract class CircularList {
    // @formatter:off
    /**
     * <p>
     *
     * <pre>
     *
     *                 head
     *                  |
     *        +---+---+---+---+
     *        | 0 | 1 | 2 | 3 |
     *        +---+---+---+---+
     *          |           |
     *         tail        EOR
     *
     * </pre>
     *
     * <p>
     */
    // @formatter:on

    protected int sizeMax = 0;
    /**
     * End Of Ring - largest valid index, not necessarily used! i.e. EOR may or
     * may not be the same as head or num-1 but always is equal to (sizeMax-1).
     */
    protected int EOR = -1;

    /**
     * index of the entry most recently added
     */
    protected int head = -1;
    /**
     * index of the oldest entry, when the ring is filled this will always be
     * head + 1
     */
    protected int tail = 0;
    /**
     * count of currently used indices
     */
    protected int num = 0;

    /**
     * Count of entries added to this list during its entire lifetime.
     */
    protected long numLifetime = 0;

    protected int minIdx = -1;
    protected int maxIdx = -1;

    /**
     * Indicates whether the minimum and maximum values&indices are updated on
     * every add(), remove() or set() call
     */
    protected boolean maintainMinMax = false;
    /**
     * Indicates whether the sum of all valid values is updated on every add(),
     * remove() or set() call
     */
    protected boolean maintainSum = false;

    @SuppressWarnings("unused")
    private CircularList() {
    }

    /**
     * Private initializer. Only called by the constructors.
     *
     * @param cacheSize
     * @param maintainMinMax
     * @param maintainSum
     */
    private void init(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        if (cacheSize <= 0)
            sizeMax = 1022;
        else
            sizeMax = cacheSize;

        EOR = sizeMax - 1;

        this.maintainMinMax = maintainMinMax;
        this.maintainSum = maintainSum;
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public CircularList(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        init(cacheSize, maintainMinMax, maintainSum);
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public CircularList(int cacheSize, boolean maintainMinMax) {
        init(cacheSize, maintainMinMax, false);
    }

    /**
     * Constructs a new CircularValueList with the given preallocated entries.
     *
     * @param cacheSize Number of preallocated entries.
     */
    public CircularList(int cacheSize) {
        init(cacheSize, false, false);
    }

    public void clear() {
        head = -1;
        tail = 0;
        num = 0;
        minIdx = -1;
        maxIdx = -1;
    }

    /**
     * Normalizes the given negative or positive index. Valid index range: [ -2
     * * num <= rIdx < 2 * num [
     *
     * @param rIdx negative or positive index to normalize
     * @return The normalized index that is within valid array bounds (if rIdx
     * was in the valid range!). Or 0 if this list doesn't contain any
     * elements
     */
    public int normIdx(int rIdx) {
        if (num == 0)
            return 0;

        else if (rIdx < -num)
            return (num << 1) + rIdx;

        else if (rIdx < 0)
            return num + rIdx;

        else if (rIdx >= num << 1)
            return rIdx % num;

        else if (rIdx >= num)
            return rIdx - num;

        return rIdx;
    }

    /**
     * @param rIdx
     * @return The distance to tail.
     */
    public int tailDistance(int rIdx) {
        if (rIdx >= tail)
            return rIdx - tail;

        return num - tail + rIdx;
    }

    /**
     * @return Count of used entries.
     */
    public int size() {
        return num;
    }

    public long sizeLifetime() {
        return numLifetime;
    }

    public boolean isEmpty() {
        return (num == 0);
    }

    public abstract int add(double newValue);

    public abstract int add(long newValue);

    public abstract void findMax();

    public abstract void findMin();

    public abstract void findMinMax();

    /**
     * Statistical information about a certain range of values.
     *
     * @author sistgrad
     */
    public static class Statistics {
        public int idxStart = 0;
        public int idxEnd = 0;
        public int num = 0;

        public double sum = 0d;
        public double average = 0d;
        public double variance = 0d;
        public double stdDeviation = 0d;
        public double rms = 0d;
    }
}
