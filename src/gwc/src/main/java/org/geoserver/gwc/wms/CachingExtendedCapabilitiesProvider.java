package org.geoserver.gwc.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geoserver.gwc.GWC;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
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
 * <p>
 * A {@code TileSet} is added at {@link #encode} for each GWC {@link TileLayer}, but respecting the
 * {@link GetCapabilitiesRequest#getNamespace() namespace} filter if set.
 * </p>
 * 
 * @author Gabriel Roldan
 * 
 */
public class CachingExtendedCapabilitiesProvider implements ExtendedCapabilitiesProvider {

    private final GWC gwc;

    public CachingExtendedCapabilitiesProvider(final GWC gwc) {
        this.gwc = gwc;
    }

    /**
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#getSchemaLocations()
     */
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[0];
    }

    /**
     * @return {@code TileSet*}
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesRoots
     */
    public List<String> getVendorSpecificCapabilitiesRoots(final GetCapabilitiesRequest request) {
        if (isTiled(request)) {
            return Collections.singletonList("TileSet*");
        }
        return Collections.emptyList();
    }

    private boolean isTiled(GetCapabilitiesRequest request) {
        return Boolean.valueOf(request.getRawKvp().get("TILED")).booleanValue();
    }

    /**
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
     */
    public List<String> getVendorSpecificCapabilitiesChildDecls(final GetCapabilitiesRequest request) {
        if (isTiled(request)) {
            List<String> wmscElements = new ArrayList<String>();
            wmscElements
                    .add("<!ELEMENT TileSet (SRS, BoundingBox?, Resolutions, Width, Height, Format, Layers*, Styles*) >");
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
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#registerNamespaces(org.xml.sax.helpers.NamespaceSupport)
     */
    public void registerNamespaces(NamespaceSupport namespaces) {
        // nothing to do
    }

    /**
     * @see org.geoserver.wms.ExtendedCapabilitiesProvider#encode(org.geoserver.wms.ExtendedCapabilitiesProvider.Translator,
     *      org.geoserver.wms.WMSInfo, org.geotools.util.Version)
     */
    public void encode(final Translator tx, final WMSInfo wms, final GetCapabilitiesRequest request)
            throws IOException {
        Version version = WMS.version(request.getVersion(), true);
        if (!WMS.VERSION_1_1_1.equals(version) || !isTiled(request)) {
            return;
        }

        String namespacePrefixFilter = request.getNamespace();
        Iterable<TileLayer> tileLayers = gwc.getTileLayersByNamespacePrefix(namespacePrefixFilter);

        for (TileLayer layer : tileLayers) {

            Map<String, GridSubset> gridSubsets = layer.getGridSubsets();
            
            Collection<GridSubset> layerGrids = gridSubsets.values();

            for (GridSubset grid : layerGrids) {
                for (MimeType mime : layer.getMimeTypes()) {
                    vendorSpecificTileset(tx, layer, grid, mime.getFormat());
                }
            }
        }
    }

    private void vendorSpecificTileset(final Translator tx, final TileLayer layer,
            final GridSubset grid, final String format) {

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
        tx.chars(layer.getName());
        tx.end("Layers");

        // TODO ignoring styles for now
        tx.start("Styles");
        tx.end("Styles");

        tx.end("TileSet");
    }

    String[] boundsPrep(BoundingBox bbox) {
        String[] bs = { Double.toString(bbox.getMinX()), Double.toString(bbox.getMinY()),
                Double.toString(bbox.getMaxX()), Double.toString(bbox.getMaxY()) };
        return bs;
    }
}
