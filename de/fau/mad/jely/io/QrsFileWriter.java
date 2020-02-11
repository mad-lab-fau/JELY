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
package de.fau.mad.jely.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.fau.mad.jely.QrsClass;
import de.fau.mad.jely.QrsComplex;

/**
 * Implements a file writer to save QRS complexes and classes.
 */
public class QrsFileWriter {

    private QrsComplex template1;
    private QrsComplex template2;
    private BufferedWriter writer;

    /**
     * Creates an ARFF file to save the QRS complexes and classes and writes the
     * header to the file.
     *
     * @param dirPath  the path to the directory of the file
     * @param fileName the name of the file
     */
    public QrsFileWriter(String dirPath, String fileName) {
        //init templates
        template1 = null;
        template2 = null;
        try {
            //create writer
            writer = new BufferedWriter(new FileWriter(new File(dirPath, fileName +
                    ".arff"), false));
            //write header
            writer.write(
                    "%File name: " + fileName + "\n" +
                            "%Timestamp: " + System.currentTimeMillis() + "\n" +
                            //WEKA can't handle empty spaces (' or _ needed)
                            "@RELATION QRS_complexes_and_classes" + "\n" +
                            "@ATTRIBUTE 'position' REAL" + "\n" +
                            "@ATTRIBUTE 'value' REAL" + "\n" +
                            "@ATTRIBUTE 'RR interval' REAL" + "\n" +
                            "@ATTRIBUTE 'QR amplitude' REAL" + "\n" +
                            "@ATTRIBUTE 'RS amplitude' REAL" + "\n" +
                            "@ATTRIBUTE 'QRS width' REAL" + "\n" +
                            "@ATTRIBUTE 'QRST area' REAL" + "\n" +
                            "@ATTRIBUTE 'minimum' REAL" + "\n" +
                            "@ATTRIBUTE 'maximum' REAL" + "\n" +
                            "@ATTRIBUTE 'mean' REAL" + "\n" +
                            "@ATTRIBUTE 'variance' REAL" + "\n" +
                            "@ATTRIBUTE 'standard deviation' REAL" + "\n" +
                            "@ATTRIBUTE 'skewness' REAL" + "\n" +
                            "@ATTRIBUTE 'kurtosis' REAL" + "\n" +
                            "@ATTRIBUTE 'energy' REAL" + "\n" +
                            "@ATTRIBUTE 'cross correlation with template 1' REAL" + "\n" +
                            "@ATTRIBUTE 'cross correlation with template 2' REAL" + "\n" +
                            "@ATTRIBUTE 'area difference with template 1' REAL" + "\n" +
                            "@ATTRIBUTE 'area difference with template 2' REAL" + "\n" +
                            //combined feature file has this additional feature
                            // individual feature files can't then be imported in WEKA
                            "@ATTRIBUTE 'Previous RR interval' REAL" + "\n" +
                            "@ATTRIBUTE 'class' {NORMAL, ABNORMAL, UNKNOWN}" + "\n" +
                            "@DATA" + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the QRS templates needed to compute certain QRS features.
     *
     * @param template1 the first QRS template
     * @param template2 the second QRS template
     */
    public void setQrsTemplates(QrsComplex template1, QrsComplex template2) {
        this.template1 = template1;
        this.template2 = template2;
    }

    /**
     * Writes a QRS complex with an unknown QRS class to the ARFF file.
     *
     * @param qrsComplex the QRS complex
     */
    public void writeToFile(QrsComplex qrsComplex) {
        if (writer != null && qrsComplex != null) {
            try {
                writer.write(String.valueOf(qrsComplex.getRPosition()) + "," +
                        String.valueOf(qrsComplex.getRValue()) + "," +
                        String.valueOf(qrsComplex.getRRInterval()) + "," +
                        String.valueOf(qrsComplex.getQRAmplitude()) + "," +
                        String.valueOf(qrsComplex.getRSAmplitude()) + "," +
                        String.valueOf(qrsComplex.getQRSWidth()) + "," +
                        String.valueOf(qrsComplex.getQRSTArea()) + "," +
                        String.valueOf(qrsComplex.getMinimum()) + "," +
                        String.valueOf(qrsComplex.getMaximum()) + "," +
                        String.valueOf(qrsComplex.getMean()) + "," +
                        String.valueOf(qrsComplex.getVariance()) + "," +
                        String.valueOf(qrsComplex.getStandardDeviation()) + "," +
                        String.valueOf(qrsComplex.getSkewness()) + "," +
                        String.valueOf(qrsComplex.getKurtosis()) + "," +
                        String.valueOf(qrsComplex.getEnergy()) + "," +
                        String.valueOf(qrsComplex.getCrossCorrelation(template1)) + "," +
                        String.valueOf(qrsComplex.getCrossCorrelation(template2)) + "," +
                        String.valueOf(qrsComplex.getAreaDifference(template1)) + "," +
                        String.valueOf(qrsComplex.getAreaDifference(template2)) + "," +
                        QrsClass.UNKNOWN.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes a QRS complex and the corresponding QRS class to the ARFF file
     *
     * @param qrsComplex the QRS complex
     * @param qrsClass   the QRS class of the QRS complex
     */
    public void writeToFile(QrsComplex qrsComplex, QrsClass qrsClass) {
        if (writer != null && qrsComplex != null) {
            try {
                writer.write(String.valueOf(qrsComplex.getRPosition()) + "," +
                        String.valueOf(qrsComplex.getRValue()) + "," +
                        String.valueOf(qrsComplex.getRRInterval()) + "," +
                        String.valueOf(qrsComplex.getQRAmplitude()) + "," +
                        String.valueOf(qrsComplex.getRSAmplitude()) + "," +
                        String.valueOf(qrsComplex.getQRSWidth()) + "," +
                        String.valueOf(qrsComplex.getQRSTArea()) + "," +
                        String.valueOf(qrsComplex.getMinimum()) + "," +
                        String.valueOf(qrsComplex.getMaximum()) + "," +
                        String.valueOf(qrsComplex.getMean()) + "," +
                        String.valueOf(qrsComplex.getVariance()) + "," +
                        String.valueOf(qrsComplex.getStandardDeviation()) + "," +
                        String.valueOf(qrsComplex.getSkewness()) + "," +
                        String.valueOf(qrsComplex.getKurtosis()) + "," +
                        String.valueOf(qrsComplex.getEnergy()) + "," +
                        String.valueOf(qrsComplex.getCrossCorrelation(template1)) + "," +
                        String.valueOf(qrsComplex.getCrossCorrelation(template2)) + "," +
                        String.valueOf(qrsComplex.getAreaDifference(template1)) + "," +
                        String.valueOf(qrsComplex.getAreaDifference(template2)) + "," +
                        qrsClass.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the ARFF file with the QRS complexes and classes.
     */
    public void saveFile() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
