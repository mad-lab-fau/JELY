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
package de.fau.mad.jely.processors;

import de.fau.mad.jely.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Axel Heinrich
 */
public class RRIProcessor {

    private final int reSampleRate = 4;
    ArrayList<RRInterval> rrIList;
    double sampRate;
    double[] rrIVal;
    double[] rrITs;
    double[] rrITrend;
    double[] rrIDetrended;
    double rmssd;
    double sdnn;
    int nn50;
    double pnn50;
    int[] hr;
    double sd1;
    double sd2;
    int peakExIdx;
    int hr_start;
    int cut_length;
    int inc;
    int[] protocol;
    double powerThres;
    double powerMax;
    char device;
    double weight;
    double[] outRatEx;
    double outRatReg;
    double endStageTs;
    ArrayList<Double> SD1List;
    ArrayList<Double> HRList;
    ArrayList<Double> HR2List;
    ArrayList<double[]> PSDList;
    ArrayList<Integer> maxIdxList;
    double[] psdAll;
    double HRThres;
    ArrayList<Double> maxFreq;
    int maxIdx;
    double VT2stage;
    Detrender mDetrender;

    public RRIProcessor(Ecg ecg, int[] protocol) {
        RRIPreProcessor rriPreProcessor = new RRIPreProcessor(ecg, protocol);
        this.rrIList = rriPreProcessor.getRRIArrayList();
        sampRate = ecg.getSamplingRate();
    }

    public RRIProcessor(ArrayList<RRInterval> rrIList, double samplingRate) {

        this.rrIList = rrIList;
        rrIVal = new double[rrIList.size()];
        rrITs = new double[rrIList.size()];
        this.sampRate = samplingRate;
        for (int i = 0; i < rrIList.size(); i++) {
            rrIVal[i] = rrIList.get(i).getValue() / samplingRate;
            rrITs[i] = rrIList.get(i).getTimeStamp() / samplingRate;
        }
    }

    public RRIProcessor(double[] rrIVal, double[] rrITs, double[] trend, int[] protocol, char device, double weight, double[] outlierRatio, double samplingRate) {
        this.sampRate = samplingRate;
        this.rrIVal = rrIVal;
        rrIDetrended = new double[rrIVal.length];
        for (int i = 0; i < rrIVal.length; i++) {
            rrIDetrended[i] = rrIVal[i] - trend[i];
        }
        this.rrITs = rrITs;
        this.rrITrend = trend;
        this.protocol = protocol;
        this.device = device;
        this.weight = weight;
        this.outRatEx = new double[outlierRatio.length - 1];
        System.arraycopy(outlierRatio, 0, this.outRatEx, 0, outlierRatio.length - 1);
        this.outRatReg = outlierRatio[outlierRatio.length - 1];

    }

    public void process() {
        calculateBasicParameters();

        int HRR[] = useHRRMethod();
        double[] HRVT = useSD1Method();
        double stageVT2 = useVT2Method();
    }

    public int[] useHRRMethod() {

        // minimum of the fitted curve equals end of the exercise load --> regeneration begins
        peakExIdx = calcPeakExIdx();
        endStageTs = rrITs[peakExIdx] / sampRate;

        // absolute power and power relative to weight at peak exercise
        powerMax = calcPowerFromTs(rrITs[peakExIdx], 0);
        double powerMaxPerKg = powerMax / weight;

        // Heart rate recovery values
        int HRR60 = calcHRR60();
        int HRR120 = calcHRR120();

        int[] HRR = {HRR60, HRR120};

        return HRR;
    }

    public double[] useSD1Method() {
        // minimum of the fitted curve equals end of the exercise load --> regeneration begins
        peakExIdx = calcPeakExIdx();
        endStageTs = rrITs[peakExIdx] / sampRate;

        // excludes stages with too much outliers from further processing
        endStageTs = checkStagesForOutliers();

        // Calculate SD1 every minute over chosen part of the signal
        segmentTDParam(endStageTs);

        // absolute power at threshold (SD1 method)
        double[] threshPar = calcThreshParam();
        powerThres = threshPar[0];
        HRThres = threshPar[1];

        return threshPar;
    }

