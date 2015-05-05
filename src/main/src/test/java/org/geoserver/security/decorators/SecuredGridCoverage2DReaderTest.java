package org.geoserver.security.decorators;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;

import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.geoserver.catalog.Predicates;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
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
        Format format = createNiceMock(Format.class);
        expect(format.getReadParameters()).andReturn(new ImageMosaicFormat().getReadParameters())
                .anyTimes();
        EasyMock.replay(format);
        GridCoverage2DReader reader = createNiceMock(GridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();


        // the assertion
        expect(reader.read(isA(GeneralParameterValue[].class))).andAnswer(
                new IAnswer<GridCoverage2D>() {

                    @Override
                    public GridCoverage2D answer() throws Throwable {
                        GeneralParameterValue[] params = (GeneralParameterValue[]) EasyMock
                                .getCurrentArguments()[0];
                        ParameterValue param = (ParameterValue) params[0];
                        Filter filter = (Filter) param.getValue();
                        assertEquals(Predicates.and(requestFilter, securityFilter), filter);

                        return null;
                    }
                });
        EasyMock.replay(reader);

        CoverageAccessLimits accessLimits = new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        SecuredGridCoverage2DReader secured = new SecuredGridCoverage2DReader(reader,
                WrapperPolicy.readOnlyHide(accessLimits));

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(new GeneralParameterValue[] { pv });

    }
}
