/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.geoserver.metadata.data.dto.impl.GeonetworkConfigurationImpl;

/**
 * Object that matches yaml structure.
 *
 * <p>Describe a geonetwork endpoint.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
@JsonDeserialize(as = GeonetworkConfigurationImpl.class)
public interface GeonetworkConfiguration extends Serializable {

    public String getName();

    public String getUrl();
}
