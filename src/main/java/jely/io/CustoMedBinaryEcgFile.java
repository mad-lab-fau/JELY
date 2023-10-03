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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Loads ECG signals from CustoMeds binary ECG streaming file format.
 *
 * @author Stefan Gradl
 */
public class CustoMedBinaryEcgFile extends Ecg {
    public CustoMedBinaryEcgFile(String pathToBinFile) {
        /*
         * Binaryfile: channel1 = R, channel 2 = L, channel3 = F; channel4 = C1; channel5 = C2; channel6 = C3; channel 7
         * = C4; channel8 = C5; channel9 = C6; leadII = F-R;
         */

        try {
            File f = new File(pathToBinFile);

            samplingRate = 1000;
            date = null;
            leadInfo = new LeadConfiguration();
	    /*leadInfo.add( EcgLead.aVR );
	    leadInfo.add( EcgLead.aVL );
	    leadInfo.add( EcgLead.aVF );*/
            leadInfo.add(EcgLead.II);
	    /*leadInfo.add( EcgLead.V1 );
	    leadInfo.add( EcgLead.V2 );
	    leadInfo.add( EcgLead.V3 );
	    leadInfo.add( EcgLead.V4 );
	    leadInfo.add( EcgLead.V5 );
	    leadInfo.add( EcgLead.V6 );*/

            this.init((int) (f.length() / 36));

            DataInputStream file = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
            int data1 = 0;
            int data2 = 0;
            int data3 = 0;
            int data4 = 0;
            int numSamples = 0;
            while (data1 != -1) {
                data1 = file.read(); // R values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                int dataR = data4 << 24 | data3 << 16 | data2 << 8 | data1;

                data1 = file.read(); // L values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                int dataL = data4 << 24 | data3 << 16 | data2 << 8 | data1;

                data1 = file.read(); // F values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                int dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;

                getSignalFromLead(EcgLead.II).add((double) (dataF - dataR));

                //getSignalFromLead( EcgLead.aVR ).add( (double) dataR - dataF * 0.5 - dataL * 0.5 );
                //getSignalFromLead( EcgLead.aVF ).add( (double) dataF... );
                //getSignalFromLead( EcgLead.aVL ).add( (double) dataL... );

                data1 = file.read(); // C1 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V1 ).add( (double) dataF );

                data1 = file.read(); // C2 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V2 ).add( (double) dataF );

                data1 = file.read(); // C3 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V3 ).add( (double) dataF );

                data1 = file.read(); // C4 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V4 ).add( (double) dataF );

                data1 = file.read(); // C5 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V5 ).add( (double) dataF );

                data1 = file.read(); // C6 values
                data2 = file.read();
                data3 = file.read();
                data4 = file.read();
                //dataF = data4 << 24 | data3 << 16 | data2 << 8 | data1;
                //getSignalFromLead( EcgLead.V6 ).add( (double) dataF );

                numSamples++;
            }
            file.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
