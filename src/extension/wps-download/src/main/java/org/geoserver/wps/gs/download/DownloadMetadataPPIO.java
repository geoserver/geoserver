/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import com.thoughtworks.xstream.converters.basic.DateConverter;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.util.DimensionWarning;
import org.geoserver.wps.ppio.XStreamPPIO;

public class DownloadMetadataPPIO extends XStreamPPIO {

    protected DownloadMetadataPPIO() {
        super(DownloadMetadata.class, "DownloadMetadata");
    }

    @Override
    protected SecureXStream buildXStream() {
        SecureXStream xstream = super.buildXStream();
        xstream.allowTypes(new Class[] {DimensionWarning.class});
        xstream.addDefaultImplementation(ArrayList.class, List.class);
        xstream.registerConverter(new DateConverter("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", null));
        return xstream;
    }
}
