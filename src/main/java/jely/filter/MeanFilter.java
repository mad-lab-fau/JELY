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

import jely.util.DescriptiveStatistics;

import java.util.ArrayList;

/**
 * Implements a mean filter.
 *
 * @author Stefan Gradl
 */
public class MeanFilter extends DigitalFilter {
    private ArrayList<Double> xarray;
    private int orderN;

    public MeanFilter(int orderN) {
        //x = new double[orderN];
        xarray = new ArrayList<Double>(orderN);
        this.orderN = orderN;
    }

    /* (non-Javadoc)
     * @see de.fau.mad.jely.filter.DigitalFilter#next(double)
     */
    @Override
    public double next(double xnow) {
        if (xarray.size() == orderN)
            xarray.remove(0);
        xarray.add(xnow);

        //System.arraycopy( x, 0, x, 1, x.length - 1 );
        //x[0] = xnow;

        //return DescriptiveStatistics.calculateMedian( x );
        return DescriptiveStatistics.mean(xarray);
    }

}
