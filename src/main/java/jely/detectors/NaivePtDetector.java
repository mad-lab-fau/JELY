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
package jely.detectors;

import jely.*;
import jely.filter.BandpassButterworth05To10;
import jely.filter.DigitalFilter;
import jely.util.DescriptiveStatistics;

/**
 * A naive implementation looking for the P and T wave by just searching for the maximum amplitude value in a certain
 * range around the QRS. The width is just given by a fixed value. This is mainly for demonstration purposes and should
 * not be used for serious arrhythmia detection.
 * <p>
 * By deault, the P wave is searched for in the last 1/3 RR-interval before the Q peak. The T wave is searched for in the following
 * 1/2 RR-interval after the S peak.
 *
 * @author Stefan Gradl
 */
public class NaivePtDetector implements PWaveDetector, TWaveDetector {

    private double mSamplingRate = 0;
    private DigitalFilter mBpFilter = null;

    private double mTsearchRegionEndRRfrac = 0.5;
    private double mTsearchRegionStart = 0.04;

    private double mPsearchRegionStartRRfrac = 0.3;
    private double mPsearchRegionEnd = 0.03;

    private boolean mUseAbsoluteMaximum = true;

    public NaivePtDetector(double samplingRate) {
        mSamplingRate = samplingRate;
        mBpFilter = BandpassButterworth05To10.newEcgFilter(mSamplingRate);
    }

    public NaivePtDetector(Ecg ecg) {
        this(ecg.getSamplingRate());
    }

    public void setFilter(DigitalFilter filter) {
        mBpFilter = filter;
    }

    public void useAbsoluteMaximum(boolean useAbsoluteMax) {
        mUseAbsoluteMaximum = useAbsoluteMax;
    }

    /**
     * Sets the search regions for the P and T waves. If any of the parameters is set to <code>0</code>, it will not be changed.
     *
     * @param pRegionStartAsRRfrac the start of the search region for the P wave as a fraction of the RR interval to the previous beat.
     * @param pRegionEndInMillis   the end of the search region for the P wave given in milliseconds to the left of the current R peak.
     * @param tRegionStartInMillis the start of the search region for the T wave given in milliseconds to the right of the current R peak.
     * @param tRegionEndAsRRfrac   the end of the search region for the T wave as a fraction of the RR interval to the next beat.
     */
    public void setSearchRegions(double pRegionStartAsRRfrac, int pRegionEndInMillis, int tRegionStartInMillis, double tRegionEndAsRRfrac) {
        if (pRegionStartAsRRfrac != 0)
            mPsearchRegionStartRRfrac = pRegionStartAsRRfrac;
        if (pRegionEndInMillis != 0)
            mPsearchRegionEnd = pRegionEndInMillis / 1000d;
        if (tRegionStartInMillis != 0)
            mTsearchRegionStart = tRegionStartInMillis / 1000d;
        if (tRegionEndAsRRfrac != 0)
            mTsearchRegionEndRRfrac = tRegionEndAsRRfrac;
    }

    @Override
    public TWave findTWave(Ecg ecg, QrsComplex currentQrs) {
        EcgSignal signal = ecg.getSignalFromBestMatchingLead(EcgLead.II);

        QrsComplex nextQrs = currentQrs.getNextQrs();

        if (nextQrs == null)
            return null;

        // calculate the absolute values for the search region
        int startSearch = currentQrs.getRPosition() + (int) (mTsearchRegionStart * mSamplingRate);
        //int startSearch = (int) Math.max( currentQrs.getSPosition(), currentQrs.getRPosition() + 0.04 * mSamplingRate );
        int stopSearch = currentQrs.getRPosition() + (int) (nextQrs.getRRDistance() * mTsearchRegionEndRRfrac);

        // is the search region valid?
        if (stopSearch <= startSearch)
            return null;

        // filter the signal?
        EcgSignal sigFiltered = signal;
        int searchOffsetDueToFilterDelay = 0;
        if (mBpFilter != null) {
            // apply filter
            sigFiltered = signal.applyFilter(mBpFilter);
            searchOffsetDueToFilterDelay = mBpFilter.getGroupDelay();
        }

        startSearch += searchOffsetDueToFilterDelay;
        stopSearch += searchOffsetDueToFilterDelay;

        // search for the max peak
        SignalView pSearchArea = sigFiltered.getSignalView(startSearch, stopSearch);
        DescriptiveStatistics.IndexValue iv;

        if (mUseAbsoluteMaximum)
            iv = DescriptiveStatistics.maxAbs(pSearchArea);
        else
            iv = DescriptiveStatistics.max(pSearchArea);

        TWave tw = new TWave(currentQrs.getHeartbeat());
        int globalPeakIndex = pSearchArea.toGlobalIndex(iv.getIndex()) - searchOffsetDueToFilterDelay;
        tw.setPeakValue(signal.get(globalPeakIndex)); // delay of 3 for 360 Hz only!
        tw.setPeakPosition(globalPeakIndex);
        tw.setOnsetPosition(tw.getPeakPosition() - 10);
        tw.setOffsetPosition(tw.getPeakPosition() + 10);

        return tw;
    }

    @Override
    public PWave findPWave(Ecg ecg, QrsComplex currentQrs) {
        EcgSignal signal = ecg.getSignalFromBestMatchingLead(EcgLead.II);

        if (currentQrs.getPreviousQrs() == null)
            return null;

        // calculate the absolute values for the search region
        int startSearch = currentQrs.getRPosition() - (int) (currentQrs.getRRDistance() * mPsearchRegionStartRRfrac);
        int stopSearch = currentQrs.getRPosition() - (int) (mPsearchRegionEnd * mSamplingRate);
        //int stopSearch = (int) Math.min( currentQrs.getQPosition(), currentQrs.getRPosition() - 0.03 * mSamplingRate );

        // is the search region valid?
        if (stopSearch <= startSearch)
            return null;

        // filter the signal?
        EcgSignal sigFiltered = signal;
        int searchOffsetDueToFilterDelay = 0;
        if (mBpFilter != null) {
            // apply filter
            sigFiltered = signal.applyFilter(mBpFilter);
            searchOffsetDueToFilterDelay = mBpFilter.getGroupDelay();
        }

        startSearch += searchOffsetDueToFilterDelay;
        stopSearch += searchOffsetDueToFilterDelay;

        // search for the max peak
        SignalView pSearchArea = sigFiltered.getSignalView(startSearch, stopSearch);
        DescriptiveStatistics.IndexValue iv;

        if (mUseAbsoluteMaximum)
            iv = DescriptiveStatistics.maxAbs(pSearchArea);
        else
            iv = DescriptiveStatistics.max(pSearchArea);

        PWave pw = new PWave(currentQrs.getHeartbeat());
        int globalPeakIndex = pSearchArea.toGlobalIndex(iv.getIndex()) - searchOffsetDueToFilterDelay;
        pw.setPeakValue(signal.get(globalPeakIndex));
        pw.setPeakPosition(globalPeakIndex);
        pw.setOnsetPosition(pw.getPeakPosition() - 10);
        pw.setOffsetPosition(pw.getPeakPosition() + 10);

        return pw;
    }

}
