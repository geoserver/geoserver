/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.Serializable;
import org.geotools.data.DefaultRepository;

/**
 * Around just to help testing
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SerializableDefaultRepository extends DefaultRepository implements Serializable {

    private static final long serialVersionUID = -4285310466894316161L;
}
