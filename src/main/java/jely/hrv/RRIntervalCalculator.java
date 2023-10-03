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
package jely.hrv;


import jely.Ecg;
import jely.Heartbeat;
import jely.RRInterval;
import jely.detectors.HeartbeatDetector;

import java.util.ArrayList;

/**
 * @author Axel Heinrich
 */
public class RRIntervalCalculator {

    Ecg mEcg;
    double samplingRate;
    HeartbeatDetector.HeartbeatDetectionListener mHeartbeatListener;
    ArrayList<Heartbeat> beatlist;
    ArrayList<RRInterval> rrIntervalList;

    public RRIntervalCalculator() {
    }

    // Constructor to calculate RRIntervals from ecg signal
    public RRIntervalCalculator(Ecg ecg) {

        mEcg = ecg;
        samplingRate = ecg.getSamplingRate();
        HeartbeatDetector heartbeatDetector = new HeartbeatDetector(mEcg, mHeartbeatListener);
        beatlist = heartbeatDetector.findHeartbeats();
        rrIntervalList = new ArrayList<RRInterval>(beatlist.size() - 1);

        extractRRIFromBeatList();
    }

    // Constructor to calculate RRIntervals from given beatlist
    public RRIntervalCalculator(ArrayList<Heartbeat> beatlist) {
        this.beatlist = beatlist;
        rrIntervalList = new ArrayList<RRInterval>(beatlist.size() - 1);

        extractRRIFromBeatList();
    }

    private void extractRRIFromBeatList() {
        RRInterval prevRRI = null;

        // create List with RRIntervals with a pointer to the previous and the next RRInterval
        for (int i = 1; i < beatlist.size(); i++) {
            Heartbeat heartbeat = beatlist.get(i);
            RRInterval currRRI = new RRInterval(heartbeat);
            if (i > 1) {
                prevRRI.setNextRRInterval(currRRI);
                currRRI.setPreviousRRInterval(prevRRI);
                rrIntervalList.add(prevRRI);
            }

            prevRRI = currRRI;
        }
    }

    public ArrayList<RRInterval> getRRList() {
        return rrIntervalList;
    }

    public int getListSize() {
        return rrIntervalList.size();
    }

    public ArrayList<Heartbeat> getBeatlist() {
        return beatlist;
    }
}
