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
package de.fau.mad.jely;

import java.util.ArrayList;
import java.util.Date;

import de.fau.mad.jely.annotations.EcgAnnotation;
import de.fau.mad.jely.annotations.AnnotationManager;
import de.fau.mad.jely.annotations.SubjectInfo;

/**
 * Represents an Electrocardiogram (ECG) in memory.
 *
 * @author Stefan Gradl
 */
public class Ecg {
    /**
     * Stores the date the ECG was taken/recorded/created.
     */
    protected Date date = null;
    /**
     * The exact (millisecond precision) time and date at which the first sample
     * of this ECG was recorded.
     */
    protected Date firstSampleDate = null;
    /**
     * The timestamp for the first sample in system nanoseconds.
     */
    protected long firstSampleTimestamp = 0;
    /**
     * Sampling rate of the ECG in Hertz.
     */
    protected double samplingRate = 0;

    /**
     * Info about the lead configuration in which the ECG is stored.
     */
    protected LeadConfiguration leadInfo = null;

    /**
     * The actual sample values of the different ECG leads.
     */
    protected ArrayList<EcgSignal> ecgLeads = null;

    // protected ArrayList<String> physicalUnit = null;

    /**
     * Information about the subject this ECG was taken from.
     */
    protected SubjectInfo subject = new SubjectInfo(this);

    /**
     * All annotations corresponding to this ECG.
     */
    protected AnnotationManager annotations = new AnnotationManager();

    /**
     * Empty constructor. Only accessible for child-classes since its empty...
     */
    protected Ecg() {
    }

    /**
     * Constructor for an ECG where the sampling rate and number of channels is
     * known.
     *
     * @param samplingRate
     * @param numChannels
     * @deprecated This constructor should not be used anymore as the lead
     * configuration is unknown.
     */
    public Ecg(double samplingRate, int numChannels) {
        leadInfo = new LeadConfiguration(numChannels);
        this.samplingRate = samplingRate;
        date = new Date();
    }

    /**
     * Constructor for an ECG where the sampling rate and used leads are known.
     *
     * @param samplingRate
     * @param leads
     */
    public Ecg(double samplingRate, LeadConfiguration leads) {
        leadInfo = leads;
        this.samplingRate = samplingRate;
        date = new Date();
    }

    public Ecg(double samplingRate, LeadConfiguration leads, SubjectInfo subject) {
        this(samplingRate, leads);
        subject.setEcg(this);
        this.subject = subject;
    }

    public Ecg(double samplingRate, LeadConfiguration leads, SubjectInfo subject, double secondsToKeepInMemory) {
        this(samplingRate, leads, subject);
        init((int) (secondsToKeepInMemory * samplingRate));
    }

    public Ecg(double[] signal, double samplingRate, EcgLead lead) {
        leadInfo = new LeadConfiguration(lead);
        this.samplingRate = samplingRate;
        date = new Date();
        ecgLeads = new ArrayList<EcgSignal>(1);
        ecgLeads.add(new EcgSignal(lead, this, signal));
    }

