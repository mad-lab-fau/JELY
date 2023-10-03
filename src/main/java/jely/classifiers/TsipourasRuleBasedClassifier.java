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
package de.fau.mad.jely.classifiers;

import java.util.ArrayList;
import java.util.List;

import de.fau.mad.jely.BeatClass;
import de.fau.mad.jely.Heartbeat;
import de.fau.mad.jely.QrsClass;
import de.fau.mad.jely.QrsComplex;

/**
 * Implements the rule based classifier of Tsipouras.
 */
public class TsipourasRuleBasedClassifier implements Classifier {

    private enum TsipourasClass {
        UNKNOWN,
        NORMAL,
        PVC,
        VF,
        BII
    }

    /**
     * Creates a default <code>TsipourasRuleBasedClassifier<code> object
     */
    public TsipourasRuleBasedClassifier() {
    }

    /**
     * Classifies a list of QRS complexes using the rule based classifier of Tsipouras.
     *
     * @param qrsComplexes the list of QRS complexes to be classified
     * @return a list of QRS classes corresponding to the QRS complexes.
     */
    public List<QrsClass> classify(List<QrsComplex> qrsComplexes) {

        //initialize QRS classes
        ArrayList<TsipourasClass> tsipourasClasses = new ArrayList<TsipourasClass>();
        for (int i = 0; i < qrsComplexes.size(); i++) {
            tsipourasClasses.add(TsipourasClass.NORMAL);
        }

        //loop over all QRS complexes
        for (int i = 1; i < qrsComplexes.size() - 1; i++) {

            //current QRS complex already classified
            if (tsipourasClasses.get(i) != TsipourasClass.NORMAL) {
                //skip current QRS complex
                continue;
            }

            //get RR intervals of interest
            double rr1 = qrsComplexes.get(i - 1).getRRInterval();
            double rr2 = qrsComplexes.get(i).getRRInterval();
            double rr3 = qrsComplexes.get(i + 1).getRRInterval();

            //RULE1: ventricular flutter/fibrilliation (VF)
            //VF episode started
            if ((rr2 < 0.6d) && (1.8 * rr2 < rr1)) {
                //classify QRS complex as VF
                tsipourasClasses.set(i, TsipourasClass.VF);
                //initialize number of QRS complexes in VF episode
                int nbr_vf = 1;
                //loop over all successive QRS complexes
                for (int k = i + 1; k < qrsComplexes.size() - 1; k++) {
                    //get RR intervals of interest
                    double rr1_tmp = qrsComplexes.get(k - 1).getRRInterval();
                    double rr2_tmp = qrsComplexes.get(k).getRRInterval();
                    double rr3_tmp = qrsComplexes.get(k + 1).getRRInterval();
                    //VF episode continued
                    if (((rr1_tmp < 0.7) && (rr2_tmp < 0.7) && (rr3_tmp < 0.7)) ||
                            (rr1_tmp + rr2_tmp + rr3_tmp < 1.7)) {
                        //classify QRS complex as VF
                        tsipourasClasses.set(k, TsipourasClass.VF);
                        //increment number of QRS complexes in VF episode
                        nbr_vf++;
                        //VF episode ended
                    } else {
                        break;
                    }
                }
                //VF episode too short
                if (nbr_vf < 4) {
                    //reset QRS classes
                    for (int k = i; k < i + nbr_vf; k++) {
                        tsipourasClasses.set(k, TsipourasClass.NORMAL);
                    }
                } else {
                    //skip QRS complex
                    continue;
                }
            }

            //RULE2: premature ventricular contraction (PVC)
            if (((1.15 * rr2 < rr1) && (1.15 * rr2 < rr3)) || ((Math.abs(rr1 - rr2) < 0.3) &&
                    (rr1 < 0.8) && (rr2 < 0.8) && (rr3 > 1.2 * ((rr1 + rr2) / 2))) ||
                    ((Math.abs(rr2 - rr3) < 0.3) && (rr2 < 0.8) && (rr3 < 0.8) &&
                            (rr1 > 1.2 * ((rr2 + rr3) / 2)))) {
                //classify QRS complex as PVC
                tsipourasClasses.set(i, TsipourasClass.PVC);
                //skip QRS complex
                continue;
            }

            //RULE3: 2nd heart block (BII)
            if (((rr2 > 2.2) && (rr2 < 3.0)) && ((Math.abs(rr1 - rr2) < 0.2) ||
                    (Math.abs(rr2 - rr3) < 0.2))) {
                //classify QRS complex as BII
                tsipourasClasses.set(i, TsipourasClass.BII);
                //skip QRS complex
                continue;
            }
        }

        //map QRS classes
        if (!tsipourasClasses.isEmpty()) {
            tsipourasClasses.set(0, TsipourasClass.UNKNOWN);
            tsipourasClasses.set(tsipourasClasses.size() - 1, TsipourasClass.UNKNOWN);
            if (tsipourasClasses.size() > 1) {
                tsipourasClasses.set(1, TsipourasClass.UNKNOWN);
            }
        }
        ArrayList<QrsClass> qrsClasses = new ArrayList<QrsClass>();
        for (int i = 0; i < qrsComplexes.size(); i++) {
            qrsClasses.add(mapQrsClass(tsipourasClasses.get(i)));
        }

        return qrsClasses;
    }

    private QrsClass mapQrsClass(TsipourasClass tsipourasClass) {
        if (tsipourasClass == TsipourasClass.UNKNOWN) {
            return QrsClass.UNKNOWN;
        } else if (tsipourasClass == TsipourasClass.NORMAL) {
            return QrsClass.NORMAL;
        } else {
            return QrsClass.ABNORMAL;
        }
    }

    @Override
    public BeatClass classify(QrsComplex qrs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BeatClass classify(Heartbeat beat) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<QrsClass> classify(ArrayList<Heartbeat> beatList) {
        // TODO Auto-generated method stub
        return null;
    }

}
