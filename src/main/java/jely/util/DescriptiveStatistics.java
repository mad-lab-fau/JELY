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
package jely.util;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


/**
 * Descriptive statistics for <code>List</code>-based collections of values.
 *
 * @author Stefan Gradl
 */
public class DescriptiveStatistics {
    private List<? extends Number> list;
    private int indexStart;
    private int indexEnd;
    private EnumSet<DescriptiveStatisticsElement> validElements;

    private int num;
    private double min;
    private double max;
    private double sum;
    private double mean;
    private double mode;
    private double median;
    private double variance;
    private double stdDeviation;
    private double skewness;
    private double kurtosis;

    /**
     * Sum of the squares.
     */
    private double energy;

    /**
     * sqrt( energy / num )
     */
    private double rms;

    /**
     * Construct statistics for the entire given list.
     *
     * @param list
     */
    public DescriptiveStatistics(List<? extends Number> list) {
        this(list, 0, list.size() - 1, EnumSet.allOf(DescriptiveStatisticsElement.class));
    }

    public DescriptiveStatistics(List<? extends Number> list, EnumSet<DescriptiveStatisticsElement> elements) {
        this(list, 0, list.size() - 1, elements);
    }

    /**
     * Construct statistics only for the values in the range [indexStart, indexEnd] of the list
     *
     * @param list
     * @param indexStart
     * @param indexEnd
     */
    public DescriptiveStatistics(List<? extends Number> list, int indexStart, int indexEnd) {
        this(list, indexStart, indexEnd, EnumSet.allOf(DescriptiveStatisticsElement.class));
    }

    /**
     * Construct statistics for the list given the restrictions.
     *
     * @param list
     * @param indexStart
     * @param indexEnd
     * @param elements
     */
    public DescriptiveStatistics(List<? extends Number> list, int indexStart, int indexEnd, EnumSet<DescriptiveStatisticsElement> elements) {
        this.list = list;
        this.indexStart = indexStart;
        this.indexEnd = indexEnd;
        this.validElements = elements;

        calculateStats();
    }

    private void calculateStats() {
        sum = rms = 0f;
        num = indexEnd - indexStart + 1;

        // calculate sum & rms, etc.
        double value = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        for (int i = indexStart; i <= indexEnd; i++) {
            value = list.get(i).doubleValue();
            sum += value;
            energy += value * value;

            if (value > max)
                max = value;

            if (value < min)
                min = value;
        }

        // average
        mean = sum / num;

        // we don't calculate the root for the RMS immediately since we need it
        // squared for the variance
        rms = energy / num;

        // calculate variance, exploiting RMS� == avg� + variance
        variance = rms - mean * mean;

        // TODO: there is something strange happening here: When doing the unit
        // test with Matlab as reference the
        // variance and stdDev calculations are only correct if done differently
        // in the way done after this comment.
        // However the Kurtosis and Skewness values are only correct (== the
        // same as in Matlab) if they use the variance
        // and stdDev values calculated above this comment... This should be
        // investigated.

        // now calculate the root for the RMS
        if (validElements.contains(DescriptiveStatisticsElement.RMS)) {
            rms = Math.sqrt(rms);
        }

        // standard deviation
        if (validElements.contains(DescriptiveStatisticsElement.STD_DEVIATION)
                || validElements.contains(DescriptiveStatisticsElement.SKEWNESS)
                || validElements.contains(DescriptiveStatisticsElement.KURTOSIS)) {
            stdDeviation = Math.sqrt(variance);
        }

        // skewness and kurtosis
        if (validElements.contains(DescriptiveStatisticsElement.VARIANCE)
                || validElements.contains(DescriptiveStatisticsElement.STD_DEVIATION)
                || validElements.contains(DescriptiveStatisticsElement.SKEWNESS)
                || validElements.contains(DescriptiveStatisticsElement.KURTOSIS)) {
            double s = 0;
            double k = 0;
            double v = 0;
            double ssum = 0;
            for (int i = indexStart; i <= indexEnd; ++i) {
                value = list.get(i).doubleValue();

                ssum = value - mean;

                if (validElements.contains(DescriptiveStatisticsElement.SKEWNESS))
                    s += Math.pow(ssum, 3);

                if (validElements.contains(DescriptiveStatisticsElement.KURTOSIS))
                    k += Math.pow(ssum, 4);

                v += ssum * ssum;
            }

            if (validElements.contains(DescriptiveStatisticsElement.SKEWNESS)) {
                s /= num;
                skewness = s / (Math.pow(stdDeviation, 3));
            }

            if (validElements.contains(DescriptiveStatisticsElement.SKEWNESS)) {
                k /= num;
                kurtosis = k / (variance * variance);
            }

            if (num > 1) {
                variance = v / (num - 1);
            } else {
                variance = value;
            }

            stdDeviation = Math.sqrt(variance);
        }

        // both elements are based on a sorted list
        if (validElements.contains(DescriptiveStatisticsElement.MEDIAN)
                || validElements.contains(DescriptiveStatisticsElement.MODE)) {
            Object[] array = list.toArray();
            Arrays.sort(array);

            // get median
            int halfLen = array.length / 2;
            if (array.length % 2 == 0)
                median = ((double) array[halfLen] + (double) array[halfLen - 1]) / 2;
            else
                median = (double) array[halfLen];

            // get mode
            Number mode = (Number) array[0];
            int temp = 1;
            int temp2 = 1;
            for (int i = 1; i < array.length; i++) {
                if (array[i - 1].equals(array[i])) {
                    temp++;
                } else {
                    temp = 1;
                }
                if (temp >= temp2) {
                    mode = (Number) array[i];
                    temp2 = temp;
                }
            }
            this.mode = mode.doubleValue();
        }

    }

