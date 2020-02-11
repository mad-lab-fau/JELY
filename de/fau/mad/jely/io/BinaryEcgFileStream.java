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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.Date;

import de.fau.mad.jely.LeadConfiguration;
import de.fau.mad.jely.annotations.SubjectInfo;

/**
 * A data output stream that lets an application write a binary ecg file.
 *
 * @author Stefan Gradl
 */
public class BinaryEcgFileStream extends BinaryEcgFile {
    private DataOutputStream mOutStream = null;
    private boolean mHeaderWritten = false;

    /**
     * Create a new file stream for streaming binary ecg data. Annotations can
     * only be added before the first ECG sample is streamed to the file!
     *
     * @param directoy
     * @param samplingRate
     * @param leads
     * @param subject
     * @throws FileNotFoundException
     */
    public BinaryEcgFileStream(String directory, double samplingRate, LeadConfiguration leads, SubjectInfo subject,
                               double secondsToKeepInMemory) {
        super(samplingRate, leads, subject, secondsToKeepInMemory);
        initStream(directory);
    }

    /**
     * Create a new file stream for streaming binary ecg data. Annotations can
     * only be added before the first ECG sample is streamed to the file!
     *
     * @param directory    Directory where the file should be streamed to.
     * @param samplingRate Sampling rate of the ECG signal.
     */
    public BinaryEcgFileStream(String directory, double samplingRate) {
        super(samplingRate, LeadConfiguration.SINGLE_UNKNOWN_LEAD, new SubjectInfo(), 10);
        initStream(directory);
    }

    private void initStream(String directory) {
        File rawFile;
        rawFile = new File(directory, getStandardizedFileName(this));
        for (int i = 1; rawFile.exists(); i++) {
            rawFile = new File(directory, getStandardizedFileName(this, i));
        }
        setFullPath(rawFile.getAbsolutePath());

        try {
            mOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rawFile)));
        } catch (FileNotFoundException e) {
            //
            e.printStackTrace();
        }
    }

    private void startStreamingSamples() {
        mHeaderWritten = true;

        if (firstSampleDate == null) {
            firstSampleDate = new Date();
            firstSampleTimestamp = System.nanoTime();
        }

        BinaryEcgFile.writeHeaderAndAnnotations(this, mOutStream);

        try {
            // write size
            mOutStream.writeInt(0);
            // write checksum
            mOutStream.writeLong(0);

            mOutStream.writeLong(firstSampleDate.getTime());
            mOutStream.writeLong(firstSampleTimestamp);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fau.mad.jely.Ecg#addSampleValue(int, double)
     */
    @Override
    public void addSampleValue(int leadIndex, double value) {
        super.addSampleValue(leadIndex, value);

        try {
            if (!mHeaderWritten)
                startStreamingSamples();
            mOutStream.writeDouble(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fau.mad.jely.Ecg#addTwoLeadSampleValues(double, double)
     */
    @Override
    public void addTwoLeadSampleValues(double valueLead0, double valueLead1) {
        super.addTwoLeadSampleValues(valueLead0, valueLead1);

        try {
            if (!mHeaderWritten)
                startStreamingSamples();
            mOutStream.writeDouble(valueLead0);
            mOutStream.writeDouble(valueLead1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private transient int currentWriteLead = 0;

    /**
     * Writes an ECG-value to long-term storage. The user of this method has to
     * make sure that the value is written in channel-specific order, according
     * to the number of channels this file was created with.
     *
     * @param ecgValue value of the ECG
     * @throws IOException
     */
    private void writeEcgSample(double ecgValue) throws IOException {
        if (!mHeaderWritten)
            startStreamingSamples();

        mOutStream.writeDouble(ecgValue);

        ecgLeads.get(currentWriteLead++).add(ecgValue);
        if (currentWriteLead == ecgLeads.size())
            currentWriteLead = 0;
    }

    /**
     * Closes an ecg file stream after data was written/streamed to it.
     */
    public void close() {
        try {
            mOutStream.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
    }
}
