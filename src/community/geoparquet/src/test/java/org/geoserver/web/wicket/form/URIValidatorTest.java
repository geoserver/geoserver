/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Comprehensive tests for the {@link URIValidator} class. */
@SuppressWarnings("unchecked")
public class URIValidatorTest {

    /** Temporary directory for file-based tests */
    private Path tempDir;

    /** Test files created during setup */
    private Path testFile;

    private Path testDir;
    private Path nestedDir;

    @SuppressWarnings("unused")
    private Path nestedFile;

    /** Sets up the test environment with temporary directories and files. */
    @Before
    public void setUp() throws IOException {
        // Create temporary test directory
        tempDir = Files.createTempDirectory("urivalidator-test");

        // Create test files and directories
        testFile = Files.createFile(tempDir.resolve("test-file.parquet"));
        testDir = Files.createDirectory(tempDir.resolve("test-dir"));
        nestedDir = Files.createDirectory(testDir.resolve("nested-dir"));
        nestedFile = Files.createFile(nestedDir.resolve("nested-file.parquet"));

        // Create multiple files for glob pattern tests
        Files.createFile(testDir.resolve("file1.parquet"));
        Files.createFile(testDir.resolve("file2.parquet"));
        Files.createFile(testDir.resolve("file3.txt"));
        Files.createFile(nestedDir.resolve("nested1.parquet"));
        Files.createFile(nestedDir.resolve("nested2.parquet"));
    }

