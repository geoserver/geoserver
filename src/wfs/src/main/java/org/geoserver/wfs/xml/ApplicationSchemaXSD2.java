/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.xml.XSD;

public class ApplicationSchemaXSD2 extends XSD {

    FeatureTypeSchemaBuilder schemaBuilder;
    Map<String,Set<FeatureTypeInfo>> featureTypes;
    String baseURL;
    
    public ApplicationSchemaXSD2(FeatureTypeSchemaBuilder schemaBuilder, 
        Map<String,Set<FeatureTypeInfo>> featureTypes) {
        
        this.schemaBuilder = schemaBuilder;
        this.featureTypes = featureTypes;
    }
    
    @Override
    public String getNamespaceURI() {
        if (featureTypes.size() == 1) {
            return featureTypes.keySet().iterator().next();
        }
        
        //TODO: return xsd namespace?
        return null;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }
    
    public String getBaseURL() {
        return baseURL;
    }
    
    @Override
    public String getSchemaLocation() {
        StringBuilder sb = new StringBuilder();
        for (Set<FeatureTypeInfo> fts : featureTypes.values()) {
            for (FeatureTypeInfo ft : fts) {
                sb.append(ft.getPrefixedName()).append(",");
            }
        }
        sb.setLength(sb.length()-1);
        
        HashMap kvp = new HashMap();
        kvp.putAll(schemaBuilder.getDescribeFeatureTypeParams());
        kvp.put("typename", sb.toString());
        
        return ResponseUtils.buildURL(baseURL, "wfs", kvp, URLType.SERVICE);
    }
    
    @Override
    protected XSDSchema buildSchema() throws IOException {
        Set<FeatureTypeInfo> types = new HashSet();
        for (Set<FeatureTypeInfo> fts : featureTypes.values()) {
            types.addAll(fts);
        }
        return schemaBuilder.build(types.toArray(new FeatureTypeInfo[types.size()]), baseURL, 1);
    }

}
