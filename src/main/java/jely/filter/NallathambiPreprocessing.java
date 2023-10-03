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
package jely.filter;

/**
 * Performs the preprocessing of ECG signals described in [1].
 * <p>
 * [1] Nallathambi, G., & Principe, J. C. (2014). Integrate and fire pulse train automaton for QRS detection. IEEE
 * Transactions on Biomedical Engineering, 61(2), 317-326. http://doi.org/10.1109/TBME.2013.2282954
 *
 * @author Stefan Gradl
 */
public class NallathambiPreprocessing extends DigitalFilter {
    MedianFilter medFilt200;
    MedianFilter medFilt600;

    public NallathambiPreprocessing(double samplingRate) {
        medFilt200 = new MedianFilter((int) (samplingRate * 0.2));
        medFilt600 = new MedianFilter((int) (samplingRate * 0.6));
    }

    /*
     * (non-Javadoc)
     *
     * @see de.fau.mad.jely.filter.DigitalFilter#next(double)
     */
    @Override
    public double next(double xnow) {
        double y = medFilt200.next(xnow);
        y = medFilt600.next(y);
        return xnow - y;
    }

}
