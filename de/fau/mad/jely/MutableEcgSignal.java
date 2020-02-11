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

import de.fau.shiftlist.*;

/**
 * @author Stefan Gradl
 *
 */
public class MutableEcgSignal extends EcgSignal {

	/**
	 * @param lead
	 * @param parentEcg
	 * @param signal
	 */
	public MutableEcgSignal(EcgLead lead, Ecg parentEcg, ShiftListDouble signal) {
		super(lead, parentEcg, signal);
		unfreeze();
	}

	/**
	 * @param lead
	 * @param parentEcg
	 * @param signalCapacity
	 */
	public MutableEcgSignal(EcgLead lead, Ecg parentEcg, int signalCapacity) {
		super(lead, parentEcg, signalCapacity);
		unfreeze();
	}

	/**
	 * @param signal
	 */
	public MutableEcgSignal(EcgSignal signal) {
		super(signal);
		unfreeze();
	}

	/**
	 * @param signal
	 * @param from
	 * @param to
	 */
	public MutableEcgSignal(EcgSignal signal, int from, int to) {
		super(signal, from, to);
		unfreeze();
	}

}
