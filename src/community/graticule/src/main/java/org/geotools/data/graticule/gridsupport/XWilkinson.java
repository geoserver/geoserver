package org.geotools.data.graticule.gridsupport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Reference: [1] An Extension of Wilkinson's Algorithm for positioning Tick Labels on Axes (Justin
 * Talbot, Sharon Lin, Pat Hanrahan) Ahmet Engin Karahan, revised implementation 20.04.2015
 */
public class XWilkinson {

    private XWilkinson(double[] Q, double base, double[] w, double eps) {
        this.w = w;
        this.Q = Q;
        this.base = base;
        this.eps = eps;
    }

    private XWilkinson(double[] Q, double base) {
        this(Q, base, new double[] {0.25, 0.2, 0.5, 0.05}, 1e-10);
    }

    public static XWilkinson of(double[] Q, double base) {
        return new XWilkinson(Q, base);
    }

    public static XWilkinson base10() {
        return XWilkinson.of(new double[] {1, 5, 2, 2.5, 4, 3}, 10);
    }

    public static XWilkinson base2() {
        return XWilkinson.of(new double[] {1}, 2);
    }

    public static XWilkinson base16() {
        return XWilkinson.of(new double[] {1, 2, 4, 8}, 16);
    }

    // - Factory methods that may be useful for time-axis implementations
    public static XWilkinson forSeconds() {
        return XWilkinson.of(new double[] {1, 2, 3, 5, 10, 15, 20, 30}, 60);
    }

    public static XWilkinson forMinutes() {
        return XWilkinson.of(new double[] {1, 2, 3, 5, 10, 15, 20, 30}, 60);
    }

    public static XWilkinson forHours24() {
        return XWilkinson.of(new double[] {1, 2, 3, 4, 6, 8, 12}, 24);
    }

    public static XWilkinson forHours12() {
        return XWilkinson.of(new double[] {1, 2, 3, 4, 6}, 12);
    }

    public static XWilkinson forDays() {
        return XWilkinson.of(new double[] {1, 2}, 7);
    }

    public static XWilkinson forWeeks() {
        return XWilkinson.of(new double[] {1, 2, 4, 13, 26}, 52);
    }

    public static XWilkinson forMonths() {
        return XWilkinson.of(new double[] {1, 2, 3, 4, 6}, 12);
    }

    public static XWilkinson forYears() {
        return XWilkinson.of(new double[] {1, 2, 5}, 10);
    }

    // Loose flag
    public boolean loose = false;

    // scale-goodness weights for simplicity, coverage, density, legibility
    private final double w[];

    // calculation of scale-goodness
    private double w(double s, double c, double d, double l) {
        return w[0] * s + w[1] * c + w[2] * d + w[3] * l;
    }

    // Initial step sizes which we use as seed of generator
    private final double[] Q;

    // Number base used to calculate logarithms
    private final double base;

    private double logB(double a) {
        return Math.log(a) / Math.log(base);
    }

    /*
     * a mod b for float numbers (reminder of a/b)
     */
    private double flooredMod(double a, double n) {
        return a - n * Math.floor(a / n);
    }

    // can be injected via c'tor depending on your application, default is 1e-10
    private final double eps;

    private double v(double min, double max, double step) {
        return (flooredMod(min, step) < eps && min <= 0 && max >= 0) ? 1 : 0;
    }

    private double simplicity(int i, int j, double min, double max, double step) {
        if (Q.length > 1) {
            return 1 - (double) i / (Q.length - 1) - j + v(min, max, step);
        } else {
            return 1 - j + v(min, max, step);
        }
    }

    private double simplicity_max(int i, int j) {
        if (Q.length > 1) {
            return 1 - (double) i / (Q.length - 1) - j + 1.0;
        } else {
            return 1 - j + 1.0;
        }
    }

    private double coverage(double dmin, double dmax, double lmin, double lmax) {
        double a = dmax - lmax;
        double b = dmin - lmin;
        double c = 0.1 * (dmax - dmin);
        return 1 - 0.5 * ((a * a + b * b) / (c * c));
    }

    private double coverage_max(double dmin, double dmax, double span) {
        double range = dmax - dmin;
        if (span > range) {
            double half = (span - range) / 2;
            double r = 0.1 * range;
            return 1 - half * half / (r * r);
        } else {
            return 1.0;
        }
    }

    /*
     *
     * @param k		number of labels
     * @param m		number of desired labels
     * @param dmin	data range minimum
     * @param dmax	data range maximum
     * @param lmin	label range minimum
     * @param lmax	label range maximum
     * @return		density
     *
     * k-1 number of intervals between labels
     * m-1 number of intervals between desired number of labels
     * r   label interval length/label range
     * rt  desired label interval length/actual range
     */
    private double density(int k, int m, double dmin, double dmax, double lmin, double lmax) {
        double r = (k - 1) / (lmax - lmin);
        double rt = (m - 1) / (Math.max(lmax, dmax) - Math.min(lmin, dmin));
        return 2 - Math.max(r / rt, rt / r); // return 1-Math.max(r/rt, rt/r); (paper is wrong)
    }

    private double density_max(int k, int m) {
        if (k >= m) {
            return 2 - (k - 1) / (m - 1); // return 2-(k-1)/(m-1); (paper is wrong)
        } else {
            return 1;
        }
    }

    private double legibility(double min, double max, double step) {
        return 1; // Maybe later more...
    }

    public class Label implements Iterable<Double> {

        private double min, max, step, score;

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("00.00");
            String s = "(Score: " + df.format(score) + ") ";
            for (double x = min; x <= max; x = x + step) {
                s += df.format(x) + "\t";
            }
            return s;
        }

