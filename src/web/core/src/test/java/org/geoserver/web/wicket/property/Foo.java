/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.property;

import java.io.Serializable;
import java.util.Properties;

public class Foo implements Serializable {

    Properties props = new Properties();

    public Properties getProps() {
        return props;
    }
}
