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

import java.lang.Math;

/**
 * Implementation of Elgendis twave detector.
 *
 * @author Reim
 */

public class ElgendiTWaveDetector implements TWaveDetector {

    private Ecg mEcg;
    private EcgSignal mSignal;
    private EcgSignal mSignalFiltered;
    private double mSamplingRate;

    private double mQrsCompensationMean;
    private int mQrsCompensationCount;
    private double mQrsResetMean;
    private int mQrsResetCount;
    private double mQrsInterpolationMean;
    private int mQrsInterpolationCount;
    private double mQrsReentryMean;
    private int mQrsReentryCount;
    private double mQrsArrhythmiasMean;
    private int mQrsArrhythmiasCount;

    public ElgendiTWaveDetector(Ecg ecg) {

        this.mEcg = ecg;
        this.mSignal = mEcg.getSignalFromBestMatchingLead(EcgLead.II);
        this.mSamplingRate = mSignal.getSamplingRate();

        this.mQrsCompensationMean = 0;
        this.mQrsCompensationCount = 0;

        this.mQrsResetMean = 0;
        this.mQrsResetCount = 0;

        this.mQrsInterpolationMean = 0;
        this.mQrsInterpolationCount = 0;

        this.mQrsReentryMean = 0;
        this.mQrsReentryCount = 0;

        this.mQrsArrhythmiasMean = 0;
        this.mQrsArrhythmiasCount = 0;
    }


    @Override
    public TWave findTWave(Ecg ecg, QrsComplex qrs) {

        this.mEcg = ecg;
        this.mSignal = mEcg.getSignalFromBestMatchingLead(EcgLead.II);
        this.mSamplingRate = mSignal.getSamplingRate();

        // we need three qrs complexes
        QrsComplex qrsNext = qrs;
        QrsComplex qrsCurrent = qrs.getPreviousQrs();
        QrsComplex qrsPrevious = null;
        if (qrsCurrent != null)
            qrsPrevious = qrsCurrent.getPreviousQrs();

        if (qrsCurrent == null || qrsPrevious == null)
            return null;


        /*
         * Bandpass Filtering
         */

        // select bounds for cutting out a small signal window
        int signalWindowLowerBound = qrsPrevious.getRPosition();
        int signalWindowUpperBound = qrsNext.getRPosition();

        // subtract the offset since we now work with relative values inside the signal window
        int signalOffset = signalWindowLowerBound;
        int signalRPositionCurrent = qrsCurrent.getRPosition() - signalOffset;
        int signalRPositionPrevious = qrsPrevious.getRPosition() - signalOffset;
        int signalRPositionNext = qrsNext.getRPosition() - signalOffset;

        // copy relevant range to mSignalFiltered
        mSignalFiltered = new MutableEcgSignal(mSignal, signalWindowLowerBound, signalWindowUpperBound);

        // apply filter
        DigitalFilter bPFilter = BandpassButterworth05To10.newEcgFilter(mEcg.getSamplingRate());
        mSignalFiltered = mSignalFiltered.applyFilter(bPFilter);


        /*
         * QRS removal
         */

        int RR1 = signalRPositionCurrent - signalRPositionPrevious;
        int RR2 = signalRPositionNext - signalRPositionCurrent;

        int flatlineFrom = 0;
        int flatlineTo = 0;

        // Reentry
        if (RR2 <= 1.33 * RR1 &&
                RR1 + RR2 <= 0.7 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.222 * mSamplingRate);

            // calculate mean values over all reentry elements
            addQrsReentry(qrsCurrent.getRValue());
        } else if (RR2 <= 0.3 * RR1 &&
                RR1 <= mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.194 * mSamplingRate);