    public double useVT2Method() {
        // minimum of the fitted curve equals end of the exercise load --> regeneration begins
        peakExIdx = calcPeakExIdx();
        endStageTs = rrITs[peakExIdx] / sampRate;

        // excludes stages with too much outliers from further processing
        endStageTs = checkStagesForOutliers();

        segmentFDParam(endStageTs);

        // search for VT2 in the trend signal of f_RSA data
        double maxError = 0.1;
        double[] maxFreq_arr = new double[maxFreq.size()];
        double[] time = new double[maxFreq.size()];
        for (int i = 0; i < maxFreq.size(); i++) {
            time[i] = 1 + i * inc;
            maxFreq_arr[i] = maxFreq.get(i);
        }
        mDetrender = new Detrender(time, maxFreq_arr);
        int f = (int) Math.round(((double) maxFreq.size()) / 100.0 * 10.0);
        int nSteps = 2;
        double delta = 0.0;
        mDetrender.smooth(f, nSteps, delta);

        double[] maxFreq_trend = mDetrender.getYs();

        BRProcessor brProcessor = new BRProcessor(maxFreq_trend, maxError);
        ArrayList<Integer> breaks = brProcessor.slidingWindow();                //apply sliding window algorithm

        double VT2 = 1 + breaks.get(0) * inc;                                      // first break equals the moment VT2 occurs
        VT2stage = VT2 / (protocol[2] + protocol[3]);
        return VT2stage; // compute the stage in which VT2 occurred
    }

    private double checkStagesForOutliers() {
        double stageEndTs = 0;
        for (int i = 0; i < outRatEx.length; i++) {
            if (outRatEx[i] < 0.03 && i + 1 < outRatEx.length) {
                stageEndTs = (i + 1) * (protocol[2] + protocol[3]);
            } else if (outRatEx[i] < 0.03 && i + 1 == outRatEx.length) {
                stageEndTs = (rrITs[peakExIdx]) / sampRate;
            } else {
                break;
            }
        }
        return stageEndTs;
    }

    public void calculateBasicParameters() {
        HRVTimeDomain hrvTimeDomain = new HRVTimeDomain(sampRate);
        HRVFrequencyDomain hrvFrequencyDomain = new HRVFrequencyDomain(rrIVal, rrITs, sampRate, reSampleRate);

        rmssd = hrvTimeDomain.compRMSSD(rrIVal);
        sdnn = hrvTimeDomain.compSDNN(rrIVal);
        nn50 = hrvTimeDomain.compNN50(rrIVal);
        pnn50 = hrvTimeDomain.compPNN50(rrIVal);
        hr = hrvTimeDomain.compHR(rrITrend);
        sd1 = hrvTimeDomain.compSD1(rrIVal);
        sd2 = hrvTimeDomain.compSD2(rrIVal);

    }

    public int calcHRR60() {
        hr_start = hr[peakExIdx];
        int hr_end = hr[peakExIdx];
        for (int i = peakExIdx; i < rrITs.length - 1; i++) {
            if ((rrITs[i] - rrITs[peakExIdx]) / sampRate < 60 && (rrITs[i + 1] - rrITs[peakExIdx]) / sampRate >= 60)
                hr_end = hr[i];
        }
        int HRR60 = hr_start - hr_end;
        return HRR60;
    }

    public int calcHRR120() {
        hr_start = hr[peakExIdx];
        int hr_end = hr[peakExIdx];
        for (int i = peakExIdx; i < rrITs.length - 1; i++) {
            if ((rrITs[i] - rrITs[peakExIdx]) / sampRate < 120 && (rrITs[i + 1] - rrITs[peakExIdx]) / sampRate >= 120)
                hr_end = hr[i];
        }
        int HRR120 = hr_start - hr_end;
        return HRR120;
    }

