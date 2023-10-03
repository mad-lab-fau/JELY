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
import jely.LeadConfiguration;
import jely.annotations.AnnotationManager;
import jely.annotations.SubjectInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class represents an ECG that exists in a binary format on long-term
 * storage.
 * 
 * @author Stefan Gradl
 *
 */
public class BinaryEcgFile extends Ecg {
	/**
	 * File extension for files stored by this class. Also used as identifier in
	 * the file header.
	 */
	protected static final String extension = "BinaryEcg";
	/**
	 * Current file version.
	 */
	protected static final long CURRENT_VERSION = 10;

	private long version = 0;

	private int numSamplesPerSignal = 0;
	private long checksum = 0;

	/**
	 * Absolute path to this file.
	 */
	private transient String fullPath = null;

	private transient DataInputStream inStream = null;

	/**
	 * Loads a BinaryEcg file from the given path into memory.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public BinaryEcgFile(String path) throws IOException {
		this(path, 0);
	}

	/**
	 * Opens an existing BinaryEcgFile and only reads the given number of
	 * samples.
	 * 
	 * @param path
	 * @param numSamplesToRead
	 * @throws IOException
	 */
	public BinaryEcgFile(String path, int numSamplesToRead) throws IOException {
		File rawFile = new File(path);

		setFullPath(path);

		inStream = new DataInputStream(new BufferedInputStream(new FileInputStream(rawFile)));

		// read the header
		readHeader();

		if (version > 3) {
			if (version == 4)
				throw new IOException("Unsupported file version!");

			// read annotations
			annotations = new AnnotationManager(inStream, version);
		}

		// TODO: maybe this should be an exception?!
		int numChan = getNumChannels();
		if (numChan != 0) {
			// approximate the number of elements
			long fileLen = rawFile.length();
			int projectedSampleNum = (int) (fileLen / numChan / 8);
			super.init(projectedSampleNum);

			if (numSamplesToRead > 0)
				projectedSampleNum = numSamplesToRead;

			// read data until the EOF is reached
			double cv;
			try {
				if (version > 9) {
					numSamplesPerSignal = inStream.readInt();
					checksum = inStream.readLong();
					long datel = inStream.readLong();
					if (datel > 0)
						firstSampleDate = new Date(datel);
					firstSampleTimestamp = inStream.readLong();
				}

				if (numSamplesPerSignal <= 0)
					numSamplesPerSignal = projectedSampleNum;

				for (int numRead = 0; numRead < numSamplesPerSignal; numRead++) {
					// read all the data from all channels
					for (int i = 0; i < numChan; i++) {
						cv = inStream.readDouble();
						ecgLeads.get(i).add(cv);
						// ecg.get(i).add(cv);
					}
				}

				inStream.close();

				if (checksum != 0) {
					long cc = calculateSignalChecksum();
					if (cc != checksum)
						throw new IOException("Signal checksum mismatch (" + cc + " vs " + checksum + ")");
				}
			} catch (EOFException e) {
				inStream.close();
			}
		}

		numSamplesPerSignal = ecgLeads.get(0).size();
	}

	protected BinaryEcgFile(double samplingRate, LeadConfiguration leads, SubjectInfo subject,
							double secondsToKeepInMemory) {
		super(samplingRate, leads, subject, secondsToKeepInMemory);
	}

	/*
	 * public BinaryEcgFile( String directoy, int samplingRate,
	 * LeadConfiguration leads, SubjectInfo subject ) { this( directoy,
	 * samplingRate, leads ); subject.setEcg( this ); this.subject = subject; }
	 */

