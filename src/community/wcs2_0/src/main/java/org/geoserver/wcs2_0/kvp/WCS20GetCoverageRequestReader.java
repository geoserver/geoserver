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

import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.wcs.v2_0.RangeSubset;
import org.geotools.wcs.v2_0.Scaling;

/**
 * KVP reader for WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageRequestReader extends EMFKvpRequestReader {

    private static final Wcs20Factory WCS20_FACTORY = Wcs20Factory.eINSTANCE;

    private static final String GEOTIFF_NS = "http://www.opengis.net/wcs/geotiff/1.0";

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
        parseScalingExtension(gc, kvp);
        parseRangeSubsetExtension(gc, kvp);

        return gc;
    }

    private void parseGeoTiffExtension(GetCoverageType gc, Map kvp) {
        List<String> geoTiffParams = Arrays.asList("compression", "jpeg_quality", "predictor",
                "interleave", "tiling", "tileheight", "tilewidth");

        for (String param : geoTiffParams) {
            String value = KvpUtils.firstValue(kvp, param);
            if (value != null) {
                ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
                item.setNamespace(GEOTIFF_NS);
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
        if(kvp.containsKey("rangesubset")) {
            ExtensionItemType item = WCS20_FACTORY.createExtensionItemType();
            item.setNamespace(RangeSubset.NAMESPACE);
            item.setName("RangeSubset");
            item.setObjectContent(kvp.get("rangesubset"));
            gc.getExtension().getContents().add(item);
        }
        
    }

}