    /** Cleans up temporary files and directories after tests. */
    @After
    public void tearDown() throws IOException {
        // Clean up the temporary directory
        if (tempDir != null && Files.exists(tempDir)) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    /** Helper method to create a mock IValidatable for testing validation. */
    private IValidatable<String> createMockValidatable(String value) {
        IValidatable<String> validatable = Mockito.mock(IValidatable.class);
        Mockito.when(validatable.getValue()).thenReturn(value);
        return validatable;
    }

    /** Helper method to assert that a validation produced no errors. */
    private void assertNoValidationErrors(IValidatable<String> validatable) {
        Mockito.verify(validatable, Mockito.never()).error(Mockito.any(ValidationError.class));
    }

    /** Helper method to assert that a validation produced a specific error. */
    private void assertValidationErrorWithKey(IValidatable<String> validatable, String expectedKey) {
        ArgumentCaptor<ValidationError> errorCaptor = ArgumentCaptor.forClass(ValidationError.class);
        Mockito.verify(validatable, Mockito.atLeastOnce()).error(errorCaptor.capture());

        boolean foundExpectedKey = false;
        for (ValidationError error : errorCaptor.getAllValues()) {
            List<String> keys = error.getKeys();
            if (keys.contains(expectedKey)) {
                foundExpectedKey = true;
                break;
            }
        }

        if (!foundExpectedKey) {
            fail("Expected validation error with key '" + expectedKey + "' but found: "
                    + errorCaptor.getAllValues().stream()
                            .flatMap(e -> e.getKeys().stream())
                            .reduce("", (a, b) -> a + ", " + b));
        }
    }

    /** Tests basic URI validation with standard URIs. */
    @Test
    public void testBasicURIValidation() {
        URIValidator validator = new URIValidator();

        // Valid URIs should not produce errors
        IValidatable<String> validHttpUri = createMockValidatable("http://example.com/data.parquet");
        validator.validate(validHttpUri);
        assertNoValidationErrors(validHttpUri);

        IValidatable<String> validHttpsUri = createMockValidatable("https://example.com/data.parquet");
        validator.validate(validHttpsUri);
        assertNoValidationErrors(validHttpsUri);

        IValidatable<String> validS3Uri = createMockValidatable("s3://bucket/path/data.parquet");
        validator.validate(validS3Uri);
        assertNoValidationErrors(validS3Uri);

        // Invalid URI should produce an error
        IValidatable<String> invalidUri = createMockValidatable("http://ex ample.com/data.parquet");
        validator.validate(invalidUri);
        assertValidationErrorWithKey(invalidUri, "URIValidator.invalidURI");
    }

    /** Tests validation of URI schemes against allowed schemes. */
    @Test
    public void testAllowedSchemes() {
        // Configure validator to only allow HTTP and HTTPS
        URIValidator validator = new URIValidator().allowedSchemes("http", "https");

        // HTTP should be valid
        IValidatable<String> httpUri = createMockValidatable("http://example.com/data.parquet");
        validator.validate(httpUri);
        assertNoValidationErrors(httpUri);

        // HTTPS should be valid
        IValidatable<String> httpsUri = createMockValidatable("https://example.com/data.parquet");
        validator.validate(httpsUri);
        assertNoValidationErrors(httpsUri);

        // S3 should be invalid
        IValidatable<String> s3Uri = createMockValidatable("s3://bucket/path/data.parquet");
        validator.validate(s3Uri);
        assertValidationErrorWithKey(s3Uri, "URIValidator.schemeNotAllowed");

        // File scheme should be invalid
        IValidatable<String> fileUri = createMockValidatable("file:/tmp/data.parquet");
        validator.validate(fileUri);
        assertValidationErrorWithKey(fileUri, "URIValidator.schemeNotAllowed");
    }

    /** Tests validation of file URIs with the File scheme. */
    @Test
    public void testFileURIValidation() {
        URIValidator validator = new URIValidator();

        // Valid file URI
        String fileUriStr = "file:" + testFile.toUri().getPath();
        IValidatable<String> validFileUri = createMockValidatable(fileUriStr);
        validator.validate(validFileUri);
        assertNoValidationErrors(validFileUri);

        // When fileMustExist is enabled, non-existent files should fail
        validator = new URIValidator().fileMustExist();

        // Existing file should be valid
        IValidatable<String> existingFile = createMockValidatable(fileUriStr);
        validator.validate(existingFile);
        assertNoValidationErrors(existingFile);

        // Non-existent file should be invalid
        String nonExistentFile =
                "file:" + tempDir.resolve("non-existent.parquet").toUri().getPath();
        IValidatable<String> nonExistentFileUri = createMockValidatable(nonExistentFile);
        validator.validate(nonExistentFileUri);
        assertValidationErrorWithKey(nonExistentFileUri, "URIValidator.fileNotFound");
    }

    /** Tests validation of file paths without the File scheme. */
    @Test
    public void testFilePathValidation() {
        // By default, paths without scheme are not allowed
        URIValidator validator = new URIValidator();

        // Path without scheme should be invalid
        IValidatable<String> pathWithoutScheme = createMockValidatable(testFile.toString());
        validator.validate(pathWithoutScheme);
        assertValidationErrorWithKey(pathWithoutScheme, "URIValidator.nullSchemeForbidden");

        // Enable allowing paths without scheme
        validator = new URIValidator().allowNullSchemeFileURI();

        // Path without scheme should now be valid
        IValidatable<String> validPath = createMockValidatable(testFile.toString());
        validator.validate(validPath);
        assertNoValidationErrors(validPath);

        // Add fileMustExist requirement
        validator = new URIValidator().allowNullSchemeFileURI().fileMustExist();

        // Existing file should be valid
        IValidatable<String> existingFilePath = createMockValidatable(testFile.toString());
        validator.validate(existingFilePath);
        assertNoValidationErrors(existingFilePath);

        // Non-existent file should be invalid
        String nonExistentPath = tempDir.resolve("non-existent.parquet").toString();
        IValidatable<String> nonExistentFilePath = createMockValidatable(nonExistentPath);
        validator.validate(nonExistentFilePath);
        assertValidationErrorWithKey(nonExistentFilePath, "URIValidator.fileNotFound");
    }

    /** Tests validation of glob patterns in file paths. */
    @Test
    public void testGlobPatternValidation() {
        // By default, glob patterns are not allowed
        URIValidator validator = new URIValidator().allowNullSchemeFileURI();

        // Path with glob pattern should be invalid
        String globPattern = testDir.resolve("*.parquet").toString();
        IValidatable<String> pathWithGlob = createMockValidatable(globPattern);
        validator.validate(pathWithGlob);
        assertValidationErrorWithKey(pathWithGlob, "URIValidator.globPatternsNotAllowed");

        // Enable glob patterns
        validator = new URIValidator().allowNullSchemeFileURI().allowGlob();

        // Path with glob pattern should now be valid
        IValidatable<String> validGlobPath = createMockValidatable(globPattern);
        validator.validate(validGlobPath);
        assertNoValidationErrors(validGlobPath);

        // Test with fileMustExist for glob patterns
        validator = new URIValidator().allowNullSchemeFileURI().allowGlob().fileMustExist();

        // Glob pattern matching existing files should be valid
        IValidatable<String> matchingGlob = createMockValidatable(globPattern);
        validator.validate(matchingGlob);
        assertNoValidationErrors(matchingGlob);

        // Glob pattern not matching any files should be invalid
        String nonMatchingGlob = testDir.resolve("*.nonexistent").toString();
        IValidatable<String> nonMatchingGlobPath = createMockValidatable(nonMatchingGlob);
        validator.validate(nonMatchingGlobPath);
        assertValidationErrorWithKey(nonMatchingGlobPath, "URIValidator.fileNotFound");

        // Test with recursive glob pattern
        String recursiveGlob = testDir.resolve("**/*.parquet").toString();
        IValidatable<String> recursiveGlobPath = createMockValidatable(recursiveGlob);
        validator.validate(recursiveGlobPath);
        assertNoValidationErrors(recursiveGlobPath);
    }

    /** Tests the containsGlobPattern method with a comprehensive set of test cases. */
    @Test
    public void testContainsGlobPattern() {
        // Simple paths without glob patterns
        assertFalse("Regular path without glob", URIValidator.containsGlobPattern("/regular/path/to/file.parquet"));
        assertFalse("HTTP URL without glob", URIValidator.containsGlobPattern("http://example.com/data.parquet"));
        assertFalse("S3 URI without glob", URIValidator.containsGlobPattern("s3://bucket/path/to/file.parquet"));

        // Basic glob patterns
        assertTrue("Should detect * as a glob pattern", URIValidator.containsGlobPattern("/path/*.parquet"));
        assertTrue("Should detect ** as a glob pattern", URIValidator.containsGlobPattern("/path/**/file.parquet"));
        assertTrue("Should detect ? as a glob pattern", URIValidator.containsGlobPattern("/path/file?.parquet"));

        // Complex glob patterns
        assertTrue("Should detect multiple globs", URIValidator.containsGlobPattern("/path/dir/**/*.parquet"));
        assertTrue("Should detect character class []", URIValidator.containsGlobPattern("/path/file[123].parquet"));
        assertTrue("Should detect alternatives {}", URIValidator.containsGlobPattern("/path/file{a,b,c}.parquet"));

        // Escaped glob characters should not be detected
        assertFalse("Should not detect escaped *", URIValidator.containsGlobPattern("/path/file\\*.parquet"));
        assertFalse("Should not detect escaped ?", URIValidator.containsGlobPattern("/path/file\\?.parquet"));
        assertFalse("Should not detect escaped []", URIValidator.containsGlobPattern("/path/file\\[123\\].parquet"));

        // URL-specific cases
        assertFalse(
                "Should not detect ? in query params",
                URIValidator.containsGlobPattern("http://example.com/data%20with%20spaces.parquet?param=value%26more"));
        assertFalse(
                "Should not detect encoded characters as globs",
                URIValidator.containsGlobPattern("http://example.com/path/to/data%20file.parquet"));
        assertTrue(
                "Should detect glob in URL path",
                URIValidator.containsGlobPattern("http://example.com/path/*/data.parquet"));
    }

    /**
     * Tests the getBaseDir method on various paths.
     *
     * <p>This test uses reflection to access the private method.
     */
    @Test
    public void testGetBaseDir() throws Exception {
        // Create a URIValidator instance
        URIValidator validator = new URIValidator();

        // Use reflection to get access to the private getBaseDir method
        java.lang.reflect.Method getBaseDirMethod = URIValidator.class.getDeclaredMethod("getBaseDir", Path.class);
        getBaseDirMethod.setAccessible(true);

        // Test with a simple file path (no glob pattern)
        Path simplePath = testFile;
        Path baseDir = (Path) getBaseDirMethod.invoke(validator, simplePath);
        assertEquals("For a simple file, the base dir should be the parent", simplePath.getParent(), baseDir);

        // Test with a directory path (no glob pattern)
        Path dirPath = testDir;
        baseDir = (Path) getBaseDirMethod.invoke(validator, dirPath);
        assertEquals("For a directory, the base dir should be the directory itself", dirPath, baseDir);

        // Test with a path containing a glob pattern in the filename
        Path fileGlobPath = tempDir.resolve("*.parquet");
        baseDir = (Path) getBaseDirMethod.invoke(validator, fileGlobPath);
        assertEquals("For a file glob, the base dir should be the parent directory", tempDir, baseDir);

        // Test with a path containing a recursive glob pattern
        Path recursiveGlobPath = tempDir.resolve("**/*.parquet");
        baseDir = (Path) getBaseDirMethod.invoke(validator, recursiveGlobPath);
        assertEquals("For a recursive glob, the base dir should be the starting directory", tempDir, baseDir);

        // Test with a path containing a glob pattern in a middle component
        Path middleGlobPath = tempDir.resolve("test-*").resolve("file.parquet");
        baseDir = (Path) getBaseDirMethod.invoke(validator, middleGlobPath);
        assertEquals("For a middle glob, the base dir should be the parent of the glob component", tempDir, baseDir);
    }

    /** Tests validation behavior with non-existing directories. */
    @Test
    public void testNonExistentDirectories() {
        // Create a validator that requires files to exist and allows glob patterns
        URIValidator validator =
                new URIValidator().allowNullSchemeFileURI().allowGlob().fileMustExist();

        // Non-existent directory with a glob pattern
        Path nonExistentDir = tempDir.resolve("non-existent-dir");
        String nonExistentGlob = nonExistentDir.resolve("*.parquet").toString();

        IValidatable<String> nonExistentDirGlob = createMockValidatable(nonExistentGlob);
        validator.validate(nonExistentDirGlob);
        assertValidationErrorWithKey(nonExistentDirGlob, "URIValidator.fileNotFound");
    }

    /** Tests validation of URIs with special characters and spaces. */
    @Test
    public void testURIsWithSpecialCharacters() {
        URIValidator validator = new URIValidator();

        // URI with encoded spaces and special characters should be valid
        IValidatable<String> uriWithEncodedChars =
                createMockValidatable("http://example.com/data%20with%20spaces.parquet?param=value%26more");
        validator.validate(uriWithEncodedChars);
        assertNoValidationErrors(uriWithEncodedChars);

        // URI with unencoded spaces should be invalid
        IValidatable<String> uriWithSpaces = createMockValidatable("http://example.com/data with spaces.parquet");
        validator.validate(uriWithSpaces);
        assertValidationErrorWithKey(uriWithSpaces, "URIValidator.invalidURI");
    }

    /** Tests combined validation scenarios with multiple requirements. */
    @Test
    public void testCombinedValidationScenarios() {
        // Configure a validator with multiple requirements
        URIValidator validator = new URIValidator()
                .allowedSchemes("file", "http", "https", "s3")
                .allowNullSchemeFileURI()
                .allowGlob()
                .fileMustExist();

        // Existing file path should be valid
        IValidatable<String> existingFile = createMockValidatable(testFile.toString());
        validator.validate(existingFile);
        assertNoValidationErrors(existingFile);

        // Non-existent file path should be invalid
        String nonExistentPath = tempDir.resolve("non-existent.parquet").toString();
        IValidatable<String> nonExistentFile = createMockValidatable(nonExistentPath);
        validator.validate(nonExistentFile);
        assertValidationErrorWithKey(nonExistentFile, "URIValidator.fileNotFound");

        // HTTP URL should be valid (fileMustExist doesn't apply to HTTP)
        IValidatable<String> httpUrl = createMockValidatable("http://example.com/data.parquet");
        validator.validate(httpUrl);
        assertNoValidationErrors(httpUrl);

        // FTP URL should be invalid (not in allowed schemes)
        IValidatable<String> ftpUrl = createMockValidatable("ftp://example.com/data.parquet");
        validator.validate(ftpUrl);
        assertValidationErrorWithKey(ftpUrl, "URIValidator.schemeNotAllowed");

        // Glob pattern matching existing files should be valid
        String matchingGlob = testDir.resolve("*.parquet").toString();
        IValidatable<String> matchingGlobPattern = createMockValidatable(matchingGlob);
        validator.validate(matchingGlobPattern);
        assertNoValidationErrors(matchingGlobPattern);

        // Glob pattern not matching any files should be invalid
        String nonMatchingGlob = testDir.resolve("*.nonexistent").toString();
        IValidatable<String> nonMatchingGlobPattern = createMockValidatable(nonMatchingGlob);
        validator.validate(nonMatchingGlobPattern);
        assertValidationErrorWithKey(nonMatchingGlobPattern, "URIValidator.fileNotFound");
    }
}