	/**
	 * Constructs a filename for an Ecg in a standardized pattern using the
	 * timestamp at which the ECG was taken. The extension is included in the
	 * returned filename.
	 * 
	 * @param ecg
	 *            the Ecg for which to construct a standardized filename.
	 * @return the standardized filename including the extension for a
	 *         BinaryEcg.
	 */
	public static String getStandardizedFileName(Ecg ecg) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
		String formattedDate = sdf.format(ecg.getDate());
		return formattedDate + "." + extension;
	}

	public static String getStandardizedFileName(Ecg ecg, int id) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
		String formattedDate = sdf.format(ecg.getDate());
		return formattedDate + "--" + id + "." + extension;
	}

	/**
	 * Writes the BinaryEcg header and the annotations to the given file output
	 * stream.
	 * 
	 * @param ecg
	 * @param os
	 */
	protected static void writeHeaderAndAnnotations(Ecg ecg, DataOutputStream os) {
		try {
			writeHeader(ecg, os);
			ecg.getAnnotations().writeToBinaryFile(os);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writeEcgSignals(Ecg ecg, DataOutputStream os) throws IOException {
		// TODO: saving an previously opened ECG can be made a lot faster if we
		// cache the entire signal block from the
		// loaded file and write it back to disk
		// e.g. if (ecg instanceof BinaryEcgFile)...

		int numSamples = ecg.getSignalFromIndex(0).size();

		os.writeInt(numSamples);
		os.writeLong(ecg.calculateSignalChecksum());
		if (ecg.getFirstSampleDate() == null)
			os.writeLong(0);
		else
			os.writeLong(ecg.getFirstSampleDate().getTime());
		os.writeLong(ecg.getFirstSampleTimestamp());

		for (int n = 0; n < numSamples; n++) {
			for (int i = 0; i < ecg.getLeads().size(); i++) {
				os.writeDouble(ecg.getSignalFromIndex(i).get(n));
			}
		}
	}

	/**
	 * Reads the binary file header from a BinaryEcg file.
	 * 
	 * @throws IOException
	 */
	private void readHeader() throws IOException {
		// read file header version
		version = inStream.readLong();

		// read id string and check if this is a valid BinaryEcg file
		String idString = "";
		if (version == 1) {
			for (int i = 0; i < 15; i++) {
				idString += inStream.readChar();
			}
			if (!idString.equals("heartyRawEcgBin")) {
				throw new IOException("Not a BinaryEcg-File (Code: 102).");
			}
		} else if (version > 1) {
			idString = inStream.readUTF();
			if (!idString.equals("BinaryEcg")) {
				throw new IOException("Not a BinaryEcg-File (Code: 103). Ver: " + version + "; id: " + idString);
			}
		} else {
			throw new IOException("Unsupported file version, or not an BinaryEcg-File.");
		}

		// read timestamp
		long datetime = inStream.readLong();
		if (datetime == -1)
			date = null;
		else
			date = new Date(datetime);

		// read subject info
		if (version > 6) {
			subject = new SubjectInfo(inStream, this, version);
		} else {
			subject = new SubjectInfo(this);
		}

		// read sampling rate
		if (version < 3)
			samplingRate = (int) inStream.readLong();
		else if (version < 8)
			samplingRate = inStream.readInt();
		else
			samplingRate = inStream.readDouble();

		if (version <= 3) {
			// read number of channels
			int numChan = 0;
			if (version == 1)
				numChan = (int) inStream.readLong();
			else
				numChan = inStream.readInt();

			leadInfo = new LeadConfiguration(numChan);
		} else {
			leadInfo = new LeadConfiguration(inStream);
		}
	}

	/**
	 * Writes the BinaryEcgFile header.
	 * 
	 * @throws IOException
	 */
	protected static void writeHeader(Ecg ecg, DataOutputStream os) throws IOException {
		os.writeLong(CURRENT_VERSION);
		os.writeUTF(extension);
		if (ecg.getDate() == null)
			os.writeLong(-1);
		else
			os.writeLong(ecg.getDate().getTime()); // timestamp
		ecg.getSubject().writeToBinaryStream(os);
		os.writeDouble(ecg.getSamplingRate()); // write sampling rate
		// outStream.writeInt(getNumChannels()); // number of ECG channels in
		// the file
		ecg.getLeads().writeToBinaryStream(os);
	}

	/**
	 * @return the fullPath
	 */
	public String getFullPath() {
		return fullPath;
	}

	/**
	 * @param fullPath
	 *            the fullPath to set
	 */
	protected void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	/**
	 * Saves this BinaryEcg to the original file.
	 */
	public void saveToOriginalFile() {
		saveToFile(getFullPath());
	}

	/**
	 * Saves this BinaryEcg to the given file.
	 * 
	 * @param filepath
	 */
	public void saveToFile(String filepath) {
		try {
			DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)));
			writeHeaderAndAnnotations(this, outStream);
			writeEcgSignals(this, outStream);
			outStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves an ECG in the BinaryEcg format to a file.
	 * 
	 * @param ecg
	 * @param filepath
	 */
	public static void saveEcgToFile(Ecg ecg, String filepath) {
		try {
			DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)));
			writeHeaderAndAnnotations(ecg, outStream);
			writeEcgSignals(ecg, outStream);
			outStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
