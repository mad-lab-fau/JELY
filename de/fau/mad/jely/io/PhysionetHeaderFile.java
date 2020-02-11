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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.fau.mad.jely.EcgLead;
import de.fau.mad.jely.LeadConfiguration;

/**
 * Reads and parses a physionet .hea file according to http://www.physionet.org/physiotools/wag/header-5.htm. The
 * described standard is not fully implemented yet (TODO), but should be functional.
 *
 * @author Stefan Gradl
 */
public class PhysionetHeaderFile {
    private String comments = "";
    private String recordName;
    private double samplingFrequency = 250;
    private int numSamples = 0;
    private String[] signalFileNames;
    private LeadConfiguration leads;
    private String signalFormat;
    private Date date = null;
    private double adcGain = 200;
    private int adcResolutionInBits = 12;
    private int adcZero = 0;
    private int baseline = 0;
    private int[] initialSignalValues;
    private int[] checksums;
    private int blockSize = 0;

    /**
     * Constructs a physionet header by reading the .hea file at the given path.
     *
     * @param pathToHeaderFile
     */
    public PhysionetHeaderFile(String pathToHeaderFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(pathToHeaderFile)));

            boolean recordLineDone = false;
            int numSignals = 0;
            int curSignal = 0;
            Integer baselineValue = null;

            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();

                // empty lines
                if (line.isEmpty())
                    continue;

                // comment lines
                if (line.charAt(0) == '#') {
                    // physionet dbs have subject/comment annotations in the comment lines, we add it later as a general
                    // annotation
                    comments += line.substring(1).trim() + "\n";
                    continue;
                }

                String[] splits = line.split(" ");

                // has the record line already been read?
                if (!recordLineDone) {
                    // read the record line
                    recordName = splits[0];
                    numSignals = Integer.parseInt(splits[1]);
                    if (splits.length > 2) {
                        String[] fsplits = splits[2].split("/");
                        samplingFrequency = Double.parseDouble(fsplits[0]);
                    }

                    // number of samples per signal [optional]
                    // This field can be present only if the sampling frequency is also present. If it is zero or
                    // missing, the number of samples is unspecified and checksum verification of the signals is
                    // disabled.
                    if (splits.length > 3) {
                        numSamples = Integer.parseInt(splits[3]);

                        // base time [optional]
                        // This field can be present only if the number of samples is also present. It gives the time of
                        // day that corresponds to the beginning of the record, in HH:MM:SS format (using a 24-hour
                        // clock; thus 13:05:00, or 13:5:0, represent 1:05 pm). If this field is absent, the
                        // time-conversion functions assume a value of 0:0:0, corresponding to midnight.
                        //
                        // base date [optional]
                        // This field can be present only if the base time is also present. It contains the date that
                        // corresponds to the beginning of the record, in DD/MM/YYYY format (e.g., 25/4/1989 is 25 April
                        // 1989).
                        if (splits.length > 5) {
                            // try to infer date and time from the strings at this location
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
                            try {
                                date = sdf.parse(splits[5] + " at " + splits[4]);
                            } catch (ParseException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }

                    signalFileNames = new String[numSignals];
                    initialSignalValues = new int[numSignals];
                    checksums = new int[numSignals];
                    leads = new LeadConfiguration();

                    recordLineDone = true;
                } else {
                    // read a signal line
                    if (curSignal >= numSignals)
                        continue;

                    signalFileNames[curSignal] = splits[0];
                    signalFormat = splits[1];

                    if (splits.length > 2) {
                        String[] gainSplits = splits[2].split("[/]");
                        if (gainSplits.length > 1) {
                            // extract unit name here...
                        }

                        String[] baselineSplits = gainSplits[0].split("[(]");
                        adcGain = Double.parseDouble(baselineSplits[0]);
                        if (baselineSplits.length > 1) {
                            String baselineString = baselineSplits[1].substring(0, baselineSplits[1].length() - 1);
                            baselineValue = new Integer(baselineString);
                        }
                    }

                    // TODO: handle all optional parameter cases with () and / and +
                    if (splits.length > 3) {
                        adcResolutionInBits = Integer.parseInt(splits[3]);
                    }

                    if (splits.length > 4) {
                        adcZero = Integer.parseInt(splits[4]);
                        if (baselineValue == null)
                            baselineValue = new Integer(adcZero);
                    }

                    if (splits.length > 5) {
                        initialSignalValues[curSignal] = Integer.parseInt(splits[5]);
                    }

                    if (splits.length > 6) {
                        checksums[curSignal] = Integer.parseInt(splits[6]);
                    }

                    if (splits.length > 7) {
                        blockSize = Integer.parseInt(splits[7]);
                    }

                    if (splits.length > 8) {
                        String strLead = splits[8].toLowerCase();
                        if (strLead.contains("iii") || strLead.equals("ml3"))
                            leads.add(EcgLead.III);
                        else if (strLead.equals("mlii") || strLead.equals("ii") || strLead.equals("ml2"))
                            leads.add(EcgLead.II);
                        else if (strLead.equals("mli") || strLead.equals("i") || strLead.equals("ml1"))
                            leads.add(EcgLead.I);
                        else if (strLead.contains("v1"))
                            leads.add(EcgLead.V1);
                        else if (strLead.contains("v2"))
                            leads.add(EcgLead.V2);
                        else if (strLead.contains("v3"))
                            leads.add(EcgLead.V3);
                        else if (strLead.contains("v4"))
                            leads.add(EcgLead.V4);
                        else if (strLead.contains("v5"))
                            leads.add(EcgLead.V5);
                        else if (strLead.contains("v6"))
                            leads.add(EcgLead.V6);
                        else
                            leads.add(EcgLead.UNKNOWN);
                    }

                    curSignal++;
                }
            }

            baseline = baselineValue;

            br.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * @return the recordName
     */
    public String getRecordName() {
        return recordName;
    }

    /**
     * @return the samplingFrequency
     */
    public double getSamplingFrequency() {
        return samplingFrequency;
    }

    /**
     * @return the numSamples
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * @return the signalFileNames
     */
    public String[] getSignalFileNames() {
        return signalFileNames;
    }

    /**
     * @return the leads
     */
    public LeadConfiguration getLeads() {
        return leads;
    }

    /**
     * @return the signalFormat
     */
    public String getSignalFormat() {
        return signalFormat;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the adcGain
     */
    public double getAdcGain() {
        return adcGain;
    }

    /**
     * @return the baseline
     */
    public int getBaseline() {
        return baseline;
    }

    /**
     * @return the adcResolutionInBits
     */
    public int getAdcResolutionInBits() {
        return adcResolutionInBits;
    }

    /**
     * @return the adcZero
     */
    public int getAdcZero() {
        return adcZero;
    }

    /**
     * @return the initialSignalValues
     */
    public int[] getInitialSignalValues() {
        return initialSignalValues;
    }

    /**
     * @return the checksums
     */
    public int[] getChecksums() {
        return checksums;
    }

    /**
     * @return the blockSize
     */
    public int getBlockSize() {
        return blockSize;
    }
}
