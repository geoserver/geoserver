/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2009 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test GetPropertyValue request, combined with local resolves
 *
 * @author Niels Charlier
 */
public class GetPropertyValueTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XLink32MockData createTestData() {
        return new XLink32MockData();
    }

    /** Test GetPropertyValue for a simple property, tests only selected property is returned */
    @Test
    public void testGetPropertyValue() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&valueReference=gml:name");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("GUNTHORPE FORMATION", "//wfs:member[1]/gml:name", doc);
        assertXpathCount(4, "//gml:name", doc);
        assertXpathCount(0, "//gsml:shape", doc);
        assertXpathCount(0, "//gsml:specification", doc);
    }

    /** Test GetPropertyValue without local resolve */
    @Test
    public void testNoResolve() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&valueReference=gsml:specification&resolve=none");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25699",
                "//wfs:member[1]/gsml:specification/@xlink:href",
                doc);
        assertXpathCount(0, "//gsml:GeologicUnit", doc);
        assertXpathCount(0, "//gsml:CompositionPart", doc);
    }

    /** Test GetPropertyValue with Local Resolve with Depth 2. */
    @Test
    public void testResolveDepth2() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&resolve=local&valueReference=gsml:specification&resolveDepth=2");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathEvaluatesTo(
                "gu.25699", "//gsml:specification[1]/gsml:GeologicUnit/@gml:id", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//wfs:member[1]/gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:role/@xlink:href",
                doc);
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
        assertXpathCount(3, "//gsml:CompositionPart", doc);

        // now test x-path & multi-valued attributes

        doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&resolve=local&valueReference=gsml:specification/gsml:GeologicUnit/gml:name&resolveDepth=2");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));
        assertXpathCount(10, "//gml:name", doc);
        assertXpathEvaluatesTo("Yaugher Volcanic Group", "//wfs:member[1]/gml:name", doc);
        assertXpathEvaluatesTo("-Py", "//wfs:member[2]/gml:name", doc);

        doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&resolve=local&valueReference=gsml:specification/gsml:GeologicUnit/gml:name[1]&resolveDepth=2");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));
        assertXpathCount(4, "//gml:name", doc);
        assertXpathEvaluatesTo("Yaugher Volcanic Group", "//wfs:member[1]/gml:name", doc);

        doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&resolve=local&valueReference=gsml:specification/gsml:GeologicUnit/gml:name[2]&resolveDepth=2");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));
        assertXpathCount(4, "//gml:name", doc);
        assertXpathEvaluatesTo("-Py", "//wfs:member[1]/gml:name", doc);
    }

    /** Test GetPropertyValue with Local Resolve with Depth 1. */
    @Test
    public void testResolveDepth1() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&valueReference=gsml:specification&resolve=local&resolveDepth=1");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathEvaluatesTo(
                "gu.25699", "//wfs:member[1]/gsml:specification/gsml:GeologicUnit/@gml:id", doc);
        assertXpathEvaluatesTo(
                "urn:x-test:CompositionPart:cp.167775491936278899",
                "//wfs:member[1]/gsml:specification/gsml:GeologicUnit/gsml:composition/@xlink:href",
                doc);
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
        assertXpathCount(0, "//gsml:CompositionPart", doc);
    }

    /** Test GetPropertyValue with count parameter */
    @Test
    public void testGetPropertyValueMax() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0.0&typename=gsml:MappedFeature&valueReference=gml:name&count=2");

        LOGGER.info("WFS GetPropertyValue response:\n" + prettyString(doc));

        assertXpathCount(2, "//gml:name", doc);
    }
}
