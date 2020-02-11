package de.fau.mad.jely;

import de.fau.mad.jely.QuickSort;

public class LowessSmoothing {
    /*-
     * Copyright © 2009 Diamond Light Source Ltd., Science and Technology
     * Facilities Council Daresbury Laboratory
     *
     * This file is part of GDA.
     *
     * GDA is free software: you can redistribute it and/or modify it under the
     * terms of the GNU General Public License version 3 as published by the Free
     * Software Foundation.
     *
     * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
     * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
     * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
     * details.
     *
     * You should have received a copy of the GNU General Public License along
     * with GDA. If not, see <http://www.gnu.org/licenses/>.
     */


    /**
     * <p>
     * COMPUTER PROGRAMS FOR LOCALLY WEIGHTED REGRESSION
     * <p>
     * Original fortran source documentation
     * <p>
     * This package consists of two FORTRAN programs for smoothing scatterplots by robust locally weighted regression, or
     * lowess. The principal routine is LOWESS which computes the smoothed values using the method described in The Elements
     * of Graphing Data, by William S. Cleveland (Wadsworth, 555 Morego Street, Monterey, California 93940). LOWESS calls a
     * support routine, LOWEST, the code for which is included. To reduce the computations, LOWESS requires that the arrays
     * X and Y, which are the horizontal and vertical coordinates, respectively, of the scatterplot, be such that X is
     * sorted from smallest to largest. To summarize the scatterplot, YS, the fitted values, should be plotted against X.
     * <p>
     * The following are data and output from LOWESS that can be used to check your implementation of the routines. The
     * notation (10)v means 10 values of v. *
     * <p>
     * X values:
     * <p>
     * 1 2 3 4 5 (10)6 8 10 12 14 50
     * <p>
     * Y values:
     * <p>
     * 18 2 15 6 10 4 16 11 7 3 14 17 20 12 9 13 1 8 5 19
     * <p>
     * YS values with F = .25, NSTEPS = 0, DELTA = 0.0
     * <p>
     * 13.659 11.145 8.701 9.722 10.000 (10)11.300 13.000 6.440 5.596 5.456 18.998
     * <p>
     * YS values with F = .25, NSTEPS = 0 , DELTA = 3.0
     * <p>
     * 13.659 12.347 11.034 9.722 10.511 (10)11.300 13.000 6.440 5.596 5.456 18.998
     * <p>
     * YS values with F = .25, NSTEPS = 2, DELTA = 0.0
     * <p>
     * 14.811 12.115 8.984 9.676 10.000 (10)11.346 13.000 6.734 5.744 5.415 18.998
     * <p>
     * LOWESS
     * <p>
     * Calling sequence CALL LOWESS(X,Y,N,F,NSTEPS,DELTA,YS,RW,RES)
     * <p>
     * Purpose
     * <p>
     * LOWESS computes the smooth of a scatterplot of Y against X using robust locally weighted regression. Fitted values,
     * YS, are computed at each of the values of the horizontal axis in X.
     * <p>
     * Argument description
     * <p>
     * X = Input; abscissas of the points on the scatterplot; the values in X must be ordered from smallest to largest.
     * <p>
     * Y = Input; ordinates of the points on the scatterplot.
     * <p>
     * N = Input; dimension of X,Y,YS,RW, and RES.
     * <p>
     * F = Input; specifies the amount of smoothing; F is the fraction of points used to compute each fitted value; as F
     * increases the smoothed values become smoother; choosing F in the range .2 to .8 usually results in a good fit; if you
     * have no idea which value to use, try F = .5.
     * <p>
     * NSTEPS = Input; the number of iterations in the robust fit; if NSTEPS = 0, the nonrobust fit is returned; setting
     * NSTEPS equal to 2 should serve most purposes.
     * <p>
     * DELTA = input; nonnegative parameter which may be used to save computations; if N is less than 100, set DELTA equal
     * to 0.0; if N is greater than 100 you should find out how DELTA works by reading the additional instructions section.
     * <p>
     * YS = Output; fitted values; YS(I) is the fitted value at X(I); to summarize the scatterplot, YS(I) should be plotted
     * against X(I).
     * <p>
     * RW = Output; robustness weights; RW(I) is the weight given to the point (X(I),Y(I)); if NSTEPS = 0, RW is not used.
     * <p>
     * RES = Output; residuals; RES(I) = Y(I)-YS(I).
     * <p>
     * Additional instructions
     * <p>
     * DELTA can be used to save computations. Very roughly the algorithm is this: on the initial fit and on each of the
     * NSTEPS iterations locally weighted regression fitted values are computed at points in X which are spaced, roughly,
     * DELTA apart; then the fitted values at the remaining points are computed using linear interpolation. The first
     * locally weighted regression (l.w.r.) computation is carried out at X(1) and the last is carried out at X(N). Suppose
     * the l.w.r. computation is carried out at X(I). If X(I+1) is greater than or equal to X(I)+DELTA, the next l.w.r.
     * computation is carried out at X(I+1). If X(I+1) is less than X(I)+DELTA, the next l.w.r. computation is carried out
     * at the largest X(J) which is greater than or equal to X(I) but is not greater than X(I)+DELTA. Then the fitted values
     * for X(K) between X(I) and X(J), if there are any, are computed by linear interpolation of the fitted values at X(I)
     * and X(J). If N is less than 100 then DELTA can be set to 0.0 since the computation time will not be too great. For
     * larger N it is typically not necessary to carry out the l.w.r. computation for all points, so that much computation
     * time can be saved by taking DELTA to be greater than 0.0. If DELTA = Range (X)/k then, if the values in X were
     * uniformly scattered over the range, the full l.w.r. computation would be carried out at approximately k points.
     * Taking k to be 50 often works well.
     * <p>
     * <p>
     * Method
     * <p>
     * <p>
     * The fitted values are computed by using the nearest neighbor routine and robust locally weighted regression of degree
     * 1 with the tricube weight function. A few additional features have been added. Suppose r is FN truncated to an
     * integer. Let h be the distance to the r-th nearest neighbor from X(I). All points within h of X(I) are used. Thus if
     * the r-th nearest neighbor is exactly the same distance as other points, more than r points can possibly be used for
     * the smooth at X(I). There are two cases where robust locally weighted regression of degree 0 is actually used at
     * X(I). One case occurs when h is 0.0. The second case occurs when the weighted standard error of the X(I) with respect
     * to the weights w(j) is less than .001 times the range of the X(I), where w(j) is the weight assigned to the j-th
     * point of X (the tricube weight times the robustness weight) divided by the sum of all of the weights. Finally, if the
     * w(j) are all zero for the smooth at X(I), the fitted value is taken to be Y(I).
     * <p>
     * <p>
     * http://www.netlib.org/ search "lowess"
     * <p>
     * Java conversion by Paul Quinn
     * <p>
     * lowess was introduced into the GDA as it handles non-equally spaced data while being more effective than "averaging"
     * based smoothing.
     * <p>
     */

