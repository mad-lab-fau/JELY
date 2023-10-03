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
package jely.util;

/**
 * @author Axel Heinrich
 */
public class SplineInterpolator {
    double [] x;
    double [] y;
    int t;
    int n;

    public SplineInterpolator(double [] x, double [] y){
        this.x = x;
        this.y = y;
        n = x.length;
    }

    public double interpolate(double t) {

        double[] a = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];
        double[] d = new double[n];

        double[] h = new double[n - 1];
        double[] sumh = new double[n - 2];
        double[] dy = new double[n - 1];
        double[][] rs = new double[n][1];

        Matrix cMatrix = new Matrix(n, n);

        for (int i = 0; i < n - 1; i++) {
            h[i] = x[i + 1] - x[i];
            dy[i] = y[i + 1] - y[i];
            a[i] = y[i];
        }

        for (int i = 0; i < n - 2; i++) {
            sumh[i] = h[i] + h[i + 1];
        }

        cMatrix = cMatrix.createTridiagonalMatrix(n, n, sumh, h);

        rs[0][0] = 0;
        rs[n - 1][0] = 0;
        for (int i = 1; i < n - 1; i++) {
            rs[i][0] = (3 * dy[i] / h[i]) - (3 * dy[i - 1] / h[i - 1]);
        }

        Matrix rsMat = new Matrix(rs);
        Matrix cSolution = cMatrix.solve(rsMat);
        c = cSolution.getRowPackedCopy();

        for (int i = 0; i < n-1; i++) {
            d[i] = (c[i + 1] - c[i]) / (3 * h[i]);
            b[i] = dy[i] / h[i] - c[i] * h[i] - d[i] * Math.pow(h[i], 2);
        }

        d[n - 1] = d[n - 2];
        b[n-1] = ((c[n-1]+c[n-2])*h[n-2]) + b[n-2];

        double interpolatedValue = evalSpline(b, c, d, t, h);

        return interpolatedValue;
    }

    public double evalSpline(double[] b, double[] c, double[] d, double eval_x, double[] h) {

        double S_j = 0;
        int j = 0;

        for (int i = 0; i < n; i++) {
            if (x[i] > eval_x) {
                j = i - 1;
                break;
            }
        }

        S_j = y[j] + b[j] * (eval_x - x[j]) + c[j] * Math.pow(eval_x - x[j], 2) + d[j] * Math.pow(eval_x - x[j], 3);

        return S_j;
    }
}