            // calculate mean values over all reentry elements
            addQrsReentry(qrsCurrent.getRValue());
        }

        // Interpolation
        else if (RR2 <= 1.5 * RR1 &&
                RR1 + RR2 <= 1.0 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.250 * mSamplingRate);

            // calculate mean values over all interpolation elements
            addQrsInterpolation(qrsCurrent.getRValue());

        } else if (RR2 <= 0.4 * RR1 &&
                RR1 <= mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.250 * mSamplingRate);

            // calculate mean values over all interpolation elements
            addQrsInterpolation(qrsCurrent.getRValue());
        }


        // Reset
        else if (RR2 <= 1.76 * RR1 &&
                RR1 + RR2 <= 1.8 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.333 * mSamplingRate);

            // calculate mean values over all reset elements
            addQrsReset(qrsCurrent.getRValue());
        } else if (RR2 <= 0.65 * RR1 &&
                RR1 <= mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.278 * mSamplingRate);

            // calculate mean values over all reset elements
            addQrsReset(qrsCurrent.getRValue());
        }


        // Compensation
        else if (RR2 <= 1.35 * RR1 &&
                RR1 + RR2 <= 2.0 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.278 * mSamplingRate);

            // calculate mean values over all compensation elements
            addQrsCompensation(qrsCurrent.getRValue());

        } else if (RR2 <= 0.8 * RR1 &&
                RR1 <= mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.361 * mSamplingRate);

            // calculate mean values over all compensation elements
            addQrsCompensation(qrsCurrent.getRValue());

        }


        // PVC Bigeminy
        else if (RR2 >= 0.9 * mSamplingRate &&
                RR1 >= 0.9 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.111 * mSamplingRate);

            // calculate mean values over all arrythmia elements
            addQrsArrhythmias(qrsCurrent.getRValue());

        }

        // PVC Trigeminy
        else if (RR2 >= 1.2 * mSamplingRate) {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.139 * mSamplingRate);

            // calculate mean values over all arrythmia elements
            addQrsArrhythmias(qrsCurrent.getRValue());

        }

        // Normal Beat
        else {

            // calculate range for flatlining the qrs interval
            flatlineFrom = signalRPositionCurrent - (int) (0.027 * mSamplingRate);
            flatlineTo = signalRPositionCurrent + (int) (0.250 * mSamplingRate);

        }

        // Remove qrs signal in the calculated range
        flatLine(flatlineFrom, flatlineTo);
        //flatLine(signalRPositionCurrent - (int)(0.05 * factor), signalRPositionCurrent + (int)(0.05 * factor));



        /*
         * Blocks of Interest
         */

        // k is the highest meanvalue from all qrs types
        double k = findMax(mQrsReentryMean,
                mQrsInterpolationMean,
                mQrsResetMean,
                mQrsCompensationMean,
                mQrsArrhythmiasMean);

        // calculate window size for block of interest in ms
        double w1_d = 0.070 * mSamplingRate * k * 1000;
        double w2_d = 0.140 * mSamplingRate * k * 1000;

        // round w1_d and w2_d to nearest odd integer
        int w1 = (int) (2 * Math.floor(w1_d / 2) + 1);
        int w2 = (int) (2 * Math.floor(w2_d / 2) + 1);



        /*
         * Thresholding
         */

        int tLowerBound = -1;
        int tUpperBound = -1;
        int tPosition = -1;
        double tValue = -1;

        TWave result = new TWave(qrs.getHeartbeat());
        result.setPeakValue(-1);
        result.setPeakPosition(-1);


        // calculating bounds tMin and tMax for twavedetection in R interval
        int tMin = signalRPositionCurrent + (int) (0.170 * mSamplingRate);
        int tMax = signalRPositionCurrent + (int) (0.800 * mSamplingRate);


        // assuming threshold equals w1 in seconds
        int thr1 = w1 / 1000;

        // searching for 'block of interest' by the calculated bounds tMin and tMax
        for (int n = tMin; n <= tMax; ++n) {

            double maPeak = movingAverage(n, w1);
            double maTwave = movingAverage(n, w2) + thr1;

            // searching lower bound of first twave block
            if (maPeak > maTwave &&
                    tLowerBound == -1) {

                tLowerBound = n;
            }

            // searching upper bound of first twave block
            if (maPeak < maTwave &&
                    tLowerBound != -1) {

                tUpperBound = n;
                break;
            }

        }

        // check whether twave found or not
        if (tLowerBound != -1 &&
                tUpperBound != -1) {

            // twave found -> search for peak in block
            tValue = Double.NEGATIVE_INFINITY;
            for (int i = tLowerBound; i <= tUpperBound; ++i) {

                if (getSignalValue(i) > tValue) {
                    tValue = getSignalValue(i);
                    tPosition = i;
                }

            }

            // setting return value with its position offset for absolute position
            result.setPeakValue(tValue);
            result.setPeakPosition(tPosition + signalOffset);

        } else {
            // no twave found... do nothing
            // result contains -1 as value and position
        }

        return result;
    }


    /*
     *  Helpers
     */

    /**
     * Returns the signals value at specific position
     *
     * @param position [int]
     * @return value [double]
     */
    private double getSignalValue(int position) {

        double value = mSignalFiltered.get(position);

        return value;
    }

    /**
     * Sets the signal at specific position to the given value
     *
     * @param position [int], value [double]
     * @return void
     */
    private void setSignalValue(int position, double value) {

        mSignalFiltered.set(position, value);

        return;
    }

    /**
     * Calculates the moving average peak over the given window size and position n
     *
     * @param n [int], window [int]
     * @return result [double]
     */
    private double movingAverage(int n, int window) {

        double sum = 0;

        int from = (-1) * (window - 1) / 2;
        int to = (window - 1) / 2;

        for (int i = from; i <= to; i++) {

            sum = sum + getSignalValue((n + i));

        }

        return sum / window;
    }

    /**
     * Add new Value to Mean Value of QrsCompensation and increase Counter
     *
     * @param value [double]
     * @return void
     */
    private void addQrsCompensation(double value) {

        mQrsCompensationMean = ((mQrsCompensationCount * mQrsCompensationMean) + value) / (++mQrsCompensationCount * mSamplingRate);

        return;
    }

    /**
     * Add new Value to Mean Value of QrsReset and increase Counter
     *
     * @param value [double]
     * @return void
     */
    private void addQrsReset(double value) {

        mQrsResetMean = ((mQrsResetCount * mQrsResetMean) + value) / (++mQrsResetCount * mSamplingRate);

        return;
    }

    /**
     * Add new Value to Mean Value of QrsInterpolation and increase Counter
     *
     * @param value [double]
     * @return void
     */
    private void addQrsInterpolation(double value) {

        mQrsInterpolationMean = ((mQrsInterpolationCount * mQrsInterpolationMean) + value) / (++mQrsInterpolationCount * mSamplingRate);

        return;
    }

    /**
     * Add new Value to Mean Value of QrsReentry and increase Counter
     *
     * @param value [double]
     * @return void
     */
    private void addQrsReentry(double value) {

        mQrsReentryMean = ((mQrsReentryCount * mQrsReentryMean) + value) / (++mQrsReentryCount * mSamplingRate);

        return;
    }

    /**
     * Add new Value to Mean Value of QrsArrhythmias and increase Counter
     *
     * @param value [double]
     * @return void
     */
    private void addQrsArrhythmias(double value) {

        mQrsArrhythmiasMean = ((mQrsArrhythmiasCount * mQrsArrhythmiasMean) + value) / (++mQrsArrhythmiasCount * mSamplingRate);

        return;
    }

    /**
     * Flattens the ECG signal in the given range
     *
     * @param from [int], to [int]
     * @return void
     */
    private void flatLine(int from, int to) {

        if (from > 0 && to > 0) {
            for (int i = from; i <= to; ++i) {

                setSignalValue(i, 0.0);
            }
        }

        return;
    }

    /**
     * Finding the maximum of double values
     *
     * @param values [double]
     * @return maximum [double]
     */
    private double findMax(double... vals) {

        double max = Double.NEGATIVE_INFINITY;

        for (double d : vals) {
            if (d > max) max = d;
        }

        return max;
    }

}