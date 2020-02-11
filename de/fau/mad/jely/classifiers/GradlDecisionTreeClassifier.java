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

import de.fau.mad.jely.QrsComplex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.fau.mad.jely.BeatClass;
import de.fau.mad.jely.Heartbeat;
import de.fau.mad.jely.QrsClass;

/**
 * Implements the decision tree classifier of Gradl et al. published in the Hearty paper.
 * <p>
 * Gradl, Stefan, Patrick Kugler, Clemens Lohmüller, and Bjoern M Eskofier. 2012.
 * “Real-Time ECG Monitoring and Arrhythmia Detection Using Android-Based Mobile Devices.”
 * In Conf Proc IEEE Eng Med Biol Soc, 2452–55. San Diego, CA, USA: IEEE.
 * https://doi.org/10.1109/EMBC.2012.6346460.
 *
 * @author Stefan Gradl
 */
public class GradlDecisionTreeClassifier implements Classifier {

    private enum GradlClass {
        UNKNOWN, NORMAL, PVC, PVC_ABERRANT, BB_BLOCK, ESCAPE, APC, APC_ABERRANT, PREMATURE, ABERRANT,
    }

    private final int NUMBER_CANDIDATES = 6;

    /*
     * private enum GradlRhythm { NONE, ARTIFACT, FUSION, AV_BLOCK }
     */

    private QrsComplex precursor;
    private QrsComplex template1;
    private QrsComplex template2;

    /**
     * Creates a default <code>GradlDecisionTreeClassifier<code> object.
     */
    public GradlDecisionTreeClassifier() {
        this.template1 = null;
        this.template2 = null;
        this.precursor = null;
    }

    /**
     * Creates a <code>GradlDecisionTreeClassifier<code> object with two QRS complex templates.
     *
     * @param template1 the first QRS complex template
     * @param template2 the second QRS complex template
     */
    public GradlDecisionTreeClassifier(QrsComplex template1, QrsComplex template2) {
        this.template1 = template1;
        this.template2 = template2;
        this.precursor = null;
    }

    /**
     * Sets the QRS templates needed to compute certain QRS features.
     *
     * @param template1 the first QRS complex template
     * @param template2 the second QRS complex template
     */
    public void setTemplates(QrsComplex template1, QrsComplex template2) {
        this.template1 = template1;
        this.template2 = template2;
    }

    /**
     * Classifies a list of QRS complexes using the decision tree classifier of Gradl.
     *
     * @param qrsComplexes the list of QRS complexes to be classified
     * @return a list of QRS classes corresponding to the QRS complexes
     */
    public List<QrsClass> classify(List<QrsComplex> qrsComplexes) {
        selectTemplates(qrsComplexes);
        ArrayList<QrsClass> qrsClasses = new ArrayList<QrsClass>();
        for (int i = 0; i < qrsComplexes.size(); i++) {
            // only ignore first QrsComplex
            if (i < 1) {
                classifyNext(qrsComplexes.get(i));
                qrsClasses.add(QrsClass.UNKNOWN);
                continue;
            }
            qrsClasses.add(classifyNext(qrsComplexes.get(i)));
        }
        return qrsClasses;
    }

    /**
     * Classifies a single QRS complex using the decision tree classifier of Gradl.
     *
     * @param qrsComplex the QRS to be classified
     * @return a QRS class corresponding to the QRS complex
     */
    public QrsClass classifyNext(QrsComplex qrsComplex) {

        // check QRS complexes
        if (qrsComplex == null || precursor == null || template1 == null || template2 == null) {
            // set QRS complex as precursor
            precursor = qrsComplex;
            // map and return QRS class
            return mapQrsClass(GradlClass.UNKNOWN);
        }

        // initialize QRS class
        GradlClass gradlClass = GradlClass.NORMAL;
        // GradlRhythm gradlRhythm = GradlRhythm.NONE;
        // extract features
        double rrInterval = qrsComplex.getRRInterval() * 1000;
        double rrIntervalPrior = precursor.getRRInterval() * 1000;
        double qrsWidth = qrsComplex.getQRSWidth() * 1000;
        double qrsWidthPrior = precursor.getQRSWidth() * 1000;
        double crossCorrelation1 = qrsComplex.getCrossCorrelation(template1);
        double crossCorrelation2 = qrsComplex.getCrossCorrelation(template2);
        double areaDifference1 = qrsComplex.getAreaDifference(template1);
        double areaDifference2 = qrsComplex.getAreaDifference(template2);

        // analyze QRS width
        if (qrsWidth > 130) {
            gradlClass = GradlClass.BB_BLOCK;
        } else if (qrsWidth < 45) {
            gradlClass = GradlClass.PVC;
        }
        // analyze cross correlation and area difference
        if (crossCorrelation1 < 0.2d || crossCorrelation2 < 0.2d) {
            // gradlRhythm = GradlRhythm.ARTIFACT;
        }
        if (crossCorrelation1 < 0.3d || crossCorrelation2 < 0.3d) {
            gradlClass = GradlClass.ABERRANT;
        } else if (crossCorrelation1 < 0.6d || crossCorrelation2 < 0.6d) {
            gradlClass = GradlClass.PVC_ABERRANT;
        } else if (crossCorrelation1 < 0.9d && crossCorrelation2 < 0.9d) {
            gradlClass = GradlClass.PVC;
        } else if (crossCorrelation1 < 0.98d && crossCorrelation2 < 0.98d) {
            if (areaDifference1 > 0.7d || areaDifference2 > 0.7d) {
                gradlClass = GradlClass.ABERRANT;
            } else if (areaDifference1 > 0.5d || areaDifference2 > 0.5d) {
                gradlClass = GradlClass.PVC_ABERRANT;
            } else if (areaDifference1 > 0.2d && areaDifference2 > 0.2d) {
                gradlClass = GradlClass.PVC;
            }
        }
        // analyze RR interval
        if ((rrInterval >= rrIntervalPrior * 1.5d && rrInterval > 800) || rrInterval > 1700) {
            // gradlRhythm = GradlRhythm.AV_BLOCK;
            if (gradlClass == GradlClass.NORMAL) {
                gradlClass = GradlClass.APC;
            }
        } else if (rrInterval > 1 && rrInterval < 460) {
            if (rrInterval > rrIntervalPrior * 0.92f) {
                if (gradlClass == GradlClass.NORMAL && (crossCorrelation1 < 0.96d || crossCorrelation2 < 0.96d)) {
                    gradlClass = GradlClass.APC;
                }
            } else {
                // gradlRhythm = GradlRhythm.FUSION;
            }
            if (rrInterval < 400) {
                if (crossCorrelation1 < 0.6d || crossCorrelation2 < 0.6d) {
                    gradlClass = GradlClass.APC_ABERRANT;
                } else {
                    gradlClass = GradlClass.APC;
                }
            }
        } else if (rrIntervalPrior > 800 && rrInterval < rrIntervalPrior * 0.6f) {
            gradlClass = GradlClass.ESCAPE;
        } else if (gradlClass == GradlClass.NORMAL && qrsWidth > 10 && qrsWidth < qrsWidthPrior * 0.6f
                && (areaDifference1 > 0.1 || areaDifference2 > 0.1)) {
            gradlClass = GradlClass.PREMATURE;
        }

        // set QRS complex as precursor
        precursor = qrsComplex;

        // map and return QRS class
        return mapQrsClass(gradlClass);
    }

