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
package de.fau.mad.jely.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


/**
 * Deprecated. Just here for compatibility with old code.
 *
 * @author Stefan Gradl
 * @deprecated Use ShiftListDouble instead.
 */
public class DoubleCircularList extends CircularList implements List<Double> {

    /**
     * The raw values
     */
    protected double[] values = null;

    protected double minValue = Double.MAX_VALUE;
    protected double maxValue = Double.MIN_VALUE;

    /**
     * the distance between the min and max value
     */
    protected double rangeMinMax = 1;

    /**
     * sum of all values in this list. is maintained if maintainSum is true.
     * allows for fast statistics calculations
     */
    protected double sum = 0;

    /**
     * TRANSIENT variables
     */
    private transient int tIter = 0;

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public DoubleCircularList(int cacheSize, boolean maintainMinMax, boolean maintainSum) {
        super(cacheSize, maintainMinMax, maintainSum);
        values = new double[sizeMax];
    }

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize      Number of preallocated entries.
     * @param maintainMinMax Whether to maintain the min and max values.
     */
    public DoubleCircularList(int cacheSize, boolean maintainMinMax) {
        super(cacheSize, maintainMinMax);
        this.maintainSum = false;
        values = new double[sizeMax];
    }

    /**
     * Constructs a new FloatValueList with the given preallocated entries.
     *
     * @param cacheSize Number of preallocated entries.
     */
    public DoubleCircularList(int cacheSize) {
        super(cacheSize);
        values = new double[sizeMax];
    }

