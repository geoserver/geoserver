/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import org.junit.Test;

/**
 * Test rounding
 *
 * @author Dean Povey
 */
public class RoundingUtilTest {

    @Test
    public void testSpecialCases() {
        for (int numDecimals = 0; numDecimals < 17; numDecimals++) {
            assertThat(Double.isNaN(RoundingUtil.round(Double.NaN, numDecimals)), is(true));
            assertThat(
                    RoundingUtil.round(Double.NEGATIVE_INFINITY, numDecimals),
                    is(equalTo(Double.NEGATIVE_INFINITY)));
            assertThat(
                    RoundingUtil.round(Double.POSITIVE_INFINITY, numDecimals),
                    is(equalTo(Double.POSITIVE_INFINITY)));
        }
    }

    @Test
    public void testSpecificCases() {
        assertThat(RoundingUtil.round(0d, 0), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.1, 0), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.1, 1), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(0.1, 2), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(0.05, 1), is(equalTo(0.1)));
        assertThat(RoundingUtil.round(-0.05, 1), is(equalTo(0d)));
        assertThat(RoundingUtil.round(0.0000001, 5), is(equalTo(0d)));
        assertThat(RoundingUtil.round(1.0000001, 7), is(equalTo(1.0000001)));
        assertThat(RoundingUtil.round(1.00000015, 7), is(equalTo(1.0000002)));
        assertThat(RoundingUtil.round(1E-3, 7), is(equalTo(0.001)));
        assertThat(RoundingUtil.round(1E-4, 3), is(equalTo(0d)));
        assertThat(RoundingUtil.round(1E-10, 10), is(equalTo(1E-10)));
    }

    @Test
    public void testNoRoundingWhenPrecisionWouldBeExceeded() {
        // Test cases where precision is exceeded.
        assertThat(RoundingUtil.round(1.01234567890123456E12, 1), is(equalTo(1.0123456789012E12)));
        assertThat(RoundingUtil.round(1.01234567890123456E12, 2), is(equalTo(1.01234567890123E12)));
        assertThat(RoundingUtil.round(1.01234567890123456E13, 1), is(equalTo(1.01234567890123E13)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E13, 2), is(equalTo(1.012345678901235E13)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E14, 1), is(equalTo(1.012345678901235E14)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E14, 2), is(equalTo(1.0123456789012346E14)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E15, 1), is(equalTo(1.0123456789012345E15)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E15, 2), is(equalTo(1.0123456789012345E15)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E16, 1), is(equalTo(1.0123456789012346E16)));
        assertThat(
                RoundingUtil.round(1.01234567890123456E16, 2), is(equalTo(1.0123456789012346E16)));
        assertThat(
                RoundingUtil.round(1.0123456789012345E17, 1), is(equalTo(1.0123456789012345E17)));
        assertThat(
                RoundingUtil.round(1.0123456789012345E18, 1), is(equalTo(1.0123456789012345E18)));
        assertThat(
                RoundingUtil.round(1.01234567890123451E19, 1), is(equalTo(1.0123456789012345E19)));
        assertThat(RoundingUtil.round(Double.MIN_VALUE, 15), is(equalTo(0d)));
        assertThat(RoundingUtil.round(Double.MAX_VALUE, 1), is(equalTo(Double.MAX_VALUE)));
    }

    @Test
    public void testRandomRoundingVsBigDecimal() {
        Random r = new Random();
        for (int i = 0; i < 10000; i++) {
            double value = r.nextDouble();
            for (int numDecimals = 0; numDecimals <= 8; numDecimals++) {
                double expected =
                        new BigDecimal(Double.toString(value))
                                .setScale(numDecimals, RoundingMode.HALF_UP)
                                .doubleValue();
                double actual = RoundingUtil.round(value, numDecimals);
                assertThat(actual, is(equalTo(expected)));
            }
        }
    }
}
