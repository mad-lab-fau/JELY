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
package jely.processors;

import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class BRProcessor {

    double[] br;
    ArrayList<Integer> breaks;
    ArrayList<Double> vals;
    double[] br_section;
    double maxError;

    public BRProcessor(double[] f_rsa, double maxError) {
        br = new double[f_rsa.length];
        for (int i = 0; i < f_rsa.length; i++) {
            br[i] = f_rsa[i] * 60;
        }
        this.maxError = maxError;
        breaks = new ArrayList<>();
        vals = new ArrayList<>();
    }

    public ArrayList<Integer> slidingWindow() {
        int right = br.length;
        double[] err = new double[br.length];
        double MSE;

        for (int left = br.length - 2; left > 1; left--) {
            int c = 0;
            br_section = new double[right - left];
            for (int j = left; j < right; j++) {
                br_section[c] = br[j];
                c++;
            }
            err = linearRegression(br_section);
            MSE = mean(square(err));

            if (MSE > maxError) {
                breaks.add(left + 1);
                vals.add(br[left + 1]);
                right = left;
            }
        }

        return breaks;
    }

    public double[] linearRegression(double[] data) {
        int n = data.length;
        double[] x = new double[n];
        double[] y = new double[n];
        double[] err = new double[n];

        double mean = mean(data);
        double sumxy = 0;
        double sumxx = 0;
        double k;

        for (int i = 0; i < n; i++) {
            x[i] = (double) i + 1 - (double) (n + 1) / 2;
            y[i] = data[i] - mean;
            sumxy = sumxy + x[i] * y[i];
            sumxx = sumxx + x[i] * x[i];
        }

        k = sumxy / sumxx;

        for (int i = 0; i < n; i++) {
            err[i] = y[i] - k * x[i];
        }

        return err;
    }

    private double mean(double[] array) {
        double mean = 0;
        double sum = 0;
        int n = array.length;

        for (int i = 0; i < array.length; i++) {
            sum = sum + array[i];
        }
        mean = sum / n;

        return mean;
    }

    private double[] square(double[] array) {

        double[] square = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            square[i] = array[i] * array[i];
        }

        return square;
    }
}