    /**
     * Adds a new entry to the ring, possibly overwriting the eldest entry.
     *
     * @param newValue The value to add to the list.
     * @return new head position
     */
    public int add(double newValue) {
        ++numLifetime;
        ++head;
        if (head == sizeMax)
            head = 0;

        if (maintainSum) {
            // update sum value, subtracting the old value that gets overwritten
            // and adding the new one
            sum = sum - values[head] + newValue;
        }

        values[head] = newValue;

        if (num < sizeMax)
            ++num;
        else {
            // if buffer is entirely filled, tail increases with head
            ++tail;
            if (tail == sizeMax)
                tail = 0;
        }

        if (maintainMinMax) {
            // check min/max
            if (newValue <= minValue) {
                // new MIN
                minValue = newValue;
                minIdx = head;
                rangeMinMax = maxValue - minValue;
            } else if (newValue >= maxValue) {
                // new MAX
                maxValue = newValue;
                maxIdx = head;
                rangeMinMax = maxValue - minValue;
            } else {
                // the new value is not a new min or max
                // check if we are going to overwrite the old min/max
                if (head == minIdx) {
                    // search for new minimum
                    findMin();
                } else if (head == maxIdx) {
                    // search for new maximum
                    findMax();
                }
            }
        }

        return head;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#clear()
     */
    public void clear() {
        super.clear();
        minValue = Float.MAX_VALUE;
        maxValue = Float.MIN_VALUE;
        rangeMinMax = 1;
        sum = 0;
        Arrays.fill(values, 0f);
    }

    /**
     * Copies the contents of sourceList into this ring buffer.
     *
     * @param sourceList
     */
    public void copy(ArrayList<? extends Number> sourceList) {
        num = sourceList.size();

        assert (num <= sizeMax);

        head = -1;
        for (Number l : sourceList) {
            values[++head] = l.doubleValue();
        }

        tail = 0;

        if (maintainMinMax) {
            findMinMax();
        }
    }

    /**
     * Copies the contents of sourceList into this ring buffer.
     *
     * @param sourceList
     */
    public void copy(DoubleCircularList sourceList) {
        num = sourceList.size();

        assert (num <= sizeMax);

        head = -1;

        if (maintainSum) {
            // only iterate if we should maintain the sum
            for (int i = 0; i < sourceList.num; ++i) {
                values[++head] = sourceList.values[i];
                sum += sourceList.values[i];
            }
        } else {
            // otherwise just copy
            System.arraycopy(sourceList, 0, values, 0, sourceList.num);
            head = sourceList.head;
        }

        tail = 0;

        if (maintainMinMax) {
            findMinMax();
        }
    }

    /**
     * Iterates over all valid elements and fills the max value.
     */
    public void findMax() {
        maxValue = minValue;
        for (tIter = 0; tIter < num; tIter++) {
            // new max?
            if (values[tIter] > maxValue) {
                maxValue = values[tIter];
                maxIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Iterates over all valid elements and fills the min value.
     */
    public void findMin() {
        minValue = maxValue;
        for (tIter = 0; tIter < num; tIter++) {
            // new min?
            if (values[tIter] < minValue) {
                minValue = values[tIter];
                minIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Iterates over all valid elements and fills the min & max value.
     */
    public void findMinMax() {
        minValue = Float.MAX_VALUE;
        minIdx = -1;
        maxValue = Float.MIN_VALUE;
        maxIdx = -1;
        for (tIter = 0; tIter < num; tIter++) {
            // new max?
            if (values[tIter] > maxValue) {
                maxValue = values[tIter];
                maxIdx = tIter;
            }
            // new min?
            else if (values[tIter] < minValue) {
                minValue = values[tIter];
                minIdx = tIter;
            }
        }
        rangeMinMax = maxValue - minValue;
    }

    /**
     * Returns the mean of all values in this list.
     *
     * @return
     */
    public double getMean() {
        if (num > 0)
            return (double) (sum / num);
        return 0f;
    }

    /**
     * Returns the value at the current head position.
     */
    public double getHeadValue() {
        if (head < 0)
            return 0f;
        return values[head];
    }

    /**
     * Returns the value that lies idxPast entries before the current head.
     *
     * @param idxPast Positive number indicating how many values to go into the
     *                past.
     * @return the value at the given index in the past.
     */
    public double getPastValue(int idxPast) {
        return getIndirect(head - idxPast);
    }

    /**
     * Returns the value at position rIdx. It has the same effect as calling
     * this.value[normIdx(rIdx)].
     *
     * @param rIdx negative or positive index in the ring
     * @return the value at rIdx or -1 if the List doesn't contain any elements
     */
    public double getIndirect(int rIdx) {
        // no elements
        if (num == 0)
            return -1f;

        else if (rIdx < -num)
            return values[(num << 1) + rIdx];

        else if (rIdx < 0)
            return values[num + rIdx];

        else if (rIdx >= num << 1)
            return rIdx % num;

        else if (rIdx >= num)
            return values[rIdx - num];

        return values[rIdx];
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Float object) {
        return add(object.doubleValue()) != -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int location, Float object) {
        values[normIdx(location)] = object.doubleValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends Double> arg0) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int arg0, Collection<? extends Double> arg1) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object object) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> arg0) {
        // === TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#get(int)
     */
    public Double get(int location) {
        return getIndirect(location);
    }

    /**
     * @return the sum
     */
    public double getSum() {
        return sum;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#iterator()
     */
    public Iterator<Double> iterator() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#listIterator()
     */
    public ListIterator<Double> listIterator() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<Double> listIterator(int location) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#remove(int)
     */
    public Double remove(int location) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Double set(int location, Double object) {
        int idx = normIdx(location);
        double prev = values[idx];
        values[idx] = object;
        return prev;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#subList(int, int)
     */
    public List<Double> subList(int start, int end) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] array) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.lme.plotview.CircularValueList#add(long)
     */
    @Override
    public int add(long newValue) {
        return add((double) newValue);
    }

    public String toString(String elementFormatter) {
        StringBuilder str = new StringBuilder(num * 16);
        for (tIter = 0; tIter < num; ++tIter) {
            str.append(String.format(elementFormatter, values[tIter])).append("; ");
        }
        return str.toString();
    }

    /**
     * Calculate statistical values in the given range, including start and end.
     *
     * @param start
     * @param end
     * @param stats receives the computed statistics. must not be null.
     */
    public void calculateStats(int start, int end, Statistics stats) {
        stats.sum = stats.rms = 0f;
        stats.num = end - start + 1;

        // calculate sum & rms
        for (tIter = start; tIter <= end; ++tIter) {
            stats.sum += values[tIter];
            stats.rms += values[tIter] * values[tIter];
        }

        // average
        stats.average = stats.sum / stats.num;

        // we don't calculate the root for the RMS immediately since we need it
        // squared for the variance
        stats.rms /= stats.num;

        // calculate variance, exploiting RMS� == avg� + variance
        stats.variance = stats.rms - stats.average * stats.average;

        // no we calculate the root for the RMS
        stats.rms = Math.sqrt(stats.rms);

        // standard deviation
        stats.stdDeviation = Math.sqrt(stats.variance);
    }

    @Override
    public boolean add(Double e) {
        add((double) e);
        return true;
    }

    @Override
    public void add(int index, Double element) {
        // TODO Auto-generated method stub
    }

}
