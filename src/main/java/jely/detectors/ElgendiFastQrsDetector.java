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

import de.fau.shiftlist.*;
import jely.*;
import jely.filter.BandpassButterworth8To21;
import jely.filter.DigitalFilter;
import jely.filter.TimeDelayedFilterArray;
import jely.processors.RPeakMaxRefinement;
import jely.processors.RPeakSlacknessReduction;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the knowledge-based fast QRS detector of Elgendi, including a
 * temporal correction of the detected R peaks.
 *
 * @author Stefan Gradl
 */
public class ElgendiFastQrsDetector extends QrsDetector {
    // BANDPASS FILTER
    private DigitalFilter bpFilter = null;
    // MOVING AVERAGE FILTERS
    private int w1, w2, w3;
    private DigitalFilter ma1Filter;
    private DigitalFilter ma2Filter;
    private DigitalFilter ma3Filter;
    private TimeDelayedFilterArray maFilters;
    // THRESHOLDS
    private final double BETA = 0.07;
    private double thr1, thr2;
    private boolean thr1Flag;
    private double blankingInterval;
    // QRS DETECTION
    private double bpFiltered;
    private double squared;
    private double ma1Filtered;
    private double ma2Filtered;
    private double ma3Filtered;
    private int qrsStart;
    private int qrsEnd;
    private int counter;
    private List<Double> originalValues;
    private List<Double> squaredValues;
    private QrsComplex previousQrsComplex;
    private QrsComplex currentQrsComplex;
    private ShiftListDouble currentQrsBuffer;
    // private List<QrsComplex> QrsComplexes;

    private int numberOfValuesLeftToRPeak;
    private int numberOfValuesRightToRPeak;
    private int delayToOriginalSignal;

    // number of values that have to be stored before qrsStart in order to have
    // valid indices
    // for the slackness correction and the feature extraction
    private int bufferSize;

    protected ElgendiFastQrsDetector(double samplingRate, EcgLead lead) {
        super(samplingRate, new LeadConfiguration(lead));
    }

    /**
     * Creates a <code>ElgendiFastQrsDetector</code> object with the specified
     * sampling rate and the specified flag stating whether the temporal
     * correction of the R peaks has to be applied or not.
     */
    public ElgendiFastQrsDetector(Ecg ecg) {
        this(ecg, true);
    }

    /**
     * Creates a <code>ElgendiFastQrsDetector</code> object with the specified
     * sampling rate and the specified flag stating whether the temporal
     * correction of the R peaks has to be applied or not.
     */
    public ElgendiFastQrsDetector(Ecg ecg, boolean applyPostProcessing) {
        super(ecg);

        bpFilter = BandpassButterworth8To21.newEcgFilter(mEcg.getSamplingRate());

        // set first moving average filter (QRS complex)
        w1 = (int) Math.round(0.0972222 * mEcg.getSamplingRate());
        double[] ma1_a = {1};
        double[] ma1_b = new double[w1];
        for (int i = 0; i < w1; i++) {
            ma1_b[i] = 1.0 / ((double) w1);
        }
        ma1Filter = new DigitalFilter(ma1_b, ma1_a, w1 / 2);
        // set second moving average filter (heart beat)
        w2 = (int) Math.round(0.6111 * mEcg.getSamplingRate());
        double[] ma2_a = {1};
        double[] ma2_b = new double[w2];
        for (int i = 0; i < w2; i++) {
            ma2_b[i] = 1.0 / ((double) w2);
        }
        ma2Filter = new DigitalFilter(ma2_b, ma2_a, w2 / 2);
        // set third moving average filter (threshold)
        w3 = (int) Math.round(2 * mEcg.getSamplingRate());
        double[] ma3_a = {1};
        double[] ma3_b = new double[w3];
        for (int i = 0; i < w3; i++) {
            ma3_b[i] = 1.0 / ((double) w3);
        }
        ma3Filter = new DigitalFilter(ma3_b, ma3_a, w3 / 2);
        ArrayList<DigitalFilter> filterArray = new ArrayList<DigitalFilter>();
        filterArray.add(ma1Filter);
        filterArray.add(ma2Filter);
        filterArray.add(ma3Filter);
        maFilters = new TimeDelayedFilterArray(filterArray);
        // init thresholds
        thr1Flag = false;
        thr2 = w1;
        blankingInterval = (int) Math.round(0.1 * mEcg.getSamplingRate());
        // init QRS detection
        counter = 0;

        originalValues = new ArrayList<Double>();
        squaredValues = new ArrayList<Double>();
        currentQrsComplex = null;
        previousQrsComplex = null;
        // QrsComplexes = new ArrayList<QrsComplex>();
        mQrsList = new ShiftListObject(30);
        numberOfValuesLeftToRPeak = (int) Math.round(0.12 * mEcg.getSamplingRate());
        numberOfValuesRightToRPeak = (int) Math.round(0.28 * mEcg.getSamplingRate());

        int t1 = (int) Math.round(0.2 * mEcg.getSamplingRate());

        delayToOriginalSignal = bpFilter.getGroupDelay() + maFilters.getMaxGroupDelay();
        // delayToOriginalSignal = maFilters.getMaxGroupDelay();
        bufferSize = Math.round(t1 / 2) + numberOfValuesLeftToRPeak + delayToOriginalSignal;

        currentQrsBuffer = new ShiftListDouble(numberOfValuesLeftToRPeak + numberOfValuesRightToRPeak);

        if (applyPostProcessing) {
            // if we have a lead II available, we can use a simple
            // max-refinement,
            // if not, we need to use a more sophisticated refinement
            if (ecg.hasLead(EcgLead.II))
                addPostProcessor(new RPeakMaxRefinement(mEcg.getSamplingRate()));
            else
                addPostProcessor(new RPeakSlacknessReduction(mEcg.getSamplingRate()));
        }

    }

