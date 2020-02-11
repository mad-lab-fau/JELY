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
package de.fau.mad.jely;

import com.sun.scenario.animation.*;

import de.fau.mad.jely.RRInterval;

import java.util.ArrayList;


/**
 * @author Axel Heinrich
 */
public class Outlier extends RRInterval {

    public int flag;
    public int counter;
    public int listLocation;

    public Outlier() {
        counter++;
        flag = 0;
    }

    public Outlier(RRInterval rrI) {
        super(rrI.value, rrI.timeStamp, rrI.refValue, rrI.mPreviousRRInterval, rrI.mNextRRInterval, rrI.outlier, rrI.rPeak1, rrI.rPeak2);
        counter++;
    }

    public Outlier(RRInterval rrI, int listlocation) {
        super(rrI.value, rrI.timeStamp, rrI.refValue, rrI.mPreviousRRInterval, rrI.mNextRRInterval, rrI.outlier, rrI.rPeak1, rrI.rPeak2);
        counter++;
        this.listLocation = listlocation;
    }

    // trying to classify the detected outlier
    public int evaluateOutlier(RRInterval rrI) {
        RRInterval prevRRI = rrI.getPreviousRRInterval();

        // outlier is due to a failed qrs detection --> insert RRInterval
        if (rrI.getValue() > 1.8 * prevRRI.getRefRRIVal() && rrI.getValue() < 2.2 * prevRRI.getRefRRIVal()) {
            flag = 1;
            // outlier is due to an ectopic heartbeat --> correct timestamps of involved RRIntervals
        } else if (rrI.getValue() > prevRRI.getRefRRIVal() * 0.675 && rrI.getValue() < prevRRI.getRefRRIVal() * 0.825
                && rrI.getValue() + rrI.getNextRRInterval().getValue() > 1.8 * prevRRI.getRefRRIVal()
                && rrI.getValue() + rrI.getNextRRInterval().getValue() < 2.2 * prevRRI.getRefRRIVal()) {
            flag = 2;
            // artifact or ectopic heart beat in between two regular heart beats
/*        } else if (rrI.getValue() + rrI.getNextRRInterval().getValue() > 0.8 * prevRRI.getRefRRIVal()
                && rrI.getValue() + rrI.getNextRRInterval().getValue() < 1.2 * prevRRI.getRefRRIVal()){
            flag = 3;*/
            // unknown origin of the outlier --> delete RRInterval
        } else {
            flag = 0;
        }

        return flag;
    }

    public void replaceOutlier(RRInterval[] rrIArray) {

        double interpolationLoc = super.timeStamp;
        int n = rrIArray.length;
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = rrIArray[i].getTimeStamp();
            y[i] = rrIArray[i].getValue();
        }

        SplineInterpolator splineInterpolator = new SplineInterpolator(x, y);

        // start spline interpolation
        double newRRIValue = splineInterpolator.interpolate(interpolationLoc);
        // replace old value with interpolated one and change outlier label
        super.setValue(newRRIValue);
        super.setOutlier(false);
    }


    public void setOutlierLocation(int listLocation) {
        this.listLocation = listLocation;
    }

    public int getOutlierLocation() {
        return listLocation;
    }

    public int getFlag() {
        return flag;
    }

}