    public void segmentTDParam(double endTs) {
        cut_length = 60;
        inc = 60;
        //double endTs = rrITs[peakExIdx] / sampRate;
        int n_cut = (int) Math.floor(((endTs - cut_length) / inc));
        SD1List = new ArrayList<>();
        HRList = new ArrayList<>();
        ArrayList<Double> rriCut = new ArrayList<>();
        ArrayList<Double> rriTsCut = new ArrayList<>();
        int c = 0;
        int j = 0;
        for (int i = 0; i < n_cut; i++) {
            if (i > 0 && inc < cut_length) {
                c = 0;
                while (rriTsCut.get(c) / sampRate - ((i - 1) * inc) < inc) {
                    c++;
                }
                for (int k = 0; k < c; k++) {
                    rriCut.remove(0);
                    rriTsCut.remove(0);
                }
            } else if (i > 0 && inc == cut_length) {
                rriCut.clear();
                rriTsCut.clear();
            }
            while (rrITs[j] / sampRate < cut_length + i * inc) {
                rriCut.add(rrIVal[j]);
                rriTsCut.add(rrITs[j]);
                j++;
            }
            HRVTimeDomain hrvTD = new HRVTimeDomain(sampRate);
            SD1List.add(hrvTD.compSD1(rriCut));
            HRList.add((double) hrvTD.compMeanHR(rriCut));
        }
    }


    public double[] calcThreshParam() {
        int thres_idx = -1;
        double[] thresParam = new double[2];
        for (int i = 0; i < SD1List.size() - 1; i++) {
            if (SD1List.get(i) < 3 && SD1List.get(i) - SD1List.get(i + 1) < 1) {
                thres_idx = i;
                break;
            }
        }
        if (thres_idx != -1) {
            double tsThres = ((double) thres_idx * (double) inc) + (double) cut_length / 2;
            thresParam[0] = calcPowerFromTs(tsThres, 0);
            thresParam[1] = HRList.get(thres_idx);
        } else {
            thresParam[0] = 0;
            thresParam[1] = 0;
        }
        return thresParam;
    }

