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
package jely.filter;

/**
 * A bandpass butterworth filter implementation using a frequency-coefficient table to accommodate for different sampling frequencies.
 *
 * @author Stefan Gradl
 */
public class BandpassButterworth05To10 {
    private static class Info {
        private double fs;
        private int delay;
        private double[] a;
        private double[] b;

        public Info(double fs, int delay, double[] a, double[] b) {
            this.fs = fs;
            this.delay = delay;
            this.a = a;
            this.b = b;
        }
    }

    private static final Info table[] = new Info[]
            {
                    new Info(
                            50,
                            3,
                            new double[]{1, -1.13759792163629058769913626747, 0.190760202218566710108405004576,},
                            new double[]{0.40461989889071670045694872897, 0, -0.40461989889071670045694872897,}),

                    new Info(
                            100,
                            3,
                            new double[]{1, -1.51393842446922777256190784101, 0.529472745182014925546809536172,},
                            new double[]{0.235263627408992592737746463172, 0, -0.235263627408992592737746463172,}),

                    new Info(
                            150,
                            3,
                            new double[]{1, -1.65700507205744096417276978173, 0.664398411513140163542345817405,},
                            new double[]{0.16780079424342986271767586004, 0, -0.16780079424342986271767586004,}),

                    new Info(
                            250,
                            3,
                            new double[]{1, -1.78295983896511689437147651915, 0.785792576664767317140558589017,},
                            new double[]{0.107103711667616355307508513306, 0, -0.107103711667616355307508513306,}),

                    new Info(
                            256,
                            3,
                            new double[]{1, -1.7876204070697678805146324521, 0.790328210879008064004835887317,},
                            new double[]{0.104835894560496009630945479785, 0, -0.104835894560496009630945479785,}),

                    new Info(
                            360,
                            3,
                            new double[]{1, -1.84515320011910888275963316119, 0.846562489443179355674828912015,},
                            new double[]{0.0767187552784103776737367752503, 0, -0.0767187552784103776737367752503,}),

                    new Info(
                            500,
                            4,
                            new double[]{1, -1.88647163718152399702887578314, 0.887217517811989098142078091769,},
                            new double[]{0.0563912410940055203178999931879, 0, -0.0563912410940055203178999931879,}),

                    new Info(
                            512,
                            4,
                            new double[]{1, -1.88900860564225947513250503107, 0.889720839312208133797810205579,},
                            new double[]{0.0551395803438959331010948972107, 0, -0.0551395803438959331010948972107,}),

                    new Info(
                            1000,
                            5,
                            new double[]{1, -1.94183113608078894429809224675, 0.942022859829615422100346222578,},
                            new double[]{0.0289885700851922681331451769893, 0, -0.0289885700851922681331451769893,}),

                    new Info(
                            1024,
                            5,
                            new double[]{1, -1.94316099881317860820217902074, 0.943343962832318139177800730977,},
                            new double[]{0.0283280185838408887777362110683, 0, -0.0283280185838408887777362110683,}),

                    new Info(
                            1500,
                            6,
                            new double[]{1, -1.96089173060017540883848141675, 0.960977759433874312300360998051,},
                            new double[]{0.0195111202830628507887134048815, 0, -0.0195111202830628507887134048815,}),

                    new Info(
                            2000,
                            7,
                            new double[]{1, -1.97054291074111542769742300152, 0.970591536547848687810358114803,},
                            new double[]{0.0147042317260756057878401392713, 0, -0.0147042317260756057878401392713,}),

                    new Info(
                            5000,
                            8,
                            new double[]{1, -1.98812479461294810967331159191, 0.988132643534076704483481989882,},
                            new double[]{0.00593367823296164255408857712837, 0, -0.00593367823296164255408857712837,}),
            };

    /**
     * Returns a newly created DigitalFilter object for a 8th order Butterworth bandpass filter with a passband of 8 Hz to 21 Hz.
     *
     * @param samplingFrequency
     * @return
     */
    public static DigitalFilter newEcgFilter(double samplingFrequency) {
        //int smallerIndex = 0;
        //int largerIndex = 0;
        int nearestIndex = -1;
        int nearestDistance = Integer.MAX_VALUE;

        // search for exact match in the lookup table
        for (int i = 0; i < table.length; i++) {
            int nd = (int) Math.abs(table[i].fs - samplingFrequency);
            if (nd < nearestDistance) {
                nearestDistance = nd;
                nearestIndex = i;
            }
            // store lower and upper matches for possible linear interpolation if exact match is not found
	    /*if (table[i].fs < samplingFrequency)
		smallerIndex = i;
	    else if (table[i].fs > samplingFrequency)
	    {
		largerIndex = i;
		break;
	    }*/

            if (table[i].fs == samplingFrequency) {
                return new DigitalFilter(table[i].b, table[i].a, table[i].delay);
            }
        }

        return new DigitalFilter(table[nearestIndex].b, table[nearestIndex].a, table[nearestIndex].delay);

        // linearly interpolate coefficients from the lookup table (this is only a very rough approximation!!!) TODO: are they stable??? probably not!
	/*Info lower = table[smallerIndex];
	Info upper = table[largerIndex];
	double factor = (samplingFrequency - lower.fs) / (upper.fs - lower.fs);
	
	double[] a = new double[lower.a.length];
	a[0] = table[smallerIndex].a[0];
	for (int i = 1; i < a.length; i++)
	{	    
	    a[i] = lower.a[i] + factor * (upper.a[i] - lower.a[i]);
	}
	
	double[] b = new double[lower.b.length];
	for (int i = 0; i < b.length; i++)
	{
	    b[i] = lower.b[i] + factor * (upper.b[i] - lower.b[i]);
	}
	
	int groupDelay = (int) (lower.delay + factor * (upper.delay - lower.delay));
		
	return new DigitalFilter( b, a, groupDelay );*/
    }
}
