/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import com.thoughtworks.xstream.XStream;
import javax.xml.bind.JAXBException;
import org.geoserver.config.util.SecureXStream;
import org.junit.Test;

public class QualityOfServiceStatementTest {

    @Test
    public void testXmlSerialization() throws JAXBException {
        QualityOfServiceStatementRoot root = new QualityOfServiceStatementRoot();
        root.setQualityOfServiceStatement(buildStatement_ex1());
        XStream xstream = new SecureXStream();
        // xstream.toXML(root, System.out);
    }

    public static QualityOfServiceStatement buildStatement_ex1() {
        QualityOfServiceStatement qs = new QualityOfServiceStatement();
        qs.setMetric(Metric.values().get(0));
        qs.setMeassure(new StringValue("90.5", "%"));
        return qs;
    }

    public static class QualityOfServiceStatementRoot {
        private QualityOfServiceStatement qualityOfServiceStatement;

        public QualityOfServiceStatementRoot() {}

        public QualityOfServiceStatement getQualityOfServiceStatement() {
            return qualityOfServiceStatement;
        }

        public void setQualityOfServiceStatement(
                QualityOfServiceStatement qualityOfServiceStatement) {
            this.qualityOfServiceStatement = qualityOfServiceStatement;
        }
    }
}
