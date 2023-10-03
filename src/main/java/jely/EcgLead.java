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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Definition of all possible (supported) ECG leads.
 * @author Stefan Gradl
 *
 */
public enum EcgLead {
    /**
     * ECG lead is invalid/unused.
     */
    INVALID,

    /**
     * ECG lead is not known.
     */
    UNKNOWN,

    /**
     * Standard limb lead (Einthoven) I (+0°)
     * == LA - RA
     */
    I,
    /**
     * Standard limb lead (Einthoven) II (+60°)
     * == LL - RA
     */
    II,
    /**
     * Standard limb lead (Einthoven) III (+120°)
     * == LL - LA
     */
    III,

    /**
     * Augmented left lateral lead (-30°)
     */
    aVL,
    /**
     * Augmented right-sided lead (-150°)
     */
    aVR,
    /**
     * Augmented inferior lead (+90°)
     */
    aVF,

    /**
     * Precordial lead V1
     */
    V1,
    /**
     * Precordial lead V2
     */
    V2,
    /**
     * Precordial lead V3
     */
    V3,
    /**
     * Precordial lead V4
     */
    V4,
    /**
     * Precordial lead V5
     */
    V5,
    /**
     * Precordial lead V6
     */
    V6;


    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        os.writeUTF(this.name());
    }

    public static EcgLead readFromBinaryStream(DataInputStream is) throws IOException {
        return EcgLead.valueOf(is.readUTF());
    }
}