    public void segmentFDParam(double endTs) {
        cut_length = 60;
        inc = 3;
        //double endTs = rrITs[peakExIdx] / sampRate;
        int n_cut = (int) Math.floor(((endTs - cut_length) / inc));
        PSDList = new ArrayList<>();
        maxFreq = new ArrayList<>();
        HR2List = new ArrayList<>();
        maxIdxList = new ArrayList<>();
        ArrayList<Double> rriCut = new ArrayList<>();
        ArrayList<Double> rriDetrendCut = new ArrayList<>();
        ArrayList<Double> rriTsCut = new ArrayList<>();
        int c = 0;
        int j = 0;
        for (int i = 0; i < n_cut; i++) {
            if (i > 0) {
                c = 0;
                while (rriTsCut.get(c) / sampRate - ((i - 1) * inc) < inc) {
                    c++;
                }
                for (int k = 0; k < c; k++) {
                    rriDetrendCut.remove(0);
                    rriCut.remove(0);
                    rriTsCut.remove(0);
                }
            }
            while (rrITs[j] / sampRate < cut_length + i * inc) {
                rriDetrendCut.add(rrIVal[j] - rrITrend[j]);
                rriCut.add(rrIVal[j]);
                rriTsCut.add(rrITs[j]);
                j++;
            }
            Double[] rrIDTCutArr = new Double[rriDetrendCut.size()];
            rrIDTCutArr = rriCut.toArray(rrIDTCutArr);
            Double[] rrITsCutArr = new Double[rriTsCut.size()];
            rrITsCutArr = rriTsCut.toArray(rrITsCutArr);
            HRVFrequencyDomain hrvFD = new HRVFrequencyDomain(rrIDTCutArr, rrITsCutArr, sampRate, reSampleRate);
            maxFreq.add(findBR(hrvFD.compPSD(), i));
            PSDList.add(hrvFD.compPSD());
            HRVTimeDomain hrvTD = new HRVTimeDomain(sampRate);
            HR2List.add((double) hrvTD.compMeanHR(rriCut));
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

    public int calcPeakExIdx() {
        int minIdx = findMinimumIndex(rrITrend);
        int newMinIdx = minIdx;
        int c = 0;
        while (minIdx + c + 1 < rrITs.length && (rrITs[minIdx + c] - rrITs[minIdx]) / sampRate < 100) {
            if (rrITrend[minIdx + c] - rrITrend[minIdx + c - 1] < 0 && rrITrend[minIdx + c + 1] - rrITrend[minIdx + c] > 0 && rrITrend[minIdx + c] < 1.1 * rrITrend[minIdx]) {
                newMinIdx = minIdx + c;
            }
            c++;
        }
        return newMinIdx;
    }

    public double calcPowerFromTs(double ts, int flag) {
        double stage = ts / (protocol[2] + protocol[3]);
        if (flag == 1) {
            stage = Math.floor(stage);
        }

        double pAtIdx = protocol[0] + stage * protocol[1];

        if (device == 'T') {// converts velocity to power output on treadmill
            pAtIdx = weight / 3.6 * 0.0099 * pAtIdx / 9.81 * 1000; // in case the slope of the treadmill is 1
        }

        return pAtIdx;
    }

    public double findBR(double[] psd, int seg) {
        int maxIdxMom;

        int n = psd.length;
        double[] freq = new double[n];
        for (int i = 0; i < n; i++) {
            freq[i] = (i + 1) * (double) reSampleRate / (2 * n);
        }
        int c1 = (int) Math.ceil(0.15 * n / 2);
        int c2 = (int) Math.ceil(0.5 * n / 2);

        int endIdx = n - c2;
        int startIdx = c1;
        int var1 = 5;
        int var2 = 10;
        if (seg != 0 && maxIdx - var1 > c1) {
            startIdx = maxIdx - var1;
        }

        if (seg != 0 && maxIdx + var2 < n - c2) {
            endIdx = maxIdx + var2;
        }
        maxIdx = 0;
        maxIdxMom = 0;
        double maxVal = 0;
        for (int i = startIdx; i < endIdx; i++) {
            if (psd[i] > maxVal) {
                maxIdxMom = i;
                maxVal = psd[i];
            }
        }

        if (maxIdxList.size() <= 5) {
            maxIdxList.add(maxIdxMom);
            maxIdx = median(maxIdxList);
        } else {
            maxIdxList.remove(0);
            maxIdxList.add(maxIdxMom);
            maxIdx = compFreqMaxValue(maxIdxList);
        }
        double maxFreq = (maxIdx + 1) * (double) reSampleRate / (2 * n);
        return maxFreq;
    }

    public int compFreqMaxValue(ArrayList<Integer> list) {
        ArrayList<Integer> sortedList = new ArrayList<Integer>(list);
        Collections.sort(sortedList);
        return median(sortedList);
    }

    private Integer median(ArrayList<Integer> m) {
        int middle = m.size() / 2;
        return m.get(middle);
    }

    public int getMinPos() {
        return peakExIdx;
    }

    public double getRMSSD() {
        return rmssd;
    }

    public double getSDNN() {
        return sdnn;
    }

    public int getNN50() {
        return nn50;
    }

    public double getPNN50() {
        return pnn50;
    }

    public int[] getHR() {
        return hr;
    }

    public int getHRStart() {
        return hr_start;
    }

    public double getSD1() {
        return sd1;
    }

    public double getSD2() {
        return sd2;
    }

    public double getEndStageTs() {
        return endStageTs;
    }

    public double getPowerThres() {
        return powerThres;
    }

    public double getHRThres() {
        return HRThres;
    }

    public ArrayList<Double> getSD1List() {
        return SD1List;
    }

    public ArrayList<Double> getHRList() {
        return HRList;
    }

    public ArrayList<double[]> getPSDList() {
        return PSDList;
    }

    public double[] getPSDAll() {
        return psdAll;
    }

    public double[] getDetrend() {
        return rrIDetrended;
    }

    public ArrayList<Double> getMaxFreq() {
        return maxFreq;
    }

    public ArrayList<Double> getHR2List() {
        return HR2List;
    }

    public double getVT2stage() {
        return VT2stage;
    }
}
