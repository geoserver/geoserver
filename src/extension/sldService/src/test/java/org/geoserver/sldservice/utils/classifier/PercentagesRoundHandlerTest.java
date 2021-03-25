/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import org.junit.Test;

public class PercentagesRoundHandlerTest {

    @Test
    public void testPercentagesRoundedToOneArr() {
        PercentagesRoundHandler handler = new PercentagesRoundHandler();
        double[] percentages = {
            34.42846516241011,
            5.684354078849492,
            19.929333002727496,
            20.078105628564344,
            19.879742127448548
        };
        double[] rounded = handler.roundPercentages(percentages);
        for (double d : rounded) {
            testRounded(1, d);
        }
        testSumTo100(rounded);
    }

    @Test
    public void testPercentagesRoundedToFiveArr() {
        PercentagesRoundHandler handler = new PercentagesRoundHandler(5);
        double[] percentages = {
            34.42846516241011,
            5.684354078849492,
            19.929333002727496,
            20.078105628564344,
            19.879742127448548
        };
        double[] rounded = handler.roundPercentages(percentages);
        for (double d : rounded) {
            testRounded(5, d);
        }
        testSumTo100(rounded);
    }

    @Test
    public void testPercentagesRoundedToTwoList() {
        PercentagesRoundHandler handler = new PercentagesRoundHandler(2);
        List<Double> percentages =
                Arrays.asList(
                        34.42846516241011,
                        5.684354078849492,
                        19.929333002727496,
                        20.078105628564344,
                        19.879742127448548);
        List<Double> rounded = handler.roundPercentages(percentages);
        for (double d : rounded) {
            testRounded(2, d);
        }
        testSumTo100(rounded);
    }

    @Test
    public void testPercentagesRoundedToThreeList() {
        PercentagesRoundHandler handler = new PercentagesRoundHandler(3);
        List<Double> percentages =
                Arrays.asList(
                        34.42846516241011,
                        5.684354078849492,
                        19.929333002727496,
                        20.078105628564344,
                        19.879742127448548);
        List<Double> rounded = handler.roundPercentages(percentages);
        for (double d : rounded) {
            testRounded(3, d);
        }
        testSumTo100(rounded);
    }

    protected void testRounded(int scale, double percentage) {
        assertTrue(String.valueOf(percentage).split("\\.")[1].length() <= scale);
    }

    protected void testSumTo100(double[] rounded) {
        assertEquals(100.0, DoubleStream.of(rounded).sum(), 0.0);
    }

    protected void testSumTo100(List<Double> rounded) {
        assertEquals(100.0, rounded.stream().mapToDouble(d -> d).sum(), 0.0);
    }
}
