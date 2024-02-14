/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wms.WMS;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;

/** Provides a list of output formats for the OpenSearch layers (only raster, so WFS is excluded) */
abstract class OutputFormatProvider {
    static class WMS extends OutputFormatProvider {
        @Override
        List<String> getFormatNames(LayerInfo layer) {
            return new ArrayList<>(org.geoserver.wms.WMS.get().getAllowedMapFormatNames());
        }
    }

    static class WCS extends OutputFormatProvider {

        @Override
        List<String> getFormatNames(LayerInfo layer) {
            CoverageResponseDelegateFinder finder =
                    GeoServerExtensions.bean(CoverageResponseDelegateFinder.class);
            List<String> formats = new ArrayList<>();
            for (String format : finder.getOutputFormats()) {
                CoverageResponseDelegate delegate = finder.encoderFor(format);
                formats.add(delegate.getMimeType(format));
                formats.add(format);
            }
            return formats;
        }
    }

    private static boolean isValidMime(String mime) {
        try {
            if (MimeTypeUtils.parseMimeType(mime) != null) return true;
        } catch (InvalidMimeTypeException e) {
            // skip it
        }
        return false;
    }

    static class WMTS extends OutputFormatProvider {

        @Override
        List<String> getFormatNames(LayerInfo layer) {
            GeoServerTileLayer tileLayer = GWC.get().getTileLayer(layer);
            if (tileLayer == null) return Collections.emptyList();
            return tileLayer.getMimeTypes().stream()
                    .map(m -> m.getMimeType())
                    .collect(Collectors.toList());
        }
    }

    abstract List<String> getFormatNames(LayerInfo layer);

    public static List<String> getFormatNames(String serviceName, LayerInfo layer) {
        String service = serviceName.toLowerCase();
        List<String> formats = new ArrayList<>();
        if ("wms".equals(service) || "maps".equals(service)) {
            formats = new WMS().getFormatNames(layer);
        } else if ("wcs".equals(service) || "coverages".equals(service)) {
            formats = new WCS().getFormatNames(layer);
        } else if ("wmts".equals(service) || "tiles".equals(service)) {
            formats = new WMTS().getFormatNames(layer);
        } else {

            throw new IllegalArgumentException("Unknown service " + service);
        }
        // clean up, deduplicate, return in a predictable order
        return formats.stream()
                .filter(f -> isValidMime(f))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
