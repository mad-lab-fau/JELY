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
package jely.filter;

import java.security.InvalidParameterException;

import src.main.java.jely.util.DoubleCircularList;


/**
 * Implementation of a filtering algorithm using numerator and denominator
 * coefficients.
 *
 * @author Stefan Gradl
 * @deprecated LmeFilter should not be used anymore. Use DigitalFilter instead.
 */
public class LmeFilter {
    protected double a[] = null;
    protected double b[] = null;
    public double y[] = null;
    public double x[] = null;


    protected LmeFilter() {
    }


    /**
     * @param b_taps numerator coefficients
     * @param a_taps denominator coefficients, can be null. if not null, a[0] must not be 0 or an
     *               {@link InvalidParameterException} will be thrown.
     */
    public LmeFilter(double[] b_taps, double[] a_taps) {
        // make sure the coefficients are valid
        if (b_taps == null || b_taps.length < 1 || (b_taps.length == 1 && b_taps[0] == 0)
                || (a_taps != null && a_taps[0] == 0)) {
            throw new InvalidParameterException();
        }

        // copy denominators
        if (a_taps == null) {
            a = new double[1];
            a[0] = 1d;
        } else {
            a = new double[a_taps.length];
            System.arraycopy(a_taps, 0, a, 0, a_taps.length);
        }

        // copy numerators
        b = new double[b_taps.length];
        System.arraycopy(b_taps, 0, b, 0, b_taps.length);

        // create x & y arrays
        y = new double[a_taps.length];
        x = new double[b_taps.length];
    }


    public LmeFilter(double b0, double b1, double b2, double b3, double b4, double a0, double a1, double a2) {
        if (a0 == 0f) {
            throw new InvalidParameterException();
        }

        a = new double[3];
        a[0] = a0;
        a[1] = a1;
        a[2] = a2;

        b = new double[5];
        b[0] = b0;
        b[1] = b1;
        b[2] = b2;
        b[3] = b3;
        b[4] = b4;

        // create x & y arrays
        y = new double[a.length];
        x = new double[b.length];
    }


    private transient int t_iter = 0;


    /**
     * Performs the filtering operation for the next x value.
     *
     * @param xnow x[n]
     * @return y[n]
     */
    public double next(double xnow) {
        if (b.length > 1)
            System.arraycopy(x, 0, x, 1, b.length - 1);
        x[0] = xnow;

        // shift y
        if (a.length > 1)
            System.arraycopy(y, 0, y, 1, a.length - 1);
        y[0] = 0d;

        // sum( b[n] * x[N-n] )
        for (t_iter = 0; t_iter < b.length; ++t_iter) {
            y[0] += b[t_iter] * x[t_iter];
        }

        // sum( a[n] * y[N-n] )
        for (t_iter = 1; t_iter < a.length; ++t_iter) {
            y[0] += a[t_iter] * y[t_iter];
        }

        // a0
        if (a[0] != 1d) {
            y[0] /= a[0];
        }

        return y[0];
    }


    /**
     * @return The current y[0] value from last calculation step
     */
    public double current() {
        return y[0];
    }


    /**
     * Implements running <i>mean</i> filter.
     * <p>
     * y[n] = 1/(N+1) * ( y[n-1] * N + x[n] )
     *
     * @author sistgrad
     */
    public static class MeanFilter extends LmeFilter {
        public int num = 0;
        public int maxNum = 0;


        public MeanFilter() {
            a = new double[2];
            a[0] = 0f;

            b = new double[2];

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /**
         * Stop increasing the num counter at maxNum values.
         *
         * @param maxNum
         */
        public MeanFilter(int maxNum) {
            a = new double[2];
            a[0] = 0f;

            b = new double[2];

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];

            this.maxNum = maxNum;
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            y[1] = y[0];
            y[0] = (y[1] * num + xnow) / (num + 1);
            if (maxNum == 0 || num < maxNum)
                ++num;
            return y[0];
        }
    }


    /**
     * Represents a statistical object tp keep track of running mean, min and max values.
     *
     * @author sistgrad
     */
    public static class StatFilter extends LmeFilter {
        protected MeanFilter meanFilter = null;

        public double mean = 0;
        public double min = Double.MAX_VALUE;
        public double max = Double.MIN_VALUE;
        public double range = 0;
        public double value = 0;


        public StatFilter() {
            meanFilter = new MeanFilter(16);
        }


