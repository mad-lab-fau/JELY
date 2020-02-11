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

import java.util.ArrayList;

/**
 * This class allows the use of multiple filters with different time-delays in a consistent manner.
 *
 * @author Stefan Gradl
 */
public class TimeDelayedFilterArray {
    private ArrayList<DigitalFilter> filterArray = new ArrayList<DigitalFilter>();
    private ArrayList<double[]> yBuffers = new ArrayList<double[]>();
    private int maxDelayDoubled;
    private double[] curResult;

    public TimeDelayedFilterArray(ArrayList<DigitalFilter> filterArray) {
        this.filterArray = filterArray;

        maxDelayDoubled = getMaxGroupDelay() * 2;
        for (int i = 0; i < filterArray.size(); i++) {
            // we would only need maxDelay/2, right?
            yBuffers.add(new double[maxDelayDoubled]);
        }

        curResult = new double[filterArray.size()];
    }

    public int getMaxGroupDelay() {
        int maxDelay = 0;
        for (DigitalFilter digitalFilter : filterArray) {
            if (digitalFilter.getGroupDelay() > maxDelay)
                maxDelay = digitalFilter.getGroupDelay();
        }
        return maxDelay;
    }


    public double[] next(double x) {
        for (int i = 0; i < filterArray.size(); i++) {
            double[] yb = yBuffers.get(i);
            DigitalFilter f = filterArray.get(i);

            System.arraycopy(yb, 0, yb, 1, yb.length - 1);
            yb[0] = f.next(x);

            if (f.getGroupDelay() * 2 == maxDelayDoubled)
                curResult[i] = yb[0];
            else
                curResult[i] = yb[maxDelayDoubled / 2 - f.getGroupDelay()];
        }


        return curResult;
    }
}
