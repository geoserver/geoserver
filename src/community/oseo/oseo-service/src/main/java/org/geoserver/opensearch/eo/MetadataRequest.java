/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

/**
 * A OpenSearch EO query for metadata of a specific product or collection
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MetadataRequest extends AbstractProductRequest {

    public static String ISO_METADATA = "application/vnd.iso.19139+xml";

    public static String OM_METADATA = "application/gml+xml";
}
