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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Manages annotations and annotators.
 *
 * @author Stefan Gradl
 */
public class AnnotationManager {
    /**
     * Annotators found in all annotations.
     */
    private ArrayList<EcgAnnotator> annotators = null;

    /**
     * All annotations for this ECG.
     */
    private ArrayList<EcgAnnotation> annotations = null;

    /**
     * Constructs an empty annotation manager.
     */
    public AnnotationManager() {
        annotations = new ArrayList<EcgAnnotation>();
        annotators = new ArrayList<EcgAnnotator>();
    }

    /**
     * Loads an annotation manager state from a binary file stream.
     *
     * @param is
     * @throws IOException
     */
    public AnnotationManager(DataInputStream is, long version) throws IOException {
        int num = is.readInt(); // number of annotators
        annotators = new ArrayList<EcgAnnotator>(num);
        for (int i = 0; i < num; i++) {
            annotators.add(new EcgAnnotator(is));
        }

        num = is.readInt(); // number of annotations
        annotations = new ArrayList<EcgAnnotation>(num);
        for (int i = 0; i < num; i++) {
            annotations.add(new EcgAnnotation(is, this, version));
        }
    }

    /**
     * @param index
     * @return the Annotator from an index.
     */
    public EcgAnnotator getAnnotator(int index) {
        if (index == -1)
            return EcgAnnotator.UNKNOWN;

        return annotators.get(index);
    }

    /**
     * Retrieves the index of an Annotator stored in this manager. If the given Annotator is not known yet it will be
     * stored in this manager.
     *
     * @param a
     * @return the index of an Annotator
     */
    public int getAnnotatorIndex(EcgAnnotator a) {
        if (a == EcgAnnotator.UNKNOWN)
            return -1;

        for (int i = 0; i < annotators.size(); i++) {
            if (annotators.get(i) == a)
                return i;
        }

        // check for unknown annotator in the identifier
        if (a.getIdentifier().trim().isEmpty() || a.getIdentifier().trim() == EcgAnnotator.UNKNOWN.getIdentifier())
            return -1;

        annotators.add(a);
        return annotators.size() - 1;
    }

    /**
     * @param a
     * @return true if this Annotator is known to the manager, or false otherwise.
     */
    public boolean isAnnotatorKnown(EcgAnnotator a) {
        if (a == EcgAnnotator.UNKNOWN)
            return true;

        for (int i = 0; i < annotators.size(); i++) {
            if (annotators.get(i) == a)
                return true;
        }

        // check for unknown annotator in the identifier
        if (a.getIdentifier().trim().isEmpty() || a.getIdentifier().trim() == EcgAnnotator.UNKNOWN.getIdentifier())
            return true;

        return false;
    }

    /**
     * Writes the state of this manager to a binary file stream.
     *
     * @param os
     * @throws IOException
     */
    public void writeToBinaryFile(DataOutputStream os) throws IOException {
        os.writeInt(annotators.size());
        for (int i = 0; i < annotators.size(); i++) {
            annotators.get(i).writeToBinaryStream(os);
        }

        os.writeInt(annotations.size());
        for (int i = 0; i < annotations.size(); i++) {
            EcgAnnotation ann = annotations.get(i);
            ann.writeToBinaryStream(os, this);
        }
    }

    /**
     * Adds an annotation to this annotation manager and creates/adds the corresponding annotator if he doesn't exist
     * yet.
     *
     * @param ann
     */
    public void addAnnotation(EcgAnnotation ann) {
        annotations.add(ann);
        // implicitly add this annotator if he doesn't exist yet by requesting his index
        getAnnotatorIndex(ann.getAnnotator());
    }


    /**
     * @return the number of annotations.
     */
    public int size() {
        return annotations.size();
    }

    /**
     * @return the number of different annotators.
     */
    public int getAnnotatorsSize() {
        return annotators.size();
    }

    /**
     * Returns the annotation at the given index. Not to be confused with <code>getAnnotations</code>, which retrieves annotations based on signal locations!
     *
     * @param index
     * @return the annotation at the given index.
     */
    public EcgAnnotation getAnnotation(int index) {
        return annotations.get(index);
    }

    /**
     * Find all annotations of one specific type.
     *
     * @param ofType Type of annotation to search for.
     * @return a list of annotations of the given type.
     */
    public ArrayList<EcgAnnotation> getAnnotations(AnnotationType ofType) {
        ArrayList<EcgAnnotation> list = new ArrayList<EcgAnnotation>();

        for (Iterator<EcgAnnotation> an = annotations.iterator(); an.hasNext(); ) {
            EcgAnnotation ecgAnnotation = an.next();
            if (ecgAnnotation.getType() == ofType)
                list.add(ecgAnnotation);
        }
        return list;
    }

    /**
     * Retrieve all annotations at a given sample index.
     *
     * @param sampleIndex
     * @return a list of all annotations that are valid at the given sample index.
     */
    public ArrayList<EcgAnnotation> getAnnotations(int sampleIndex) {
        ArrayList<EcgAnnotation> list = new ArrayList<EcgAnnotation>();

        for (Iterator<EcgAnnotation> anIter = annotations.iterator(); anIter.hasNext(); ) {
            EcgAnnotation an = anIter.next();
            // check if this annotation is at the given sample, or the given sampleIndex is within the range of a windowed annotation.
            if (an.getSampleIndex() == sampleIndex ||
                    (an.getSampleIndex() < sampleIndex && an.getSampleIndexEnd() >= sampleIndex)) {
                list.add(an);
            }
        }
        return list;
    }


    /**
     * Finds an annotation key = value pair and returns the value.
     *
     * @param key
     * @return
     */
    public String findAnnotationValue(String key) {
        // TODO
        return null;
    }

}
