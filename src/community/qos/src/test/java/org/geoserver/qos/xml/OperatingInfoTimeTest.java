/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.XStream;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.config.util.SecureXStream;
import org.junit.Test;

public class OperatingInfoTimeTest {

    @Test
    public void testDeserialize() {
        OperatingInfoTime ot = buildOperatingInfoTimeEx1();
        XStream xstream = new SecureXStream();
        // xstream.toXML(ot, System.out);
    }

    public static OperatingInfoTime buildOperatingInfoTimeEx1() {

        OperatingInfoTime ot = new OperatingInfoTime();
        ot.setDays(
                new ArrayList<DayOfWeek>(
                        Arrays.asList(new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.FRIDAY})));
        ot.setStartTime(OffsetTime.of(12, 15, 30, 0, ZoneOffset.of("-03:00")));
        ot.setEndTime(OffsetTime.of(18, 30, 00, 0, ZoneOffset.of("-03:00")));
        return ot;
    }

    public static OperatingInfoTime buildOperatingInfoTimeEx2() {
        OperatingInfoTime ot = new OperatingInfoTime();
        ot.setDays(
                new ArrayList<DayOfWeek>(
                        Arrays.asList(new DayOfWeek[] {DayOfWeek.SATURDAY, DayOfWeek.SUNDAY})));
        ot.setStartTime(OffsetTime.of(15, 15, 30, 0, ZoneOffset.of("-03:00")));
        ot.setEndTime(OffsetTime.of(22, 30, 00, 0, ZoneOffset.of("-03:00")));
        return ot;
    }
}
