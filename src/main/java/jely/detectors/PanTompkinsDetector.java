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

import de.fau.shiftlist.ShiftListDouble;
import de.fau.shiftlist.ShiftListObject;
import jely.Ecg;
import jely.EcgSignal;
import jely.QrsComplex;
import jely.filter.LmeFilter;
import jely.filter.MeanFilter;
import jely.util.DescriptiveStatistics;

public class PanTompkinsDetector extends QrsDetector {

	/** total group delay of the entire filter pipeline */
	public static final int TOTAL_DELAY = 24;
	/** group delay of the filter steps after the bandpassing */
	public int intDelay = 12;

	public MeanFilter mean = null;

	/** LOW-PASS filter */
	public static final double lp_a[] = { 1d, 2d, -1d };
	public static final double lp_b[] = { 0.03125d, 0, 0, 0, 0, 0, -0.0625d, 0, 0, 0, 0, 0, 0.03125d };
	public LmeFilter lowpass = new LmeFilter(lp_b, lp_a);

	/** HIGH-PASS filter */
	public static final double hp_a[] = { 1d, 1d };
	public static final double hp_b[] = { -0.03125d, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1d, -1d, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.03125d };
	public LmeFilter highpass = new LmeFilter(hp_b, hp_a);

	/** DIFF filter */
	public static final double diff_a[] = { 8d };
	public static final double diff_b[] = { 2d, 1d, 0d, -1d, -2d };
	public LmeFilter diff = new LmeFilter(diff_b, diff_a);

	public LmeFilter.WndIntFilter wndInt = null;
	public MeanFilter wndMean = null;

	public double qrsThreshold = 1d;

	public double samplingRate = 0;
	/** sampling time in ms */
	public double samplingTime = 5d;

	public int maxQrsSize = 1;
	/** length of the integrator window */
	public int wndLength = 1;

	/** amount of samples to copy pre R */
	public int preSegment = 1;
	/** samples to copy post R */
	public int postSegment = 1;
	public int rPassNum = 0;

	public static boolean learning = true;

	public StepHistory bandOut = null;
	public StepHistory intOut = null;

	// public boolean invalidQrs = false;

	public LmeFilter.StatFilter heartRateStats = new LmeFilter.StatFilter(3);
	public LmeFilter.StatFilter qrstaStats = new LmeFilter.StatFilter(3);

	public MeanFilter rrMeanLong = new MeanFilter(16);
	public LmeFilter.StatFilter rrStats = new LmeFilter.StatFilter(8);

	public LmeFilter.PeakDetectionFilter risingPeak = new LmeFilter.PeakDetectionFilter(3, 0);
	public LmeFilter.PeakDetectionFilter rPeak = new LmeFilter.PeakDetectionFilter(1, 0);
	public int lastBandPeak = 0;
	public int lastCrossing = 0;

	public LmeFilter.MinDetectionFilter qPeak = new LmeFilter.MinDetectionFilter(1, 0);
	// public PeakDetectionFilter sbRPeak = new PeakDetectionFilter( 1, 0 );
	public LmeFilter.MinDetectionFilter sPeak = new LmeFilter.MinDetectionFilter(1, 0);

	public int startProcessing = 100;

	public int beatCounter = 0;
	/** Time passed since last beat */
	public int lastBeat = 0;

	public double wndIntCompensation = 0.85d;

	/** the last 8 qrs complexes */
	public ShiftListObject qrsHistory = new ShiftListObject(8);

	public QRS qrsRefTemp = null;
	public QRS qrsRefTemp2 = null;

	public static class StepHistory {
		public ShiftListDouble history = null;
		public double peakOverall = 0f;
		public MeanFilter peakAverage = new MeanFilter(8);
		public double peakSignal = 0f;
		public double peakNoise = 0f;
		public double threshold1 = 0f;
		public double threshold2 = 0f;

		public double min = Float.MAX_VALUE;
		public double max = Float.MIN_VALUE;
		public double range = 0f;

		public StepHistory(int size) {
			history = new ShiftListDouble(size);
		}

