/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
package org.geoserver.wcs2_0;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.TestResourceAccessManager;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

public class ResourceAccessManagerWCSTest extends WCSTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final String UNITS = "foot";
    protected static final String UNIT_SYMBOL = "ft";

    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/wcs2_0/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** Add the users */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        File security = new File(testData.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("admin", "geoserver,ROLE_ADMINISTRATOR");
        props.put("cite", "cite,ROLE_DUMMY");
        props.store(new FileOutputStream(users), "");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // these users follow a full auth
        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        // a raster with dimensions
        Catalog catalog = getCatalog();
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, catalog);
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.ELEVATION,
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        // populate the access manager
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        CoverageInfo waterTemp = catalog.getCoverageByName(getLayerId(WATTEMP));
        tam.putLimits(
                "cite",
                waterTemp,
                new CoverageAccessLimits(CatalogMode.CHALLENGE, Filter.EXCLUDE, null, null));
    }

    /**
     * DescribeCoverage requires a special exemption to run as it needs to access actual data to
     * fill in the time and elevation
     */
    @Test
    public void testDescribeWithTimeElevation() throws Exception {
        setRequestAuth("cite", "cite");
        Document dom =
                getAsDOM(
                        "wcs?request=DescribeCoverage&service=WCS&version=2.0.0&coverageId=sf__watertemp");
        print(dom);

        // print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant)",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/gml:timePosition",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/gml:timePosition",
                dom);
        // and a list of elevations
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue)",
                dom);
        assertXpathEvaluatesTo(
                "0.0", "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/@default", dom);
        assertXpathEvaluatesTo(
                "ft", "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/@uom", dom);
    }
}
