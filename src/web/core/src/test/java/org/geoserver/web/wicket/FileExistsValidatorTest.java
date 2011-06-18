package org.geoserver.web.wicket;

import java.io.File;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.web.StringValidatable;

public class FileExistsValidatorTest extends GeoServerTestSupport {

    private static FileExistsValidator validator;

    public static Test suite() {
        validator = new FileExistsValidator();
        return new OneTimeTestSetup(new FileExistsValidatorTest());
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWellKnownCoverageTypes();
    }
    
    public void testAbsoluteRaw() throws Exception {
        File tazbm = new File(getTestData().getDataDirectoryRoot(), "wcs/BlueMarble.tiff");
        StringValidatable validatable = new StringValidatable(tazbm.getAbsolutePath());
        
        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }
    
    public void testAbsoluteURI() throws Exception {
        File tazbm = new File(getTestData().getDataDirectoryRoot(), "wcs/BlueMarble.tiff");
        StringValidatable validatable = new StringValidatable(tazbm.toURI().toString());
        
        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }
    
    public void testRelative() throws Exception {
        StringValidatable validatable = new StringValidatable("file:wcs/BlueMarble.tiff");
        
        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }
    

}
