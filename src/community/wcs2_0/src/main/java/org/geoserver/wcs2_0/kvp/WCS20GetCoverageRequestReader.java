package org.geoserver.wcs2_0.kvp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.GetCoverageType;
import net.opengis.wcs20.Wcs20Factory;

import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.ows.util.KvpUtils;

/**
 * KVP reader for WCS 2.0 GetCoverage request
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20GetCoverageRequestReader extends EMFKvpRequestReader {

    private static final String GEOTIFF_NS = "http://www.opengis.net/wcs/geotiff/1.0";

    public WCS20GetCoverageRequestReader() {
        super(GetCoverageType.class, Wcs20Factory.eINSTANCE);
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
        gc.setExtension(Wcs20Factory.eINSTANCE.createExtensionType());

        // parse the extensions. Note, here we do only the validation bits that are not shared
        // with the XML, everything else is in GetCoverage
        parseGeoTiffExtension(gc, kvp);

        return gc;
    }

    private void parseGeoTiffExtension(GetCoverageType gc, Map kvp) {
        List<String> geoTiffParams = Arrays.asList("compression", "jpeg_quality", "predictor",
                "interleave", "tiling", "tileheight", "tilewidth");

        for (String param : geoTiffParams) {
            String value = KvpUtils.firstValue(kvp, param);
            if (value != null) {
                ExtensionItemType item = Wcs20Factory.eINSTANCE.createExtensionItemType();
                item.setNamespace(GEOTIFF_NS);
                item.setName(param);
                item.setSimpleContent(value);
                
                gc.getExtension().getContents().add(item);
            }
        }

    }

}
