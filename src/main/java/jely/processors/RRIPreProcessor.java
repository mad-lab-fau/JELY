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

import jely.Ecg;
import jely.Outlier;
import jely.RRInterval;
import jely.detectors.OutlierDetector;
import jely.hrv.RRIntervalCalculator;

import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class RRIPreProcessor {

    Ecg mEcg;

    double samplingRate;
    RRIntervalCalculator mRRIntervalCalculator;
    OutlierDetector mOutlierDetector;
    Detrender mDetrender;
    ArrayList<RRInterval> rrIList;
    ArrayList<Outlier> outlierList;
    ArrayList<Outlier> outlierReplaceList;
    boolean doOutlierInterpolation;
    int numOutliers;
    int rrIList_origlength;
    int mProtocol[] = new int[4];
    double[] trend;
    double[] ts;
    double[] val;
    double[] res;
    int start;
    int step_inc;
    int step_len;
    int pause;
    int[] rrIPerStage;
    int[] outlierPerStage;
    double[] outlierRatio;
    int endExIdx;

    /**
     * @param ecg
     * @param protocol
     */
    public RRIPreProcessor(Ecg ecg, int[] protocol) {
        this(ecg, protocol, true);
    }

    public RRIPreProcessor(Ecg ecg, int[] protocol, boolean doOutlierInterpolation) {
        mEcg = ecg;
        start = protocol[0];
        step_inc = protocol[1];
        step_len = protocol[2];
        pause = protocol[3];
        samplingRate = mEcg.getSamplingRate();
        mRRIntervalCalculator = new RRIntervalCalculator(ecg);
        rrIList = mRRIntervalCalculator.getRRList();
        rrIList_origlength = rrIList.size();
        mOutlierDetector = new OutlierDetector(rrIList, samplingRate);
        mDetrender = new Detrender(rrIList);
        this.doOutlierInterpolation = doOutlierInterpolation;
        numOutliers = 0;
        trend = new double[rrIList.size()];
        ts = new double[rrIList.size()];
        val = new double[rrIList.size()];
        res = new double[rrIList.size()];
    }

    public void preProcessing() {

        // general
        double rec_length = rrIList.get(rrIList.size() - 1).getTimeStamp() / samplingRate;
        ArrayList<RRInterval> rrIList_orig = new ArrayList<RRInterval>(rrIList);

        // outlier calculation
        boolean useOutlierThreshold = true;
        double outlierThreshold = 0.05;

        // detrending parameters
        int f = 50;
        int nSteps = 2;
        double delta = 100.0;

        // identify outliers and create outlier list
        findOutlier();

        outlierList = mOutlierDetector.getOutlierList();

        interpolateOutliers();

        mDetrender = new Detrender(rrIList);
        mDetrender.smooth(f, nSteps, delta);

        val = mDetrender.getRRIValues();
        ts = mDetrender.getRRITimestamps();
        // the curve fitted to the data
        trend = mDetrender.getYs();
        // curve representing original - fitted curve
        res = mDetrender.getRes();


        // minimum of the fitted curve equals end of the exercise load --> regeneration begins
        endExIdx = calcEndExIdx();
        double endTs = ts[endExIdx] / samplingRate;

        double fact = endTs / (double) (step_len + pause);
        int numStages = (int) Math.floor(fact);
        double dec = (fact - (double) numStages) * 100;
 /*       if(dec > 16){
            numStages++;
        }*/

        // outlier evaluation: count RRIs per stage
        rrIPerStage = new int[numStages + 2];
        outerloop:
        for (int j = 0; j < rrIList_orig.size(); j++) {
            double rrILoc = rrIList_orig.get(j).getTimeStamp() / samplingRate;
            for (int i = 0; i < numStages + 2; i++) {
                if (rrILoc > i * (step_len + pause) && rrILoc <= (i + 1) * (step_len + pause)) {
                    rrIPerStage[i] = rrIPerStage[i] + 1;
                    continue outerloop;
                } else if (rrILoc > numStages * (step_len + pause) && rrILoc <= fact * (step_len + pause)) {
                    rrIPerStage[numStages] = rrIPerStage[numStages] + 1;
                    continue outerloop;
                } else if (rrILoc > endTs && rrILoc <= rec_length) {
                    rrIPerStage[numStages + 1] = rrIPerStage[numStages + 1] + 1;
                    continue outerloop;
                }
            }
        }

        // outlier evaluation: how many outliers per stage
        outlierPerStage = new int[numStages + 2];
        outerloop:
        for (int j = 0; j < outlierList.size(); j++) {
            double outLoc = outlierList.get(j).getTimeStamp() / samplingRate;
            for (int i = 0; i < numStages + 2; i++) {
                if (outLoc > i * (step_len + pause) && outLoc <= (i + 1) * (step_len + pause)) {
                    outlierPerStage[i] = outlierPerStage[i] + 1;
                    continue outerloop;
                } else if (outLoc > numStages * (step_len + pause) && outLoc <= fact * (step_len + pause)) {
                    outlierPerStage[numStages] = outlierPerStage[numStages] + 1;
                    continue outerloop;
                } else if (outLoc > endTs && outLoc <= rec_length) {
                    outlierPerStage[numStages + 1] = outlierPerStage[numStages + 1] + 1;
                    continue outerloop;
                }
            }
        }

        // calculate outlier ratio in every stage
        outlierRatio = new double[outlierPerStage.length];
        for (int i = 0; i < outlierPerStage.length; i++) {
            outlierRatio[i] = (double) outlierPerStage[i] / (double) rrIPerStage[i];
        }


        /* try {
            if (outlierRatio <= outlierThreshold || (outlierRatio > outlierThreshold && useOutlierThreshold == false)) {
                interpolateOutliers();
            } else {
                throw new TooManyOutliersException();
            }
        } catch (TooManyOutliersException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }*/

    }

    private void findOutlier() {

        for (int i = 0; i < rrIList.size(); i++) {

            mOutlierDetector.next(i);

        }
    }

    // spline interpolation needs two RRIntervals before and after the outlier
    private void interpolateOutliers() {

        outlierReplaceList = mOutlierDetector.getOutlierReplaceList();
        outerloop:
        for (int i = 0; i < outlierReplaceList.size(); i++) {

            int outlierLoc = outlierReplaceList.get(i).getOutlierLocation();
            Outlier outlier = outlierReplaceList.get(i);
            RRInterval splineRRI[] = new RRInterval[4];
            int j = 1;
            int c = 0;
            // looking for valid RRintervals (no outliers)
            while (c < 2) {
                if (outlierLoc - j >= 0 && rrIList.get(outlierLoc - j).isOutlier() == false) {
                    splineRRI[1 - c] = rrIList.get(outlierLoc - j);
                    c++;
                } else if (outlierLoc - j == -1) {
                    splineRRI[1 - c] = rrIList.get(0);
                    c++;
                }
                j++;
            }

            j = 1;
            while (c < 4) {
                if (outlierLoc + j < rrIList.size() && rrIList.get(outlierLoc + j).isOutlier() == false) {
                    splineRRI[c] = rrIList.get(outlierLoc + j);
                    c++;
                    //not enough valid RRIntervals in the list --> remove outlier and successive RRIntervals
                } else if (outlierLoc + j >= rrIList.size()) {
                    for (int k = rrIList.size() - 1; k >= outlierLoc; k--) {
                        rrIList.remove(k);
                    }
                    break outerloop;
                }
                j++;
            }
            // assembelled List is used for spline interpolation to replace the outlier
            outlier.replaceOutlier(splineRRI);
        }
    }


    public int findMinimumIndex(double[] values) {
        int minIdx = 0;
        double min = values[0];
        for (int i = 0; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
                minIdx = i;
            }
        }
        return minIdx;
    }

    public int calcEndExIdx() {
        int minIdx = findMinimumIndex(trend);
        int newMinIdx = minIdx;
        int c = 0;
        while ((ts[minIdx + c] - ts[minIdx]) / samplingRate < 100 && minIdx + c + 1 < ts.length) {
            if (trend[minIdx + c] - trend[minIdx + c - 1] < 0 && trend[minIdx + c + 1] - trend[minIdx + c] > 0 && trend[minIdx + c] < 1.2 * trend[minIdx]) {
                newMinIdx = minIdx + c;
            }
            c++;
        }
        return newMinIdx;
    }

    public RRIntervalCalculator getRRIntervalList() {
        return mRRIntervalCalculator;
    }

    public ArrayList<RRInterval> getRRIArrayList() {
        return rrIList;
    }

    public int getNumOutlier() {
        return numOutliers;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public ArrayList<Outlier> getOutlierList() {
        return mOutlierDetector.getOutlierList();
    }

    public int[] getOutlierPerStage() {
        return outlierPerStage;
    }

    public int[] getRRIPerStage() {
        return rrIPerStage;
    }

    public double[] getOutlierRatio() {
        return outlierRatio;
    }

    public double[] getTrend() {
        return trend;
    }

    public double[] getTs() {
        return ts;
    }

    public double[] getVal() {
        return val;
    }

    public double[] getRes() {
        return res;
    }

    public double getMinPos() {
        return endExIdx;
    }


}


class TooManyOutliersException extends Exception {
    TooManyOutliersException() {
        super("The amount of detected outliers is too damn high!");
    }
}