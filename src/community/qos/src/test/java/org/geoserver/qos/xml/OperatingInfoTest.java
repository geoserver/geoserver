/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.geoserver.config.util.SecureXStream;
import org.junit.Test;

public class OperatingInfoTest {

    @Test
    public void testXmlSerialization() throws JAXBException {
        MyOperatingInfoRoot root = new MyOperatingInfoRoot();
        root.setOperatingInfo(buildOperationInfoEx1());
        XStream xstream = new SecureXStream();
        // xstream.toXML(root, System.out);
    }

    public static OperatingInfo buildOperationInfoEx1() {
        OperatingInfo oinfo = new OperatingInfo();
        oinfo.setOperationalStatus(
                new ReferenceType(
                        "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#Operational",
                        "Operational"));
        List<OperatingInfoTime> times =
                new ArrayList<>(
                        Arrays.asList(
                                new OperatingInfoTime[] {
                                    OperatingInfoTimeTest.buildOperatingInfoTimeEx1(),
                                    OperatingInfoTimeTest.buildOperatingInfoTimeEx2()
                                }));
        oinfo.setByDaysOfWeek(times);
        return oinfo;
    }

    public static class MyOperatingInfoRoot {

        private OperatingInfo operatingInfo;

        public OperatingInfo getOperatingInfo() {
            return operatingInfo;
        }

        public void setOperatingInfo(OperatingInfo operatingInfo) {
            this.operatingInfo = operatingInfo;
        }
    }
}