		public void add(double value) {
			// check for max/min
			if (value > max) {
				max = value;
				peakOverall = max;
				range = Math.abs(max) - Math.abs(min);
			} else if (value < min) {
				min = value;
				range = Math.abs(max) - Math.abs(min);
			}

			history.add(value);

			// peakFinder.next( value );
			// if (peakFinder.peakIdx != -1)
			// {
			// // new peak found
			//
			// // adaptive thresholding
			// peakSignal = 0.125f * peakOverall + 0.875f * peakSignal;
			// peakNoise = 0.125f * peakOverall + 0.875f * peakNoise;
			//
			// threshold1 = peakNoise + 0.25f * (peakSignal - peakNoise);
			// threshold2 = 0.5f * threshold1;
			//
		}

		/**
		 * @return the current threshold for an assumed R-peak
		 */
		public double threshold() {
			return min + range * 0.15f;
		}
	}

	/**
	 * Represents a detected QRS complex
	 * 
	 * @author sistgrad
	 * 
	 */
	/**
	 * @author Falling
	 * 
	 */
	public static class QRS {
		public enum SegmentationStatus {
			INVALID, THESHOLD_CROSSED, R_FOUND, FINISHED, PROCESSED
		}

		public enum QrsClass {
			/** Rejection class */
			UNKOWN,
			/** No valid QRS complex, probably detection failure */
			INVALID,
			/** Normal QRS morphology */
			NORMAL,
			/** Premature Ventricular Contraction recognized in QRS (cc small) */
			PVC,
			/** PVC like aberrant beat (cc very small) */
			PVC_ABERRANT,
			/** Bundle branch block (q->s > 130ms) */
			BB_BLOCK,
			/** Various escape beats (> 600ms no QRS) */
			ESCAPE,
			/** Atrial premature complexes/beats */
			APC,
			/** Aberrated atrial premature beats */
			APC_ABERRANT,
			/** Various premature beats, unspecified, possibly junctional premature */
			PREMATURE,
			/**
			 * waveform differs significantly, potentially a/v flutter or fibrillation (cc <
			 * 0.4)
			 */
			ABERRANT,
			/** Virtual beats, inserted for missed beats */
			VIRTUAL
		}

		public enum QrsArrhythmia {
			/** no arrhythmia, normal pace */
			NONE,
			/**
			 * Very likely a normal beat, shows several deviations, but not enough to
			 * classify as ectopic
			 */
			ARTIFACT,
			/** Fusion of two beats (rr < 0.65 * prev && no APC) */
			FUSION,
			/**
			 * atrioventricular block, generic (rr > 1.6 * prev => ESCAPE if nothing else)
			 */
			AV_BLOCK,
			/** Heart rate > 130 bpm */
			TACHYCARDIA,
			/** Heart rate < 40 bpm */
			BRADYCARDIA,
			/** a/v flutter or fibrillation (ABERRANT && rr <<) */
			FIBRILLATION,
			/** Heart failure */
			CARDIAC_ARREST
		}

		/** Segmentation state of this QRS */
		public SegmentationStatus segState = SegmentationStatus.INVALID;

		/** Timestamp of the R-deflection in milliseconds */
		public long rTimestamp = -1;

		public double qAmplitude;
		public int qIdx = -1;
		public double rAmplitude;
		public int rIdx = -1;
		public double sAmplitude;
		public int sIdx = -1;

		/** mean value of all values */
		public double mean = 0;

		/** QRS width in milliseconds */
		public long feat_width;
		/** R-R interval to last QRS in milliseconds */
		public long feat_rr = 0;

		/** QRA amplitude, currently not used */
		public double feat_qra;
		public double feat_rsa;

		/** area total */
		public double feat_qrsta;

		/** CC normalized to templates */
		public double feat_cct1;
		public double feat_cct2;

		/** ArDiff to templates */
		public double feat_arT1diff;
		public double feat_arT2diff;

		public QrsClass classification = QrsClass.INVALID;
		public QrsArrhythmia arrhythmia = QrsArrhythmia.NONE;

		/** qrs from filtered signal */
		public ShiftListDouble values = null;

