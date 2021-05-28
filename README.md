# JELY - Java Ecg LibrarY
Java-based library providing classes for the analysis of ECG signals (Filters tuned for the ECG, QRS detectors, beat classifiers, ...).

Some of the contents
- R-Peak detectors: Elgendi, Pan-Tompkins
- QRS Classifiers: Gradl et al., Leutheuser et al., Tsipouras et al., naive physiological classifier
- Loading methods for text-based or binary ECG file formats like PhysioNET files, CustoMeds binary ECG file or CSV.

## Required additional library
The ShiftList package is required to compile the project. You can find it at https://github.com/mad-lab-fau/ShiftList

## Usage Examples
This is a very basic example of how to load an ECG from a file and detect all R-peak locations
```java
// Load BinaryEcg file, the proprietary file format of the JELY...
Ecg ecgFile = FileLoader.LoadKnownEcgFile('some_random_ecg.BinaryEcg');

// ... or load csv-based ECG, e.g. single column just voltage values, with a known sampling rate of 1024 Hz
ecgFile = FileLoader.LoadKnownEcgFile('some_random_csv_ecg.csv', LeadConfiguration.SINGLE_UNKNOWN_LEAD, 1024);

// Initiate the default heartbeat detector
HeartbeatDetector detector = new HeartbeatDetector( ecgFile, null );
// and extract all heartbeats
ArrayList<Heartbeat> beatList = detector.findHeartbeats();

for(int i=1; i<beatList.size(); i++) {
    Heartbeat heartbeat = beatList.get(i);
    QrsComplex qrs = heartbeat.getQrs();
    int rPeak = qrs.getRPosition();
}
```

If you want to extract an array of signal values, you can call
```java
double[] signal = ecgFile.getSignalFromIndex(0).toDoubleArray();
```

You can also run the compiled jar directly with java from the command-line e.g. like this
```bash
java -jar Jely.jar --ecgfile test_ecg.csv --samplingrate 500 --rout --rfile rpeaks.csv
```
This will read the CSV file test_ecg with a single lead ECG sampled with 500 Hz and find all R-peaks using the default QRS detector and write the indices of the R-peaks to stdout as well as into the file rpeaks.csv.

# EcgEditor
The EcgEditor is a subproject of the JELY. It used this library to provide a graphical user interface (GUI) for viewing and annotating ECG signals.

# Trivia
During development, this library was originally called "EcgLib" (ecglib) but was later renamed to JELY (Java Ecg LibrarY) to avoid confusion with similar projects on the Internet.
Development was originally started at the "Lehrstuhl für Mustererkennung" at the FAU, which is why there might be remnants of the package name "de.fau.lme.ecglib".

# Authors
Original author: Stefan Gradl
Additional work by: Axel Heinrich and Heike Leutheuser
See the individual code files.

# Publications
The following publications made use of the JELY, or contributed to it:

Gradl, Stefan, Patrick Kugler, Clemens Lohmüller, and Bjoern M Eskofier. 2012. “Real-Time ECG Monitoring and Arrhythmia Detection Using Android-Based Mobile Devices.” In Conf Proc IEEE Eng Med Biol Soc, 2452–55. San Diego, CA, USA: IEEE. https://doi.org/10.1109/EMBC.2012.6346460.

Leutheuser, Heike, Stefan Gradl, Patrick Kugler, Lars Anneken, Martin Arnold, Stephan Achenbach, and Bjoern M. Eskofier. 2014. “Comparison of Real-Time Classification Systems for Arrhythmia Detection on Android-Based Mobile Devices.” In 2014 36th Annual International Conference of the IEEE Engineering in Medicine and Biology Society, 2690–93. Chicago, IL: IEEE. https://doi.org/10.1109/EMBC.2014.6944177.

Gradl, Stefan, Heike Leutheuser, Mohamed Elgendi, Nadine Lang, and Bjoern M. Eskofier. 2015. “Temporal Correction of Detected R-Peaks in ECG Signals: A Crucial Step to Improve QRS Detection Algorithms.” In 2015 37th Annual International Conference of the IEEE Engineering in Medicine and Biology Society (EMBC), 522–25. Milan: IEEE. https://doi.org/10.1109/EMBC.2015.7318414.

Leutheuser, Heike, Stefan Gradl, Bjoern M. Eskofier, Andreas Tobola, Nadine Lang, Lars Anneken, Martin Arnold, and Stephan Achenbach. 2015. “Arrhythmia Classification Using RR Intervals: Improvement with Sinusoidal Regression Feature.” In 2015 IEEE 12th International Conference on Wearable and Implantable Body Sensor Networks (BSN), 1–5. Cambridge, MA, USA: IEEE. https://doi.org/10.1109/BSN.2015.7299371.

Leutheuser, Heike, Stefan Gradl, Lars Anneken, Martin Arnold, Nadine Lang, Stephan Achenbach, and Bjoern M. Eskofier. 2016. “Instantaneous P- and T-Wave Detection: Assessment of Three ECG Fiducial Points Detection Algorithms.” In 2016 IEEE 13th International Conference on Wearable and Implantable Body Sensor Networks (BSN), 329–34. San Francisco, CA, USA: IEEE. https://doi.org/10.1109/BSN.2016.7516283.

Heinrich, Axel. 2016. “Automated Fitness Level Determination.” Master’s Thesis, Erlangen: Friedrich-Alexander-Universität Erlangen-Nürnberg (FAU).

Leutheuser, Heike, Stefan Gradl, Lars Anneken, Martin Arnold, Nadine Lang, Stephan Achenbach, and Bjoern M. Eskofier. 2016. “Performance Evaluation of Real-Time Implementation for P- and T-Wave Detection.” In . San Francisco.

Gradl, Stefan, Tobias Cibis, Jasmine Lauber, Robert Richer, Ruslan Rybalko, Norman Pfeiffer, Heike Leutheuser, Markus Wirth, Vinzenz von Tscharner, and Bjoern M Eskofier. 2017. “Wearable Current-Based ECG Monitoring System with Non-Insulated Electrodes for Underwater Application.” Applied Sciences 7 (12): 1277. https://doi.org/10.3390/app7121277.

Leutheuser, Heike, Nadine R. Lang, Stefan Gradl, Matthias Struck, Andreas Tobola, Christian Hofmann, Lars Anneken, and Bjoern M. Eskofier. 2017. “Textile Integrated Wearable Technologies for Sports and Medical Applications.” In Smart Textiles, edited by Stefan Schneegass and Oliver Amft, 359–82. Cham: Springer International Publishing. https://doi.org/10.1007/978-3-319-50124-6_16.

