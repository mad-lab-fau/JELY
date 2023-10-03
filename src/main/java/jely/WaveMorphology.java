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
 * Represents morphological informations about a characteristic signal wave.
 *
 * @author Stefan Gradl
 */
public class WaveMorphology {
    int mOnPosition;
    double mOnValue;

    int mPeakPosition;
    double mPeakValue;

    int mOffPosition;
    double mOffValue;

    /**
     * Creates an P wave
     */
    public WaveMorphology() {
        mOnPosition = -1;
        mPeakPosition = -1;
        mOffPosition = -1;

        mOnValue = Double.NaN;
        mPeakValue = Double.NaN;
        mOffValue = Double.NaN;
    }

    /**
     * Creates a wave with the specified position and the specified value of the onset, wave and offset
     *
     * @param onPosition  the position of the Onset in the entire ECG signal
     * @param position    the position of the P peak in the entire ECG signal
     * @param offPosition the position of the Offset in the entire ECG signal
     * @param onValue     the ECG value of the Onset
     * @param value       the ECG value of the P peak
     * @param offValue    the ECG value of the Offset
     */
    public WaveMorphology(int onPosition, double onValue, int position, double value, int offPosition, double offValue) {
        this.mOnPosition = onPosition;
        this.mPeakPosition = position;
        this.mOffPosition = offPosition;
        this.mOnValue = onValue;
        this.mPeakValue = value;
        this.mOffValue = offValue;
    }

    // Set position

    /**
     * Sets the position of the Onset
     *
     * @param onPosition the position of the Onset in the entire ECG signal
     */
    public void setOnsetPosition(int onPosition) {
        this.mOnPosition = onPosition;
    }

    /**
     * Sets the position of the P peak
     *
     * @param position the position of the P peak in the entire ECG signal
     */
    public void setPeakPosition(int position) {
        this.mPeakPosition = position;
    }

    /**
     * Sets the position of the Offset
     *
     * @param offPosition the position of the Offset in the entire ECG signal
     */
    public void setOffsetPosition(int offPosition) {
        this.mOffPosition = offPosition;
    }

    // Set value

    /**
     * Sets the value of the Onset
     *
     * @param onValue the ECG value of the Onset
     */
    public void setOnsetValue(double onValue) {
        this.mOnValue = onValue;
    }

    /**
     * Sets the value of the peak
     *
     * @param value the ECG value of the P peak
     */
    public void setPeakValue(double value) {
        this.mPeakValue = value;
    }

    /**
     * Sets the value of the Offset
     *
     * @param offValue the ECG value of the Offset
     */
    public void setOffsetValue(double offValue) {
        this.mOffValue = offValue;
    }

    // Get position

    /**
     * Returns the position of the Onset
     *
     * @return the position of the Onset in the entire ECG signal
     */
    public int getOnsetPosition() {
        return mOnPosition;
    }

    /**
     * Returns the position of the peak
     *
     * @return the position of the P peak in the entire ECG signal
     */
    public int getPeakPosition() {
        return mPeakPosition;
    }

    /**
     * Returns the position of the Offset
     *
     * @return the position of the Offset in the entire ECG signal
     */
    public int getOffsetPosition() {
        return mOffPosition;
    }

    // Get value

    /**
     * Return the value of the Onset
     *
     * @return the ECG value of the Onset
     */
    public double getOnsetValue() {
        return mOnValue;
    }

    /**
     * Return the value of the peak
     *
     * @return the ECG value of the P peak
     */
    public double getPeakValue() {
        return mPeakValue;
    }

    /**
     * Return the value of the Offset
     *
     * @return the ECG value of the Offset
     */
    public double getOffsetValue() {
        return mOffValue;
    }

    public double getWaveWidth() {
        return mOffPosition - mOnPosition;
    }
}
