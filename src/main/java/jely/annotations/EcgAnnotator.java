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
package jely.annotations;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Annotator of ECG annotations.
 * @author Stefan Gradl
 *
 */
public class EcgAnnotator {
    /**
     * Unknown annotator or annotator not required.
     */
    public static final EcgAnnotator UNKNOWN = new EcgAnnotator("n/a");

    /**
     * Unique identifier for this annotator. 
     */
    private String identifier;

    /**
     * Constructs an annotator from a binary file stream.
     * @param is
     * @throws IOException
     */
    public EcgAnnotator(DataInputStream is) throws IOException {
        identifier = is.readUTF();
    }

    /**
     * Constructs a new annotator given the unique identifier.
     * @param identifier
     */
    public EcgAnnotator(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Write this annotator to the given binary file stream.
     * @param os
     * @throws IOException
     */
    public void writeToBinaryStream(DataOutputStream os) throws IOException {
        os.writeUTF(identifier);
    }
}
