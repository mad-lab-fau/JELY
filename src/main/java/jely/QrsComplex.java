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

import java.util.EnumSet;
import java.util.List;

import de.fau.shiftlist.ShiftListDouble;
import jely.util.DescriptiveStatistics;
import jely.util.DescriptiveStatisticsElement;

/**
 * Represents a QRS complex of a heartbeat in an ECG.
 *
 * @author Stefan Gradl
 */
public class QrsComplex extends HeartbeatWave {
    /**
     * The EcgSignal where this QrsComplex was detected in/derived from.
     */
    EcgSignal mSignal;

    /**
     * The QrsComplex that preceded this one.
     */
    private QrsComplex mPreviousQrs = null;
    /**
     * The QrsComplex that followed this one.
     */
    private QrsComplex mNextQrs = null;

    /**
     * Absolute (global) sample index from the beginning of the EcgSignal for the start of this QRS complex vicinity.
     */
    private int mSampleIndexStart = -1;
    /**
     * Absolute (global) sample index from the beginning of the EcgSignal for the end of this QRS complex vicinity.
     */
    private int mSampleIndexEnd = -1;
    /**
     * Sample buffer for the QRS complex vicinity. This is null, unless this QrsComplex was initialized with the
     * respective constructor!
     */
    private ShiftListDouble mSampleBuffer = null;

    /**
     * Statistical data about this QRS complex vicinity.
     */
    protected DescriptiveStatistics mStats = null;
    private double mQrstArea = 0;
    /**
     * The width (in samples) of the QRS complex (starting at the Q deflection and ending at the S deflection).
     */
    private int mQrsWidth = 0;

    /**
     * The sampling rate of the original signal.
     */
    private double mSamplingRate;

    // Q peak
    private int mQPosition;
    private double mQValue;
    // R peak
    private int mRPosition;
    private double mRValue;
    // S peak
    private int mSPosition;
    private double mSValue;

    /**
     * Assumed baseline amplitude value. Generated from the values found at the beginning and end of the QRS complex.
     */
    private double mBaselineValue;

    /**
     * Beginning of the (downward) deflection of the Q peak, required to determine the QRS width
     */
    private int mQDeflectionStart;
    /**
     * End of the (upward) deflection of the S peak, required to determine the QRS width
     */
    private int mSDeflectionEnd;

    /**
     * Constructor.
     *
     * @param samples
     * @param fromSignal
     * @param copyFeatures
     */
    public QrsComplex(List<Double> samples, EcgSignal fromSignal, boolean copyFeatures) {
        mSignal = fromSignal;
        mSampleBuffer = new ShiftListDouble(samples);
        mSamplingRate = fromSignal.getSamplingRate();
        setHeartbeat(new Heartbeat(this));
    }

    /**
     * Constructor.
     *
     * @param fromSignal
     * @param previousQrs
     */
    public QrsComplex(EcgSignal fromSignal) {
        mSignal = fromSignal;
        this.mSamplingRate = fromSignal.getSamplingRate();

        // init Q peak
        mQPosition = -1;
        mQValue = Double.NaN;
        // init R peak
        mRPosition = -1;
        mRValue = Double.NaN;
        // init S peak
        mSPosition = -1;
        mSValue = Double.NaN;

        setHeartbeat(new Heartbeat(this));
    }

    /**
     * Creates a new QrsComplex copying this one using the fixed sample buffer. Should be used to provide permanent
     * access to the sample values, e.g. for templates during streaming operations of an ECG.
     *
     * @return
     */
    public QrsComplex createWithFixedBuffer() {
        QrsComplex qrs = new QrsComplex(mSignal.subList(mSampleIndexStart, mSampleIndexEnd), mSignal, true);
        qrs.setRPeak(getRPosition(), getRValue());
        qrs.setQPeak(getQPosition(), getQValue());
        qrs.setSPeak(getSPosition(), getSValue());
        qrs.mSampleIndexStart = mSampleIndexStart;
        qrs.mSampleIndexEnd = mSampleIndexEnd;
        qrs.calculateFeatures();
        return qrs;
    }

    public double getSamplingRate() {
        return mSamplingRate;
    }

    public EcgSignal getSignal() {
        return mSignal;
    }

    public int toBufferIndex(int index) {
        int ret = index - mSampleIndexStart;
        if (ret < 0)
            ret = 0;
        if (ret >= mSampleBuffer.size())
            ret = mSampleBuffer.size() - 1;
        return ret;
    }

    public int fromBufferIndex(int index) {
        return index + mSampleIndexStart;
    }

    /**
     * Sets the Q peak of the QRS complex.
     *
     * @param qPosition the position of the Q peak in the entire signal
     * @param qValue    the value of the Q peak
     */
    public void setQPeak(int qPosition, double qValue) {
        this.mQPosition = qPosition;
        this.mQValue = qValue;
    }

