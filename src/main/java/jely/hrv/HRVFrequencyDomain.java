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
package de.fau.mad.jely.hrv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import de.fau.mad.jely.util.*;

/**
 * @author Stefan Gradl
 */
public class HRVFrequencyDomain {

    Complex[] complex;
    double[] y;
    double[] x;
    double samplingRate;
    double[] real;
    double[] imag;

    public HRVFrequencyDomain(double[] rrIValues, double[] rrITimestamps, double samplingRate, double reSamplingRate) {

        double[] rrIVal = new double[rrIValues.length];
        double[] rrITs = new double[rrITimestamps.length];

        for (int i = 0; i < rrIValues.length; i++) {
            rrIVal[i] = rrIValues[i] / samplingRate;
            rrITs[i] = rrITimestamps[i] / samplingRate;
        }

        this.samplingRate = samplingRate;

        double x_start = rrITs[0];
        double x_end = rrITs[rrITs.length - 1];
        x = new double[(int) Math.floor((x_end - x_start) * reSamplingRate)];

        for (int i = 0; i < x.length; i++) {
            x[i] = x_start + (i / reSamplingRate);
        }

        y = Interpolator.interpolate(rrITs, rrIVal, x, reSamplingRate);

        double y_mean = mean(y);
        for (int i = 0; i < y.length; i++) {
            y[i] = (y[i] - y_mean) * 1000;
        }

        y = HammingWindow(y, 0, y.length);

        real = y.clone();
        imag = new double[y.length];
        FFT.transform(real, imag);

    }

    public HRVFrequencyDomain(Double[] rrIValues, Double[] rrITimestamps, double samplingRate, double reSamplingRate) {

        double[] rrIVal = new double[rrIValues.length];
        double[] rrITs = new double[rrITimestamps.length];

        for (int i = 0; i < rrIValues.length; i++) {
            rrIVal[i] = rrIValues[i].doubleValue() / samplingRate;
            rrITs[i] = rrITimestamps[i].doubleValue() / samplingRate;
        }

        this.samplingRate = samplingRate;

        double x_start = rrITs[0];
        double x_end = rrITs[rrITs.length - 1];
        x = new double[(int) Math.floor((x_end - x_start) * reSamplingRate)];

        for (int i = 0; i < x.length; i++) {
            x[i] = x_start + (i / reSamplingRate);
        }

        y = Interpolator.interpolate(rrITs, rrIVal, x, reSamplingRate);

        double y_mean = mean(y);
        for (int i = 0; i < y.length; i++) {
            y[i] = (y[i] - y_mean) * 1000;
        }

        y = HammingWindow(y, 0, y.length);
        real = y.clone();
        imag = new double[y.length];
        FFT.transform(real, imag);

    }

    // Constructor receives interpolated values of original RRInterval series
    public HRVFrequencyDomain(double[] time_series) {
/*        int n = time_series.length;
        complex = new Complex [n];
        FFT fftObj = new FFT();
        for (int i = 0; i < time_series.length; i++) {
            complex[i] = new Complex(time_series[i], 0);
        }

        Complex[] freq = fftObj.fft(complex);*/

    }

    public double[] compPSD() {
        double[] power = new double[real.length / 2];
        for (int i = 0; i < real.length / 2; i++) {
            double mag = Math.pow(real[i], 2.0) + Math.pow(imag[i], 2.0);
            mag = Math.sqrt(mag);
            power[i] = (mag * mag) / (samplingRate * real.length);
        }
        return power;
    }

    public double[] HanningWindow(double[] signal_in, int pos, int size) {
        for (int i = pos; i < pos + size; i++) {
            int j = i - pos; // j = index into Hann window function
            signal_in[i] = signal_in[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * j / size));
        }
        return signal_in;
    }

    public double[] HammingWindow(double[] signal_in, int pos, int size) {
        for (int i = pos; i < pos + size; i++) {
            int j = i - pos; // j = index into Hamming window function
            signal_in[i] = signal_in[i] * (0.54 - 0.46 * Math.cos(2.0 * Math.PI * j / size));
        }
        return signal_in;
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

    private double mean(Double[] array) {
        double mean = 0;
        double sum = 0;
        int n = array.length;

        for (int i = 0; i < array.length; i++) {
            sum = sum + array[i].doubleValue();
        }
        mean = sum / n;

        return mean;
    }

    public double[] getRRIValues() {
        return y;
    }

    public double[] getRRITimestamps() {
        return x;
    }
}
