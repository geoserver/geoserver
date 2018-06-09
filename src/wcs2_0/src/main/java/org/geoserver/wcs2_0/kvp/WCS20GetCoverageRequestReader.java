/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.ScaleAxisByFactorType;
import net.opengis.wcs20.ScaleByFactorType;
import net.opengis.wcs20.ScaleToExtentType;
import net.opengis.wcs20.ScaleToSizeType;
import net.opengis.wcs20.ScalingType;
import net.opengis.wcs20.Wcs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.wcs.v2_0.Interpolation;
import org.geotools.wcs.v2_0.RangeSubset;
import org.geotools.wcs.v2_0.Scaling;

/**
 * KVP reader for WCS 2.0 GetCoverage request
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("rawtypes")
public class WCS20GetCoverageRequestReader extends EMFKvpRequestReader {

    private static final Wcs20Factory WCS20_FACTORY = Wcs20Factory.eINSTANCE;

    private static final String GEOTIFF_NS = "http://www.opengis.net/wcs/geotiff/1.0";

    private static final String CRS_NS = "http://www.opengis.net/wcs/service-extension/crs/1.0";

    public WCS20GetCoverageRequestReader() {
        super(GetCoverageType.class, WCS20_FACTORY);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        GetCoverageType gc = (GetCoverageType) super.read(request, kvp, rawKvp);

        // handle dimension subsets
        Object subsets = kvp.get("subset");
        if (subsets instanceof DimensionSubsetType) {
            gc.getDimensionSubset().add((DimensionSubsetType) subsets);
        } else if (subsets instanceof List) {
            for (Object subset : (List) subsets) {
                gc.getDimensionSubset().add((DimensionSubsetType) subset);
            }
        }

        // prepare for extensions
        gc.setExtension(WCS20_FACTORY.createExtensionType());

        // parse the extensions. Note, here we do only the validation bits that are not shared
        // with the XML, everything else is in GetCoverage
        parseGeoTiffExtension(gc, kvp);
        parseCRSExtension(gc, rawKvp);
        parseScalingExtension(gc, kvp);
        parseRangeSubsetExtension(gc, kvp);
        parseInterpolationExtension(gc, kvp);
        parseOverviewPolicyExtension(gc, kvp);

        return gc;
    }

    private void parseGeoTiffExtension(GetCoverageType gc, Map kvp) {
        // the early spec draft had un-qualified params, keeping it for backwards compatibility
        List<String> geoTiffParams =
                Arrays.asList(
                        "compression",
                        "jpeg_quality",
                        "predictor",
                        "interleave",
                        "tiling",
                        "tileheight",
                        "tilewidth");
        parseSimpleContentList(gc, kvp, geoTiffParams, GEOTIFF_NS, null);
        // the current has the qualified as "geotiff:xyz"
        parseSimpleContentList(gc, kvp, geoTiffParams, GEOTIFF_NS, "geotiff");
    }

    private void parseCRSExtension(GetCoverageType gc, Map kvp) {
        List<String> geoTiffParams = Arrays.asList("subsettingCrs", "outputCrs");
        parseSimpleContentList(gc, kvp, geoTiffParams, CRS_NS, null);
    }

    private void parseSimpleContentList(
            GetCoverageType gc,
            Map kvp,
            List<String> geoTiffParams,
            String namespace,
            String kvpPrefix) {
        for (String param : geoTiffParams) {
            String key = param;
            if (kvpPrefix != null) {
                key = kvpPrefix + ":" + param;
            }
            String value = KvpUtils.firstValue(kvp, key);
            if (value != null) {
                ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
                item.setNamespace(namespace);
                item.setName(param);
                item.setSimpleContent(value);

                gc.getExtension().getContents().add(item);
            }
        }
    }

    private void parseScalingExtension(GetCoverageType gc, Map kvp) {
        boolean found = false;
        ScalingType scaling = WCS20_FACTORY.createScalingType();
        if (kvp.containsKey("scalefactor")) {
            found = true;
            ScaleByFactorType sf = WCS20_FACTORY.createScaleByFactorType();
            sf.setScaleFactor(((Double) kvp.get("scalefactor")));
            scaling.setScaleByFactor(sf);
        }
        if (kvp.containsKey("scaleaxes")) {
            found = true;
            scaling.setScaleAxesByFactor((ScaleAxisByFactorType) kvp.get("scaleaxes"));
        }
        if (kvp.containsKey("scalesize")) {
            found = true;
            scaling.setScaleToSize((ScaleToSizeType) kvp.get("scalesize"));
        }
        if (kvp.containsKey("scaleextent")) {
            found = true;
            scaling.setScaleToExtent((ScaleToExtentType) kvp.get("scaleextent"));
        }

        // if we found at least one, put it in the extension map (it's the duty of
        // GetCoverage to complain about multiple scaling constructs)
        if (found == true) {
            ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
            item.setNamespace(Scaling.NAMESPACE);
            item.setName("Scaling");
            item.setObjectContent(scaling);
            gc.getExtension().getContents().add(item);
        }
    }

    private void parseRangeSubsetExtension(GetCoverageType gc, Map kvp) {
        if (kvp.containsKey("rangesubset")) {
            ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
            item.setNamespace(RangeSubset.NAMESPACE);
            item.setName("RangeSubset");
            item.setObjectContent(kvp.get("rangesubset"));
            gc.getExtension().getContents().add(item);
        }
    }

    private void parseInterpolationExtension(GetCoverageType gc, Map kvp) {
        if (kvp.containsKey("interpolation")) {
            ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
            item.setNamespace(Interpolation.NAMESPACE);
            item.setName("Interpolation");
            item.setObjectContent(kvp.get("interpolation"));
            gc.getExtension().getContents().add(item);
        }
    }

    private void parseOverviewPolicyExtension(GetCoverageType gc, Map kvp) {
        if (kvp.containsKey(WCS20Const.OVERVIEW_POLICY_EXTENSION_LOWERCASE)) {
            Object item = kvp.get(WCS20Const.OVERVIEW_POLICY_EXTENSION_LOWERCASE);
            if (item instanceof ExtensionItemType) {
                gc.getExtension().getContents().add((ExtensionItemType) item);
            }
        }
    }

    @Override
    protected void setValue(EObject eObject, String property, Object value) {
        if ("sortBy".equalsIgnoreCase(property)) {
            // we get an arraylist of arraylists
            List sorts = (List) value;
            final int sortsSize = sorts.size();
            if (sortsSize != 1) {
                throw new OWS20Exception(
                        "Invalid sortBy specification, expecting sorts for just one coverage, but got "
                                + sortsSize
                                + " instead",
                        WCS20Exception.WCS20ExceptionCode.InvalidParameterValue,
                        "sortBy");
            }
            final GetCoverageType getCoverage = (GetCoverageType) (eObject);
            getCoverage.getSortBy().addAll((List) sorts.get(0));
        } else {
            super.setValue(eObject, property, value);
        }
    }
}
