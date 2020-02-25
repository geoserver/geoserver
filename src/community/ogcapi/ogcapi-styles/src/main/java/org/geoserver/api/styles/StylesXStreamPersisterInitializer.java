package org.geoserver.api.styles;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.springframework.stereotype.Component;

/** Setus */
@Component
public class StylesXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        XStream xs = persister.getXStream();
        xs.alias("StyleMetadata", StyleMetadataInfo.class);
        xs.aliasField("abstract", StyleMetadataInfo.class, "abstrct");
        persister.registerBreifMapComplexType("styleMetadata", StyleMetadataInfo.class);
        xs.allowTypes(new Class[] {StyleMetadataInfo.class});
    }
}
