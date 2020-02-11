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

import java.io.File;
import java.io.IOException;

import de.fau.mad.jely.Ecg;
import de.fau.mad.jely.LeadConfiguration;

/**
 * An automated static file loader class that detects the format of the given
 * file and invokes the corresponding loading methods.
 *
 * @author Stefan Gradl
 */
public class FileLoader {

    /**
     * Loads an ECG from any supported/known file type.
     *
     * @param path
     * @return
     */
    public static Ecg loadKnownEcgFile(String path) {
        Ecg ecg = null;

        try {

            if (path.endsWith(".BinaryEcg")) {
                ecg = new BinaryEcgFile(path);
            } else if (path.endsWith(".hea")) {
                ecg = new PhysionetEcgFile(path);
            } else if (path.endsWith(".dat")) {
                // could be a physionet file, search for .hea file
                File dat = new File(path);
                File fdir = dat.getParentFile();
                File hea = new File(fdir, dat.getName().substring(0, dat.getName().length() - 4) + ".hea");
                if (hea.exists()) {
                    ecg = new PhysionetEcgFile(hea.getAbsolutePath());
                } else {
                    throw new Exception("Missing .hea file for assumed physionet data .dat file.");
                }
            } else if (path.endsWith(".bin")) {
                ecg = new CustoMedBinaryEcgFile(path);
            } else if (path.endsWith(".csv")) {
                ecg = new CsvEcgFile(path);
            } else if (path.endsWith(".txt")) {
                ecg = new CsvEcgFile(path);
            } else {
                // try our luck with the BinaryEcgFile
                try {
                    ecg = new BinaryEcgFile(path);
                } catch (IOException e) {
                    throw new Exception("Unknown file type.");
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ecg;
    }

    /**
     * Use loadKnownEcgFile() instead!
     *
     * @param path
     * @return
     * @deprecated use {@link #loadKnownEcgFile(String)} instead.
     */
    @Deprecated
    public static Ecg LoadKnownEcgFile(String path) {
        return loadKnownEcgFile(path);
    }

    /**
     * Use loadKnownEcgFile() instead!
     *
     * @param path
     * @param leads
     * @return
     * @deprecated use {@link #loadKnownEcgFile(String, LeadConfiguration, double)}
     * instead.
     */
    @Deprecated
    public static Ecg LoadKnownEcgFile(String path, LeadConfiguration leads) {
        return LoadKnownEcgFile(path, leads);
    }

    /**
     * Loads an ECG from any supported/known file type.
     *
     * @param path
     * @param leads Known lead configuration found in the file. This is very
     *              useful if you load e.g. a CSV that is missing all meta
     *              information.
     * @return
     */
    public static Ecg loadKnownEcgFile(String path, LeadConfiguration leads, double samplingRate) {
        Ecg ecg = null;

        try {

            if (path.endsWith(".BinaryEcg")) {
                ecg = new BinaryEcgFile(path);
            } else if (path.endsWith(".hea")) {

                ecg = new PhysionetEcgFile(path);

            } else if (path.endsWith(".dat")) {
                // could be a physionet file, search for .hea file
                File dat = new File(path);
                File fdir = dat.getParentFile();
                File hea = new File(fdir, dat.getName().substring(0, dat.getName().length() - 4) + ".hea");
                if (hea.exists()) {
                    ecg = new PhysionetEcgFile(hea.getAbsolutePath());
                } else {
                    throw new Exception("Missing .hea file for assumed physionet data .dat file.");
                }
            } else if (path.endsWith(".bin")) {
                ecg = new CustoMedBinaryEcgFile(path);
            } else if (path.endsWith(".csv")) {
                ecg = new CsvEcgFile(path, leads, samplingRate);
            } else {
                // try our luck with the BinaryEcgFile
                try {
                    ecg = new BinaryEcgFile(path);
                } catch (IOException e) {
                    throw new Exception("Unknown file type.");
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ecg;
    }
}
