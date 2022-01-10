/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.rabbitmq.client.LongString;
import com.rabbitmq.client.SaslMechanism;
import com.rabbitmq.client.impl.LongStringHelper;

/**
 * Handles anonymous authentication challenge
 *
 * @author Xandros
 * @see CustomSaslConfig
 */
public class AnonymousMechanism implements SaslMechanism {

    @Override
    public String getName() {
        return "ANONYMOUS";
    }

    @Override
    public LongString handleChallenge(LongString challenge, String username, String password) {
        return LongStringHelper.asLongString("");
    }
}