        /**
         * Stop increasing the num counter at maxNum values.
         *
         * @param maxNum
         */
        public StatFilter(int maxNum) {
            meanFilter = new MeanFilter(maxNum);
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            mean = meanFilter.next(xnow);
            value = xnow;

            if (xnow > max) {
                max = xnow;
                range = max - min;
            }

            if (xnow < min) {
                min = xnow;
                range = max - min;
            }

            return value;
        }


        public String formatValue() {
            return String.format("%.0f", value);
        }


        public String formatMean() {
            return String.format("%.0f", mean);
        }


        public String formatMin() {
            return String.format("%.0f", min);
        }


        public String formatMax() {
            return String.format("%.0f", max);
        }
    }


    /**
     * Implements the <i>von-Hann</i> filter using the last 3 values.
     * <p>
     * y[n] = 1/4 * ( x[n] + 2 * x[n-1] + x[n-2] )
     *
     * @author sistgrad
     */
    public static class HannFilter extends LmeFilter {
        public HannFilter() {
            a = new double[1];
            a[0] = 1f;

            b = new double[3];
            b[0] = b[2] = 0.25f;
            b[1] = 0.5f;

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            x[2] = x[1];
            x[1] = x[0];
            x[0] = xnow;
            return (0.25f * x[0] + 0.5f * x[1] + 0.25f * x[2]);
        }
    }


    /**
     * Implements a <i>peak detection</i> filter.
     * <p>
     * next() will always return the peak-decision for the central value of minRange. If minRange == 3 then the third
     * call to next() returns the peak-decision for the previous value. The very first and second call ever made to
     * next() after creating the object will, in this exemplary case, always return <code>Double.NaN</code>.
     *
     * @author sistgrad
     */
    public static class PeakDetectionFilter extends LmeFilter {
        protected int minRange;
        protected double minDiff;
        public int peakIdx;
        public double peakValue = Double.NaN;
        protected int block = 1;


        /**
         * @param minRange range of surrounding values to test against for peak evaluation (must be >= 1)
         * @param minDiff  minimal (absolute) difference between two values to be considered different
         */
        public PeakDetectionFilter(int minRange, double minDiff) {
            if (minRange > 0)
                this.minRange = minRange;
            else
                this.minRange = 1;

            this.minDiff = Math.abs(minDiff);

            // create x & y arrays
            y = new double[1];
            x = new double[minRange << 1 + 1];

            peakIdx = minRange;

            block = x.length;
        }


        /**
         *
         */
        public PeakDetectionFilter() {
            this.minRange = 1;
            this.minDiff = 0f;

            // create x & y arrays
            y = new double[1];
            x = new double[3];

            peakIdx = 1;
            block = 3;
        }


        /**
         * resets blocking to reuse the filter
         */
        public void reset() {
            block = x.length;
        }


        protected transient int _i;


        /**
         * @return Double.NaN, if not part of a peak, peakIdx will also be set to -1. Or the value of the peak, if it IS
         * part of a peak. this.peakIdx will contain the current index of said peak.
         */
        @Override
        public double next(double xnow) {
            System.arraycopy(x, 0, x, 1, x.length - 1);
            x[0] = xnow;

            peakValue = Double.NaN;
            peakIdx = -1;

            // block until the buffer is filled entirely
            if (block > 0) {
                --block;
                return Double.NaN;
            }


            for (_i = 1; _i <= minRange; ++_i) {
                // values before mid-value
                if (x[minRange] - minDiff <= x[minRange + _i])
                    return Double.NaN;

                // values after mid-value
                if (x[minRange] - minDiff < x[minRange - _i])
                    return Double.NaN;
            }


            // value IS part of a peak
            peakValue = x[minRange];
            peakIdx = minRange;

            return peakValue;
        }
    }


    /**
     * Implements a <i>minimum detection</i> filter.
     * <p>
     * next() will always return the peak-decision for the central value of minRange. If minRange == 3 then the third
     * call to next() returns the peak-decision for the previous value. The very first and second call ever made to
     * next() after creating the object will, in this exemplary case, always return <code>Double.NaN</code>.
     *
     * @author sistgrad
     */
    public static class MinDetectionFilter extends PeakDetectionFilter {

        /**
         *
         */
        public MinDetectionFilter() {
            super();
            // TODO Auto-generated constructor stub
        }