    /**
     * @return the list
     */
    public List<?> getList() {
        return list;
    }

    /**
     * @return the indexStart
     */
    public int getIndexStart() {
        return indexStart;
    }

    /**
     * @return the indexEnd
     */
    public int getIndexEnd() {
        return indexEnd;
    }

    /**
     * @return the validElements
     */
    public EnumSet<DescriptiveStatisticsElement> getValidElements() {
        return validElements;
    }

    /**
     * @return the num
     */
    public int getNum() {
        return num;
    }

    /**
     * @return the min
     */
    public double getMin() {
        return min;
    }

    /**
     * @return the max
     */
    public double getMax() {
        return max;
    }

    /**
     * @return the sum
     */
    public double getSum() {
        return sum;
    }

    /**
     * @return the mean
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return the median
     */
    public double getMedian() {
        return median;
    }

    /**
     * @return the mode
     */
    public double getMode() {
        return mode;
    }

    /**
     * @return the variance
     */
    public double getVariance() {
        return variance;
    }

    /**
     * @return the stdDeviation
     */
    public double getStandardDeviation() {
        return stdDeviation;
    }

    /**
     * @return the skewness
     */
    public double getSkewness() {
        return skewness;
    }

    /**
     * @return the kurtosis
     */
    public double getKurtosis() {
        return kurtosis;
    }

    /**
     * @return the energy
     */
    public double getEnergy() {
        return energy;
    }

    /**
     * @return the rms
     */
    public double getRms() {
        return rms;
    }

    /**
     * Stores an index and value pair.
     *
     * @author gradl
     */
    public static class IndexValue {
        private double value;
        private int index;

        public IndexValue(double val, int idx) {
            value = val;
            index = idx;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }
    }

    /**
     * Obtain the maximum value and index position from a list of numbers.
     *
     * @param list
     * @return
     */
    public static IndexValue max(List<? extends Number> list) {
        double max = Double.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).doubleValue() > max) {
                max = list.get(i).doubleValue();
                index = i;
            }
        }
        return new IndexValue(max, index);
    }

    /**
     * Obtain the absolute maximum value and index position from a list of numbers.
     *
     * @param list
     * @return
     */
    public static IndexValue maxAbs(List<? extends Number> list) {
        double max = Double.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (Math.abs(list.get(i).doubleValue()) > max) {
                max = (double) list.get(i).doubleValue();
                index = i;
            }
        }
        return new IndexValue(max, index);
    }

    /**
     * Obtain the minimum value and index position from a list of numbers.
     *
     * @param list
     * @return
     */
    public static IndexValue min(List<? extends Number> list) {
        double min = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).doubleValue() < min) {
                min = list.get(i).doubleValue();
                index = i;
            }
        }
        return new IndexValue(min, index);
    }

    public static double mean(List<? extends Number> list) {
        double mean = 0;
        for (int i = 0; i < list.size(); i++) {
            mean += list.get(i).doubleValue();
        }
        return mean / list.size();
    }

    public static double mean(long[] list) {
        double mean = 0;
        for (int i = 0; i < list.length; i++) {
            mean += list[i];
        }
        return mean / list.length;
    }

    /**
     * Calculates the median of the array.
     *
     * @param numberArray
     * @return
     */
    public static double median(double[] numberArray) {
        double[] array = numberArray.clone();
        Arrays.sort(array);
        int halfLen = array.length / 2;
        double median = 0;
        if (array.length % 2 == 0)
            median = ((double) array[halfLen] + (double) array[halfLen - 1]) / 2;
        else
            median = (double) array[halfLen];
        return median;
    }

    /**
     * Calculates the median of a list of numbers.
     *
     * @param list
     * @return
     */
    public static double median(List<?> list) {
        double median = 0;
        Object[] array = list.toArray();
        Arrays.sort(array);
        int halfLen = array.length / 2;
        if (array.length % 2 == 0)
            median = ((double) array[halfLen] + (double) array[halfLen - 1]) / 2;
        else
            median = (double) array[halfLen];
        return median;
    }
}
