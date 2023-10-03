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
package de.fau.mad.jely.annotations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Annotation types for any annotation in a signal.
 *
 * @author Stefan Gradl
 */
public enum AnnotationType {
    UNKNOWN,
    GENERAL,
    SUBJECT,
    /**
     * Annotations related to the location of beat characteristics, e.g. R-peak location, Q-point location, P-wave onset or peak locations, etc.
     */
    BEAT_LOCATIONS,
    BEAT_TYPE,
    RHYTHM,
    SIGNAL_QUALITY,
    COMMENT,
    LINK;

    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        os.writeByte(this.ordinal());
    }

    public static AnnotationType readFromBinaryStream(DataInputStream is) throws IOException {
        return AnnotationType.values()[is.readByte()];
    }
}
