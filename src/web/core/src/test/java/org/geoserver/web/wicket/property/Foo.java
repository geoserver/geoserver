package org.geoserver.web.wicket.property;

import java.io.Serializable;
import java.util.Properties;

public class Foo implements Serializable {

    Properties props = new Properties();

    public Properties getProps() {
        return props;
    }
}