		/** Static reference to the template slot 1 */
		public static QRS template1 = null;
		/** Static reference to the template slot 2 */
		public static QRS template2 = null;
		/** Static reference to the current QRS */
		public static QRS qrsCurrent = null;
		/** Static reference to the previous QRS */
		public static QRS qrsPrevious = null;

		/**
		 * 
		 */
		public QRS(int size) {
			values = new ShiftListDouble(size);
		}

		public void copy(QRS source) {
			segState = source.segState;
			rTimestamp = source.rTimestamp;
			qAmplitude = source.qAmplitude;
			qIdx = source.qIdx;
			rAmplitude = source.rAmplitude;
			rIdx = source.rIdx;
			sAmplitude = source.sAmplitude;
			sIdx = source.sIdx;
			mean = source.mean;
			feat_width = source.feat_width;
			feat_qra = source.feat_qra;
			feat_rsa = source.feat_rsa;
			feat_qrsta = source.feat_qrsta;
			feat_cct1 = source.feat_cct1;
			feat_cct2 = source.feat_cct2;
			feat_rr = source.feat_rr;
			classification = source.classification;
			arrhythmia = source.arrhythmia;
			values.addAll(source.values);
		}

		public void reset() {
			values.clear();
			rTimestamp = -1;
			qIdx = rIdx = sIdx = -1;
			mean = 0;
			feat_cct1 = feat_cct2 = feat_qra = feat_qrsta = feat_rsa = 0;
			feat_width = feat_rr = 0;
			segState = SegmentationStatus.INVALID;
			classification = QrsClass.INVALID;
			arrhythmia = QrsArrhythmia.NONE;
		}

		/**
		 * Estimates the timestamps of a missed/virtual beat
		 */
		public void estimateMissedTimestamps() {
			if (feat_rr < 1 || feat_rr > 6000)
				return;

			feat_rr /= 2;
			rTimestamp -= feat_rr;
		}

		private transient int _i;
		private transient double _x, _y, _sumx, _sumy;
		