    /**
     * Sets the R peak of the QRS complex.
     *
     * @param rPosition the position of the R peak in the entire signal
     * @param rValue    the value of the R peak
     */
    public void setRPeak(int rPosition, double rValue) {
        this.mRPosition = rPosition;
        this.mRValue = rValue;
    }

    /**
     * Sets the S peak of the QRS complex.
     *
     * @param sPosition the position of the S peak in the entire signal
     * @param sValue    the value of the S peak
     */
    public void setSPeak(int sPosition, double sValue) {
        this.mSPosition = sPosition;
        this.mSValue = sValue;
    }

    public void setSampleIndexStart(int idx) {
        mSampleIndexStart = idx;
    }

    /**
     * Sets the end of this QRS complex vicinity and searches for the Q and S peak, the QRS width and calculates
     * features for this heartbeat.
     *
     * @param index
     */
    public void setSampleIndexEnd(int index) {
        mSampleIndexEnd = index;
        findQPeak();
        findSPeak();
        calculateFeatures();
    }

    // finds the value with the maximum absolute difference to the value of the
    // R peak
    // within the buffer, starting from the right
    private void findQPeak() {
        int firstIndex = (int) (getRPosition() - mSamplingRate * 0.1);
        int pos = getRPosition();
        double maxDiff = Double.MIN_VALUE;
        for (int i = getRPosition(); i > firstIndex; i--) {
            if (Math.abs(getValueAtLGlobalIndex(i) - getRValue()) > maxDiff) {
                maxDiff = Math.abs(getValueAtLGlobalIndex(i) - getRValue());
                pos = i;
            }
        }

        setQPeak(pos, getValueAtLGlobalIndex(pos));
    }

    // finds the value with the maximum absolute difference to the value of the
    // R peak
    // with the buffer, starting from the left
    private void findSPeak() {
        // select a close lastIndex, since we expect the S peak to be quite
        // close to the R peak.
        int lastIndex = (int) (getRPosition() + mSamplingRate * 0.1);
        int pos = 0;
        double maxDiff = Double.MIN_VALUE;
        for (int i = getRPosition(); i < lastIndex; i++) {
            if (Math.abs(getValueAtLGlobalIndex(i) - getRValue()) > maxDiff) {
                maxDiff = Math.abs(getValueAtLGlobalIndex(i) - getRValue());
                pos = i;
            }
        }
        setSPeak(pos, getValueAtLGlobalIndex(pos));
    }

    /**
     * Determines the QRS width.
     *
     * @return the QRS width.
     */
    public int findQrsWidth() {
        // max left index for Q point search
        int maxLeft = (int) (mSamplingRate * 0.15);
        int maxRight = (int) (mSamplingRate * 0.15);

        // determine maximum index to go to the left
        maxLeft -= (mRPosition - mQPosition);
        // find q deflection start
        mQDeflectionStart = -1;
        double lastValue = mSignal.get(mQPosition);
        // double lastValue = values.get( toValueIndex( qPosition ) );
        for (int i = 1; i < maxLeft; i++) {
            int curIdx = mQPosition - i;
            // int curIdx = toValueIndex( qPosition ) - i;
            if (curIdx < 0 || mSignal.get(curIdx) < lastValue) // values.get(
            // curIdx ) <
            // lastValue)
            {
                mQDeflectionStart = curIdx + 1;
                // qDeflectionStart = fromValueIndex( curIdx + 1 );
                mBaselineValue = mSignal.get(curIdx + 1);
                // baselineValue = values.get( curIdx + 1 );
                break;
            }
            // lastValue = values.get( curIdx );
            lastValue = mSignal.get(curIdx);
        }

        if (mQDeflectionStart == -1) {
            // qDeflectionStart = fromValueIndex( 0 );
            mQDeflectionStart = mSampleIndexStart;
            // baselineValue = values.get( 0 );
            mBaselineValue = mSignal.get(mQDeflectionStart);
        }

        // determine maximum index to go to the right
        maxRight -= (mSPosition - mRPosition);

        // find s deflection end
        mSDeflectionEnd = -1;
        // lastValue = values.get( toValueIndex( sPosition ) );
        lastValue = mSignal.get(mSPosition);
        for (int i = 1; i < maxRight; i++) {
            // int curIdx = toValueIndex( sPosition ) + i;
            int curIdx = mSPosition + i;
            // if (curIdx >= values.size() || values.get( curIdx ) < lastValue)
            if (curIdx >= mSignal.getTotalLength() || mSignal.get(curIdx) < lastValue) {
                // sDeflectionEnd = fromValueIndex( curIdx - 1 );
                mSDeflectionEnd = curIdx - 1;
                // baseline is the mean of the one extracted at the beginning of
                // the Q deflection and end of S
                // deflection.
                // baselineValue = (baselineValue + values.get( curIdx - 1 )) *
                // 0.5;
                mBaselineValue = (mBaselineValue + mSignal.get(curIdx - 1)) * 0.5;
                break;
            }
            // lastValue = values.get( curIdx );
            lastValue = mSignal.get(curIdx);
        }

        if (mSDeflectionEnd == -1) {
            // sDeflectionEnd = fromValueIndex( values.size() - 1 );
            mSDeflectionEnd = mSampleIndexEnd;
            // baselineValue = (baselineValue + values.get( values.size() - 1 ))
            // * 0.5;
            mBaselineValue = (mBaselineValue + mSignal.get(mSDeflectionEnd)) * 0.5;
        }

        mQrsWidth = mSDeflectionEnd - mQDeflectionStart;
        return mQrsWidth;
    }

