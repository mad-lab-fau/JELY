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
package de.fau.lme.ecglib;

import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class Detrender {

    ArrayList<RRInterval> rrIList;
    double[] rrIValues;
    double[] rrITimestamps;
    double[] ys;
    double[] rw;
    double[] res;
    double[] x;
    double[] y;

    public Detrender(ArrayList<RRInterval> rrIList) {

        this.rrIList = rrIList;
        rrIValues = new double[rrIList.size()];
        rrITimestamps = new double[rrIList.size()];

        for (int i = 0; i < rrIList.size(); i++) {
            rrIValues[i] = rrIList.get(i).getValue();
            rrITimestamps[i] = rrIList.get(i).getTimeStamp();
        }

        x = rrITimestamps;
        y = rrIValues;
    }

    public Detrender(double ts[], double[] data) {
        x = ts;
        y = data;
    }

    public void smooth(int f, int nSteps, double delta) {

        int n = x.length;

        ys = new double[n];
        rw = new double[n];
        res = new double[n];

        LowessSmoothing.lowess(x, y, f, nSteps, delta, ys, rw, res);
    }


    public double[] getRes() {
        return res;
    }

    public double[] getYs() {
        return ys;
    }

    public double[] getRRITimestamps() {
        return rrITimestamps;
    }

    public double[] getRRIValues() {
        return rrIValues;
    }
}
