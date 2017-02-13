/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests covering the former functionality of GeoServerDataDirectory.
 * 
 * Much of this functionality depends on the availability of GeoServerResourceLoader
 * in the application context as the bean "resourceLoader".
 * 
 * @author Antonio Cerciello - Geocat
 */
public class IndexComparatorsTest {
    
    @Test
    public void TestFeatureTypeSort() throws IOException {
        
        Set<FeatureTypeInfo> unordered = new TreeSet<FeatureTypeInfo>(new FeatureTypeInfoIndexComparator());
        
        FeatureTypeInfo f1 = new FeatureTypeInfoImpl(null);
        f1.setName("A");
        f1.setTitle("A");
        f1.setSortIndex(2);
        unordered.add(f1);
        FeatureTypeInfo f2 = new FeatureTypeInfoImpl(null);
        f2.setName("C");
        f2.setTitle("C");
        f2.setSortIndex(1);
        unordered.add(f2);
        FeatureTypeInfo f3 = new FeatureTypeInfoImpl(null);
        f3.setName("B");
        f3.setTitle("B");
        f3.setSortIndex(1);
        unordered.add(f3);
        
        StringBuffer s1 = new StringBuffer();        
        
        for (FeatureTypeInfo featureTypeInfo : unordered) {
            s1.append(featureTypeInfo.getName());
        }
        
        Assert.assertEquals(s1.toString(), "BCA");
        
    }
    
    @Test
    public void TestSort() throws IOException {
        
        Set<ResourceInfo> unordered = new TreeSet<ResourceInfo>(new CoverageInfoIndexComparator());
        
        ResourceInfo f1 = new CoverageInfoImpl(null);
        f1.setName("A");
        f1.setTitle("A");
        f1.setSortIndex(2);
        unordered.add(f1);
        ResourceInfo f2 = new CoverageInfoImpl(null);
        f2.setName("C");
        f2.setTitle("C");
        f2.setSortIndex(1);
        unordered.add(f2);
        ResourceInfo f3 = new CoverageInfoImpl(null);
        f3.setName("B");
        f3.setTitle("B");
        f3.setSortIndex(1);
        unordered.add(f3);
        
        StringBuffer s1 = new StringBuffer();        
        
        for (ResourceInfo featureTypeInfo : unordered) {
            s1.append(featureTypeInfo.getName());
        }
        
        Assert.assertEquals(s1.toString(), "BCA");
        
    }

}
