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
package de.fau.mad.jely.detectors;

import de.fau.mad.jely.Ecg;
import de.fau.mad.jely.EcgSignal;
import de.fau.mad.jely.QrsComplex;
import de.fau.mad.jely.TWave;

/**
 * The interface that all T wave detectors have to implement.
 *
 * @author Stefan Gradl
 */
public interface TWaveDetector {
    /**
     * Searches for the T wave for the given QRS complex in the given ECG.
     *
     * @param ecg        The ECG in which the QrsComplex was found and is present.
     * @param currentQrs The found QRS complex for which the T wave is desired.
     * @return a new instance of TWave, representing the T wave for the given QRS complex, or null if no T wave could be found.
     */
    TWave findTWave(Ecg ecg, QrsComplex currentQrs);
}