        /**
         * @param minRange
         * @param minDiff
         */
        public MinDetectionFilter(int minRange, double minDiff) {
            super(minRange, minDiff);
            // TODO Auto-generated constructor stub
        }


        @Override
        public double next(double xnow) {
            System.arraycopy(x, 0, x, 1, x.length - 1);
            x[0] = xnow;

            peakValue = Double.NaN;
            peakIdx = -1;

            // block until the buffer is filled entirely
            if (block > 0) {
                --block;
                return Double.NaN;
            }


            for (_i = 1; _i <= minRange; ++_i) {
                // values before mid-value
                if (x[minRange] - minDiff >= x[minRange + _i])
                    return Double.NaN;

                // values after mid-value
                if (x[minRange] - minDiff > x[minRange - _i])
                    return Double.NaN;
            }


            // value IS part of a peak
            peakValue = x[minRange];
            peakIdx = minRange;

            return peakValue;
        }
    }


    /**
     * Implements the <i>Savitzky-Golay</i> filter using 5 points.
     * <p>
     * y[n] = ...
     *
     * @author sistgrad
     */
    public static class SavGolayFilter extends LmeFilter {
        /**
         * @param sg_order The Savitzky-Golay order to use.
         */
        public SavGolayFilter(int sg_order) {
            a = new double[1];
            a[0] = 1f;

            if (sg_order <= 1) {
                b = new double[5];
                b[0] = -0.0857f;
                b[1] = 0.3429f;
                b[2] = 0.4857f;
                b[3] = 0.3429f;
                b[4] = -0.0857f;
            } else if (sg_order == 2) {
                b = new double[7];
                b[0] = -0.095238f;
                b[1] = 0.1428571f;
                b[2] = 0.285714f;
                b[3] = 0.33333f;
                b[4] = 0.285714f;
                b[5] = 0.1428571f;
                b[6] = -0.095238f;
            } else
                throw new InvalidParameterException();

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            if (b.length == 7) {
                // TODO: check at what point System.arraycopy is worth the call! Is it inlined?
                x[6] = x[5];
                x[5] = x[4];
                x[4] = x[3];
                x[3] = x[2];
                x[2] = x[1];
                x[1] = x[0];
                x[0] = xnow;
                y[0] = (b[0] * x[0] + b[1] * x[1] + b[2] * x[2] + b[3] * x[3] + b[4] * x[4] + b[5]
                        * x[5] + b[6] * x[6]);
                return y[0];
            } else {
                x[4] = x[3];
                x[3] = x[2];
                x[2] = x[1];
                x[1] = x[0];
                x[0] = xnow;
                y[0] = (b[0] * x[0] + b[1] * x[1] + b[2] * x[2] + b[3] * x[3] + b[4] * x[4]);
                return y[0];
            }
        }
    }


    /**
     * Implements a <i>moving window integrator</i> filter.
     * <p>
     * y[n] = 1/N * ( x[n] + x[n-1] + x[n-2] + ... )
     *
     * @author sistgrad
     */
    public static class WndIntFilter extends LmeFilter {
        protected DoubleCircularList m_int = null;


        public WndIntFilter(int wndLength) {
            a = new double[1];
            a[0] = wndLength;

            b = new double[1];

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];

