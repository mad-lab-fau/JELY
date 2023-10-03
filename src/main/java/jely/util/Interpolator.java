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

/**
 * @author Axel Heinrich
 */
public class Interpolator {

    public static double[] interpolate(double[] x, double[] y, double[] x_new, double fs) {

        // x and y in seconds
        double[] splineTs = new double[4];
        double[] splineValues = new double[4];
        double[] y_new = new double[x_new.length];

        //initialize values used for spline interpolation
        for (int i = 0; i < splineValues.length; i++) {
            splineTs[i] = x[i];
            splineValues[i] = y[i];
        }

        y_new[0] = y[0];
        int c = 0;
        for (int i = 2; i < x.length - 1; i++) {
            for (int j = 1; c + j < x_new.length; j++) {
                if (x_new[c + j] > x[i] && i < x.length - 2) {
                    System.arraycopy(splineTs, 1, splineTs, 0, 3);
                    System.arraycopy(splineValues, 1, splineValues, 0, 3);
                    splineTs[3] = x[i + 2];
                    splineValues[3] = y[i + 2];
                    SplineInterpolator splineInterpolator = new SplineInterpolator(splineTs, splineValues);
                    y_new[c + j] = splineInterpolator.interpolate(x_new[c + j]);
                    c = c + j;
                    break;
                } else if (x_new[c + j] > x[i] && i >= x.length - 2) {
                    SplineInterpolator splineInterpolator = new SplineInterpolator(splineTs, splineValues);
                    y_new[c + j] = splineInterpolator.interpolate(x_new[c + j]);
                } else {
                    SplineInterpolator splineInterpolator = new SplineInterpolator(splineTs, splineValues);
                    y_new[c + j] = splineInterpolator.interpolate(x_new[c + j]);
                }

            }

        }
        return y_new;
    }
}
