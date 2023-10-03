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
package jely.classifiers;

import jely.BeatClass;
import jely.Heartbeat;
import jely.QrsClass;
import jely.QrsComplex;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a physiological classifier purely based on ground-truth medical knowledge.
 *
 * @author Stefan Gradl
 */
public class PhysiologicalClassifier implements Classifier {

    @Override
    public List<QrsClass> classify(List<QrsComplex> qrsComplexes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeatClass classify(QrsComplex qrs) {
        double qrsWidth = qrs.getQRSWidth();

        // ========= QRS width =========
        // detect QRS width outside of normal range (60-100ms), including ~20% error since we still suck at detecting the deflection points, TODO: this needs to be determine by some ROC-decision
        if (qrsWidth < 0.05 || qrsWidth > 0.13)
            return new BeatClass(new Heartbeat(qrs), QrsClass.ABNORMAL, "Abnormal QRS width: " + String.format("%.4f", qrsWidth) + "s");

        // ========= Q height =========
        double q2r = Math.abs(qrs.getQValue() - qrs.getBaselineValue()) / Math.abs(qrs.getRValue() - qrs.getBaselineValue());
        if (q2r > 0.35) // starting at 0.25 it becomes abnormal, but we include some error
            return new BeatClass(new Heartbeat(qrs), QrsClass.ABNORMAL, "Abnormal Q wave height: " + String.format("%.2f", q2r));

        return new BeatClass(new Heartbeat(qrs), QrsClass.NORMAL);
    }

    @Override
    public BeatClass classify(Heartbeat beat) {
        BeatClass beatclass = classify(beat.getQrs());

        if (beatclass.isAbnormal())
            return beatclass;

        //  ========= PQ time =========
        double pqTime = beat.getPQTime();
        if (pqTime > 0) {
            if (pqTime > 0.25)
                return new BeatClass(beat, QrsClass.ABNORMAL, "Abnormal PQ time: TODO");
        }

        return new BeatClass(beat, QrsClass.NORMAL);
    }

    @Override
    public List<QrsClass> classify(ArrayList<Heartbeat> beatList) {
        // TODO Auto-generated method stub
        return null;
    }

}
