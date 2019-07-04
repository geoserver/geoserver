/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.geoserver.web.StringValidatable;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileExistsValidatorTest {

    private static File root;
    private static FileExistsValidator validator;

    @BeforeClass
    public static void init() throws IOException {
        root = File.createTempFile("file", "tmp", new File("target"));
        root.delete();
        root.mkdirs();

        File wcs = new File(root, "wcs");
        wcs.mkdir();

        Files.touch(new File(wcs, "BlueMarble.tiff"));

        validator = new FileExistsValidator();
        validator.baseDirectory = root;
    }

    @Test
    public void testAbsoluteRaw() throws Exception {
        File tazbm = new File(root, "wcs/BlueMarble.tiff");
        StringValidatable validatable = new StringValidatable(tazbm.getAbsolutePath());

        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }

    @Test
    public void testAbsoluteURI() throws Exception {
        File tazbm = new File(root, "wcs/BlueMarble.tiff");
        StringValidatable validatable = new StringValidatable(tazbm.toURI().toString());

        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }

    @Test
    public void testRelative() throws Exception {
        StringValidatable validatable = new StringValidatable("file:wcs/BlueMarble.tiff");

        validator.validate(validatable);
        assertTrue(validatable.isValid());
    }
}
