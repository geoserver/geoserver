/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.rabbitmq.client.SaslConfig;
import com.rabbitmq.client.SaslMechanism;
import com.rabbitmq.client.impl.ExternalMechanism;
import com.rabbitmq.client.impl.PlainMechanism;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation to allow the ANONYMOUS auth mechanism
 *
 * @author Xandros
 */
public class CustomSaslConfig implements SaslConfig {
    private final String[] mechanisms;

    public static final CustomSaslConfig PLAIN = new CustomSaslConfig("PLAIN");

    public static final CustomSaslConfig EXTERNAL = new CustomSaslConfig("EXTERNAL");

    public static final CustomSaslConfig ANONYMOUS = new CustomSaslConfig("ANONYMOUS");

    public CustomSaslConfig() {
        this.mechanisms = new String[] {"PLAIN", "EXTERNAL", "ANONYMOUS"};
    }

    private CustomSaslConfig(String mechanism) {
        this.mechanisms = new String[] {mechanism};
    }

    @Override
    public SaslMechanism getSaslMechanism(String[] serverMechanisms) {
        Set<String> server = new HashSet<String>(Arrays.asList(serverMechanisms));
        for (String m : mechanisms) {
            if (server.contains(m)) {
                if (m.equals("PLAIN")) {
                    return new PlainMechanism();
                } else if (m.equals("EXTERNAL")) {
                    return new ExternalMechanism();
                } else if (m.equals("ANONYMOUS")) {
                    return new AnonymousMechanism();
                }
            }
        }

        return null;
    }
}
