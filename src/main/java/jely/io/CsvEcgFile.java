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
import jely.EcgLead;
import jely.LeadConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * ECG stored as an CSV file.
 * 
 * @author Stefan Gradl
 *
 */
public class CsvEcgFile extends Ecg {
	public enum CsvFormat {
		UNKNOWN, SINGLE_COLUMN, MR32_ASCII
	}

	String mPathToFile;
	CsvFormat mFormat = CsvFormat.UNKNOWN;

	public CsvFormat getFormat() {
		return mFormat;
	}

	public CsvEcgFile(String pathToFile) {
		load(pathToFile, -1, null);
	}

	public CsvEcgFile(String pathToFile, double samplingRate) {
		this.samplingRate = samplingRate;
		load(pathToFile, -1, null);
	}

	public CsvEcgFile(String pathToFile, LeadConfiguration leads, double samplingRate) {
		this.samplingRate = samplingRate;
		load(pathToFile, -1, leads);
	}

	public CsvEcgFile(String pathToFile, LeadConfiguration leads) {
		load(pathToFile, -1, leads);
	}

	public CsvEcgFile(String pathToFile, int ecgColumn, EcgLead lead) {
		load(pathToFile, ecgColumn, new LeadConfiguration(lead));
	}

	public CsvEcgFile(String pathToFile, int ecgColumn, EcgLead lead, double samplingRate) {
		this.samplingRate = samplingRate;
		load(pathToFile, ecgColumn, new LeadConfiguration(lead));
	}

	private void load(String pathToFile, int ecgColumn, LeadConfiguration leads) {
		mPathToFile = pathToFile;
		try {
			String splitCharacters = "[,\\s+]"; // split on any comma or any
			// whitespace/tab
			boolean nextLineContainsData = true;

			BufferedReader br = new BufferedReader(new FileReader(new File(pathToFile)));

			ArrayList<ArrayList<Double>> cols = new ArrayList<ArrayList<Double>>(3);
			cols.add(new ArrayList<Double>());
			cols.add(new ArrayList<Double>());
			cols.add(new ArrayList<Double>());

			int timestampColumn = -1;
			long numDataLines = 0;
			NumberFormat nf = NumberFormat.getInstance(Locale.US);

			for (String line; (line = br.readLine()) != null;) {
				line = line.trim();

				// empty lines
				if (line.isEmpty())
					continue;

				// comment lines
				if (line.charAt(0) == '#') {
					continue;
				}

				numDataLines++;

				// parse line
				String[] splits = line.split(splitCharacters);
				Double sample = null;
				try {
					if (splits.length < 1)
						continue;

					// format information should be somewhere at the very
					// beginning of the file
					if (numDataLines < 4) {
						if (splits[0].equalsIgnoreCase("MR32") && splits.length > 1
								&& splits[1].equalsIgnoreCase("ASCII")) {
							splitCharacters = "\\s+";
							mFormat = CsvFormat.MR32_ASCII;
							timestampColumn = 0;
							ecgColumn = 1;
							nextLineContainsData = false;
							nf = NumberFormat.getInstance(Locale.GERMAN);
						}
					}

					// frequency and field information in MR32 files should be
					// somewhere in the first 20 lines
					if (mFormat == CsvFormat.MR32_ASCII && numDataLines < 20) {
						if (splits[0].equalsIgnoreCase("Frequency")) {
							samplingRate = Double.parseDouble(splits[1]);
						} else if (splits[0].charAt(0) == '"') {
							// this is probably the last header line...
							nextLineContainsData = true;
							continue;
						}
					}

					if (!nextLineContainsData)
						continue;

					cols.get(0).add(nf.parse(splits[0]).doubleValue());
					if (splits.length > 1)
						cols.get(1).add(nf.parse(splits[1]).doubleValue());
					if (splits.length > 2)
						cols.get(2).add(nf.parse(splits[2]).doubleValue());
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}

			br.close();

			// infer sampling rate if we can assume that a timestamp is present
			// in the data
			if (samplingRate <= 0 && cols.get(1).size() > 1) {
				// we have more than one column with more than 1 data point
				// we iterate through 100 data points, if we find only
				// increasing numbers then this is very likely a
				// timestamp
				int maxNum = Math.min(cols.get(0).size(), 100);
				for (int i = 1; i <= maxNum; i++) {
					if (cols.get(0).get(i) < cols.get(0).get(i - 1)) {
						// data point did not increase --> not the timestamp
						// column
						timestampColumn = 1;
						if (ecgColumn == -1)
							ecgColumn = 0;
						break;
					}
				}

				maxNum = Math.min(cols.get(1).size(), 100);
				for (int i = 1; i <= maxNum; i++) {
					if (cols.get(1).get(i) < cols.get(1).get(i - 1)) {
						// data point did not increase --> not the timestamp
						// column
						timestampColumn = 2;
						break;
					}
				}

				if (cols.get(2).size() > 1 && timestampColumn == 2) {
					maxNum = Math.min(cols.get(2).size(), 100);
					for (int i = 1; i <= maxNum; i++) {
						if (cols.get(2).get(i) < cols.get(2).get(i - 1)) {
							// data point did not increase --> not the timestamp
							// column
							timestampColumn = 3;
							break;
						}
					}
				}

				if (timestampColumn < 3) {
					// we found a timestamp column, infer the sampling rate
					double first = cols.get(timestampColumn).get(0);
					double second = cols.get(timestampColumn).get(1);
					// seconds or milliseconds?
					if (first < 1.0 && second < 1.0)
						samplingRate = 1d / (second - first);
					else
						samplingRate = 1000d / (second - first);
				}
			}

			// default fallback sampling rate
			if (samplingRate <= 0)
				samplingRate = 1024;

			if (ecgColumn == -1)
				ecgColumn = 0;

			if (mFormat == CsvFormat.UNKNOWN && cols.get(1).size() < cols.get(0).size() / 4) {
				mFormat = CsvFormat.SINGLE_COLUMN;
			}

			date = null;
			if (leads == null)
				leadInfo = LeadConfiguration.SINGLE_UNKNOWN_LEAD;
			else
				leadInfo = leads;

			init(cols.get(ecgColumn).size());
			getSignalFromIndex(0).addAll(cols.get(ecgColumn));

		} catch (

		FileNotFoundException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		IOException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {		
		return super.toString() + ":: CSV ECG File :: " + this.samplingRate;
	}

}
