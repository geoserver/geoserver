/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

public class RetypingFeatureCollectionTest {
    
    FeatureVisitor lastVisitor = null;
    private ListFeatureCollection collection;
    private SimpleFeatureType renamedSchema;
    
    @Before
    public void setup() throws SchemaException {
        SimpleFeatureType originalSchema = DataUtilities.createType("BasicPolygons", "the_geom:MultiPolygon:srid=4326,ID:String,value:int");
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.init(originalSchema);
        tb.setName("BasicPolygons2");
        renamedSchema = tb.buildFeatureType();
        
        collection = new ListFeatureCollection(originalSchema) {
            public void accepts(FeatureVisitor visitor, ProgressListener progress) throws java.io.IOException {
                lastVisitor = visitor; 
            };
        };
    }

    @Test
    public void testMaxVisitorDelegation() throws SchemaException, IOException {
        MaxVisitor visitor = new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        assertOptimalVisit(visitor);
    }
    
    @Test
    public void testCountVisitorDelegation() throws SchemaException, IOException {
        FeatureVisitor visitor = new CountVisitor();
        assertOptimalVisit(visitor);
    }


    private void assertOptimalVisit(FeatureVisitor visitor) throws IOException {
        RetypingFeatureCollection retypedCollection = new RetypingFeatureCollection(collection, renamedSchema);
        retypedCollection.accepts(visitor, null);
        assertSame(lastVisitor, visitor);
    }
}