    /**
     * Inits the ECG memory. During streaming mode the initial capacity can not
     * increase. If it does, the oldest sample gets overwritten by the newest.
     *
     * @param initialSampleCapacity Number of samples the ECG can store.
     */
    public void init(int initialSampleCapacity) {
        ecgLeads = new ArrayList<EcgSignal>(getNumLeads());
        for (int i = 0; i < getNumLeads(); i++) {
            ecgLeads.add(new EcgSignal(leadInfo.getLead(i), this, initialSampleCapacity));
        }
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the date of the first signal sample.
     */
    public Date getFirstSampleDate() {
        return firstSampleDate;
    }

    /**
     * @return the timestamp of the first signal sample in system nanoseconds.
     * Independent of the wall clock time.
     */
    public long getFirstSampleTimestamp() {
        return firstSampleTimestamp;
    }

    /**
     * @return the samplingRate
     */
    public double getSamplingRate() {
        return samplingRate;
    }

    /**
     * Calculates the sampling time interval in seconds.
     *
     * @return Sampling time interval in seconds.
     */
    public double getSamplingInterval() {
        return 1d / samplingRate;
    }

    /**
     * @return the numChannels
     */
    public int getNumChannels() {
        return getNumLeads();
    }

    /**
     * @return the number of leads.
     */
    public int getNumLeads() {
        return leadInfo.size();
    }

    /**
     * @return
     */
    public LeadConfiguration getLeads() {
        return new LeadConfiguration(leadInfo);
    }

    /**
     * Calculates the time for the given sample index relative to the start
     * (zero-based) of this ECG in seconds.
     *
     * @param sampleIdx
     * @return
     */
    public double getSampleTime(int sampleIdx) {
        return sampleIdx * getSamplingInterval();
    }

    /**
     * Calculates the time for the given sample index in real (epoch) time in
     * milliseconds.
     *
     * @param sampleIdx
     * @return
     */
    public long getSampleTimestamp(int sampleIdx) {
        return date.getTime() + Math.round(getSampleTime(sampleIdx) * 1000);
    }

    /**
     * @param sampleIdx
     * @return a formatted time string (h:m:s'ms) for the given sample index.
     */
    public String getFormattedSampleTime(int sampleIdx) {
        double diffInSeconds = getSampleTime(sampleIdx);

        long diff[] = new long[]{0, 0, 0, 0};
        diff[3] = (long) ((diffInSeconds - (long) diffInSeconds) * 1000);
        /* sec */
        diff[2] = (long) (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
        /* min */
        diff[1] = (long) ((diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds);
        /* hours */
        diff[0] = (long) ((diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds);

        return String.format("%02d:%02d:%02d'%03d", diff[0], diff[1], diff[2], diff[3]);
    }

    /**
     * @param leadIndex
     * @return the EcgSignal for the given lead index.
     */
    public EcgSignal getSignalFromIndex(int leadIndex) {
        return ecgLeads.get(leadIndex);
    }

    /**
     * @param lead
     * @return the EcgSignal for the given lead, or <code>null</code> if the
     * lead is not present in this ECG.
     */
    public EcgSignal getSignalFromLead(EcgLead lead) {
        if (!leadInfo.hasLead(lead))
            return null;

        return ecgLeads.get(leadInfo.getLeadIndex(lead));
    }

    /**
     * Provides the EcgSignal from the given lead or, if that is not present,
     * from the best matching/closest representative lead.
     *
     * @param lead the lead which is ideally desired.
     * @return the EcgSignal matching the given lead most closely.
     */
    public EcgSignal getSignalFromBestMatchingLead(EcgLead lead) {
        int i = leadInfo.getLeadIndex(lead);
        if (i == -1) {
            // TODO: select the best matching lead to the desired one
            i = 0;
        }
        return ecgLeads.get(i);
    }

    public EcgSignal getSignal(int leadIndex) {
        return getSignalFromIndex(leadIndex);
    }

    public EcgSignal getSignal(EcgLead lead) {
        return getSignalFromLead(lead);
    }

    /**
     * @return the length in samples of this ECG signal, based on the first lead
     * (if available).
     */
    public long size() {
        EcgSignal s = getSignal(0);
        if (s != null)
            return s.size();
        return 0;
    }

    /**
     * @param lead
     * @return true if the given lead is present in this ECG, false if not.
     */
    public boolean hasLead(EcgLead lead) {
        return leadInfo.hasLead(lead);
    }

    public void addAnnotation(EcgAnnotation ann) {
        annotations.addAnnotation(ann);
    }

    public AnnotationManager getAnnotations() {
        return annotations;
    }

    public SubjectInfo getSubject() {
        return subject;
    }

    private void checkFirstSampleTime() {
        if (firstSampleDate == null) {
            firstSampleDate = new Date();
            firstSampleTimestamp = System.nanoTime();
        }
    }

    public void addSampleValue(int leadIndex, double value) {
        checkFirstSampleTime();
        ecgLeads.get(leadIndex).add(value);
    }

    public void addSampleValue(EcgLead lead, double value) {
        checkFirstSampleTime();
        addSampleValue(leadInfo.getLeadIndex(lead), value);
    }

    public void addTwoLeadSampleValues(double valueLead0, double valueLead1) {
        checkFirstSampleTime();
        ecgLeads.get(0).add(valueLead0);
        ecgLeads.get(1).add(valueLead1);
    }

    /**
     * @return a checksum for all signals.
     */
    public long calculateSignalChecksum() {
        long checksum = 0;
        for (int i = 0; i < getNumLeads(); i++) {
            checksum += ecgLeads.get(i).calculateChecksum();
        }
        return checksum;
    }
}
