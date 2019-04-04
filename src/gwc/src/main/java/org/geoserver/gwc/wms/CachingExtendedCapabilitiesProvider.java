/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.NumberRange;
import org.geotools.util.Version;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Implementation of the {@link ExtendedCapabilitiesProvider} extension point to contribute WMS-C
 * DTD elements and TileSet definitions to the capabilities document of the regular GeoServer WMS.
 *
 * <p>A {@code TileSet} is added at {@link #encode} for each GWC {@link TileLayer}, but respecting
 * the {@link GetCapabilitiesRequest#getNamespace() namespace} filter if set.
 *
 * @author Gabriel Roldan
 */
public class CachingExtendedCapabilitiesProvider implements ExtendedCapabilitiesProvider {

    private final GWC gwc;

    public CachingExtendedCapabilitiesProvider(final GWC gwc) {
        this.gwc = gwc;
    }

    /** @see org.geoserver.wms.ExtendedCapabilitiesProvider#getSchemaLocations(String) */
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[0];
    }

    /**
     * @return {@code TileSet*}
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots
     */
    public List<String> getVendorSpecificCapabilitiesRoots(final GetCapabilitiesRequest request) {
        if (gwc.getConfig().isDirectWMSIntegrationEnabled() && isTiled(request)) {
            return Collections.singletonList("TileSet*");
        }
        return Collections.emptyList();
    }

    private boolean isTiled(GetCapabilitiesRequest request) {
        return Boolean.valueOf(request.getRawKvp().get("TILED")).booleanValue();
    }

    /**
     * @see
     *     org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest)
     */
    public List<String> getVendorSpecificCapabilitiesChildDecls(
            final GetCapabilitiesRequest request) {
        if (gwc.getConfig().isDirectWMSIntegrationEnabled() && isTiled(request)) {
            List<String> wmscElements = new ArrayList<String>();
            wmscElements.add(
                    "<!ELEMENT TileSet (SRS, BoundingBox?, Resolutions, Width, Height, Format, Layers*, Styles*) >");
            wmscElements.add("<!ELEMENT Resolutions (#PCDATA) >");
            wmscElements.add("<!ELEMENT Width (#PCDATA) >");
            wmscElements.add("<!ELEMENT Height (#PCDATA) >");
            wmscElements.add("<!ELEMENT Layers (#PCDATA) >");
            wmscElements.add("<!ELEMENT Styles (#PCDATA) >");
            return wmscElements;
        }
        return Collections.emptyList();
    }

    /**
     * Empty implementation, no namespaces to add until we support the WMS-C 1.3 profile
     *
     * @see
     *     org.geoserver.wms.ExtendedCapabilitiesProvider#registerNamespaces(org.xml.sax.helpers.NamespaceSupport)
     */
    public void registerNamespaces(NamespaceSupport namespaces) {
        // nothing to do
    }

    /**
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#encode(Translator, ServiceInfo, Object)
     */
    public void encode(final Translator tx, final WMSInfo wms, final GetCapabilitiesRequest request)
            throws IOException {
        if (!gwc.getConfig().isDirectWMSIntegrationEnabled()) {
            return;
        }
        Version version = WMS.version(request.getVersion(), true);
        if (!WMS.VERSION_1_1_1.equals(version) || !isTiled(request)) {
            return;
        }

        final String namespacePrefixFilter = request.getNamespace();
        Iterable<? extends TileLayer> tileLayers;
        tileLayers = gwc.getTileLayersByNamespacePrefix(namespacePrefixFilter);

        final String nsPrefix;
        {
            final WorkspaceInfo localWorkspace = LocalWorkspace.get();
            if (localWorkspace == null) {
                nsPrefix = null;
            } else {
                nsPrefix = localWorkspace.getName() + ":";
            }
        }

        for (TileLayer layer : tileLayers) {
            String advertisedName = layer.getName();
            if (nsPrefix != null) {
                if (!advertisedName.startsWith(nsPrefix)) {
                    continue;
                }
                advertisedName = advertisedName.substring(nsPrefix.length());
            }
            for (String gridSetId : layer.getGridSubsets()) {
                GridSubset grid = layer.getGridSubset(gridSetId);
                for (MimeType mime : layer.getMimeTypes()) {
                    vendorSpecificTileset(tx, layer, advertisedName, grid, mime.getFormat());
                }
            }
        }
    }

    private void vendorSpecificTileset(
            final Translator tx,
            final TileLayer layer,
            final String advertisedLayerName,
            final GridSubset grid,
            final String format) {

        String srsStr = grid.getSRS().toString();
        StringBuilder resolutionsStr = new StringBuilder();
        double[] res = grid.getResolutions();
        for (int i = 0; i < res.length; i++) {
            resolutionsStr.append(Double.toString(res[i]) + " ");
        }

        String[] bs = boundsPrep(grid.getCoverageBestFitBounds());

        tx.start("TileSet");

        tx.start("SRS");
        tx.chars(srsStr);
        tx.end("SRS");

        AttributesImpl atts;
        atts = new AttributesImpl();
        atts.addAttribute("", "SRS", "SRS", "", srsStr);
        atts.addAttribute("", "minx", "minx", "", bs[0]);
        atts.addAttribute("", "miny", "miny", "", bs[1]);
        atts.addAttribute("", "maxx", "maxx", "", bs[2]);
        atts.addAttribute("", "maxy", "maxy", "", bs[3]);

        tx.start("BoundingBox", atts);
        tx.end("BoundingBox");

        tx.start("Resolutions");
        tx.chars(resolutionsStr.toString());
        tx.end("Resolutions");

        tx.start("Width");
        tx.chars(String.valueOf(grid.getTileWidth()));
        tx.end("Width");

        tx.start("Height");
        tx.chars(String.valueOf(grid.getTileHeight()));
        tx.end("Height");

        tx.start("Format");
        tx.chars(format);
        tx.end("Format");

        tx.start("Layers");
        tx.chars(advertisedLayerName);
        tx.end("Layers");

        // TODO ignoring styles for now
        tx.start("Styles");
        tx.end("Styles");

        tx.end("TileSet");
    }

    String[] boundsPrep(BoundingBox bbox) {
        String[] bs = {
            Double.toString(bbox.getMinX()),
            Double.toString(bbox.getMinY()),
            Double.toString(bbox.getMaxX()),
            Double.toString(bbox.getMaxY())
        };
        return bs;
    }

    @Override
    public void customizeRootCrsList(Set<String> srs) {
        // nothing to do
    }

    @Override
    public NumberRange<Double> overrideScaleDenominators(
            PublishedInfo layer, NumberRange<Double> scaleDenominators) {
        return scaleDenominators;
    }
}
