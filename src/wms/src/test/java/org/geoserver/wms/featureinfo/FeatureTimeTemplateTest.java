/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public class FeatureTimeTemplateTest extends WMSTestSupport {

    static SimpleFeature feature;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new FeatureTimeTemplateTest());
    }
    
    
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
    
        SimpleFeatureSource source = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        SimpleFeatureCollection features = source.getFeatures();
        FeatureIterator <SimpleFeature> iterator = features.features();
        while( iterator.hasNext() ) {
            SimpleFeature f = iterator.next();
            if ( f.getAttribute("dateProperty") != null ) {
                feature = f;
                break;
            }
        }
        features.close(iterator);
    }
    
    public void testEmpty() throws Exception {
        FeatureTimeTemplate template = new FeatureTimeTemplate();
        String[] result = template.execute( feature );
        
        assertEquals( 0, result.length );
    }
    
    public void testTimestamp() throws Exception {
        setupTemplate(MockData.PRIMITIVEGEOFEATURE,"time.ftl","${dateProperty.value}");
        
        FeatureTimeTemplate template = new FeatureTimeTemplate();
        String[] result = template.execute( feature );
        
        assertEquals( 1, result.length );
        assertNotNull( result[0] );
    }
    
    public void testTimeSpan() throws Exception {
        setupTemplate(MockData.PRIMITIVEGEOFEATURE,"time.ftl","${dateProperty.value}||${dateProperty.value}");
        FeatureTimeTemplate template = new FeatureTimeTemplate();
        String[] result = template.execute( feature );
        
        assertEquals( 2, result.length );
        assertNotNull( result[0] );
        assertNotNull( result[1] );
    }
    
    public void testTimeSpanOpenEndedStart() throws Exception {
        setupTemplate(MockData.PRIMITIVEGEOFEATURE,"time.ftl","||${dateProperty.value}");
        FeatureTimeTemplate template = new FeatureTimeTemplate();
        String[] result = template.execute( feature );
        
        assertEquals( 2, result.length );
        assertNull( result[0] );
        assertNotNull( result[1] );
    }
    
    public void testTimeSpanOpenEndedEnd() throws Exception {
        setupTemplate(MockData.PRIMITIVEGEOFEATURE,"time.ftl","${dateProperty.value}||");
        FeatureTimeTemplate template = new FeatureTimeTemplate();
        String[] result = template.execute( feature );
        
        assertEquals( 2, result.length );
        assertNotNull( result[0] );
        assertNull( result[1] );
    }
}
