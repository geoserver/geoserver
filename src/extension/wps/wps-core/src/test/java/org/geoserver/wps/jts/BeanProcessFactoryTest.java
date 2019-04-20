/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.vector.BoundsProcess;
import org.geotools.process.vector.NearestProcess;
import org.geotools.process.vector.SnapProcess;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.factory.FactoryIteratorProvider;
import org.geotools.util.factory.GeoTools;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Tests some processes that do not require integration with the application context
 *
 * @author Andrea Aime - OpenGeo
 */
public class BeanProcessFactoryTest extends WPSTestSupport {

    public class BeanProcessFactory extends AnnotatedBeanProcessFactory {

        public BeanProcessFactory() {
            super(
                    new SimpleInternationalString("Some bean based processes custom processes"),
                    "bean",
                    BoundsProcess.class,
                    NearestProcess.class,
                    SnapProcess.class);
        }
    }

    BeanProcessFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new BeanProcessFactory();

        // check SPI will see the factory if we register it using an iterator
        // provider
        GeoTools.addFactoryIteratorProvider(
                new FactoryIteratorProvider() {

                    public <T> Iterator<T> iterator(Class<T> category) {
                        if (ProcessFactory.class.isAssignableFrom(category)) {
                            return (Iterator<T>) Collections.singletonList(factory).iterator();
                        } else {
                            return null;
                        }
                    }
                });
    }

    @Test
    public void testNames() {
        Set<Name> names = factory.getNames();
        assertTrue(names.size() > 0);
        // System.out.println(names);
        assertTrue(names.contains(new NameImpl("bean", "Bounds")));
    }

    @Test
    public void testDescribeBounds() {
        NameImpl boundsName = new NameImpl("bean", "Bounds");
        InternationalString desc = factory.getDescription(boundsName);
        assertNotNull(desc);

        Map<String, Parameter<?>> params = factory.getParameterInfo(boundsName);
        assertEquals(1, params.size());

        Parameter<?> features = params.get("features");
        assertEquals(FeatureCollection.class, features.type);
        assertTrue(features.required);

        Map<String, Parameter<?>> result = factory.getResultInfo(boundsName, null);
        assertEquals(1, result.size());
        Parameter<?> bounds = result.get("bounds");
        assertEquals(ReferencedEnvelope.class, bounds.type);
    }

    @Test
    public void testExecuteBounds() throws ProcessException {
        // prepare a mock feature collection
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("test");
        final ReferencedEnvelope re = new ReferencedEnvelope(-10, 10, -10, 10, null);
        FeatureCollection fc =
                new ListFeatureCollection(tb.buildFeatureType()) {
                    @Override
                    public synchronized ReferencedEnvelope getBounds() {
                        return re;
                    }
                };

        org.geotools.process.Process p = factory.create(new NameImpl("bean", "Bounds"));
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put("features", fc);
        Map<String, Object> result = p.execute(inputs, null);

        assertEquals(1, result.size());
        ReferencedEnvelope computed = (ReferencedEnvelope) result.get("bounds");
        assertEquals(re, computed);
    }

    @Test
    public void testSPI() throws Exception {
        NameImpl boundsName = new NameImpl("bean", "Bounds");
        ProcessFactory factory = GeoServerProcessors.createProcessFactory(boundsName, false);
        assertNotNull(factory);

        org.geotools.process.Process buffer = GeoServerProcessors.createProcess(boundsName);
        assertNotNull(buffer);
    }
}
