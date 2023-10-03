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
package jely.io;

import jely.Ecg;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Loads a physionet file converted to csv.
 *
 * @author Stefan Gradl
 * @deprecated Physionet files can be loaded much more efficiently using the PhysionetEcgFile class.
 */
public class PhysionetCsvEcgFile extends Ecg {
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTES /////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // data column
    private final int SIG_DATA_COLUMN = 2;
    private final int ANN_INDEX_COLUMN = 2;
    private final int ANN_LABEL_COLUMN = 3;
    // file specification
    private String m_dirPath;
    private String m_fileName;
    private int m_maxSamples;
    private float m_samplingInterval = -1;
    // buffered readers
    private BufferedReader m_sigReader = null;
    private BufferedReader m_annReader = null;
    // array list for calibrated data
    //private ArrayList<CalibratedData> m_data		 = null;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // METHODS ////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public PhysionetCsvEcgFile(String dirPath, String fileName, int maxSamples) {

        m_dirPath = dirPath;
        m_fileName = fileName;
        m_maxSamples = maxSamples;
        // init signal and annotation file
        File sigFile = new File(m_dirPath, m_fileName + "sig.csv.gz");
        // File annFile = new File(m_dirPath, m_fileName + "ann.csv");
        // init buffered reader for signal and annotation file
        try {
            m_sigReader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(new BufferedInputStream(new FileInputStream(sigFile)))));
            // m_annReader = new BufferedReader(new FileReader(annFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // init array for values and labels
        //m_data = new ArrayList<CalibratedData>( (int) sigFile.length() / 3 );
    }


    public void close() {
        // close buffered reader for signal and annotation file
        try {
            m_sigReader.close();
            // m_annReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void load() {
        // load signal and annotation file
        //loadSignalFile();
        // loadAnnotationFile();
    }
/*
    private void loadSignalFile()
    {
	try
	{
	    String line = null;

	    // skip header line
	    m_sigReader.readLine();

	    // read sampling interval
	    line = m_sigReader.readLine();
	    String[] splits = line.split( "," );
	    
	    for (int i = 0; i < splits.length; i++)
	    {
		String str = splits[i];
		if (str != null && str.startsWith( "'" ))
		{
		    int n = str.indexOf( ' ' );
		    if (n != -1)
		    {
			str = str.substring( 1, n );
			try
			{
			    m_samplingInterval = Float.parseFloat( str );
			}
			catch (NumberFormatException e)
			{
			    e.printStackTrace();
			}
		    }
		}
	    }

	    // sampling interval invalid
	    if (m_samplingInterval < 0)
	    {
		// reset to default sampling intervals
		if (m_fileName.startsWith( "faudb" ))
		{
		    // sampling frequency equals 512Hz
		    m_samplingInterval = 0.00195313f;
		}
		else if (m_fileName.startsWith( "mitdb" ))
		{
		    // sampling frequency equals 360Hz
		    m_samplingInterval = 0.00277778f;
		}
	    }

	    int counter = 0;
	    float timestamp = 0;
	    int currentColumn;

	    // read data lines
	    while ((line = m_sigReader.readLine()) != null)
	    {
		splitter.setString( line );
		try
		{
		    // iterate over columns
		    currentColumn = 1;
		    for (String split : splitter)
		    {
			if (currentColumn == SIG_DATA_COLUMN)
			{
			    // add calibrated data
			    m_data.add( new CalibratedData() );
			    // add ecg in mV
			    m_data.get( counter ).ecg = Double.parseDouble( split );
			    // add timestamp in ms
			    m_data.get( counter ).timeStamp = timestamp * 1000;
			    // increment counter and timestamp
			    timestamp += m_samplingInterval;
			    ++counter;
			}
			++currentColumn;
		    }
		}
		catch (NumberFormatException e)
		{
		    e.printStackTrace();
		}

		if (m_maxSamples > 0 && counter >= m_maxSamples)
		{
		    break;
		}
	    }
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }

    private void loadAnnotationFile()
    {
	try
	{

	    char delimiter = ' ';
	    if (m_fileName.startsWith( "faudb" ))
	    {
		delimiter = ',';
	    }
	    else if (m_fileName.startsWith( "mitdb" ))
	    {
		delimiter = ' ';
	    }
	    // initialize line splitter
	    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter( delimiter );
	    String line = null;

	    // skip header line
	    m_annReader.readLine();

	    int currentColumn;
	    int sample = 0;

	    // read data lines
	    while ((line = m_annReader.readLine()) != null)
	    {
		splitter.setString( line );
		try
		{
		    // iterate over columns
		    currentColumn = 1;
		    for (String split : splitter)
		    {
			if (split.length() < 1)
			{
			    continue;
			}
			if (currentColumn == ANN_INDEX_COLUMN)
			{
			    sample = Integer.parseInt( split.trim() );
			    if (m_maxSamples > 0 && sample >= m_maxSamples)
			    {
				break;
			    }
			}
			if (currentColumn == ANN_LABEL_COLUMN)
			{
			    m_data.get( sample ).label = split.trim().charAt( 0 );
			}
			++currentColumn;
		    }
		}
		catch (NumberFormatException e)
		{
		    e.printStackTrace();
		}
	    }
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }


    public int getSamplingRate()
    {
	// round
	return (int) Math.round( 1d / m_samplingInterval );
    }
*/
}
