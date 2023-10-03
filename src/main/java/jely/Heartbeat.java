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
 * A heartbeat, comprised of P wave, QRS complex and T wave.
 * 
 * @author Stefan Gradl
 *
 */
public class Heartbeat {
	private Heartbeat mPreviousBeat;
	private Heartbeat mNextBeat;

	private PWave mPWave;
	private QrsComplex mQrs;
	private TWave mTWave;

	/**
	 * Timestamp in milliseconds for this heartbeat centered at the R-peak.
	 */
	private long timestamp;

	public Heartbeat(PWave pWave, QrsComplex qrs, TWave tWave) {
		this(qrs);
		mPWave = pWave;
		mTWave = tWave;
	}

	public Heartbeat(QrsComplex qrs) {
		mQrs = qrs;
		mQrs.setHeartbeat(this);
		if (mQrs.getPreviousQrs() != null) {
			mPreviousBeat = mQrs.getPreviousQrs().getHeartbeat();
		}
	}

	public static Heartbeat getHeartbeat(QrsComplex qrs) {
		if (qrs.getHeartbeat() != null)
			return qrs.getHeartbeat();
		return new Heartbeat(qrs);
	}

	/**
	 * @return the previousBeat
	 */
	public Heartbeat getPreviousBeat() {
		return mPreviousBeat;
	}

	/**
	 * @param previousBeat the previousBeat to set
	 */
	public void setPreviousBeat(Heartbeat previousBeat) {
		mPreviousBeat = previousBeat;
	}

	/**
	 * @return the nextBeat
	 */
	public Heartbeat getNextBeat() {
		return mNextBeat;
	}

	/**
	 * @param nextBeat the nextBeat to set
	 */
	public void setNextBeat(Heartbeat nextBeat) {
		mNextBeat = nextBeat;
	}

	/**
	 * @return the pWave
	 */
	public PWave getPWave() {
		return mPWave;
	}

	/**
	 * @return the qrs
	 */
	public QrsComplex getQrs() {
		return mQrs;
	}

	/**
	 * @return the tWave
	 */
	public TWave getTWave() {
		return mTWave;
	}

	/**
	 * @param pWave the pWave to set
	 */
	public void setPWave(PWave pWave) {
		mPWave = pWave;
	}

	/**
	 * @param qrs the qrs to set
	 */
	public void setQrs(QrsComplex qrs) {
		mQrs = qrs;
	}

	/**
	 * @param tWave the tWave to set
	 */
	public void setTWave(TWave tWave) {
		mTWave = tWave;
	}

	/**
	 * 
	 * @return PQ time in seconds
	 */
	public double getPQTime() {
		if (mPWave == null)
			return -1;

		// this is just an approximation, actual PQ time is beginning of P and Q
		int pq = mQrs.getRPosition() - mPWave.getPeakPosition();
		return pq / mQrs.getSamplingRate();
	}

	/**
	 * 
	 * @return the heart rate in bpm based on the last RR-interval.
	 */
	public double getHeartRate() {
		if (mPreviousBeat == null || mPreviousBeat.getQrs() == null)
			return 0;

		int rr = mQrs.getRPosition() - mPreviousBeat.getQrs().getRPosition();
		return 60 / (rr / mQrs.getSamplingRate());
	}

}
