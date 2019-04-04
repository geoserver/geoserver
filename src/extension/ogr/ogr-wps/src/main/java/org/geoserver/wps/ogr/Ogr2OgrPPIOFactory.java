/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.ogr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.ogr.core.Format;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wfs.response.Ogr2OgrOutputFormat;
import org.geoserver.wps.ppio.PPIOFactory;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.util.Version;

/** Factory to create an output PPIO for each OGR format managed by ogr2ogr libraries. */
public class Ogr2OgrPPIOFactory implements PPIOFactory {

    private Ogr2OgrOutputFormat ogr2OgrOutputFormat;

    public Ogr2OgrPPIOFactory(Ogr2OgrOutputFormat ogr2OgrOutputFormat) {
        this.ogr2OgrOutputFormat = ogr2OgrOutputFormat;
    }

    /**
     * This allow to instantiate the right type of PPIO subclass, {@link
     * org.geoserver.wps.ppio.BinaryPPIO} for binary, {@link org.geoserver.wps.ppio.CDataPPIO} for
     * text, {@link org.geoserver.wps.ppio.XMLPPIO} for xml to serve the format as a process
     * parameter output.
     */
    @Override
    public List<ProcessParameterIO> getProcessParameterIO() {
        List<ProcessParameterIO> ogrParams = new ArrayList<ProcessParameterIO>();
        for (Format of : this.ogr2OgrOutputFormat.getFormats()) {
            ProcessParameterIO ppio = null;
            GetFeatureType gft = WfsFactory.eINSTANCE.createGetFeatureType();
            gft.setOutputFormat(of.getGeoserverFormat());
            Operation operation =
                    new Operation(
                            "GetFeature",
                            new Service(
                                    "WFS", null, new Version("1.1.0"), Arrays.asList("GetFeature")),
                            null,
                            new Object[] {gft});
            // String computedMimeType = of.mimeType;
            // if (computedMimeType == null || computedMimeType.isEmpty()) {
            String computedMimeType = ogr2OgrOutputFormat.getMimeType(null, operation);
            if (of.getGeoserverFormat() != null && !of.getGeoserverFormat().isEmpty()) {
                computedMimeType = computedMimeType + "; subtype=" + of.getGeoserverFormat();
            }
            // }
            if (of.getType() == null) {
                // Binary is default type
                ppio =
                        new OgrBinaryPPIO(
                                computedMimeType,
                                of.getFileExtension(),
                                ogr2OgrOutputFormat,
                                operation);
            } else {
                switch (of.getType()) {
                    case BINARY:
                        ppio =
                                new OgrBinaryPPIO(
                                        computedMimeType,
                                        of.getFileExtension(),
                                        ogr2OgrOutputFormat,
                                        operation);
                        break;
                    case TEXT:
                        ppio =
                                new OgrCDataPPIO(
                                        computedMimeType,
                                        of.getFileExtension(),
                                        ogr2OgrOutputFormat,
                                        operation);
                        break;
                    case XML:
                        ppio =
                                new OgrXMLPPIO(
                                        computedMimeType,
                                        of.getFileExtension(),
                                        ogr2OgrOutputFormat,
                                        operation);
                        break;
                    default:
                        break;
                }
            }
            if (ppio != null) {
                ogrParams.add(ppio);
            }
        }
        return ogrParams;
    }
}
