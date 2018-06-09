/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/** A layer in a map/animation download */
@XmlRootElement(name = "Layer")
public class Layer extends AbstractParametricEntity {

    String capabilities;

    @XmlElement(name = "Capabilities")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }
}
