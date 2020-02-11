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
package de.fau.lme.ecglib.filter;

/**
 * A bandpass butterworth filter implementation using a frequency-coefficient table to accommodate for different sampling frequencies.
 *
 * @author Stefan Gradl
 */
public class BandpassButterworth8To21 {
    private static class Info {
        private double fs;
        private int delay;
        /**
         * wrong delay ignore it!
         **/
        private double[] a;
        private double[] b;

        public Info(double fs, int delay, double[] a, double[] b) {
            this.fs = fs;
            this.delay = delay;
            this.a = a;
            this.b = b;
        }
    }
    
    /*
     * This lookup table was created in MATLAB using the following script:
     * @formatter:off
     * 
     *
=======================================================================
%% Create filter taps for lookup tables in Java
% array of desired sampling frequencies
fsArray = [50, 100, 150, 250, 256, 360, 500, 512, 1000, 1024, 1500, 2000, 5000];

%filterOrder = 8;
filterOrder = 2;
% fc1 = 8;
fc1 = 0.5;
% fc2 = 21;
fc2 = 10;

% loop over all desired frequencies
for fs = fsArray
    
    % create the filter
    h  = fdesign.bandpass('N,F3dB1,F3dB2', filterOrder, fc1, fc2, fs);
    Hd = design(h, 'butter');    
    [B,A] = sos2tf(Hd.sosMatrix,Hd.ScaleValues);
    delay = round(max(grpdelay(Hd, 16, fs)));
    
    fprintf('new Info(\n\t%d,\n\t%d,', fs, delay);    
    fprintf('\n\tnew double[]{ ');
    for i = 1:length(A)
        fprintf('%.30g, ', A(i));
    end
    fprintf(' },\n');

    fprintf('\tnew double[]{ ');
    for i = 1:length(B)
        fprintf('%.30g, ', B(i));
    end
    fprintf(' } ),\n\n');
end
=======================================================================
     * @formatter:on
     */

    private static final Info table[] = new Info[]
            {
                    new Info(
                            50,
                            8,
                            new double[]{1, 1.3964145751800054, 0.63898620577291521, 0.40358400550939044, 0.70256156492819177, 0.36148240047573116, 0.033590263748935326, 0.014401256143693385, 0.01813707703045082},
                            new double[]{0.10631234573760737, 0, -0.42524938295042947, 0, 0.63787407442564414, 0, -0.42524938295042947, 0, 0.10631234573760737}),

                    new Info(
                            100,
                            11,
                            new double[]{1, -3.9325918942457712, 7.820577143749599, -10.01946782336077, 9.0292961776830829, -5.7919816802141826, 2.5900817568481456, -0.74135144349204296, 0.10999905804811899},
                            new double[]{0.011623323621399621, 0, -0.046493294485598483, 0, 0.069739941728397731, 0, -0.046493294485598483, 0, 0.01162332362139962}),

                    new Info(
                            150,
                            18,
                            new double[]{1, -5.6138173596258669, 14.525164962755026, -22.558600512650223, 22.974752553387852, -15.701743459326291, 7.0358598635596135, -1.8938965140292927, 0.23595365871301038},
                            new double[]{0.0029518766694281938, 0, -0.011807506677712775, 0, 0.017711260016569162, 0, -0.011807506677712775, 0, 0.0029518766694281938}),

                    new Info(
                            250,
                            23,
                            new double[]{1, -6.768695088963730, 20.393559630973179, -35.713323258643484, 39.754003863987926, -28.803378875578726, 13.267224205160016, -3.553202340681965, 0.423894454616578},
                            new double[]{0.000480609295774, 0, -0.001922437183096, 0, 0.002883655774644, 0, -0.001922437183096, 0, 0.000480609295774}),

                    new Info(
                            360,
                            28,
                            new double[]{1, -7.2181515677673085, 22.981555065165807, -42.152402910366092, 48.71524912755514, -36.32559550424395, 17.068159164013458, -4.6207035114467967, 0.55189523323038281},
                            new double[]{0.00012518901340446951, 0, -0.00050075605361787803, 0, 0.00075113408042681699, 0, -0.00050075605361787803, 0, 0.00012518901340446951}),

                    new Info(
                            500,
                            29,
                            new double[]{1, -7.472880290474369, 24.534690499191818, -46.222917344103479, 54.655756569497662, -41.535509733279113, 19.811429156879875, -5.4227415813655391, 0.65217312371193981},
                            new double[]{3.6269682595201704e-05, 0, -0.00014507873038080682, 0, 0.00021761809557121022, 0, -0.00014507873038080682, 0, 3.6269682595201704e-05}),

                    new Info(
                            512,
                            30,
                            new double[]{1, -7.487397502980866, 24.625277681698513, -46.465532872107872, 55.017138576889877, -41.858676375374820, 19.984827859628609, -5.474393287464419, 0.658756252296786},
                            new double[]{3.313945290774877e-05, 0, -1.325578116309951e-04, 0, 1.988367174464926e-04, 0, -1.325578116309951e-04, 0, 3.313945290774877e-05}),

                    new Info(
                            1000,
                            32,
                            new double[]{1, -7.760763047765094, 26.377989121939255, -51.28563913108195, 62.385705466915404, -48.619466381587856, 23.706746984898317, -6.6123143716104416, 0.80774136003144736},
                            new double[]{2.505966646042068e-06, 0, -1.0023866584168272e-05, 0, 1.5035799876252409e-05, 0, -1.0023866584168272e-05, 0, 2.505966646042068e-06}),

                    new Info(
                            1024,
                            33,
                            new double[]{1, -7.7669440727849395, 26.418740887841114, -51.400791706568832, 62.566469177271571, -48.789699422193522, 23.802914350487345, -6.6424853748700112, 0.81179616225940032},
                            new double[]{2.284641273602989e-06, 0, -9.138565094411956e-06, 0, 1.3707847641617933e-05, 0, -9.138565094411956e-06, 0, 2.284641273602989e-06}),

                    new Info(
                            1500,
                            48,
                            new double[]{1, -7.8461314566401725, 26.945815903524259, -52.90433490124142, 64.949161397789325, -51.055008504560504, 25.094928365626778, -7.0517775235102107, 0.86734671908225425},
                            new double[]{5.1231087458339681e-07, 0, -2.0492434983335872e-06, 0, 3.0738652475003806e-06, 0, -2.0492434983335872e-06, 0, 5.1231087458339681e-07}),

                    new Info(
                            2000,
                            64,
                            new double[]{1, -7.8867377878334377, 27.219903493085514, -53.697180527010531, 66.223211886561899, -52.283288350644725, 25.805340184678819, -7.2800166718074797, 0.89876777297710408},
                            new double[]{1.6493702203055305e-07, 0, -6.597480881222122e-07, 0, 9.8962213218331831e-07, 0, -6.597480881222122e-07, 0, 1.6493702203055305e-07}),

                    new Info(
                            5000,
                            161,
                            new double[]{1, -7.9562559512902595, 27.69579012440817, -55.093319420906795, 68.498704519811838, -54.508468050990416, 27.110894354279736, -7.7055546177179952, 0.95820904240572902},
                            new double[]{4.3577433586805567e-09, 0, -1.7430973434722227e-08, 0, 2.6146460152083339e-08, 0, -1.7430973434722227e-08, 0, 4.3577433586805567e-09}),
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
