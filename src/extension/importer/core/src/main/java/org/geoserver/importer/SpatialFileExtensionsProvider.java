/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.*;

/** A Class reporting a Set of default file extensions for some commonly used spatial files */
public class SpatialFileExtensionsProvider implements SupplementalFileExtensionsProvider {

    public SpatialFileExtensionsProvider() {};

    static class JPEGFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        JPEGFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("jpeg", "jpg")),
                    new HashSet<String>(Arrays.asList("jpw", "wld", "prj")));
        }
    };

    static class TIFFFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        TIFFFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("tif", "tiff")),
                    new HashSet<String>(
                            Arrays.asList(
                                    "tfw", "wld", "aux", "rrd", "xml", "tif.aux.xml", "prj")));
        }
    };

    static class PNGFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        PNGFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("png")),
                    new HashSet<String>(Arrays.asList("pnw", "wld", "aux.xml", "xml", "prj")));
        }
    };

    static class NetCDFFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        NetCDFFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("nc")),
                    new HashSet<String>(Arrays.asList("ncx", "aux.xml", "xml", "prj")));
        }
    };

    static class GribFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        GribFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("grib", "grb", "grib2", "grb2")),
                    new HashSet<String>(
                            Arrays.asList(
                                    "grb2.ncx3",
                                    "gbx9",
                                    "ncx3",
                                    "gbx9.ncx3",
                                    "aux.xml",
                                    "xml",
                                    "prj")));
        }
    };

    static class ShapeFileExtensionsProvider extends DefaultSupplementalFileExtensionsProvider {

        ShapeFileExtensionsProvider() {
            super(
                    new HashSet<String>(Arrays.asList("shp")),
                    new HashSet<String>(
                            Arrays.asList(
                                    "shx",
                                    "dbf",
                                    "aux.xml",
                                    "idx",
                                    "sbx",
                                    "sbn",
                                    "shp.ed.lock",
                                    "shp.xml",
                                    "prj")));
        }
    };

    private static final ShapeFileExtensionsProvider SHAPEFILE_PROVIDER =
            new ShapeFileExtensionsProvider();
    private static final TIFFFileExtensionsProvider TIF_PROVIDER = new TIFFFileExtensionsProvider();
    private static final JPEGFileExtensionsProvider JPEG_PROVIDER =
            new JPEGFileExtensionsProvider();
    private static final PNGFileExtensionsProvider PNG_PROVIDER = new PNGFileExtensionsProvider();
    private static final NetCDFFileExtensionsProvider NETCDF_PROVIDER =
            new NetCDFFileExtensionsProvider();
    private static final GribFileExtensionsProvider GRIB_PROVIDER =
            new GribFileExtensionsProvider();

    static Map<String, SupplementalFileExtensionsProvider> PROVIDERS = new HashMap();

    {
        // Providers being setup on the available tests
        PROVIDERS.put("tif", TIF_PROVIDER);
        PROVIDERS.put("tiff", TIF_PROVIDER);
        PROVIDERS.put("jpeg", JPEG_PROVIDER);
        PROVIDERS.put("jpg", JPEG_PROVIDER);
        PROVIDERS.put("png", PNG_PROVIDER);
        PROVIDERS.put("shp", SHAPEFILE_PROVIDER);
        PROVIDERS.put("nc", NETCDF_PROVIDER);
        PROVIDERS.put("grib", GRIB_PROVIDER);
        PROVIDERS.put("grib2", GRIB_PROVIDER);
    }

    public Set<String> getExtensions(String baseExtension) {
        if (canHandle(baseExtension)) {
            return PROVIDERS.get(baseExtension.toLowerCase()).getExtensions(baseExtension);
        }
        return Collections.emptySet();
    }

    public boolean canHandle(String baseExtension) {
        return baseExtension != null && PROVIDERS.containsKey(baseExtension.toLowerCase());
    }
}
