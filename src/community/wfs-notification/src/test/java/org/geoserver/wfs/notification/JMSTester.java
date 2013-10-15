package org.geoserver.wfs.notification;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.geoserver.wfs.notification.StaticJMSEventHelper;

public class JMSTester {
    public static void main(String[] args) throws Throwable {
        StaticJMSEventHelper eh = new StaticJMSEventHelper();
        eh.setPropertiesFileName(JMSTester.class.getResource("amqp.properties").toExternalForm());
        eh.init();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while((line = br.readLine()) != null) {
                eh.publish(line);
            }
        } finally {
            eh.destroy();
        }
    }
}
