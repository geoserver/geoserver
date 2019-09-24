/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import java.util.List;

public interface AttributeCollection {

    List<AttributeConfiguration> getAttributes();

    AttributeConfiguration findAttribute(String attName);

    List<String> getCsvImports();
}
