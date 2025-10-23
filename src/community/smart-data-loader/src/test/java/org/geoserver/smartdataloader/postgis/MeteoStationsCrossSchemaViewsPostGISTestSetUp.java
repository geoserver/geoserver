/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.postgis;

/** PostGIS test setup loading dataset where the virtual view lives in a different schema. */
public class MeteoStationsCrossSchemaViewsPostGISTestSetUp extends MeteoStationsPostGISTestSetUp {

    public MeteoStationsCrossSchemaViewsPostGISTestSetUp() {
        super("meteo_db_views_smartschema2.sql");
    }
}