    private static double ycurrent = 0.0;

    /**
     * See class doc
     *
     * @param x
     * @param y
     * @param n
     * @param xs
     * @param nleft
     * @param nright
     * @param w
     * @param userw
     * @param rw
     * @return boolean
     */
    static boolean lowest(double[] x, double[] y, int n, double xs, int nleft, int nright, double[] w, boolean userw,
                          double[] rw) {
        int nrt, j;
        double a, b, c, h, h1, h9, r, range;
        boolean ok;

        range = x[n - 1] - x[0];
        h = Math.max(xs - x[nleft], x[nright] - xs);
        h9 = 0.999 * h;
        h1 = 0.001 * h;
        /* sum of weights */
        a = 0.0;
        for (j = nleft; j < n; j++) {
            /* compute weights */
            /* (pick up all ties on right) */
            w[j] = 0.;
            r = Math.abs(x[j] - xs);
            if (r <= h9) {
                if (r <= h1) {
                    w[j] = 1.;
                } else {
                    w[j] = cube(1. - cube(r / h));
                }

                if (userw) {
                    w[j] *= rw[j];
                }
                a += w[j];
            } else if (x[j] > xs) {
                break;
            }
        }

        /* rightmost pt (may be greater */
        /* than nright because of ties) */

        nrt = j - 1;
        // nrt = j;

        if (a <= 0.) {
            ok = false;
        } else {
            ok = true;

            /* weighted least squares */
            /* make sum of w[j] == 1 */

            for (j = nleft; j <= nrt; j++) {
                w[j] /= a;
            }
            if (h > 0.) {
                a = 0.0;

                /* use linear fit */
                /* weighted center of x values */

                for (j = nleft; j <= nrt; j++) {
                    a += w[j] * x[j];
                }
                b = xs - a;
                c = 0.;
                for (j = nleft; j <= nrt; j++) {
                    c += w[j] * square(x[j] - a);
                }
                if (Math.sqrt(c) > 0.001 * range) {
                    b /= c;

                    /* points are spread out */
                    /* enough to compute slope */

                    for (j = nleft; j <= nrt; j++) {
                        w[j] *= (b * (x[j] - a) + 1.);
                    }
                }
            }
            ycurrent = 0.0;
            for (j = nleft; j <= nrt; j++) {
                ycurrent += w[j] * y[j];
            }

        }
        return ok;
    }