        @Override
        public Iterator<Double> iterator() {
            return getList().iterator();
        }

        public List<Double> getList() {
            List<Double> list = new ArrayList<>();
            for (double i = min; i < max; i += step) {
                list.add(i);
            }
            return list;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        public double getStep() {
            return step;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * @param dmin data range min
     * @param dmax data range max
     * @param m desired number of labels
     * @return XWilkinson.Label
     */
    public Label search(double dmin, double dmax, int m) {
        Label best = new Label();
        double bestScore = -2;
        double sm, dm, cm, delta;
        int j = 1;

        main_loop:
        while (j < Integer.MAX_VALUE) {
            for (int _i = 0; _i < Q.length; _i++) {
                int i = _i + 1;
                double q = Q[_i];
                sm = simplicity_max(i, j);
                if (w(sm, 1, 1, 1) < bestScore) {
                    break main_loop;
                }
                int k = 2;
                while (k < Integer.MAX_VALUE) {
                    dm = density_max(k, m);
                    if (w(sm, 1, dm, 1) < bestScore) {
                        break;
                    }
                    delta = (dmax - dmin) / (k + 1) / (j * q);
                    int z = (int) Math.ceil(logB(delta));
                    while (z < Integer.MAX_VALUE) {
                        double step = j * q * Math.pow(base, z);
                        cm = coverage_max(dmin, dmax, step * (k - 1));
                        if (w(sm, cm, dm, 1) < bestScore) {
                            break;
                        }
                        int min_start = (int) (Math.floor(dmax / step - (k - 1)) * j);
                        int max_start = (int) (Math.ceil(dmin / step)) * j;

                        for (int start = min_start; start <= max_start; start++) {
                            double lmin = start * step / j;
                            double lmax = lmin + step * (k - 1);
                            double c = coverage(dmin, dmax, lmin, lmax);
                            double s = simplicity(i, j, lmin, lmax, step);
                            double d = density(k, m, dmin, dmax, lmin, lmax);
                            double l = legibility(lmin, lmax, step);
                            double score = w(s, c, d, l);

                            // later legibility logic can be implemented hier

                            if (score > bestScore && (!loose || (lmin <= dmin && lmax >= dmax))) {
                                best.min = lmin;
                                best.max = lmax;
                                best.step = step;
                                best.score = score;
                                bestScore = score;
                            }
                        }
                        z = z + 1;
                    }
                    k = k + 1;
                }
            }
            j = j + 1;
        }
        return best;
    }

    // Demo for usage
    public static void main(String[] args) {

        XWilkinson x = XWilkinson.base10();

        // First examples taken from the paper pg 6, Fig 4
        x.loose = true;
        System.out.println(x.search(-98.0, 18.0, 3).toString());
        x.loose = false;
        System.out.println(x.search(-98.0, 18.0, 3).toString());

        System.out.println();

        x.loose = true;
        System.out.println(x.search(-1.0, 200.0, 3).toString());
        x.loose = false;
        System.out.println(x.search(-1.0, 200.0, 3).toString());

        System.out.println();

        x.loose = true;
        System.out.println(x.search(119.0, 178.0, 3).toString());
        x.loose = false;
        System.out.println(x.search(119.0, 178.0, 3).toString());

        System.out.println();

        x.loose = true;
        System.out.println(x.search(-31.0, 27.0, 4).toString());
        x.loose = false;
        System.out.println(x.search(-31.0, 27.0, 3).toString());

        System.out.println();

        x.loose = true;
        System.out.println(x.search(-55.45, -49.99, 2).toString());
        x.loose = false;
        System.out.println(x.search(-55.45, -49.99, 3).toString());

        System.out.println();
        x.loose = false;
        System.out.println(x.search(0, 100, 2).toString());
        System.out.println(x.search(0, 100, 3).toString());
        System.out.println(x.search(0, 100, 4).toString());
        System.out.println(x.search(0, 100, 5).toString());
        System.out.println(x.search(0, 100, 6).toString());
        System.out.println(x.search(0, 100, 7).toString());
        System.out.println(x.search(0, 100, 8).toString());
        System.out.println(x.search(0, 100, 9).toString());
        System.out.println(x.search(0, 100, 10).toString());

        System.out.println("Some additional tests: Testing with base2");
        x = XWilkinson.base2();
        System.out.println(x.search(0, 32, 8).toString());

        System.out.println("Quick experiment with minutes: Check the logic");
        x = XWilkinson.forMinutes();
        System.out.println(x.search(0, 240, 16));
        System.out.println(x.search(0, 240, 9));

        System.out.println("test for long");
        x = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 10);
        x.loose = false;
        System.out.println(x.search(-180, 180, 10));

        System.out.println(x.search(-180, 180, 20));
        System.out.println(x.search(-180, 180, 5));
        System.out.println(x.search(-180, 180, 6));
        x = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 36);
        x.loose = false;
        System.out.println(x.search(-180, 180, 10));

        System.out.println(x.search(-180, 180, 20));
        System.out.println(x.search(-180, 180, 5));
        System.out.println(x.search(-180, 180, 6));

        System.out.println("test for lat");
        x = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 10);
        x.loose = false;
        System.out.println(x.search(-90, 90, 10));

        System.out.println(x.search(-90, 90, 20));
        System.out.println(x.search(-90, 90, 5));
        System.out.println(x.search(-90, 90, 6));
        x = XWilkinson.of(new double[] {1, 2, 5, 10, 20}, 36);
        x.loose = false;
        System.out.println(x.search(-90, 90, 10));

        System.out.println(x.search(-90, 90, 20));
        System.out.println(x.search(-90, 90, 5));
        System.out.println(x.search(-90, 90, 6));
    }
}
