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

import de.fau.shiftlist.*;
import jely.Ecg;
import jely.Ecglib;
import jely.Heartbeat;
import jely.QrsComplex;

import java.util.ArrayList;

/**
 * The default heartbeat detector.
 *
 * @author Stefan Gradl
 */
public class HeartbeatDetector {
    QrsDetector mQrsDetector;
    PWaveDetector mPWaveDetector;
    TWaveDetector mTWaveDetector;
    Ecg mEcg;
    HeartbeatDetectionListener mHeartbeatListener;
    protected ShiftListObject mBeatList;

    /**
     * A listener for new detected heartbeats.
     *
     * @author gradl
     */
    public interface HeartbeatDetectionListener {
        /**
         * Gets called every time a new heartbeat is detected.
         *
         * @param heartbeat the detected heartbeat.
         */
        void onHeartbeatDetected(Heartbeat heartbeat);
    }

    /**
     * Default constructor.
     *
     * @param ecg
     * @param heartbeatListener
     */
    public HeartbeatDetector(Ecg ecg, HeartbeatDetectionListener heartbeatListener) {
        init(ecg, null, null, null, heartbeatListener);
    }
    
    /**
     * Constructor specifying the QRS detector to use.
     * 
     * @param ecg
     * @param qrsDetector
     */
    public HeartbeatDetector(Ecg ecg, QrsDetector qrsDetector) {
        init(ecg, qrsDetector, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param ecg
     * @param qrsDetector
     * @param pWaveDetector
     * @param tWaveDetector
     * @param heartbeatListener
     */
    public HeartbeatDetector(Ecg ecg, QrsDetector qrsDetector, PWaveDetector pWaveDetector, TWaveDetector tWaveDetector,
                             HeartbeatDetectionListener heartbeatListener) {
        init(ecg, qrsDetector, pWaveDetector, tWaveDetector, heartbeatListener);
    }

    private void init(Ecg ecg, QrsDetector qrsDetector, PWaveDetector pWaveDetector, TWaveDetector tWaveDetector,
                      HeartbeatDetectionListener heartbeatListener) {
        mEcg = ecg;

        if (qrsDetector != null) {
            mQrsDetector = qrsDetector;
        } else {
            mQrsDetector = new ElgendiFastQrsDetector(ecg);
        }

        if (pWaveDetector != null)
            mPWaveDetector = pWaveDetector;
        else
            mPWaveDetector = new NaivePtDetector(ecg);

        if (tWaveDetector != null)
            mTWaveDetector = tWaveDetector;
        else
            mTWaveDetector = new NaivePtDetector(ecg);

        mHeartbeatListener = heartbeatListener;
        mBeatList = new ShiftListObject(32);

        if (Ecglib.isDebugMode())
            System.out.println("HeatbeatDetector initiated " + mQrsDetector + "; " + mPWaveDetector + "; " + mTWaveDetector + "; " + mHeartbeatListener);
    }
    
    /**
     * 
     * @return The QRS detector used in this HeartbeatDetector.
     */
    public QrsDetector getQrsDetector() {
    	return mQrsDetector;
    }

    /**
     * Process the next sample from an ECG.
     *
     * @param sampleIndex
     */
    public void processNextSample(int sampleIndex) {
        QrsComplex qrs = mQrsDetector.next(sampleIndex);
        if (qrs != null) {
            if (mBeatList.size() > 0) {
                Heartbeat prevBeat = (Heartbeat) mBeatList.getHeadValue();

                prevBeat.setNextBeat(qrs.getHeartbeat());
                qrs.getHeartbeat().setPreviousBeat(prevBeat);

                findHeartbeat(prevBeat);

                // always the previous beat detection is reported so we have
                // enough signal samples in real-time mode for processing
                if (mHeartbeatListener != null)
                    mHeartbeatListener.onHeartbeatDetected(prevBeat);
            }

            mBeatList.add(qrs.getHeartbeat());
        }
    }

    /**
     * Finishes heartbeat detection for the given beat
     *
     * @param beat
     */
    private void findHeartbeat(Heartbeat beat) {
        if (mPWaveDetector != null)
            beat.setPWave(mPWaveDetector.findPWave(mEcg, beat.getQrs()));
        if (mTWaveDetector != null)
            beat.setTWave(mTWaveDetector.findTWave(mEcg, beat.getQrs()));
    }

    /**
     * Finds all heartbeats in the current ECG signal.
     * @return ArrayList of Heatbeats
     */
    public ArrayList<Heartbeat> findHeartbeats() {
        final ArrayList<Heartbeat> beatList = new ArrayList<Heartbeat>();

        HeartbeatDetectionListener oldListener = mHeartbeatListener;
        mHeartbeatListener = new HeartbeatDetectionListener() {
            @Override
            public void onHeartbeatDetected(Heartbeat heartbeat) {
                if (Ecglib.isDebugMode())
                    System.out.println("Detected heartbeat: " + heartbeat);

                beatList.add(heartbeat);
            }
        };

        for (int i = 0; i < mEcg.getSignalFromIndex(0).size(); i++) {
            processNextSample(i);
        }

        mHeartbeatListener = oldListener;

        return beatList;
    }
    
    /**
     * Convert an ArrayList of Heartbeats to an array of R-peak positions.
     * @param beatList
     * @return int-Array of R-peak indices.
     */
    public static int[] toPeakArray(ArrayList<Heartbeat> beatList) {
    	int[] array = new int [beatList.size()];
    	for (int i = 0; i < beatList.size(); i++) {
			Heartbeat heartbeat = beatList.get(i);
			QrsComplex qrs = heartbeat.getQrs();
			int rPeakIndex = qrs.getRPosition();
			array[i] = rPeakIndex;

		}
    	return array;
    }
}