    public ArrayList<Double> mDebugSignal = new ArrayList<>();

    public double[] getDebugSignal() {
        double[] da = new double[mDebugSignal.size()];
        for (int i = 0; i < da.length; i++) {
            da[i] = mDebugSignal.get(i);
        }
        return da;
    }

    /**
     * Returns the values of all important variables for the algorithm for
     * debugging purposes.
     *
     * @return double Array of debugging values.
     */
    public double[] debugOutput() {
        double out[] = new double[7];

        out[0] = bpFiltered;
        out[1] = squared;

        out[2] = ma1Filtered;
        out[3] = ma2Filtered;
        out[4] = ma3Filtered;

        out[5] = thr1;

        out[6] = counter;

        return out;
    }

    public double getBandpassFilteredValue() {
        return bpFiltered;
    }

    @Override
    public QrsComplex next(int sampleIndex) {
        EcgSignal signal = mEcg.getSignalFromBestMatchingLead(EcgLead.II);

        double value;
        if (sampleIndex >= 0) {
            value = signal.get(sampleIndex);
        } else {
            value = signal.getSignal().getHeadValue();
        }

        // init return value
        QrsComplex detectedQrsComplex = null;

        // apply bandpass filter
        bpFiltered = bpFilter.next(value);

        // square bandpass filtered value
        squared = bpFiltered * bpFiltered;
        // apply moving average filters
        // ma1Filtered = ma1Filter.next( squared );
        // ma2Filtered = ma2Filter.next( squared );
        // ma3Filtered = ma3Filter.next( squared );

        double[] res = maFilters.next(squared);
        ma1Filtered = res[0];
        ma2Filtered = res[1];
        ma3Filtered = res[2];

        // compute threshold
        thr1 = (BETA * ma3Filtered) + ma2Filtered;

        if (Ecglib.isDebugMode()) {
            mDebugSignal.add(ma2Filtered);
        }

        // R peak detected but QRS detection not finished
        if (currentQrsComplex != null) {
            currentQrsBuffer.add(value);
            // QRS detection finished
            if (currentQrsBuffer.size() >= numberOfValuesLeftToRPeak + numberOfValuesRightToRPeak) {
                currentQrsComplex.setSampleIndexEnd(currentQrsComplex.getRPosition() + numberOfValuesRightToRPeak);
                detectedQrsComplex = currentQrsComplex;
                onQrsComplexFound(currentQrsComplex);
                previousQrsComplex = currentQrsComplex;
                currentQrsComplex = null;
            }
        }

        // test for threshold crossing (below thr1 -> above thr1)
        if (ma1Filtered > thr1 && !thr1Flag) {
            // test for valid QRS complex
            if (qrsEnd - qrsStart >= thr2) {
                // test for minimum blanking interval
                if (qrsStart > 0 && counter - qrsEnd > blankingInterval) {

                    // search maximum absolute value in the bandpass filtered
                    // and squared signal
                    // (range [qrsStart,qrsEnd])
                    double absMax = 0;
                    int absMaxPosition = 0;
                    // delayToOriginalSignal + bpFilter.getGroupDelay():
                    // groupDelay correction not necessary
                    // when accessing squared FILTERED values
                    for (int i = 0; i <= qrsEnd - qrsStart; i++) {
                        if (Math.abs(squaredValues.get(qrsStart - delayToOriginalSignal + bpFilter.getGroupDelay()
                                - counter + squaredValues.size() + 1 + i)) > absMax) {
                            absMax = Math.abs(squaredValues.get(qrsStart - delayToOriginalSignal
                                    + bpFilter.getGroupDelay() - counter + squaredValues.size() + 1 + i));
                            absMaxPosition = i;
                        }
                    }

                    // set found value as R peak
                    currentQrsComplex = new QrsComplex(signal);
                    currentQrsComplex.setRPeak(qrsStart + absMaxPosition - delayToOriginalSignal + 1,
                            signal.get(qrsStart + absMaxPosition - delayToOriginalSignal + 1));

                    // get position of the R Peak in the original signal
                    int relativePosition = absMaxPosition + bufferSize - delayToOriginalSignal;

                    // add values left to R peak to QRS complex
                    int listStart = relativePosition - numberOfValuesLeftToRPeak;
                    if (listStart < 0)
                        listStart = 0;

                    currentQrsBuffer.addAll(originalValues.subList(listStart, relativePosition));
                    currentQrsComplex
                            .setSampleIndexStart(qrsStart + relativePosition - bufferSize - numberOfValuesLeftToRPeak);

                    // add values right to R peak to QRS complex
                    currentQrsBuffer.addAll(originalValues.subList(relativePosition,
                            Math.min(relativePosition + numberOfValuesRightToRPeak, originalValues.size())));

                    // QRS detection finished
                    if (currentQrsBuffer.size() >= numberOfValuesLeftToRPeak + numberOfValuesRightToRPeak) {
                        currentQrsComplex
                                .setSampleIndexEnd(currentQrsComplex.getRPosition() + numberOfValuesRightToRPeak);
                        detectedQrsComplex = currentQrsComplex;
                        onQrsComplexFound(currentQrsComplex);
                        previousQrsComplex = currentQrsComplex;
                        currentQrsComplex = null;
                    }

                    if (originalValues.size() < bufferSize) {
                        System.out.println(
                                "buffer: " + bufferSize + "; " + originalValues.size() + "; " + squaredValues.size());
                        //originalValues.clear();
                        //squaredValues.clear();
                    } else {
                        // clear all values of the original signal expect the
                        // last
                        // <SCBufferSize> values
                        originalValues.subList(0, originalValues.size() - bufferSize).clear();
                        // clear all values of the bandpass filtered and squared
                        // signal
                        squaredValues.subList(0, squaredValues.size() - bufferSize).clear();
                    }
                    // set starting position for QRS complex
                    qrsStart = counter;

                }

                if (qrsStart == 0) {
                    // clear all values of the original signal expect the last
                    // <buffer> values
                    originalValues.subList(0, Math.max(originalValues.size() - bufferSize, 0)).clear();
                    // clear all values of the bandpass filtered and squared
                    // signal
                    squaredValues.subList(0, Math.max(squaredValues.size() - bufferSize, 0)).clear();
                    // set staring position for QRS complex
                    qrsStart = counter;
                }
            }

            // threshold crossed (above thr1 -> below thr1)
        } else if (ma1Filtered <= thr1 && thr1Flag) {
            // set end position for QRS complex
            qrsEnd = counter;
        }

        // update attributes
        originalValues.add(value);
        squaredValues.add(squared);
        thr1Flag = ma1Filtered > thr1;
        counter++;

        return detectedQrsComplex;
    }

}
