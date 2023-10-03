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

public enum SubjectSex {
    FEMALE,
    MALE,
    UNKNOWN;

    /**
     * Writes this enum to a binary stream.
     *
     * @param os
     * @throws IOException
     */
    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        os.writeByte(this.ordinal());
    }

    /**
     * Reads this enum from a binary stream.
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static SubjectSex readFromBinaryStream(DataInputStream is) throws IOException {
        return SubjectSex.values()[is.readByte()];
    }
}
