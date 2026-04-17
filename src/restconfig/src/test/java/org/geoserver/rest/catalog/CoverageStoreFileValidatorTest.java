/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredGridFormat;
import org.geotools.coverage.grid.io.UnknownFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.image.WorldImageFormat;
import org.geotools.util.URLs;
import org.junit.jupiter.api.Test;

public class CoverageStoreFileValidatorTest {

    /** Tests a geotiff (this is a single-file upload of a geotiff). */
    @Test
    public void testValidGeotiff() throws IOException {
        InputStream stream = getFile("test-data/NCOM_wattemp_020_20081031T0000000_12.tiff");

        CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
        validator.accept(stream, "NCOM_wattemp_020_20081031T0000000_12.tiff");
        // should not throw
    }

    /** Tests a geotiff INSIDE a zip (zip with a geotiff inside). */
    @Test
    public void testValidGeotiffZip() throws IOException {
        InputStream stream = getFile("test-data/NCOM_wattemp_020_20081031T0000000_12.zip");

        CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
        validator.accept(stream, "NCOM_wattemp_020_20081031T0000000_12.zip");
        // should not throw
    }

    /** tests a world image file (multiple files inside a .zip). */
    @Test
    public void testValidWorldImageZip() throws IOException {
        InputStream stream = getFile("test-data/usa.zip");
        CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new WorldImageFormat());
        validator.accept(stream, "usa.zip");
        // should not throw
    }

    /** tests a world image file reader with an empty zip. */
    @Test
    public void testEmptyWorldImageZip() throws IOException {
        assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/empty-zip.zip");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new WorldImageFormat());
            validator.accept(stream, "empty-zip.zip");
        });
    }

    /** tests a geotiff file reader with an empty zip. */
    @Test
    public void testEmptyGeoTiffZip() throws IOException {
        assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/empty-zip.zip");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
            validator.accept(stream, "empty-zip.zip");
        });
    }

    /**
     * malicious.zip is a zip file that tries to "escape" the directory its being unzipped into.
     *
     * <p>1. mkdir -p payload/../../tmp/ <br>
     * 2. echo "Malicious Payload" > payload/../../tmp/evil.txt <br>
     * 3.zip -y malicious.zip payload/../../tmp/evil.txt
     */
    @Test
    public void testZipEscape() throws IOException {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/malicious.zip");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
            validator.accept(stream, "malicious.zip");
        });
        assertTrue(throwable.getMessage().contains("escape"));
    }

    /** Checks that you cannot have a ".." in the filename. */
    @Test
    public void testFileNameEscape() {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/usa.zip");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
            validator.accept(stream, "../usa.zip");
        });
        assertTrue(throwable.getMessage().contains("fname is illegal"));
    }

    /** try to upload a text file as a coverage. */
    @Test
    public void testSimpleInvalidFile() throws IOException {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/text-file.txt");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
            validator.accept(stream, "image.geotiff");
        });
        assertTrue(throwable.getMessage().contains("Unsupported"));
    }

    /** try to upload a text file in a zipas a coverage. */
    @Test
    public void testSimpleInvalidFileZip() {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            InputStream stream = getFile("test-data/text-file.zip");
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new GeoTiffFormat());
            validator.accept(stream, "image.geotiff");
        });
        assertTrue(throwable.getMessage().contains("primary"));
    }

    /** null format as input (bad) */
    @Test
    public void testMustBeValidFormat() {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(null);
        });
        assertTrue(throwable.getMessage().contains("format"));
    }

    /** UnknownFormat format as input (bad) */
    @Test
    public void testMustBeValidFormat2() {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(new UnknownFormat());
        });
        assertTrue(throwable.getMessage().contains("format"));
    }

    /** bad format - Format is not a subclass of AbstractGridFormat. */
    @Test
    public void testMustBeValidFormat3() {
        Throwable throwable = assertThrows(Throwable.class, () -> {
            WrapperPolicy wp = WrapperPolicy.hide(null);
            SecuredGridFormat securedGridFormat = new SecuredGridFormat(new GeoTiffFormat(), wp);
            CoverageStoreFileValidator validator = new CoverageStoreFileValidator(securedGridFormat);
        });
        assertTrue(throwable.getMessage().contains("AbstractGridFormat"));
    }

    /**
     * gets an InputStream from a resource.
     *
     * @param fname name of the resource
     * @return InputStream to read the file
     * @throws IOException problem occurred - wrong file name?
     */
    public InputStream getFile(String fname) throws IOException {
        URL file = getClass().getResource(fname);
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(file));
        return new ByteArrayInputStream(bytes);
    }
}
