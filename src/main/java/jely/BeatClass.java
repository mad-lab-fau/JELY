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
package jely;

/**
 * A classification for a whole heartbeat.
 *
 * @author Stefan Gradl
 */
public class BeatClass {
    public static BeatClass NORMAL = new BeatClass(null, QrsClass.NORMAL);

    private Heartbeat mBeat;
    private QrsClass mQrsClass;
    private boolean mIsAbnormal = false;
    private String mExplanation = "n/a";

    public BeatClass(Heartbeat beat, QrsClass qrsClass) {
        mBeat = beat;
        this.mQrsClass = qrsClass;
        if (qrsClass == QrsClass.ABNORMAL)
            mIsAbnormal = true;
    }

    public BeatClass(Heartbeat beat, QrsClass qrsClass, String explanation) {
        this(beat, qrsClass);
        this.mExplanation = explanation;
    }

    /**
     * @return the qrsClass
     */
    public QrsClass getQrsClass() {
        return mQrsClass;
    }

    /**
     * @return the isAbnormal
     */
    public boolean isAbnormal() {
        return mIsAbnormal;
    }

    /**
     * @return the explanation
     */
    public String getExplanation() {
        return mExplanation;
    }

    public Heartbeat getBeat() {
        return mBeat;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (mIsAbnormal ? "ABNORMAL" : "Normal") + " beat --> " + mExplanation;
    }


}
