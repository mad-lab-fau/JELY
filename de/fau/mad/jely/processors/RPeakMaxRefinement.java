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

import de.fau.mad.jely.Ecglib;
import de.fau.mad.jely.QrsComplex;
import de.fau.mad.jely.util.DescriptiveStatistics;

/**
 * R peak refinement using a simple maximum search in a given window.
 *
 * @author Stefan Gradl
 */
public class RPeakMaxRefinement extends RPeakRefinement {
    private int leftMargin = 1;
    private int rightMargin = 1;

    public RPeakMaxRefinement(double samplingRate) {
        this(samplingRate, 0.4); // originally: 0.55
    }

    public RPeakMaxRefinement(double samplingRate, double refinementWindowInSeconds) {
        leftMargin = (int) (samplingRate * refinementWindowInSeconds * 0.5);
        rightMargin = (int) (samplingRate * refinementWindowInSeconds * 0.5);
    }

    @Override
    public int process(QrsComplex qrs) {
        int oldRPos = qrs.getRPosition();

        int idx1 = oldRPos - leftMargin;
        int idx2 = oldRPos + rightMargin;

        int newRPos = oldRPos;

        // test for valid indices
        if (idx1 >= 0 && idx2 < qrs.getSignal().getTotalLength()) {
            newRPos = DescriptiveStatistics.max(qrs.getSignal().subList(idx1, idx2)).getIndex() + idx1;
            // set found value as R peak
            qrs.setRPeak(newRPos, qrs.getSignal().get(newRPos));
        }

        if (Ecglib.isDebugMode())
            System.out.println("refinement: " + idx1 + " : " + idx2 + " --> from " + oldRPos + " to " + newRPos);

        return oldRPos - newRPos;
    }

}