    /**
     * Returns the position of the Q peak.
     *
     * @return the position of the Q peak in the entire signal
     */
    public int getQPosition() {
        return this.mQPosition;
    }

    /**
     * Returns the value of the Q peak.
     *
     * @return the value of the Q peak
     */
    public double getQValue() {
        return this.mQValue;
    }

    /**
     * Returns the position of the R peak.
     *
     * @return the position of the R peak in the entire signal
     */
    public int getRPosition() {
        return this.mRPosition;
    }

    /**
     * Returns the value of the R peak.
     *
     * @return the value of the R peak
     */
    public double getRValue() {
        return this.mRValue;
    }

    /**
     * Returns the position of the S peak.
     *
     * @return the position of the S peak in the entire signal
     */
    public int getSPosition() {
        return this.mSPosition;
    }

    /**
     * Returns the value of the S peak.
     *
     * @return the value of the S peak
     */
    public double getSValue() {
        return this.mSValue;
    }

    /**
     * @return the qDeflectionStart
     */
    public int getqDeflectionStart() {
        return mQDeflectionStart;
    }

    /**
     * @return the sDeflectionEnd
     */
    public int getsDeflectionEnd() {
        return mSDeflectionEnd;
    }

    /**
     * @return the baselineValue
     */
    public double getBaselineValue() {
        return mBaselineValue;
    }

    /**
     * Returns the number of values of the QRS complex.
     *
     * @return the number of values of the QRS complex
     */
    public long getNumberOfValues() {
        if (mSampleIndexEnd == -1)
            return 0;

        return mSampleIndexEnd - mSampleIndexStart;
    }

    /**
     * Returns the value of the QRS complex at the specified index
     *
     * @param index the index of the value of the QRS complex
     * @return the value of the QRS complex at the specified index
     */
    public double getValueAtLocalIndex(int index) {
        if (mSampleBuffer != null)
            return mSampleBuffer.get(index);
        if (mSignal == null)
            return 0;
        return mSignal.get(mSampleIndexStart + index);
    }

    public double getValueAtLGlobalIndex(int index) {
        if (mSampleBuffer != null)
            return mSampleBuffer.get(index - mSampleIndexStart);
        if (mSignal == null)
            return 0;
        return mSignal.get(index);
    }

    /**
     * @return the nextQrs
     */
    public QrsComplex getNextQrs() {
        return mNextQrs;
    }

    /**
     * @param nextQrs the nextQrs to set
     */
    public void setNextQrs(QrsComplex nextQrs) {
        mNextQrs = nextQrs;
    }

    public void setPreviousQrs(QrsComplex previousQrs) {
        mPreviousQrs = previousQrs;
    }

    /**
     * @return the previousQrs
     */
    public QrsComplex getPreviousQrs() {
        return mPreviousQrs;
    }

    /**
     * Returns the RR interval of the QRS complex.
     *
     * @return the RR interval of the QRS complex (in s), or <code>Double.NaN</code> if <code>precursor</code> is null
     */
    public double getRRInterval() {
        if (mPreviousQrs == null)
            return Double.NaN;

        return getRRDistance() / mSamplingRate;
    }

    /**
     * @return the RR interval in samples, or <code>0</code> if not previous QRS is available.
     */
    public int getRRDistance() {
        if (mPreviousQrs == null)
            return 0;

        return mRPosition - mPreviousQrs.getRPosition();
    }

    /**
     * Returns the QR amplitude of the QRS complex.
     *
     * @return the QR amplitude of the QRS complex
     */
    public double getQRAmplitude() {
        return mRValue - mQValue;
    }

