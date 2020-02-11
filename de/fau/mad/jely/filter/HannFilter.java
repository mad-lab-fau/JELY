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
package de.fau.mad.jely.filter;

/**
 * Implements the <i>von-Hann</i> filter using the last 3 values.
 * <p>
 * y[n] = 1/4 * ( x[n] + 2 * x[n-1] + x[n-2] )
 *
 * @author Stefan Gradl
 */
public class HannFilter extends DigitalFilter {

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

    /*
     * (non-Javadoc)
     *
     * @see de.lme.plotview.DigitalFilter#next(double)
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
