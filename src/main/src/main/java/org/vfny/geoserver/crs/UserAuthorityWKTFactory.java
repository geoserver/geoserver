/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.net.URL;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;

/** Authority allowing users to define their own CRS custom authority in a separate file */
public class UserAuthorityWKTFactory extends FactoryUsingWKT {

    protected static final int PRIORITY = MAXIMUM_PRIORITY - 10;

    private Citation authority;
    private URL url;

    public UserAuthorityWKTFactory(Citation authority, Resource properties) {
        super(null, PRIORITY);
        this.hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, false);
        this.authority = authority;
        // turn the resource into a file and then URL, the GeoTools code needs files
        this.url = URLs.fileToUrl(properties.file());
    }

    @Override
    protected Citation[] getAuthorities() {
        return new Citation[] {authority};
    }

    /**
     * Returns the URL to the property file that contains CRS definitions. The default implementation returns the URL to
     * the {@value #FILENAME} file.
     *
     * @return The URL, or {@code null} if none.
     */
    @Override
    protected URL getDefinitionsURL() {
        return url;
    }
}
