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

import jely.QrsComplex;

import java.util.Arrays;

/**
 * Implements the R peak slackness reduction algorithm according to Gradl et al. [1].
 * <p>
 * [1] Gradl, S., Leutheuser, H., Elgendi, M., Lang, N., & Eskofier, B. M. (2015). Temporal correction of detected
 * R-peaks in ECG signals : A crucial step to improve QRS detection algorithms. In Conference Proceedings of the 37th
 * Annual International Conference of the IEEE Engineering in Medicine and Biology Society (EMBC'15) (pp. 522-525).
 *
 * @author Stefan Gradl
 */
public class RPeakSlacknessReduction extends RPeakRefinement {
    private int t1, t2;

    public RPeakSlacknessReduction(double samplingRate, double slacknessWindowInSeconds) {
        t1 = (int) Math.round(0.2 * samplingRate);
        t2 = (int) Math.round(slacknessWindowInSeconds * samplingRate);
    }

    public RPeakSlacknessReduction(double samplingRate) {
        this(samplingRate, 0.25);
    }

    @Override
    public int process(QrsComplex qrs) {
        int oldRPos = qrs.getRPosition();
        int newRPos = oldRPos;

        // get the position of the R peak in the original signal
        int idx2 = qrs.getRPosition();
        // get the indices t1/2 on the left and the right of the R peak
        int idx1 = idx2 - Math.round(t1 / 2);
        int idx3 = idx2 + Math.round(t1 / 2);

        // test for valid indices
        if (idx1 >= 0 && idx2 < qrs.getSignal().getTotalLength()) {
            // get the median of all three values as reference value
            double[] values = new double[3];
            values[0] = qrs.getValueAtLGlobalIndex(idx1);
            values[1] = qrs.getValueAtLGlobalIndex(idx2);
            values[2] = qrs.getValueAtLGlobalIndex(idx3);
            Arrays.sort(values);
            double reference = values[1];

            // search the value with the maximum absolute difference
            // to the reference value in the original signal (range [idx1,idx3])
            idx1 = idx2 - Math.round(t2 / 2);
            idx3 = idx2 + Math.round(t2 / 2);
            double absMax = 0;
            for (int i = idx1; i <= idx3; i++) {
                if (Math.abs(qrs.getValueAtLGlobalIndex(i) - reference) > absMax) {
                    absMax = Math.abs(qrs.getValueAtLGlobalIndex(i) - reference);
                    newRPos = i;
                }
            }

            // set found value as R peak
            qrs.setRPeak(newRPos, qrs.getValueAtLGlobalIndex(newRPos));
        }

        return oldRPos - newRPos;
    }

}
