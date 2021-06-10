/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import com.thoughtworks.xstream.converters.basic.DateConverter;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.wps.ppio.XStreamPPIO;

public class AnimationMetadataPPIO extends XStreamPPIO {

    protected AnimationMetadataPPIO() {
        super(AnimationMetadata.class, "AnimationMetadata");
    }

    @Override
    protected SecureXStream buildXStream() {
        SecureXStream xstream = super.buildXStream();
        xstream.alias("Warning", FrameWarning.class);
        xstream.allowTypes(new Class[] {FrameWarning.class});
        xstream.addDefaultImplementation(ArrayList.class, List.class);
        xstream.registerConverter(new DateConverter("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", null));
        return xstream;
    }
}
