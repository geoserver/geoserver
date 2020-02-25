/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Predicates;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.opengis.coverage.grid.Format;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

public class SecuredGridCoverage2DReaderTest extends SecureObjectsTest {

    @Test
    public void testFilter() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");

        // create the mocks we need
        Format format = setupFormat();
        GridCoverage2DReader reader = createNiceMock(GridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits =
                new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        SecuredGridCoverage2DReader secured =
                new SecuredGridCoverage2DReader(reader, WrapperPolicy.readOnlyHide(accessLimits));

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(new GeneralParameterValue[] {pv});
    }

    @Test
    public void testFilterOnStructured() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");
        DefaultSecureDataFactory factory = new DefaultSecureDataFactory();

        // create the mocks we need
        Format format = setupFormat();
        StructuredGridCoverage2DReader reader =
                createNiceMock(StructuredGridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits =
                new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        Object securedObject = factory.secure(reader, WrapperPolicy.readOnlyHide(accessLimits));
        assertTrue(securedObject instanceof SecuredStructuredGridCoverage2DReader);
        SecuredStructuredGridCoverage2DReader secured =
                (SecuredStructuredGridCoverage2DReader) securedObject;

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(new GeneralParameterValue[] {pv});
    }

    private static void setupReadAssertion(
            GridCoverage2DReader reader, final Filter requestFilter, final Filter securityFilter)
            throws IOException {
        // the assertion
        expect(reader.read(isA(GeneralParameterValue[].class)))
                .andAnswer(
                        new IAnswer<GridCoverage2D>() {

                            @Override
                            public GridCoverage2D answer() throws Throwable {
                                GeneralParameterValue[] params =
                                        (GeneralParameterValue[]) EasyMock.getCurrentArguments()[0];
                                ParameterValue param = (ParameterValue) params[0];
                                Filter filter = (Filter) param.getValue();
                                assertEquals(Predicates.and(requestFilter, securityFilter), filter);
                                return null;
                            }
                        });
        EasyMock.replay(reader);
    }

    private Format setupFormat() {
        Format format = createNiceMock(Format.class);
        expect(format.getReadParameters())
                .andReturn(new ImageMosaicFormat().getReadParameters())
                .anyTimes();
        EasyMock.replay(format);
        return format;
    }
}
