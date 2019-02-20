/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.io.Serializable;
import org.geoserver.catalog.LayerGroupInfo;

/**
 * Interface for a layer group filter. It extends Serializable so it can be serialized in a Wicket
 * session.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public interface LayerGroupProviderFilter extends Serializable {

    boolean accept(LayerGroupInfo group);
}