		/**
		 * @param template
		 *            Specific QRS-template to use for correlation classification
		 * @return the class most likely to fit this QRS
		 */
		public QrsClass classify ()
		{
			if (rIdx == -1)
			{
				// no R peak found
				classification = QrsClass.INVALID;
			}
			else
			{
				// current rr-time
				feat_rr = rTimestamp - qrsPrevious.rTimestamp;

				feat_qra = rAmplitude - qAmplitude;
				feat_rsa = rAmplitude - sAmplitude;

				mean = DescriptiveStatistics.mean(values);

				// CC feature variables
				feat_qrsta = _sumx = _sumy = 0;

				// calculate qrsta
				for (_i = 0; _i < values.size(); ++_i)
				{
					if (values.get(_i) > 0)
						feat_qrsta += values.get(_i);
					else
						feat_qrsta -= values.get(_i);
				}

				// check for templates
				if (template1.classification == QrsClass.INVALID || template2.classification == QrsClass.INVALID)
				{
					// no templates yet, unknown and return
					classification = QrsClass.UNKOWN;
					return classification;
				}


				// calculate correlation to templates
				feat_cct1 = maxCorr( template1 );
				feat_cct2 = maxCorr( template2 );

				feat_arT1diff = arDiff( template1 );
				feat_arT2diff = arDiff( template2 );


				// normal QRS duration is 60-120 ms
				if (feat_width > 130)
				{
					// possibly bundle branch block
					classification = QrsClass.BB_BLOCK;
				}
				else if (feat_width < 45)
				{
					classification = QrsClass.PVC;
				}
				else
				{
					classification = QrsClass.NORMAL;
				}


				// template matchings

				// template CC tests
				if (feat_cct1 < 0.2d || feat_cct2 < 0.2d)
					arrhythmia = QrsArrhythmia.ARTIFACT;


				if (feat_cct1 < 0.3d || feat_cct2 < 0.3d)
				{
					classification = QrsClass.ABERRANT;
				}

				else if (feat_cct1 < 0.6d || feat_cct2 < 0.6d)
				{
					classification = QrsClass.PVC_ABERRANT;
				}

				else if (feat_cct1 < 0.9d && feat_cct2 < 0.9d)
					classification = QrsClass.PVC;

				else if (feat_cct1 < 0.98d && feat_cct2 < 0.98d)
				{
					if (feat_arT1diff > 0.7d || feat_arT2diff > 0.7d)
					{
						classification = QrsClass.ABERRANT;
					}
					else if (feat_arT1diff > 0.5d || feat_arT2diff > 0.5d)
					{
						classification = QrsClass.PVC_ABERRANT;
					}
					else if (feat_arT1diff > 0.2d && feat_arT2diff > 0.2d)
					{
						classification = QrsClass.PVC;
					}
					else
					{

					}
				}


				// RR tests

				// -|----|-----------|--
				if ( (feat_rr >= qrsPrevious.feat_rr * 1.5d && feat_rr > 800) || feat_rr > 1700)
				{
					arrhythmia = QrsArrhythmia.AV_BLOCK;

					// escape beat
					if (classification == QrsClass.NORMAL)
						classification = QrsClass.APC;
				}

				// -|-|--
				else if (feat_rr > 1 && feat_rr < 460)
				{
					// premature & fusion types

					if (feat_rr > qrsPrevious.feat_rr * 0.92f)
					{
						// could be "normal" heart rate change
						if (classification == QrsClass.NORMAL && (feat_cct1 < 0.96d || feat_cct2 < 0.96d))
							classification = QrsClass.APC;
					}
					else
					{
						arrhythmia = QrsArrhythmia.FUSION;
					}

					if (feat_rr < 400)
					{
						if (feat_cct1 < 0.6d || feat_cct2 < 0.6d)
							classification = QrsClass.APC_ABERRANT;
						else
							classification = QrsClass.APC;
					}

					else
					{

					}
				}

				// -|-------------|----|--
				else if (qrsPrevious.feat_rr > 800 && feat_rr < qrsPrevious.feat_rr * 0.6f)
				{
					classification = QrsClass.ESCAPE;
				}

				else if (classification == QrsClass.NORMAL && feat_width > 10 && feat_width < qrsPrevious.feat_width * 0.6f
						&& (feat_arT1diff > 0.1 || feat_arT2diff > 0.1))
				{
					classification = QrsClass.PREMATURE;
				}


				/*Log.d(	"s",
						"   " + classification
								+ String.format( " [%.3f %.3f] [%.3f %.3f]", feat_cct1, feat_cct2, feat_arT1diff, feat_arT2diff )
								+ String.format( " [%d %d  %.3f] ", feat_width, feat_rr, feat_qrsta ) );*/

			}
			return classification;
		}

		private transient double _cc, _maxcc;
		private transient int _n;

		public double maxCorr(QRS other) {
			_maxcc = 0d;

			for (_n = -8; _n < 8; ++_n) {
				_x = _y = _sumx = _sumy = _cc = 0d;

				for (_i = 0; _i < values.getFilledSize(); ++_i) {
					_x = (values.get(_i) - mean);
					_y = (other.values.get(_n + _i) - other.mean);

					_cc += _x * _y;

					_sumx += _x * _x;
					_sumy += _y * _y;
				}

				if (_cc != 0) {
					_cc = _cc / (Math.sqrt(_sumx * _sumy));
					if (_cc > _maxcc)
						_maxcc = _cc;
				}
			}

			return _maxcc;
		}

		public double arDiff(QRS other) {
			if (other.feat_qrsta == 0d)
				return 0d;

			if (feat_qrsta > other.feat_qrsta)
				return (feat_qrsta - other.feat_qrsta) / other.feat_qrsta;

			return (other.feat_qrsta - feat_qrsta) / other.feat_qrsta;
		}
	}

