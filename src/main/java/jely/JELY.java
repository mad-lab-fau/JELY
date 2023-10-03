package jely;

import jely.detectors.ElgendiFastQrsDetector;
import jely.detectors.HeartbeatDetector;
import jely.detectors.QrsDetector;
import jely.io.FileLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class JELY {

	/**
	 * Call this e.g. via "java -jar Jely.jar --ecgfile xxx.csv --samplingrate 500".
	 * @param args
	 */
	public static void main(String[] args) {
		String ecgFile = null;
		float samplingRate = 0f;
		String rFile = null;
		boolean rOut = false;
		
		for (int i=0; i<args.length; i++) {
			//System.out.println("Argument " + i + ": " + args[i]);
			
			if (args[i].equalsIgnoreCase("--ecgfile")) {
				ecgFile = args[++i];				
			}
			
			if (args[i].equalsIgnoreCase("--samplingrate")) {
				samplingRate = Float.parseFloat(args[++i]);				
			}
			
			if (args[i].equalsIgnoreCase("--rfile")) {
				rFile = args[++i];		
			}
			
			if (args[i].equalsIgnoreCase("--rout")) {
				rOut = true;		
			}
		}
		
		if (ecgFile != null) {
			System.out.println("Trying to load ECG file... " + ecgFile);
			Ecg ecg = FileLoader.loadKnownEcgFile(ecgFile, null, samplingRate);
			if (ecg == null) {
				System.out.println("ERROR: Unknown file type.");
				return;
			}
			
			System.out.println("Loaded ECG: " + ecg);
			
			//QrsDetector det = new PanTompkinsDetector(ecg);
			QrsDetector det = new ElgendiFastQrsDetector(ecg);
			HeartbeatDetector detector = new HeartbeatDetector(ecg, det);
			ArrayList<Heartbeat> beatList = detector.findHeartbeats();
			
			System.out.println("Found " + beatList.size() + " heartbeats using " + detector.getQrsDetector());

			try {
				FileWriter myWriter = null;
				if (rFile != null) {
					myWriter = new FileWriter(rFile);
					myWriter.write("// JELY R-peak indices @" + ecg.getSamplingRate() + "Hz\nRpeakIndex\n");
				}

				for (int i = 1; i < beatList.size(); i++) {
					Heartbeat heartbeat = beatList.get(i);
					QrsComplex qrs = heartbeat.getQrs();
					int rPeak = qrs.getRPosition();

					if (rOut)
						System.out.println(rPeak);

					if (myWriter != null)
						myWriter.append(rPeak + "\n");

				}

				if (myWriter != null)
					myWriter.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		try {
			System.out.println("Press any button to exit.");
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
