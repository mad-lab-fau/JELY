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
package jely.io;

import jely.Ecg;
import jely.annotations.GeneralAnnotation;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;

/**
 * Loads a physionet signal file.
 *
 * @author Stefan Gradl
 */
public class PhysionetEcgFile extends Ecg {
    private PhysionetHeaderFile header;
    private String baseDir;

    private short[] checksums;

    /**
     * Loads a .hea physionet signal header file and parses its information. If it is a valid ECG signal a .dat file in
     * the same directory as the header file is searched and parsed.
     *
     * @param pathToHeaderFile Path to the .hea physionet signal header file.
     * @throws Exception
     */
    public PhysionetEcgFile(String pathToHeaderFile) throws Exception {
        // read the header file
        header = new PhysionetHeaderFile(pathToHeaderFile);

        // init the structures from the Ecg class
        leadInfo = header.getLeads();
        samplingRate = header.getSamplingFrequency();
        date = null;
        if (header.getDate() != null)
            date = header.getDate();

        firstSampleDate = date;

        // add comments found in header file as general annotation
        annotations.addAnnotation(new GeneralAnnotation(header.getComments()));

        baseDir = new File(pathToHeaderFile).getParentFile().getAbsolutePath();

        // init signal structures
        this.init(header.getNumSamples());

        if (!header.getSignalFormat().equals("212") || leadInfo.size() != 2) {
            throw new Exception("Unsupported signal format.");
        }
        loadSignalsIn212Format();

        testChecksums();

        loadAnnotations();
    }

    private void loadSignalsIn212Format() throws Exception {
        File f = new File(baseDir, header.getSignalFileNames()[0]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));

        checksums = new short[header.getChecksums().length];
        try {
            while (true) {
                //
                // Format 212
                // Each sample is represented by a 12-bit two's complement amplitude. The first sample is obtained from
                // the 12 least significant bits of the first byte pair (stored least significant byte first). The
                // second sample is formed from the 4 remaining bits of the first byte pair (which are the 4 high bits
                // of the 12-bit sample) and the next byte (which contains the remaining 8 bits of the second sample).
                // The process is repeated for each successive pair of samples.
                // Most of the signal files in PhysioBank are written in format 212.
                //
                // From: http://www.physionet.org/physiotools/wag/signal-5.htm

                byte b0 = in.readByte();
                byte b1 = in.readByte();
                byte b2 = in.readByte();

                short ampValue0, ampValue1;

                // since the 12 bit "shorts" are actually signed, we need to
                // check for that negative sign bit and fill
                // the 4 high bits in the 16-bit Java short to correctly
                // compensate.
                if ((b1 & 0x8) == 0) {
                    // positive value
                    ampValue0 = (short) ((b0 & 0x000000FF) | (b1 & 0x0000000F) << 8);
                } else {
                    // negative 12-bit value, shift fill the highest 4 bits in
                    // the 16 bit short
                    ampValue0 = (short) ((short) ((b0 & 0x000000FF) | (b1 & 0x0000000F) << 8) | (0xF << 12));
                }

                if ((b1 & 0x80) == 0) {
                    // positive value
                    ampValue1 = (short) ((b2 & 0x000000FF) | (b1 & 0x000000F0) << 4);
                } else {
                    // negative 12-bit value, shift fill the highest 4 bits in
                    // the 16 bit short
                    ampValue1 = (short) ((short) ((b2 & 0x000000FF) | (b1 & 0x000000F0) << 4) | (0xF << 12));
                }

                checksums[0] += ampValue0;
                checksums[1] += ampValue1;

                // convert from digital to physical units and store the samples
                this.ecgLeads.get(0).add((ampValue0 - header.getBaseline()) / header.getAdcGain());
                this.ecgLeads.get(1).add((ampValue1 - header.getBaseline()) / header.getAdcGain());

                // for debugging...
                //this.ecgLeads.get( 0 ).add( (double) (ampValue0) );
                //this.ecgLeads.get( 1 ).add( (double) (ampValue1) );

                // if (this.ecgLeads.get( 0 ).size() < 10)
                // {
                // System.out.println( "b0: " + b0 + "; b1: " + b1 + "; b2: " +
                // b2 );
                // System.out.println( "a0: " + ampValue0 + "; a1: " + ampValue1
                // );
                // }
            }
        } catch (EOFException e) {
        }

        in.close();
    }

    private void testChecksums() throws Exception {
        // for each lead/signal
        for (int i = 0; i < this.ecgLeads.size(); i++) {
            // check if we can check the checksum
            if (this.ecgLeads.get(i).size() == header.getNumSamples()) {
                int realChecksum = header.getChecksums()[i];
                if (checksums[i] != realChecksum) {
                    throw new Exception("Checksum mismatch for signal #" + i + ". Checksum is: " + checksums[i] + ", expected: "
                            + realChecksum);
                }
            }
        }
    }

    private void loadAnnotations() throws Exception {
        String filename = header.getSignalFileNames()[0];
        File f = new File(baseDir, filename.substring(0, filename.length() - 4) + ".atr");
        if (!f.exists())
            return;
        // TODO: load other/all annotation files

        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));

        try {
            while (true) {

                byte b0 = in.readByte();
                byte b1 = in.readByte();
                byte b2 = in.readByte();

            }
        } catch (EOFException e) {
        }

        in.close();
    }
}
