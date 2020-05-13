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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import de.fau.mad.jely.detectors.QrsDetector;
import de.fau.mad.jely.filter.DigitalFilter;
import de.fau.shiftlist.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Represents (part of) a sampled signal from a single channel/lead of an ECG recording.
 * 
 * @author gradl
 *
 */
public class EcgSignal implements List<Double>
{
    private EcgLead	    mLead;
    private ShiftListDouble mSignal;
    private Ecg		    mParentEcg;
    private String	    mPhysicalUnit;
    private boolean	    mIsMutable = false;

    public EcgSignal( EcgLead lead, Ecg parentEcg, ShiftListDouble signal )
    {
	mLead = lead;
	mParentEcg = parentEcg;
	mSignal = signal;
    }

    public EcgSignal( EcgLead lead, Ecg parentEcg, int signalCapacity )
    {
	mLead = lead;
	mParentEcg = parentEcg;
	mSignal = new ShiftListDouble( signalCapacity );
    }
    
    public EcgSignal( EcgLead lead, Ecg parentEcg, double[] signal )
    {
	this( lead, parentEcg, new ShiftListDouble(signal) );
    }

    /**
     * (Deep) copy constructor.
     * 
     * @param signal
     *            Original signal which should be cloned.
     */
    public EcgSignal( EcgSignal signal )
    {
	mLead = signal.mLead;
	mParentEcg = signal.mParentEcg;
	mPhysicalUnit = signal.mPhysicalUnit;
	mSignal = new ShiftListDouble( signal.mSignal );
    }

    /**
     * (Deep) copy constructor.
     * 
     * @param signal
     *            Original signal which should be cloned.
     */
    public EcgSignal( EcgSignal signal, int from, int to )
    {
	mLead = signal.mLead;
	mParentEcg = signal.mParentEcg;
	mSignal = new ShiftListDouble( signal.subList( from, to ) );
    }

    protected void unfreeze()
    {
	mIsMutable = true;
    }

    /**
     * 
     * @return the lead from which this signal was taken.
     */
    public EcgLead getLead()
    {
	return mLead;
    }

    /**
     * @return the samplingRate
     */
    public double getSamplingRate()
    {
	return mParentEcg.getSamplingRate();
    }

    /**
     * Calculates the sampling time interval in seconds.
     * 
     * @return Sampling time interval in seconds.
     */
    public double getSamplingInterval()
    {
	return 1d / getSamplingRate();
    }

    public ShiftListDouble getSignal()
    {
	return mSignal;
    }

    public SignalView getSignalView( int from, int to )
    {
	return new SignalView( this, from, to );
    }

    /**
     * 
     * @return the exact (millisecond precision) time and date at which the first sample of this signal was recorded.
     */
    public Date getDate()
    {
	return mParentEcg.getFirstSampleDate();
    }

    public long getSyncTimestamp()
    {
	return mParentEcg.getFirstSampleTimestamp();
    }

    /**
     * Applies the given DigitalFilter to this ECG signal and returns a new EcgSignal with the filtered signal.
     * 
     * @param filter
     * @return
     */
    public EcgSignal applyFilter( DigitalFilter filter )
    {
	ShiftListDouble sig = new ShiftListDouble( size() );

	for (int i = 0; i < size(); i++)
	{
	    sig.add( filter.next( get( i ) ) );
	}

	return new EcgSignal( mLead, mParentEcg, sig );
    }

    @Override
    public boolean add( Double e )
    {
	return mSignal.add( e );
    }

    @Override
    public void add( int index, Double element )
    {
	throw new NotImplementedException();
    }

    @Override
    public boolean addAll( Collection<? extends Double> c )
    {
	return mSignal.addAll( c );
    }

    @Override
    public boolean addAll( int index, Collection<? extends Double> c )
    {
	throw new NotImplementedException();
    }

    @Override
    public void clear()
    {
	throw new NotImplementedException();
    }

    @Override
    public boolean contains( Object o )
    {
	return mSignal.contains( o );
    }

    @Override
    public boolean containsAll( Collection<?> c )
    {
	return mSignal.containsAll( c );
    }

    @Override
    public Double get( int index )
    {
	return mSignal.get( index );
    }

    @Override
    public int indexOf( Object o )
    {
	return mSignal.indexOf( o );
    }

    @Override
    public boolean isEmpty()
    {
	return mSignal.isEmpty();
    }

    @Override
    public Iterator<Double> iterator()
    {
	throw new NotImplementedException();
    }

    @Override
    public int lastIndexOf( Object o )
    {
	return mSignal.lastIndexOf( o );
    }

    @Override
    public ListIterator<Double> listIterator()
    {
	throw new NotImplementedException();
    }

    @Override
    public ListIterator<Double> listIterator( int index )
    {
	throw new NotImplementedException();
    }

    @Override
    public boolean remove( Object o )
    {
	throw new NotImplementedException();
	// return signal.remove( o );
    }

    @Override
    public Double remove( int index )
    {
	throw new NotImplementedException();
    }

    @Override
    public boolean removeAll( Collection<?> c )
    {
	throw new NotImplementedException();
    }

    @Override
    public boolean retainAll( Collection<?> c )
    {
	throw new NotImplementedException();
    }

    @Override
    public Double set( int index, Double element )
    {
	if (!mIsMutable)
	{
	    System.err.println( "This EcgSignal can not be edited. Call unfreeze() to edit samples of a signal." );
	    return 0d;
	}

	Double old = mSignal.get( index );
	mSignal.set( index, element );
	return old;
	// throw new NotImplementedException();
    }

    // the number of samples in this signal.
    @Override
    public int size()
    {
	return mSignal.getMaxSize();
    }

    public int getTotalLength()
    {
	return mSignal.size();
    }

    public boolean isSampleValid( int atIndex )
    {
	return mSignal.isIndexValid( atIndex );
    }

    @Override
    public List<Double> subList( int fromIndex, int toIndex )
    {
	return mSignal.subList( fromIndex, toIndex );
    }

    @Override
    public Object[] toArray()
    {
	return mSignal.toArray();
    }

    @Override
    public <T> T[] toArray( T[] a )
    {
	return mSignal.toArray( a );
    }

    /**
     * Returns a reference to the underlying double array buffer of the signal.
     * 
     * @return double[] array of ecg signal data.
     */
    public double[] toDoubleArray()
    {
	return mSignal.getBuffer();
    }

    /**
     * Converts part of the signal into a simple array.
     * 
     * @param firstSample
     *            the first sample which should be included in the resulting array.
     * @param lastSample
     *            the last sample which should be included in the resulting array.
     * @return an array of double values that represent the part [firstSample, lastSample] of the original signal.
     */
    public double[] toDoubleArray( int firstSample, int lastSample )
    {
	double[] ll = new double[lastSample - firstSample + 1];
	for (int i = firstSample; i <= lastSample; i++)
	{
	    ll[i] = mSignal.get( i );
	}
	return ll;
    }

    /**
     * 
     * @return a checksum based on all signal values.
     */
    public long calculateChecksum()
    {
	long checksum = 0;
	for (int i = 0; i < mSignal.getMaxSize(); i++)
	{
	    checksum += (mSignal.get( i ) * 1000);
	}
	return checksum;
    }
}
