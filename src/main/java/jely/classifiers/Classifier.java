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
package jely.classifiers;

import jely.BeatClass;
import jely.Heartbeat;
import jely.QrsClass;
import jely.QrsComplex;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for a classifier.
 *
 * @author Stefan Gradl
 */
public interface Classifier {

    public BeatClass classify(Heartbeat beat);

    public BeatClass classify(QrsComplex qrs);

    public List<QrsClass> classify(List<QrsComplex> qrsComplexes);

    public List<QrsClass> classify(ArrayList<Heartbeat> beatList);

}
