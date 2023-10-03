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
package jely.detectors;

import java.util.ArrayList;

import jely.Ecg;
import jely.Ecglib;
import jely.LeadConfiguration;
import jely.QrsComplex;
import de.fau.shiftlist.*;
import jely.processors.PostProcessor;
import jely.processors.QrsDetectionPostProcessor;
import jely.processors.RPeakRefinement;

/**
 * Base class for all QRS detectors.
 *
 * @author Gradl
 */
public abstract class QrsDetector {
	protected Ecg mEcg;
	protected ArrayList<PostProcessor> mPostProcessors = new ArrayList<>();
	protected ShiftListObject mQrsList;
	protected boolean mDebugMode = false;

	/**
	 * Constructs a QRS detector sensitive to the given sampling rate and requiring
	 * signals from the given leads.
	 *
	 * @param samplingRate  the sampling rate in Hz for which this QRS detector
	 *                      should be sensitive to.
	 * @param requiredLeads specifies leads from which signals are needed for this
	 *                      detector to work.
	 */
	public QrsDetector(double samplingRate, LeadConfiguration requiredLeads) {
		this(new Ecg(samplingRate, requiredLeads));
	}

	public QrsDetector(Ecg ecg) {
		mEcg = ecg;
	}

	public void addPostProcessor(PostProcessor processor) {
		// TODO: search for a similar processor and replace it
		// processor.getClass().getSuperclass() ??
		mPostProcessors.add(processor);
	}

	/**
	 * Delivers the next signal values to the detector.
	 *
	 * @param sampleIndex the index of the most recently added signal sample in all
	 *                    EcgSignals required for this detector or -1 to use the
	 *                    last added sample.
	 * @return the detected QrsComplex, or null when no QrsComplex was detected
	 *         after processing the sampled signal values.
	 */
	public abstract QrsComplex next(int sampleIndex);

	/**
	 * Processes the next/latest signal value in the attached ECG.
	 *
	 * @return the detected QrsComplex, or null when no QrsComplex was detected
	 *         after processing the sampled signal values.
	 */
	public QrsComplex next() {
		return next(-1);
	}

	/**
	 * This is called every time a new QRS complex has been found by the detection
	 * algorithm.
	 *
	 * @param qrs the new QRS complex that was found.
	 */
	protected void onQrsComplexFound(QrsComplex qrs) {
		// TODO: maybe we should differentiate between found R-peaks and found
		// QrsComplexes. Some detectors only find
		// the R-peak and some, e.g. Pan-Tompkins, also infer the QRS-width from
		// their algorithms.
		qrs.findQrsWidth();

		if (Ecglib.isDebugMode())
			System.out.println("R-pos: " + qrs.getRPosition());
		
		mQrsList.add(qrs);

		// set the next qrs of the previous qrs to this qrs, if a previous one
		// is available
		if (mQrsList.size() > 1) {
			QrsComplex previousQrs = (QrsComplex) mQrsList.get(-1);
			qrs.setPreviousQrs(previousQrs);
			previousQrs.setNextQrs(qrs);
		}
		
		// apply all post-processing for QRS complexes, this is always done
		// for the previous QRS complex, so a
		// real-time recorded
		// signal contains enough samples for a thorough processing and each
		// QRS complex has a previous and next one
		// it can refer to.
		for (PostProcessor postProcessor : mPostProcessors) {
			if (postProcessor instanceof QrsDetectionPostProcessor) {
				int output;
				if (postProcessor instanceof RPeakRefinement)
					output = ((QrsDetectionPostProcessor) postProcessor).process(qrs);
				else
					output = ((QrsDetectionPostProcessor) postProcessor).process(qrs.getPreviousQrs());

				// write some debug output?
				if (postProcessor instanceof RPeakRefinement && Ecglib.isDebugMode())
					System.out.println("Refinement distance: " + output);
			}
		}
	}

	public void setDebugMode(boolean activate) {
		mDebugMode = activate;
	}

	/**
	 * Returns the most recently found QRS complex.
	 *
	 * @return the most recently found QRS complex, or <code>null</code> if no QRS
	 *         complex has been detected so far
	 */
	public QrsComplex getCurrentQrsComplex() {
		if (mQrsList != null && mQrsList.size() > 0) {
			return (QrsComplex) mQrsList.getHeadValue();
		}
		return null;
	}

	/**
	 * Runs this QrsDetector on the entire signal array and returns a list of all
	 * the found <code>QrsComplex</code>es. The signal is assumed to be sampled at
	 * the proper sampling rate given at construction time of this QrsDetector. The
	 * lead is assumed to be UNKNOWN. Use findQrsComplexes(EcgSignal) instead, if
	 * you need the lead information.
	 *
	 * @param signal an array of sampled amplitude values for an ECG.
	 * @return an ordered list of QrsComplexes found in the given signal by using
	 *         this QrsDetector.
	 */
	public ArrayList<QrsComplex> findQrsComplexes(double[] signal) {
		mEcg.init(signal.length);
		mEcg.getSignalFromIndex(0).addAll(new ShiftListDouble(signal));
		return findQrsComplexes();
	}

	public ArrayList<QrsComplex> findQrsComplexes() {
		ArrayList<QrsComplex> qrsList = new ArrayList<QrsComplex>();

		for (int i = 0; i < mEcg.getSignalFromIndex(0).size(); i++) {
			QrsComplex complex = this.next(i);
			if (complex != null)
				qrsList.add(complex);
		}

		return qrsList;
	}

	/**
	 * Runs this QrsDetector on the entire signal array and returns an array of
	 * detected R-peak positions.
	 *
	 * @param signal
	 * @return
	 */
	public int[] findRPeaks(double[] signal) {
		ArrayList<QrsComplex> qrsList = findQrsComplexes(signal);

		int[] rList = new int[qrsList.size()];
		for (int i = 0; i < rList.length; i++) {
			rList[i] = qrsList.get(i).getRPosition();
		}

		return rList;
	}

	public int[] findRPeaks() {
		ArrayList<QrsComplex> qrsList = findQrsComplexes();

		int[] rList = new int[qrsList.size()];
		for (int i = 0; i < rList.length; i++) {
			rList[i] = qrsList.get(i).getRPosition();
		}

		return rList;
	}

}