    /**
     * See class doc
     *
     * @param x
     * @param y
     * @param f
     * @param nsteps
     * @param delta
     * @param ys
     * @param rw
     * @param res
     */
    public static void lowess(double[] x, double[] y, int f, int nsteps, double delta, double[] ys, double[] rw,
                              double[] res) {
        int i, iter, j, last, m1, m2, nleft, nright, ns, n;
        boolean ok;
        double alpha, c1, c9, cmad, cut, d1, d2, denom, r, sc;
        n = y.length;
        if (n < 2) {
            ys[0] = y[0];
            return;
        }

        /* nleft, nright, last, etc. must all be shifted to get rid of these: */

        /* at least two, at most n points */
        ns = Math.max(2, Math.min(n, f));

        /* robustness iterations */

        for (iter = 0; iter <= nsteps; iter++) {
            nleft = 0;
            nright = ns - 1;
            last = -1; /* index of prev estimated point */
            i = 0; /* index of current point */

            for (; ; ) {
                if (nright < n - 1) {

                    /* move nleft, nright to right */
                    /* if radius decreases */

                    d1 = x[i] - x[nleft];
                    d2 = x[nright + 1] - x[i];

                    /* if d1 <= d2 with */
                    /* x[nright+1] == x[nright], */
                    /* lowest fixes */

                    if (d1 > d2) {

                        /* radius will not */
                        /* decrease by */
                        /* move right */

                        nleft++;
                        nright++;
                        continue;
                    }
                }

                /* fitted value at x[i] */

                ok = lowest(x, y, n, x[i], nleft, nright, res, iter > 0, rw);
                ys[i] = ycurrent;

                if (!ok) {
                    ys[i] = y[i];
                }
                /* all weights zero */
                /* copy over value (all rw==0) */

                if (last < i - 1) {
                    denom = x[i] - x[last];

                    /* skipped points -- interpolate */
                    /* non-zero - proof? */

                    for (j = last + 1; j < i; j++) {
                        alpha = (x[j] - x[last]) / denom;
                        ys[j] = alpha * ys[i] + (1. - alpha) * ys[last];
                    }
                }

                /* last point actually estimated */
                last = i;

                /* x coord of close points */
                cut = x[last] + delta;
                for (i = last + 1; i < n; i++) {
                    if (x[i] > cut)
                        break;
                    if (x[i] == x[last]) {
                        ys[i] = ys[last];
                        last = i;
                    }
                }
                i = Math.max(last + 1, i - 1);
                if (last >= n - 1)
                    break;
            }
            /* residuals */
            for (i = 0; i < n; i++) {
                res[i] = y[i] - ys[i];
            }

            /* overall scale estimate */
            sc = 0.;
            for (i = 0; i < n; i++) {
                sc += Math.abs(res[i]);
            }
            sc /= n;

            /* compute robustness weights */
            /*
             * Note: The following code, biweight_{6 MAD|Ri|} is also used in stl(), loess and several other places. -->
             * should provide API here (MM)
             */
            for (i = 0; i < n; i++) {
                rw[i] = Math.abs(res[i]);
            }

            QuickSort.quicksort(rw, 0, rw.length - 1);
            /* Compute cmad := 6 * median(rw[], n) ---- */
            m1 = n / 2;
            m2 = n - m1 - 1;

            if (n % 2 == 0) {
                cmad = 3. * (rw[m1] + rw[m2]);
            } else { /* n odd */
                cmad = 6. * rw[m1];
            }
            /* effectively zero */
            if (cmad < 1.0E-7 * sc) {
                break;
            }
            c9 = 0.999 * cmad;
            c1 = 0.001 * cmad;
            for (i = 0; i < n; i++) {
                r = Math.abs(res[i]);
                if (r <= c1) {
                    rw[i] = 1.;
                } else if (r <= c9) {
                    rw[i] = square(1. - square(r / cmad));
                } else {
                    rw[i] = 0.;
                }
            }
        }
    }

    /**
     * convenient method used to provide x cubed
     *
     * @param x
     * @return x**3
     */
    private static double cube(double x) {
        return x * x * x;
    }

    /**
     * convenient method to provide x squared
     *
     * @param x
     * @return x**2
     */
    private static double square(double x) {
        return x * x;
    }

    //
    // lowess test....
    //
    /**
     * Test main method.
     *
     * @param args
     */

}

