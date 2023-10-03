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

/**
 * 1-D digital filter that filters the input data, x, using a rational transfer function defined by the numerator and
 * denominator coefficients b and a, respectively. Works exactly like the Matlab command "filter".
 *
 * @author Stefan Gradl
 */
public class DigitalFilter {
    protected double a[] = null;
    protected double b[] = null;
    public double y[] = null;
    public double x[] = null;

    private int groupDelay = 0;

    public double[] getB() {
        return b;
    }

    public double[] getA() {
        return a;
    }

    protected DigitalFilter() {
    }

    /**
     * @param b_taps numerator coefficients
     * @param a_taps denominator coefficients, can be null. if not null, a[0] must not be 0 or an
     *               {@link InvalidParameterException} will be thrown.
     */
    public DigitalFilter(double[] b_taps, double[] a_taps) {
        init(b_taps, a_taps);
    }

    public DigitalFilter(double[] b_taps, double[] a_taps, int groupDelay) {
        init(b_taps, a_taps);
        this.groupDelay = groupDelay;
    }

    protected void init(double[] b_taps, double[] a_taps) {
        // make sure the coefficients are valid
        if (b_taps == null || b_taps.length < 1 || (b_taps.length == 1 && b_taps[0] == 0) || (a_taps != null && a_taps[0] == 0)) {
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
            y[0] -= a[t_iter] * y[t_iter];
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
     * @return the groupDelay
     */
    public int getGroupDelay() {
        return groupDelay;
    }
}
