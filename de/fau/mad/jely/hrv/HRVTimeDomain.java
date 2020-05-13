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

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Axel on 11.05.2016.
 */
public class HRVTimeDomain {

    double samplingRate;

    public HRVTimeDomain(double samplingRate) {
        this.samplingRate = samplingRate;
    }

    public int[] compHR(double[] trend) {
        int[] HR = new int[trend.length];
//        ArrayList<Double> rriInSeconds = new ArrayList();
        for (int i = 0; i < trend.length; i++) {
/*            if(i < 10) {
                rriInSeconds.add(rrIValues[i]/samplingRate);
            }else{
                rriInSeconds.remove(0);
                rriInSeconds.add(rrIValues[i]/samplingRate);
            }
            double meanRRI = mean(rriInSeconds);*/
            Double hr = new Double(60.0 / (trend[i] / samplingRate));
            HR[i] = hr.intValue();
        }
        return HR;
    }

    public int compMeanHR(ArrayList<Double> rrIVal) {
        double meanRRI = mean(rrIVal);
        Double hr = new Double(60.0 / (meanRRI / samplingRate));
        int meanHR = hr.intValue();
        return meanHR;
    }

    public double compSDNN(double[] rrIVal) {

        double SDNN = std(rrIVal);

        return SDNN * 1000;
    }

    public double compRMSSD(double[] rrIVal) {
        int n = rrIVal.length;
        double RMSSD = Math.sqrt(sumSquaredDiff(rrIVal) / (double) (n - 1));

        return RMSSD * 1000;
    }

    public double compRMSSD(ArrayList<Double> rrIVal) {
        int n = rrIVal.size();

        Double[] rrIValArr = new Double[n];
        rrIValArr = rrIVal.toArray(rrIValArr);
        double RMSSD = Math.sqrt(sumSquaredDiff(rrIValArr) / (double) (n - 1));

        return RMSSD * 1000 / samplingRate;
    }

    public int compNN50(double[] rrIVal) {
        int nn50 = 0;
        int n = rrIVal.length;
        for (int i = 1; i < n; i++) {
            double diff = Math.abs(rrIVal[i] - rrIVal[i - 1]);
            if (diff > 50) {
                nn50++;
            }
        }
        return nn50;
    }

    public double compPNN50(double[] rrIVal) {
        int nn50 = compNN50(rrIVal);
        int n = rrIVal.length;
        return (double) nn50 / (n - 1);
    }

    public double compSD1(double[] rrIVal) {
        double SD1 = compSDSD(rrIVal) / Math.sqrt(2);
        return SD1;
    }

    public double compSD1(ArrayList<Double> rrIVal) {
        double SD1 = compSDSD(rrIVal) / Math.sqrt(2);
        return SD1;
    }

    public double compSD2(double[] rrIVal) {
        double SD2 = Math.sqrt(2 * Math.pow(compSDNN(rrIVal), 2) - (0.5 * Math.pow(compSDSD(rrIVal), 2)));
        return SD2;
    }

    public double compSDSD(double[] rrIVal) {
        double[] drri = diff(rrIVal);
        double SDSD = std(drri);

        return SDSD * 1000 / samplingRate;
    }

    public double compSDSD(ArrayList<Double> rrIVal) {
        ArrayList<Double> drri = diff(rrIVal);
        double SDSD = std(drri);

        return SDSD * 1000 / samplingRate;
    }

    private double std(double[] array) {
        double mean = mean(array);
        double sum = 0.0;
        int n = array.length;
        for (int i = 0; i < array.length; i++) {
            sum = sum + Math.pow((array[i] - mean), 2);
        }
        double std = Math.sqrt(sum / (n - 1));

        return std;
    }

    private double std(ArrayList<Double> array) {
        double mean = mean(array);
        double sum = 0.0;
        int n = array.size();
        for (int i = 0; i < array.size(); i++) {
            sum = sum + Math.pow((array.get(i) - mean), 2);
        }
        double std = Math.sqrt(sum / (n - 1));

        return std;
    }

    private double[] diff(double[] array) {
        double[] diff = new double[array.length - 1];
        for (int i = 1; i < array.length; i++) {
            diff[i - 1] = array[i] - array[i - 1];
        }

        return diff;
    }

    private ArrayList<Double> diff(ArrayList<Double> array) {
        ArrayList<Double> diff = new ArrayList(array.size() - 1);
        for (int i = 1; i < array.size(); i++) {
            diff.add(array.get(i) - array.get(i - 1));
        }
        return diff;
    }


    private double sumSquaredDiff(double[] array) {
        double diff = 0.0;
        for (int i = 1; i < array.length; i++) {
            diff = diff + Math.pow(array[i] - array[i - 1], 2);
        }
        return diff;
    }

    private Double sumSquaredDiff(Double[] array) {
        Double diff = 0.0;
        for (int i = 1; i < array.length; i++) {
            diff = diff + Math.pow(array[i] - array[i - 1], 2);
        }
        return diff;
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

    private double mean(ArrayList<Double> arrayList) {
        double mean = 0;
        double sum = 0;
        int n = arrayList.size();

        for (int i = 0; i < arrayList.size(); i++) {
            sum = sum + arrayList.get(i);
        }
        mean = sum / n;

        return mean;
    }

}