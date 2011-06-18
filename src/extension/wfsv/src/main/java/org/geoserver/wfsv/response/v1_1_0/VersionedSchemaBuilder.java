package org.geoserver.wfsv.response.v1_1_0;

import static org.geoserver.ows.util.ResponseUtils.params;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.GML3Profile;
import org.geoserver.wfsv.xml.v1_1_0.WFSVConfiguration;
import org.opengis.feature.type.AttributeDescriptor;

public class VersionedSchemaBuilder extends FeatureTypeSchemaBuilder {
    /**
     * Cached gml3 schema
     */
    private static XSDSchema gml3Schema;

    public VersionedSchemaBuilder(GeoServer gs, WFSVConfiguration configuration) {
        super(gs);

        profiles.add(new GML3Profile());

        gmlNamespace = org.geoserver.wfsv.xml.v1_1_0.WFSV.NAMESPACE;
        gmlSchemaLocation = "wfs/1.1.0/wfsv.xsd";
        baseType = "AbstractVersionedFeatureType";
        substitutionGroup = "_VersionedFeature";
        describeFeatureTypeParams = params("request", "DescribeFeatureType", 
                "version", "1.0.0",
                "service", "WFS",
                "versioned", "true");
        gmlPrefix = "wfsv";
        xmlConfiguration = new org.geotools.gml3.GMLConfiguration();
    }

    protected XSDSchema gmlSchema() {
        if (gml3Schema == null) {
            gml3Schema = xmlConfiguration.schema();
        }

        return gml3Schema;
    }
    
    @Override
    protected GMLInfo getGMLConfig(WFSInfo wfs) {
        return wfs.getGML().get(WFSInfo.Version.V_11);
    }
    
    protected boolean filterAttributeType( AttributeDescriptor attribute ) {
        return super.filterAttributeType( attribute ) || 
            "metaDataProperty".equals( attribute.getName() ) || 
            "location".equals( attribute.getName() );
    }
}