    /**
     * Returns the RS amplitude of the QRS complex.
     *
     * @return the RS amplitude of the QRS complex
     */
    public double getRSAmplitude() {
        return mRValue - mSValue;
    }

    /**
     * Returns the QRS width of the QRS complex.
     *
     * @return the QRS width of the QRS complex (in s)
     */
    public double getQRSWidth() {
        return ((double) (mSDeflectionEnd - mQDeflectionStart)) / mSamplingRate;
    }

    /**
     * Returns the QRST area of the QRS complex.
     *
     * @return the QRST area of the QRS complex
     */
    /*
     * public double getQRSTArea() { double qrstArea = 0; double mean = getMean(); for (int i = 0; i < values.size();
     * i++) { qrstArea += Math.abs( values.get( i ) - mean ); } return qrstArea; }
     */
    public double getQRSTArea() {
        return mQrstArea;
    }

    public double calculateQRSTArea() {
        double qrstArea = 0;
        double mean = getMean();
        for (int i = mSampleIndexStart; i <= mSampleIndexEnd; i++) {
            qrstArea += Math.abs(mSignal.get(i) - mean);
        }
        return qrstArea;
    }

    private void calculateFeatures() {
        findQrsWidth();

        // don't calculate median and mode, they are quite expensive
        EnumSet<DescriptiveStatisticsElement> elements = EnumSet.allOf(DescriptiveStatisticsElement.class);
        elements.remove(DescriptiveStatisticsElement.MEDIAN);
        elements.remove(DescriptiveStatisticsElement.MODE);

        if (mSampleBuffer != null)
            mStats = new DescriptiveStatistics(mSampleBuffer, elements);
        else if (mSignal != null)
            mStats = new DescriptiveStatistics(mSignal.getSignal(), mSampleIndexStart, mSampleIndexEnd, elements);

        mQrstArea = calculateQRSTArea();
    }

    /**
     * Returns the minimum of all values of the QRS complex.
     *
     * @return the minimum of all values of the QRS complex
     */
    public double getMinimum() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getMin();
    }

    /**
     * Returns the maximum of all values of the QRS complex.
     *
     * @return the maximum of all values of the QRS complex
     */
    public double getMaximum() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getMax();
    }

    /**
     * Returns the mean of all values of the QRS complex.
     *
     * @return the mean of all values of the QRS complex
     */
    public double getMean() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getMean();
    }

    /**
     * Returns the variance of all values of the QRS complex.
     *
     * @return the variance of all values of the QRS complex
     */
    public double getVariance() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getVariance();
    }

    /**
     * Returns the standard deviation of all values of the QRS complex.
     *
     * @return the standard deviation of all values of the QRS complex
     */
    public double getStandardDeviation() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getStandardDeviation();
    }

    /**
     * Returns the skewness of all values of the QRS complex.
     *
     * @return the skewness of all values of the QRS complex
     */
    public double getSkewness() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getSkewness();
    }

    /**
     * Returns the kurtosis of all values of the QRS complex.
     *
     * @return the kurtosis of all values of the QRS complex
     */
    public double getKurtosis() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getKurtosis();
    }

    /**
     * Returns the energy of all values of the QRS complex.
     *
     * @return the energy of all values of the QRS complex
     */
    public double getEnergy() {
        if (mStats == null)
            calculateFeatures();
        return mStats.getEnergy();
    }

    /**
     * Returns the cross correlation coefficient of the QRS complex with the template.
     *
     * @param template a QRS complex template
     * @return the cross correlation coefficient of the QRS complex with the template, or <code>Double.NaN</code> if
     * <code>template</code> is null
     */
    public double getCrossCorrelation(QrsComplex template) {
        if (template != null) {
            double tmp1;
            double tmp2;
            double sum1 = 0;
            double sum2 = 0;
            double sum3 = 0;
            for (int i = mSampleIndexStart; i <= mSampleIndexEnd; i++) {
                tmp1 = mSignal.get(i) - getMean();
                tmp2 = template.getValueAtLGlobalIndex(i) - template.getMean();
                sum1 += tmp1 * tmp2;
                sum2 += tmp1 * tmp1;
                sum3 += tmp2 * tmp2;
            }
            return sum1 / Math.sqrt(sum2 * sum3);
        }
        return Double.NaN;
    }

    /**
     * Returns the area difference of the QRS complex with the template.
     *
     * @param template a QRS complex template
     * @return the area difference of the QRS complex with the template, or <code>Double.NaN</code> if
     * <code>template</code> is null
     */
    public double getAreaDifference(QrsComplex template) {
        if (template != null) {
            if (template.getQRSTArea() != 0) {
                return Math.abs(getQRSTArea() - template.getQRSTArea()) / template.getQRSTArea();
            }
        }
        return Double.NaN;
    }

}
