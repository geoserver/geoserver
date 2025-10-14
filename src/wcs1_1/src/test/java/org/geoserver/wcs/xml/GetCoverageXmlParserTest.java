/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Arrays;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.GridCrsType;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.wcs.v1_1.WCSConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class GetCoverageXmlParserTest {

    private WCSConfiguration configuration;

    private WcsXmlReader reader;

    @Before
    public void setUp() throws Exception {
        configuration = new WCSConfiguration();
        reader = new WcsXmlReader(
                "GetCoverage", "1.1.1", configuration, EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
    }

    @Test
    public void testInvalid() throws Exception {
        String request =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wcs:GetCoverage service="WCS" \
                xmlns:ows="http://www.opengis.net/ows/1.1"
                  xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wcs/1.1.1 \
                                       schemas/wcs/1.1.1/wcsAll.xsd"
                  version="1.1.1" >
                  <Identifier>wcs:BlueMarble</Identifier>
                    <ows:BoundingBox crs="urn:ogc:def:crs:EPSG:6.6:4326">
                      <ows:LowerCorner>-90 -180</ows:LowerCorner>
                      <ows:UpperCorner>90 180</ows:UpperCorner>
                    </ows:BoundingBox>
                  <wcs:Output format="image/tiff"/>
                </wcs:GetCoverage>""";

        try {
            reader.read(null, new StringReader(request), null);
            fail("This request is not valid!!!");
        } catch (WcsException e) {
            // ok, we do expect a validation exception in fact
        }
    }

    @Test
    public void testBasic() throws Exception {
        String request =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wcs:GetCoverage service="WCS" \
                xmlns:ows="http://www.opengis.net/ows/1.1"
                  xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wcs/1.1.1 \
                                       schemas/wcs/1.1.1/wcsAll.xsd"
                  version="1.1.1" >
                  <ows:Identifier>wcs:BlueMarble</ows:Identifier>
                  <wcs:DomainSubset>
                    <ows:BoundingBox crs="urn:ogc:def:crs:EPSG:6.6:4326">
                      <ows:LowerCorner>-90 -180</ows:LowerCorner>
                      <ows:UpperCorner>90 180</ows:UpperCorner>
                    </ows:BoundingBox>
                  </wcs:DomainSubset>
                  <wcs:Output format="image/tiff"/>
                </wcs:GetCoverage>""";
        // System.out.println(request);

        // smoke test, we only try out a very basic request
        GetCoverageType gc = (GetCoverageType) reader.read(null, new StringReader(request), null);
        assertEquals("WCS", gc.getService());
        assertEquals("1.1.1", gc.getVersion());
        assertEquals("wcs:BlueMarble", gc.getIdentifier().getValue());
        assertEquals(
                "urn:ogc:def:crs:EPSG:6.6:4326",
                gc.getDomainSubset().getBoundingBox().getCrs());
        assertEquals(
                Arrays.asList(-90.0, -180.0),
                gc.getDomainSubset().getBoundingBox().getLowerCorner());
        assertEquals(
                Arrays.asList(90.0, 180.0),
                gc.getDomainSubset().getBoundingBox().getUpperCorner());
        assertEquals("image/tiff", gc.getOutput().getFormat());
        assertNull(gc.getOutput().getGridCRS());
    }

    @Test
    public void testRangeSubsetKeys() throws Exception {
        String request =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wcs:GetCoverage service="WCS" \
                xmlns:ows="http://www.opengis.net/ows/1.1"
                  xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wcs/1.1.1 \
                schemas/wcs/1.1.1/wcsAll.xsd"
                  version="1.1.1" >
                  <ows:Identifier>wcs:BlueMarble</ows:Identifier>
                  <wcs:DomainSubset>
                    <ows:BoundingBox crs="urn:ogc:def:crs:EPSG:6.6:4326">
                      <ows:LowerCorner>-90 -180</ows:LowerCorner>
                      <ows:UpperCorner>90 180</ows:UpperCorner>
                    </ows:BoundingBox>
                  </wcs:DomainSubset>
                  <wcs:RangeSubset>
                    <wcs:FieldSubset>
                      <ows:Identifier>BlueMarble</ows:Identifier>
                      <wcs:InterpolationType>bicubic</wcs:InterpolationType>
                      <wcs:AxisSubset>
                        <wcs:Identifier>Bands</wcs:Identifier>
                        <wcs:Key>Red_band</wcs:Key>
                      </wcs:AxisSubset>
                    </wcs:FieldSubset>
                  </wcs:RangeSubset>
                  <wcs:Output format="image/tiff"/>
                </wcs:GetCoverage>""";

        GetCoverageType gc = (GetCoverageType) reader.read(null, new StringReader(request), null);
        assertEquals(1, gc.getRangeSubset().getFieldSubset().size());
        FieldSubsetType field =
                (FieldSubsetType) gc.getRangeSubset().getFieldSubset().get(0);
        assertEquals("BlueMarble", field.getIdentifier().getValue());
        assertEquals("bicubic", field.getInterpolationType());
        assertEquals(1, field.getAxisSubset().size());
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        assertEquals("Bands", axis.getIdentifier());
        assertEquals(1, axis.getKey().size());
        String key = (String) axis.getKey().get(0);
        assertEquals("Red_band", key);
    }

    @Test
    public void testGridCRS() throws Exception {
        String request =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wcs:GetCoverage service="WCS" \
                xmlns:ows="http://www.opengis.net/ows/1.1"
                  xmlns:wcs="http://www.opengis.net/wcs/1.1.1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.opengis.net/wcs/1.1.1 \
                schemas/wcs/1.1.1/wcsAll.xsd"
                  version="1.1.1" >
                  <ows:Identifier>wcs:BlueMarble</ows:Identifier>
                  <wcs:DomainSubset>
                    <ows:BoundingBox crs="urn:ogc:def:crs:EPSG:6.6:4326">
                      <ows:LowerCorner>-90 -180</ows:LowerCorner>
                      <ows:UpperCorner>90 180</ows:UpperCorner>
                    </ows:BoundingBox>
                  </wcs:DomainSubset>
                  <wcs:Output format="image/tiff">
                    <wcs:GridCRS>
                      <wcs:GridBaseCRS>urn:ogc:def:crs:EPSG:6.6:4326</wcs:GridBaseCRS>
                      <wcs:GridType>urn:ogc:def:method:WCS:1.1:2dSimpleGrid</wcs:GridType>
                      <wcs:GridOrigin>10 20</wcs:GridOrigin>
                      <wcs:GridOffsets>1 2</wcs:GridOffsets>
                      <wcs:GridCS>urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS</wcs:GridCS>
                    </wcs:GridCRS>
                  </wcs:Output>
                </wcs:GetCoverage>""";

        GetCoverageType gc = (GetCoverageType) reader.read(null, new StringReader(request), null);
        final GridCrsType gridCRS = gc.getOutput().getGridCRS();
        assertEquals("urn:ogc:def:crs:EPSG:6.6:4326", gridCRS.getGridBaseCRS());
        assertEquals("urn:ogc:def:method:WCS:1.1:2dSimpleGrid", gridCRS.getGridType());
        assertEquals("urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS", gridCRS.getGridCS());
        // System.out.println(gridCRS.getGridOrigin().getClass() + ": " + gridCRS.getGridOrigin());
        assertArrayEquals(new Double[] {10.0, 20.0}, (Double[]) gridCRS.getGridOrigin());
        assertArrayEquals(new Double[] {1.0, 2.0}, (Double[]) gridCRS.getGridOffsets());
    }
}
