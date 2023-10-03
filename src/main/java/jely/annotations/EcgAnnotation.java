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

import jely.EcgLead;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Base annotation class.
 *
 * @author Stefan Gradl
 */
public class EcgAnnotation {
    private AnnotationType type = AnnotationType.UNKNOWN;

    /**
     * Annotator for this annotation.
     */
    private EcgAnnotator annotator;

    /**
     * From which lead this incident was annotated, or in which lead it is most dominantly visible.
     */
    private EcgLead lead = EcgLead.UNKNOWN;

    /**
     * Index of the sample in the ECG where the annotated event was observed.
     */
    private int sampleIndex = -1;

    /**
     * Index of the sample in the ECG where the annotated event ended, if applicable. -1 if the event has no end, or the end is the same as the start.
     */
    private int sampleIndexEnd = -1;

    /**
     * Description for this annotation.
     */
    protected String description;


    protected EcgAnnotation() {

    }

    public EcgAnnotation(DataInputStream is, AnnotationManager am, long version) throws IOException {
        int ai = is.readInt();
        if (version > 5) {
            type = AnnotationType.readFromBinaryStream(is);
        }
        annotator = am.getAnnotator(ai);
        description = is.readUTF();
        lead = EcgLead.readFromBinaryStream(is);
        sampleIndex = is.readInt();
        sampleIndexEnd = is.readInt();
    }

    protected EcgAnnotation(AnnotationType type) {
        this.type = type;
        this.annotator = EcgAnnotator.UNKNOWN;
    }

    public EcgAnnotation(AnnotationType type, String description) {
        this(type);
        this.description = description;
    }

    public EcgAnnotation(AnnotationType type, EcgAnnotator annotator, String description) {
        this(type, description);
        this.annotator = annotator;
    }

    public EcgAnnotation(AnnotationType type, EcgAnnotator ann, String description, int sampleIndex) {
        this(type, ann, description);
        this.sampleIndex = sampleIndex;
    }

    public EcgAnnotation(AnnotationType type, EcgAnnotator ann, String description, int sampleIndex, EcgLead lead) {
        this(type, ann, description, sampleIndex);
        this.lead = lead;
    }

    public EcgAnnotation(AnnotationType type, EcgAnnotator ann, String description, int sampleIndex, int sampleIndexEnd, EcgLead lead) {
        this(type, ann, description, sampleIndex, lead);
        this.sampleIndexEnd = sampleIndexEnd;
    }

    /**
     * @return the annotator
     */
    public EcgAnnotator getAnnotator() {
        return annotator;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the sampleIndex
     */
    public int getSampleIndex() {
        return sampleIndex;
    }

    /**
     * @return the sampleIndexEnd
     */
    public int getSampleIndexEnd() {
        return sampleIndexEnd;
    }

    /**
     * @return the type
     */
    public AnnotationType getType() {
        return type;
    }

    public void writeToBinaryStream(DataOutputStream os, AnnotationManager am) throws IOException {
        os.writeInt(am.getAnnotatorIndex(annotator));
        type.writeToBinaryStream(os);
        os.writeUTF(description);
        lead.writeToBinaryStream(os);
        os.writeInt(sampleIndex);
        os.writeInt(sampleIndexEnd);
    }


}