	public PanTompkinsDetector(Ecg ecg) {
		super(ecg);

		samplingRate = this.mEcg.getSamplingRate();
		samplingTime = 1000f / samplingRate;

		wndLength = (int) (150d * samplingRate / 1000d);

		preSegment = (int) (120d * samplingRate / 1000d);
		postSegment = (int) (280d * samplingRate / 1000d);

		// buffer for historic values, MUST be > wndLength + filterDelay
		maxQrsSize = preSegment + postSegment;
		if (maxQrsSize < wndLength + TOTAL_DELAY + 2)
			maxQrsSize = wndLength + TOTAL_DELAY + 2;

		mean = new MeanFilter((int) (350d * samplingRate / 1000d));

		// window integrator width proposed by Pan&Tompkins, 150ms, that is 30 samples @
		// 200 Hz
		wndInt = new LmeFilter.WndIntFilter(wndLength);

		// Log.d( "lme.pants", "sampling: " + samplingRateInHz + " wndLength: " +
		// wndLength );

		// mean of the wnd integrator output over 150 ms
		wndMean = new MeanFilter(maxQrsSize);

		bandOut = new StepHistory(maxQrsSize);
		intOut = new StepHistory(maxQrsSize);

		// init qrs history
		for (int i = 0; i < qrsHistory.getMaxSize(); ++i) {
			qrsHistory.add(new QRS(maxQrsSize));
		}

		QRS.template1 = new QRS(maxQrsSize);
		QRS.template2 = new QRS(maxQrsSize);

		QRS.qrsCurrent = new QRS(maxQrsSize);
		qrsHistory.add(QRS.qrsCurrent);
		//QRS.qrsCurrent = (QRS) qrsHistory.getHeadValue();
		QRS.qrsCurrent.reset();

		QRS.qrsPrevious = null;

		// start processing after 2 seconds
		startProcessing = (int) (samplingRate * 2);

		// x = new double[ 16 ];
		y = new double[12];

		beatCounter = 0;
		learning = true;
	}

	private transient int _i;
	private double y[] = null;

