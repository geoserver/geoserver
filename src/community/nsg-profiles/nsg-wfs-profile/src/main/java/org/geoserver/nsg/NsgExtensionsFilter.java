/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg;

import org.geoserver.platform.ExtensionFilter;
import org.geoserver.wfs.xml.v2_0.WfsXmlReader;

/** Filter GeoServer extensions that are override by NSG extensions. */
public class NsgExtensionsFilter implements ExtensionFilter {

    @Override
    public boolean exclude(String beanId, Object bean) {
        return bean instanceof WfsXmlReader
                && ((WfsXmlReader) bean).getElement().getLocalPart().equalsIgnoreCase("GetFeature");
    }
}
