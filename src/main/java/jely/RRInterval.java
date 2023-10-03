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
package jely;

import jely.Heartbeat;
import jely.QrsComplex;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Axel Heinrich
 */
public class RRInterval {

    protected double refValue;
    protected double value;
    protected double timeStamp;
    protected RRInterval mPreviousRRInterval;
    protected RRInterval mNextRRInterval;
    protected boolean outlier;
    protected double rPeak1;
    protected double rPeak2;

    public RRInterval() {
    }

    public RRInterval(Heartbeat heartbeat) {
        refValue = 0;

        QrsComplex qrs = heartbeat.getQrs();
        value = qrs.getRRDistance();
        rPeak1 = qrs.getPreviousQrs().getRPosition();
        rPeak2 = qrs.getRPosition();
        timeStamp = (rPeak1 + rPeak2) / 2;
    }

    public RRInterval(double a, double b) {
        value = b;
        timeStamp = a;
    }

    public RRInterval(double val, double ts, double refVal, RRInterval prevInt, RRInterval nexInt, boolean o, double rPeak1, double rPeak2) {
        this.value = val;
        this.timeStamp = ts;
        this.refValue = refVal;
        this.mPreviousRRInterval = prevInt;
        this.mNextRRInterval = nexInt;
        this.rPeak1 = rPeak1;
        this.rPeak2 = rPeak2;

        this.outlier = o;
    }

    public void compAvgValue(ArrayList<Double> rrIList, int n) {
        double sumValue = 0;
        for (int i = 0; i < rrIList.size(); i++) {
            sumValue = sumValue + rrIList.get(i);
        }
        refValue = sumValue / n;
    }

    // reference value is computed using the median
    public void compReferenceValue(ArrayList<Double> rrIList) {
        ArrayList<Double> sortedList = new ArrayList<Double>(rrIList);
        Collections.sort(sortedList);
        refValue = median(sortedList);
    }

    private double median(ArrayList<Double> m) {
        int middle = m.size() / 2;
        if (m.size() % 2 == 1) {
            return m.get(middle);
        } else {
            return (m.get(middle - 1) + m.get(middle)) / 2.0;
        }
    }

    public void setValue(double val) {
        value = val;
    }

    public double getValue() {
        return value;
    }

    public void setTimeStamp(double ts) {
        timeStamp = ts;
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return the previousRRInterval
     */
    public RRInterval getPreviousRRInterval() {
        return mPreviousRRInterval;
    }

    /**
     * @param previousRRInterval the previousRRInterval to set
     */
    public void setPreviousRRInterval(RRInterval previousRRInterval) {
        mPreviousRRInterval = previousRRInterval;
    }

    /**
     * @return the nextRRInterval
     */
    public RRInterval getNextRRInterval() {
        return mNextRRInterval;
    }

    /**
     * @param nextRRInterval the nextRRInterval to set
     */
    public void setNextRRInterval(RRInterval nextRRInterval) {
        mNextRRInterval = nextRRInterval;
    }

    public void setRefRRIVal(double ref) {
        refValue = ref;
    }

    public double getRefRRIVal() {
        return refValue;
    }

    public void setOutlier(boolean b) {
        outlier = b;
    }

    public boolean isOutlier() {
        return outlier;
    }

    public double getRPeak1() {
        return rPeak1;
    }

    public void setRPeak1(double rPeak) {
        rPeak1 = rPeak;
    }

    public double getRPeak2() {
        return rPeak2;
    }

    public void setRPeak2(double rPeak) {
        rPeak2 = rPeak;
    }


}