            m_int = new DoubleCircularList(wndLength, false, true);
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            m_int.add(xnow);
            y[0] = m_int.getMean();
            return y[0];
        }
    }


    /**
     * Implements a <i>moving window accumulation</i> filter.
     * <p>
     * y[n] = ( x[n] + x[n-1] + x[n-2] + ... )
     *
     * @author sistgrad
     */
    public static class AccuFilter extends WndIntFilter {
        /**
         * @param wndLength
         */
        public AccuFilter(int wndLength) {
            super(wndLength);
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            m_int.add((float) xnow);

            y[0] = m_int.getSum();

            return y[0];
        }
    }


    /**
     * Implements the <i>first order derivative</i> filter.
     * <p>
     * y[n] = 1/T * ( x[n] - x[n-1] )
     *
     * @author sistgrad
     */
    public static class FirstDerivativeFilter extends LmeFilter {
        public FirstDerivativeFilter(double T) {
            a = new double[1];
            a[0] = 1 / T;

            b = new double[2];
            b[0] = 1f;
            b[1] = -1f;

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            x[1] = x[0];
            x[0] = xnow;

            return a[0] * (x[0] - x[1]);
        }
    }


    /**
     * Implements the <i>second order derivative</i> filter.
     * <p>
     * y[n] = 1/T * ( x[n] - 2 * x[n-1] + x[n-2] )
     *
     * @author sistgrad
     */
    public static class SecondDerivativeFilter extends LmeFilter {
        public SecondDerivativeFilter(double T) {
            a = new double[1];
            a[0] = 1 / (T * T);

            b = new double[3];
            b[0] = 1f;
            b[1] = -2f;
            b[2] = 1f;

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            x[2] = x[1];
            x[1] = x[0];
            x[0] = xnow;
            return a[0] * (x[0] + 2 * x[1] + x[2]);
        }
    }


    /**
     * Implements the <i>three point central difference</i> filter.
     * <p>
     * y[n] = 1/2T * ( x[n] - x[n-2] )
     *
     * @author sistgrad
     */
    public static class TpcdFilter extends LmeFilter {
        public TpcdFilter(double T) {
            a = new double[1];
            a[0] = 1 / (2 * T);

            b = new double[3];
            b[0] = 1f;
            b[1] = 0f;
            b[2] = -1f;

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            x[2] = x[1];
            x[1] = x[0];
            x[0] = xnow;

            return a[0] * (x[0] - x[2]);
        }
    }


    /**
     * Implements an <i>improved derivative</i> filter.
     * <p>
     * y[n] = 1/T * ( x[n] - x[n-1] ) + (1 - T) * y[n-1]
     *
     * @author sistgrad
     */
    public static class ImpDerivativeFilter extends LmeFilter {
        public ImpDerivativeFilter(double T, Double initValue) {
            a = new double[2];
            a[0] = 1 / T;
            a[1] = 1 - T;

            b = new double[2];
            b[0] = b[1] = 1f;

            // create x & y arrays
            y = new double[a.length];
            x = new double[b.length];

            if (initValue != null) {
                x[0] = initValue.floatValue();
            }
        }


        /* (non-Javadoc)
         * @see de.lme.plotview.LmeFilter#next(double)
         */
        @Override
        public double next(double xnow) {
            // performance override
            x[1] = x[0];
            x[0] = xnow;

            y[1] = y[0];
            y[0] = a[0] * (x[0] - x[1]) + a[1] * y[1];

            return y[0];
        }
    }


    /**
     * Implements the <i>Butterworth</i> filter.
     * <p>
     * y[n] = sum( b[k] * x[n-k] ) - sum( a[k] * y[n-k] )
     *
     * @author sistgrad
     */
    public static class ButterworthFilter extends LmeFilter {
        // double b[] = { 0.046582f, 0.186332f, 0.279497f, 0.186332f, 0.046583f };
        // double a[] = { 1f, -0.776740f, 0.672706f, -0.180517f, 0.029763f };

        public ButterworthFilter(double[] b_taps, double[] a_taps) {
            // make sure the coefficients are valid
            if (b_taps == null || b_taps.length < 1 || (b_taps.length == 1 && b_taps[0] == 0)
                    || (a_taps != null && a_taps[0] == 0)) {
                throw new InvalidParameterException();
            }

            // copy denominators
            if (a_taps == null) {
                a = new double[1];
                a[0] = 1d;
            } else {
                a = new double[a_taps.length];
                System.arraycopy(a_taps, 0, a, 0, a_taps.length);
            }

            // copy numerators
            b = new double[b_taps.length];
            System.arraycopy(b_taps, 0, b, 0, b_taps.length);

            // create x & y arrays
            y = new double[a_taps.length];
            x = new double[b_taps.length];

        }

        private transient int t_iter = 0;

        public double next(double xnow) {
            double tmp1 = 0;
            double tmp2 = 0;
            if (b.length > 1)
                System.arraycopy(x, 0, x, 1, b.length - 1);
            x[0] = xnow;

            // shift y
            if (a.length > 1)
                System.arraycopy(y, 0, y, 1, a.length - 1);
            y[0] = 0d;

            // sum( b[n] * x[N-n] )
            for (t_iter = 0; t_iter < b.length; ++t_iter) {
                tmp1 += b[t_iter] * x[t_iter];
            }

            // sum( a[n] * y[N-n] )
            for (t_iter = 1; t_iter < a.length; ++t_iter) {
                tmp2 += a[t_iter] * y[t_iter];
            }
            y[0] = tmp1 - tmp2;
            return y[0];
        }

    }

}
