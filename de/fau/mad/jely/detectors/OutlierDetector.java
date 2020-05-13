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
package de.fau.mad.jely.detectors;

import de.fau.mad.jely.util.Outlier;
import de.fau.mad.jely.RRInterval;

import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class OutlierDetector {


    private final double PERCENT = 0.15;
    private final double PERCENT_HIGH = 0.2;
    private final double PERCENT_LOW = 0.1;
    private final int refListLength = 6;
    private double samplingRate;
    private int outlierCounter;
    private ArrayList<Double> rrIReferenceValueList;
    //List for evaluation
    public ArrayList<Outlier> outlierList = new ArrayList<>();

    public ArrayList<Outlier> outlierReplaceList = new ArrayList<>();
    public ArrayList<RRInterval> mRRIArrayList = new ArrayList<>();


    public OutlierDetector(ArrayList<RRInterval> rrIArrayList, double samplingRate) {
        this(rrIArrayList, samplingRate, true);
    }

    public OutlierDetector(ArrayList<RRInterval> rrIArrayList, double samplingRate, boolean outlierCorrection) {
        outlierCounter = 0;
        mRRIArrayList = rrIArrayList;
        rrIReferenceValueList = new ArrayList<>();
        this.samplingRate = samplingRate;
    }

    public void next(int sampleIndex) {

        RRInterval rrI = mRRIArrayList.get(sampleIndex);

        // delete RRIntervals which are 0
        if (rrI.getValue() == 0.0) {
            deleteRRIntervalFromList(sampleIndex);
            next(sampleIndex);
        }

        // don't process already labeled RRIntervals again
        if (rrI.isOutlier() == true) {
            return;
        }

        // first RRInterval: always declared as non-outlier
        // first value is added to the reference list
        if (sampleIndex == 0) {
            // make sure it is a plausible value
            if (rrI.getValue() / samplingRate * 1000 > 300 && rrI.getValue() / samplingRate * 1000 < 1200) {
                rrI.setOutlier(false);
                rrIReferenceValueList.add(rrI.getValue());
                rrI.compReferenceValue(rrIReferenceValueList);
            } else {
                deleteRRIntervalFromList(sampleIndex);
                next(sampleIndex);
            }
            // for 2nd till 6th RRInterval the reference list is built up and outlier detection begins
        } else if (sampleIndex > 0 && sampleIndex < refListLength) {
            locateOutlier(rrI);
            if (rrI.isOutlier() == false) { // make sure that no outlier is in reference list
                rrIReferenceValueList.add(rrI.getValue());
                rrI.compReferenceValue(rrIReferenceValueList);
            } else { //
                deleteRRIntervalFromList(sampleIndex);
                next(sampleIndex);
            }

        } else if (sampleIndex >= refListLength) {
            locateOutlier(rrI);
            // no outlier detection --> RRInterval is added to the reference list
            if (rrI.isOutlier() == false) {
                rrIReferenceValueList.remove(0);
                rrIReferenceValueList.add(rrI.getValue());
                rrI.compReferenceValue(rrIReferenceValueList);
            } else {
                outlierCounter++;
                handleOutlier(rrI, sampleIndex);
            }
        }


    }

    private void handleOutlier(RRInterval rrI, int idx) {
        rrI.setRefRRIVal(rrI.getPreviousRRInterval().getRefRRIVal());
        RRInterval outlier = new Outlier(rrI, idx);
        outlierList.add((Outlier) outlier);
        int flag = ((Outlier) outlier).evaluateOutlier(outlier);

        switch (flag) {
            case (0):
                deleteRRIntervalFromList(idx);
                if (idx < mRRIArrayList.size()) {
                    next(idx);
                }
                break;
            case (1):       // missed RRInterval --> add RRInterval
                addRRIntervalToList(outlier, idx);
                // correct outlier location due to added RRInterval
                ((Outlier) outlier).setOutlierLocation(idx + 1);
                outlierReplaceList.add((Outlier) mRRIArrayList.get(idx));
                outlierReplaceList.add((Outlier) outlier);
                mRRIArrayList.set(idx + 1, outlier);
                break;
            // ectopic heartbeat leads to two consecutive outlier intervals
            case (2):
                RRInterval outlier2 = new Outlier(outlier.getNextRRInterval(), idx + 1);
                //correct timestamps of both outliers
                outlier.setTimeStamp(outlier.getPreviousRRInterval().getTimeStamp() + outlier.getPreviousRRInterval().getRefRRIVal());
                outlier2.setTimeStamp(outlier.getPreviousRRInterval().getTimeStamp() + 2 * outlier.getPreviousRRInterval().getRefRRIVal());
                outlier2.setOutlier(true);
                outlier2.setRefRRIVal(outlier.getPreviousRRInterval().getRefRRIVal());
                outlierReplaceList.add((Outlier) outlier);
                outlierReplaceList.add((Outlier) outlier2);
                mRRIArrayList.set(idx, outlier);
                mRRIArrayList.set(idx + 1, outlier2);
                // integrate outlier in the list
                if (mRRIArrayList.get(idx - 1) != null)
                    mRRIArrayList.get(idx - 1).setNextRRInterval(outlier);
                if (mRRIArrayList.get(idx + 2) != null)
                    mRRIArrayList.get(idx + 2).setPreviousRRInterval(outlier2);
                break;
            case (3):
                if (idx < mRRIArrayList.size() - 2) {
                    outlier.setRPeak2(outlier.getNextRRInterval().getRPeak2());
                    outlier.setTimeStamp((outlier.getRPeak1() + outlier.getRPeak2()) / 2);
                    outlierReplaceList.add((Outlier) outlier);
                    mRRIArrayList.set(idx, outlier);
                    deleteRRIntervalFromList(idx + 1);
                } else {
                    deleteRRIntervalFromList(idx);
                }
                break;
            default:
                return;
        }
    }

    public void locateOutlier(RRInterval rrI) {
        boolean o = percentFilter(rrI);
        /*(rrI1, rrI2);
        aboveFilter(rrI1, rrI2);
        belowFilter(rrI1, rrI2);
        medianFilter(rrI1, rrI2);*/
        rrI.setOutlier(o);
    }

    // compares current RRInterval with the median of the last 6 RRIntervals
    // labels as outlier if difference is above a certain percentage
    private boolean percentFilter(RRInterval rrI) {
        double refRRIVal = 0;
        if (rrI.getPreviousRRInterval() != null) {
            refRRIVal = rrI.getPreviousRRInterval().getRefRRIVal();
        } else {
            refRRIVal = rrI.getValue();
        }
        double percDiff_ref = Math.abs(rrI.getValue() - refRRIVal) / refRRIVal;
        double percDiff_prev = 0;
        if (rrI.getPreviousRRInterval().isOutlier() == false) {  //only if previous RRI is not an outlier
            percDiff_prev = Math.abs(rrI.getValue() - rrI.getPreviousRRInterval().getValue()) / rrI.getPreviousRRInterval().getValue();
        } else {
            percDiff_prev = percDiff_ref;
        }
        if (rrI.getValue() / samplingRate * 1000 > 500) {
            if (percDiff_ref > PERCENT_HIGH && percDiff_prev > PERCENT_HIGH) {
                return true;
            } else {
                return false;
            }
        } else {
            if (percDiff_ref > PERCENT_LOW) {
                return true;
            } else {
                return false;
            }
        }
    }

    // creates a new RRInterval prior to the RRInterval given as an argument
    public RRInterval createRRI(RRInterval rrI) {
        RRInterval prevRRI = rrI.getPreviousRRInterval();
        double newTimeStamp = prevRRI.getTimeStamp() + prevRRI.getRefRRIVal();
        double newValue = prevRRI.getRefRRIVal();
        RRInterval newRRI = new RRInterval(newTimeStamp, newValue);
        newRRI.setRefRRIVal(prevRRI.getRefRRIVal());
        newRRI.setOutlier(true);
        newRRI.setPreviousRRInterval(rrI.getPreviousRRInterval());
        newRRI.setNextRRInterval(rrI);
        rrI.setPreviousRRInterval(newRRI);
        double newTimeStamp2 = prevRRI.getTimeStamp() + 2 * prevRRI.getRefRRIVal();
        rrI.setTimeStamp(newTimeStamp2);
        return newRRI;
    }


    private void deleteRRIntervalFromList(int pos) {
        mRRIArrayList.remove(pos);
        if (pos < mRRIArrayList.size() - 1 && pos > 0 && mRRIArrayList.get(pos - 1) != null) {
            mRRIArrayList.get(pos).setPreviousRRInterval(mRRIArrayList.get(pos - 1));
            mRRIArrayList.get(pos - 1).setNextRRInterval(mRRIArrayList.get(pos));
        }
    }

    private void addRRIntervalToList(RRInterval rrI, int pos) {
        RRInterval newRRI = new Outlier(createRRI(rrI), pos);
        mRRIArrayList.add(pos, newRRI);
    }

    public ArrayList<Outlier> getOutlierReplaceList() {
        return outlierReplaceList;
    }

    public ArrayList<Outlier> getOutlierList() {
        return outlierList;
    }

    public int getOutlierCounter() {
        return outlierCounter;
    }
}

