/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.geoserver.config.util.SecureXStream;
import org.junit.Test;

public class ReferenceTypeTest {

    @Test
    public void testOperationalAnomalyFeedXmlDeserial() throws JAXBException {
        QoSMetadata mdata = new QoSMetadata();
        mdata.setOperationAnomalyFeed(
                new ArrayList<>(
                        Arrays.asList(new ReferenceType[] {buildOperationalAnomalyFeed_ex1()})));
        XStream xstream = new SecureXStream();
        // xstream.toXML(mdata, System.out);
    }

    public static ReferenceType buildOperationalAnomalyFeed_ex1() {
        ReferenceType oaf = new ReferenceType();
        oaf.setHref("myservice.ics");
        oaf.setFormat("text/calendar");
        OwsAbstract abs = new OwsAbstract();
        abs.setValue("A iCalendar (rfc5545) feed for operation anomalies considering this service");
        oaf.setAbstracts(new ArrayList<>(Arrays.asList(new OwsAbstract[] {abs})));
        return oaf;
    }
}