	@Override
	public QrsComplex next(int sampleIndex) {

		double xnow = 0;
		QrsComplex currentQrsComplex = null;
		EcgSignal signal = this.mEcg.getSignal(0);
		if (sampleIndex >= 0) {
            xnow = signal.get(sampleIndex);
        } else {
            xnow = signal.getSignal().getHeadValue();
        }

		// cancel dc component
		// y[ 1 ] = xnow - mean.next( xnow );
		y[1] = xnow;

		// runtimeCounter += samplingTime;

		// LOW PASS (5 samples delay)
		y[2] = lowpass.next(y[1]);

		// HIGH PASS (16 samples delay)
		y[3] = highpass.next(y[2]);

		// save original ecg after bandpass filtering
		bandOut.add(y[3]);

		// Log.d( "lme.pants", "band " + y[ 3 ] );

		// Log.d( "pants", "event " + xnow );

		// DIFFERENTIATOR (2 samples delay)
		y[4] = diff.next(y[3]);

		// SQUARING
		y[5] = y[4] * y[4];

		// hard limit to 255
		// if (y[ 5 ] > 1024)
		// y[ 5 ] = 1024;

		// WND INTEGRATOR
		y[6] = wndInt.next(y[5]);

		// save value in history
		intOut.add(y[6]);

		// wndOut-mean
		y[7] = wndMean.next(y[6]);

		// Log.d( "sd", " " + y[ 1 ] + " " + y[ 3 ] + " " + y[ 4 ] + " " + y[ 6 ] + " "
		// + y[ 7 ] );

		// all further processing is only done after an initial timeout, is only used to
		// handle display issues with the
		// plot views
		if (startProcessing <= 0) {
			// check for potential cardiac arrest
			/*if (lastBeat > 3500) {
				QRS.qrsCurrent.rIdx = 0;
				QRS.qrsCurrent.rTimestamp = timestamp;
				QRS.qrsCurrent.rAmplitude = y[3];
				QRS.qrsCurrent.classification = QrsClass.VIRTUAL;
				QRS.qrsCurrent.arrhythmia = QrsArrhythmia.CARDIAC_ARREST;
				QRS.qrsCurrent.feat_width = lastBeat;
				QRS.qrsCurrent.segState = SegmentationStatus.FINISHED;
				return y[6];
			}*/

			// threshold
			// if (lastCrossing != 0)
			// qrsThreshold = y[ 7 ] * (1 / lastCrossing);
			// else
			qrsThreshold = y[7];

			// is intOut or bandOut above threshold?
			if (y[3] > qrsThreshold || y[6] > qrsThreshold || QRS.qrsCurrent.segState == QRS.SegmentationStatus.R_FOUND) {
				++lastCrossing;

				if (QRS.qrsCurrent.segState == QRS.SegmentationStatus.INVALID) {
					// initialize R peak detector
					rPeak.reset();
					//rPeak.next(bandOut.history.getPastValue(2));
					rPeak.next(bandOut.history.get(-2));
					rPeak.next(bandOut.history.get(-1));
					rPeak.next(y[3]);

					lastCrossing = 0;

					QRS.qrsCurrent.segState = QRS.SegmentationStatus.THESHOLD_CROSSED;
				}

				if (QRS.qrsCurrent.segState == QRS.SegmentationStatus.THESHOLD_CROSSED) {
					if (lastCrossing > preSegment && QRS.template2.classification == QRS.QrsClass.NORMAL) {
						// if lastCrossing is larger than preSegment samples but no R peak was found it
						// probably was an
						// aberrant beat
						// it is only considered if we already have two template beats
						//Log.d("lme.pants", "abb beat " + lastCrossing);
						QRS.qrsCurrent.rIdx = 0;
						//QRS.qrsCurrent.rTimestamp = timestamp;
						QRS.qrsCurrent.rAmplitude = y[3];
						QRS.qrsCurrent.classification = QRS.QrsClass.ABERRANT;
						QRS.qrsCurrent.arrhythmia = QRS.QrsArrhythmia.ARTIFACT;
						QRS.qrsCurrent.feat_width = lastCrossing;
						QRS.qrsCurrent.segState = QRS.SegmentationStatus.FINISHED;
						// qrsCurrent.feat_rr = 1;
					}
				}
			} else {
				// below all thresholds
				if (lastCrossing > 0)
					--lastCrossing;

				if (QRS.qrsCurrent.segState == QRS.SegmentationStatus.PROCESSED) {
					// QRS was processed, reset
					//QRS.qrsCurrent = (QRS) qrsHistory.next();
					QRS.qrsCurrent = new QRS(maxQrsSize);
					qrsHistory.add(QRS.qrsCurrent);
					QRS.qrsCurrent.reset();
				}
			}

			// check for mean crossing
			if (QRS.qrsCurrent.segState == QRS.SegmentationStatus.THESHOLD_CROSSED) {
				// R peak detector
				rPeak.next(y[3]);

				// rising peak detector
				risingPeak.reset();
				risingPeak.next(y[6]);

				qPeak.reset();
				sPeak.reset();

				// find peak in bandOut
				if (rPeak.peakIdx != -1) {
					// R peak found, check if intOut is above threshold
					if (y[6] < qrsThreshold) {
						if (lastCrossing > 0) {
							rPeak.reset();
							QRS.qrsCurrent.segState = QRS.SegmentationStatus.THESHOLD_CROSSED;
							lastCrossing = (int) (-1000 * samplingTime);
							//return y[6];
							return null;
						}
					}

					// reverse copy all qrs values
					for (_i = 0; _i <= preSegment; ++_i) {
						// from bandfiltered signal
						//y[8] = bandOut.history.getPastValue(preSegment - _i);
						y[8] = bandOut.history.get(- preSegment + _i);

						// to current qrs object
						QRS.qrsCurrent.values.add(y[8]);

						// find Q only if it hasn't been found yet
						if (QRS.qrsCurrent.qIdx == -1) {
							// find q-min
							//qPeak.next(bandOut.history.getPastValue(_i));
							qPeak.next(bandOut.history.get(-_i));
							if (qPeak.peakIdx != -1) {
								QRS.qrsCurrent.qAmplitude = qPeak.peakValue;
								QRS.qrsCurrent.qIdx = preSegment - _i;
							}
						}
					}

					// if no Q has been found, we use the first sample
					if (QRS.qrsCurrent.qIdx == -1) {
						//QRS.qrsCurrent.qAmplitude = QRS.qrsCurrent.values.values[0];
						QRS.qrsCurrent.qAmplitude = QRS.qrsCurrent.values.get(0);
						QRS.qrsCurrent.qIdx = 0;
					}

					// r peak in filtered signal
					//QRS.qrsCurrent.rIdx = QRS.qrsCurrent.values.head - rPeak.peakIdx;
					QRS.qrsCurrent.rIdx = QRS.qrsCurrent.values.getHeadIndex() - rPeak.peakIdx;
					QRS.qrsCurrent.rAmplitude = rPeak.peakValue;
					//QRS.qrsCurrent.rTimestamp = (long) (timestamp - rPeak.peakIdx * samplingTime);
					rPassNum = 1;

					// Log.d( "pants", "rtime " + qrsCurrent.rTimestamp );

					// check if the amplitudes are valid
					if (QRS.qrsCurrent.rAmplitude - QRS.qrsCurrent.qAmplitude < bandOut.range * 0.1) {
						//Log.d("lme.pants", "Amplitude validation error "
						//		+ (QRS.qrsCurrent.rAmplitude - QRS.qrsCurrent.qAmplitude));
						// probably misdetected
						QRS.qrsCurrent.reset();
					} else {
						// wait for S min
						lastBandPeak = 0;
						QRS.qrsCurrent.segState = QRS.SegmentationStatus.R_FOUND;

						// pre-initialize sPeak detector
						sPeak.next(y[3]);
					}
				}
			}

			// ==============================================
			// == R peak found... looking for S min
			// ====>
			else if (QRS.qrsCurrent.segState == QRS.SegmentationStatus.R_FOUND) {
				// R has been found, we wait for S min
				QRS.qrsCurrent.values.add(y[3]);

				// continue looking for rising peak
				if (rPassNum > 0) {
					++rPassNum;
					risingPeak.next(y[6]);
					if (risingPeak.peakIdx != -1) {
						// rising peak of integration window found
						// the length of the ridge equals the width of the QRS complex
						QRS.qrsCurrent.feat_width = (long) (rPassNum * wndIntCompensation * samplingTime);
						rPassNum = 0;
					}
				}

				++lastBandPeak;

				// find S
				if (QRS.qrsCurrent.sIdx == -1) {
					// find S as min
					sPeak.next(y[3]);
					if (sPeak.peakIdx != -1) {
						QRS.qrsCurrent.sAmplitude = sPeak.peakValue;
						QRS.qrsCurrent.sIdx = QRS.qrsCurrent.values.getHeadIndex() - sPeak.peakIdx;
					}
				}

				// check for max range
				if (lastBandPeak >= postSegment) {
					// ==============================================
					// == segmentation finished
					// ====>
					QRS.qrsCurrent.segState = QRS.SegmentationStatus.FINISHED;

					// if no S has been found, we use the last sample
					if (QRS.qrsCurrent.sIdx == -1) {
						QRS.qrsCurrent.sAmplitude = y[3];
						QRS.qrsCurrent.sIdx = QRS.qrsCurrent.values.getHeadIndex();
					}

					QRS.qrsPrevious = (QRS) qrsHistory.get(-1);

					// make sure that we have a width
					if (QRS.qrsCurrent.feat_width < 1) {
						// substitute width estimation
						QRS.qrsCurrent.feat_width = (long) ((QRS.qrsCurrent.sIdx - QRS.qrsCurrent.qIdx)
								* wndIntCompensation * samplingTime);
					}

					// find a template
					if (QRS.template1.classification == QRS.QrsClass.INVALID
							|| QRS.template2.classification == QRS.QrsClass.INVALID) {
						// no templates, wait for six beats
						++beatCounter;
						if (QRS.qrsCurrent.classify() == QRS.QrsClass.INVALID) {
							--beatCounter;
						}
						if (beatCounter == 6) {
							ArrayList<Integer> sortList = new ArrayList<Integer>(6);

							// 6 beats encountered, choose the templates
							double avg = 0d;
							for (int i = 0; i < 6; ++i) {
								qrsRefTemp = (QRS) qrsHistory.get(-i);
								avg += qrsRefTemp.feat_qrsta;
							}
							avg /= 6;

							sortList.add(0);

							// sort the indices in ascending order
							for (int i = 0; i < 6; ++i) {
								qrsRefTemp = (QRS) qrsHistory.get(-i);
								for (int n = 0; n < sortList.size(); ++n) {
									qrsRefTemp2 = (QRS) qrsHistory.get(-sortList.get(n));
									if (qrsRefTemp.feat_qrsta < avg && qrsRefTemp2.feat_qrsta < avg
											&& qrsRefTemp2.feat_qrsta > qrsRefTemp.feat_qrsta) {
										sortList.add(n, i);
										break;
									} else if (n == sortList.size() - 1) {
										sortList.add(i);
										break;
									}
								}
							}

							// select
							for (int i = 0; i < 3; ++i) {
								qrsRefTemp = (QRS) qrsHistory.get(-sortList.get(i));
								qrsRefTemp2 = (QRS) qrsHistory.get(-sortList.get(i + 1));
								if (qrsRefTemp.maxCorr(qrsRefTemp2) > 0.9) {
									// take those two as templates
									QRS.template1.copy(qrsRefTemp);
									QRS.template2.copy(qrsRefTemp2);
									QRS.template1.classification = QRS.QrsClass.NORMAL;
									QRS.template2.classification = QRS.QrsClass.NORMAL;
								}
							}

							// see if we have two templates
							if (QRS.template2.classification != QRS.QrsClass.NORMAL) {
								// no, only one template, so take the two smallest
								QRS.template1.copy((QRS) qrsHistory.get(-sortList.get(0)));
								QRS.template2.copy((QRS) qrsHistory.get(-sortList.get(1)));
								QRS.template1.classification = QRS.QrsClass.NORMAL;
								QRS.template2.classification = QRS.QrsClass.NORMAL;
							}

							// end learning time
							learning = false;
						}
					} else {

						// classify current QRS and only proceed if beat is not invalid
						if (QRS.qrsCurrent.classify() != QRS.QrsClass.INVALID) {
							// missed beat?
							if (QRS.qrsCurrent.classification == QRS.QrsClass.ESCAPE) {
								// insert copy of current beat between current and last beat
								QRS.qrsPrevious = QRS.qrsCurrent;
								//QRS.qrsCurrent = (QRS) qrsHistory.next();
								QRS.qrsCurrent = new QRS(maxQrsSize);
								qrsHistory.add(QRS.qrsCurrent);
								//QRS.qrsCurrent = (QRS) qrsHistory.getHeadValue();
								QRS.qrsCurrent.copy(QRS.qrsPrevious);

								QRS.qrsPrevious.classification = QRS.QrsClass.VIRTUAL;

								// estimate the timestamps of the inserted (missed/virtual) beat
								QRS.qrsPrevious.estimateMissedTimestamps();

								// reclassify the beat
								QRS.qrsCurrent.classify();
								// make sure it is not classified normal, since it certainly is the escape beat
								if (QRS.qrsCurrent.classification == QRS.QrsClass.NORMAL)
									QRS.qrsCurrent.classification = QRS.QrsClass.ESCAPE;

							} else if (QRS.qrsCurrent.classification == QRS.QrsClass.NORMAL) {
								if (QRS.qrsCurrent.feat_cct1 > QRS.qrsCurrent.feat_cct2) {
									// replace template 1
									QRS.template1.copy(QRS.qrsCurrent);
								} else {
									// replace template 2
									QRS.template2.copy(QRS.qrsCurrent);
								}
							}

							// calculate averages
							rrMeanLong.next(QRS.qrsCurrent.feat_rr);

							if (QRS.qrsCurrent.feat_rr > 180 && QRS.qrsCurrent.feat_rr < 4000) {
								rrStats.next(QRS.qrsCurrent.feat_rr);

								// calculate heart rate
								heartRateStats.next(60000 / rrStats.value);

								qrstaStats.next(QRS.qrsCurrent.feat_qrsta);
							}
							
							System.out.println("QRS found");
							currentQrsComplex = new QrsComplex(signal);
							currentQrsComplex.setRPeak(QRS.qrsCurrent.rIdx, QRS.qrsCurrent.rAmplitude);
						}
					}

					// <====
					// ==============================================
				}
			}
			// <====
			// ==============================================

		} else {
			--startProcessing;
		}

		// return y[ 6 ];
		return currentQrsComplex;
	}

}