    private QrsClass mapQrsClass(GradlClass gradlClass) {
        if (gradlClass == GradlClass.UNKNOWN) {
            return QrsClass.UNKNOWN;
        } else if (gradlClass == GradlClass.NORMAL) {
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

    // selects two templates of out <NUMBER_CANDIDATES> candidates
    private void selectTemplates(List<QrsComplex> qrsList) {

        if (qrsList.size() < NUMBER_CANDIDATES || NUMBER_CANDIDATES < 2) {
            return;
        }

        // get candidates
        List<QrsComplex> candidates = qrsList.subList(0, NUMBER_CANDIDATES);

        // compute mean QRST area
        double sum = 0;
        // for (QrsComplex QrsComplex : candidates)
        for (int i = 0; i < candidates.size(); i++) {
            sum += ((QrsComplex) candidates.get(i)).getQRSTArea();
        }
        // {
        // sum += QrsComplex.getQRSTArea();
        // }
        final double meanQRSTArea = sum / NUMBER_CANDIDATES;

        // split candidates into two groups
        ArrayList<QrsComplex> group1 = new ArrayList<QrsComplex>();
        ArrayList<QrsComplex> group2 = new ArrayList<QrsComplex>();
        for (int i = 0; i < NUMBER_CANDIDATES; i++) {
            if (((QrsComplex) candidates.get(i)).getQRSTArea() <= meanQRSTArea) {
                group1.add(candidates.get(i));
            } else {
                group2.add(candidates.get(i));
            }
        }
        // sort both groups
        Comparator<Object> comparator = new Comparator<Object>() {
            @Override
            public int compare(Object qrso1, Object qrso2) {
                QrsComplex qrs1 = (QrsComplex) qrso1;
                QrsComplex qrs2 = (QrsComplex) qrso2;
                if (Math.abs(qrs1.getQRSTArea() - meanQRSTArea) < Math.abs(qrs2.getQRSTArea() - meanQRSTArea)) {
                    return -1;
                } else if (Math.abs(qrs1.getQRSTArea() - meanQRSTArea) > Math.abs(qrs2.getQRSTArea() - meanQRSTArea)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
        Collections.sort(group1, comparator);
        Collections.sort(group2, comparator);

        // get ranked candidates
        candidates = group1;
        candidates.addAll(group2);

        // set templates
        for (int i = 0; i < NUMBER_CANDIDATES - 1; i++) {
            if (((QrsComplex) candidates.get(i)).getCrossCorrelation((QrsComplex) candidates.get(i + 1)) > 0.95) {
                template1 = ((QrsComplex) candidates.get(i)).createWithFixedBuffer();
                template2 = ((QrsComplex) candidates.get(i + 1)).createWithFixedBuffer();
            }
        }
        if (template1 == null || template2 == null) {
            template1 = ((QrsComplex) candidates.get(0)).createWithFixedBuffer();
            template2 = ((QrsComplex) candidates.get(1)).createWithFixedBuffer();
        }
    }

    /**
     * Returns the first template.
     *
     * @return the first template, or <code>null</code>, if no template is available
     */
    public QrsComplex getFirstTemplate() {
        return template1;
    }

    /**
     * Returns the second template.
     *
     * @return the second template, or <code>null</code>, if no template is available
     */
    public QrsComplex getSecondTemplate() {
        return template2;
    }

}
