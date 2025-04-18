/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Validates URIs and file paths with comprehensive syntax and existence checking capabilities.
 *
 * <p>This validator provides flexible validation of URI strings with features including:
 *
 * <ul>
 *   <li>Validation of basic URI syntax (scheme, path, etc.)
 *   <li>Support for file paths with or without "file:" scheme
 *   <li>Optional validation of file/directory existence
 *   <li>Support for glob patterns to match multiple files
 *   <li>Configurable allowed URI schemes (protocols)
 * </ul>
 *
 * <p>The validator is designed to be customizable through method chaining:
 *
 * <pre>
 * URIValidator validator = new URIValidator()
 *     .fileMustExist()
 *     .allowGlob()
 *     .allowNullSchemeFileURI()
 *     .allowedSchemes("file", "http", "https", "s3");
 * </pre>
 *
 * <p>It handles various edge cases including:
 *
 * <ul>
 *   <li>File paths with and without scheme prefixes
 *   <li>Network URIs with different protocols
 *   <li>Glob patterns for matching multiple files
 *   <li>Non-existent paths (optional validation)
 * </ul>
 */
@SuppressWarnings("serial")
public class URIValidator implements IValidator<String> {

    /**
     * Regular expression pattern that matches glob wildcards and special characters. Matches *, **, ?, [], {}, but
     * ignores escaped characters (e.g., \*).
     */
    private static final String GLOB_REGEX = "(?<!\\\\)[*?\\[\\]{}\\(\\)]|(?<!\\\\\\*)\\*\\*";

    /** Set of URI schemes/protocols that are allowed by this validator */
    private Set<String> allowedSchemes = new HashSet<>();

    /** Whether to allow file URIs without an explicit "file:" scheme */
    private boolean allowNullSchemeFileURI;

    /** Whether to validate that the referenced file or directory exists */
    private boolean fileMustExist;

    /** Whether to allow glob patterns in file paths */
    private boolean allowGlob;

    /**
     * Creates a new URIValidator with default settings.
     *
     * <p>By default, the validator:
     *
     * <ul>
     *   <li>Accepts all URI schemes/protocols
     *   <li>Requires an explicit URI scheme (like "file:")
     *   <li>Does not verify file existence
     *   <li>Does not allow glob patterns
     * </ul>
     */
    public URIValidator() {
        // By default, allow all schemes (with '*' as a wildcard)
        allowedSchemes.add("*");
    }

    /**
     * Validates that the input string is a valid URI according to the configured rules.
     *
     * <p>The validation process:
     *
     * <ol>
     *   <li>Checks basic URI syntax by attempting to create a {@link URI} object
     *   <li>Applies specific validation based on the URI scheme:
     *       <ul>
     *         <li>For file URIs (with or without "file:" scheme): validates path and existence
     *         <li>For other URIs: validates scheme and glob pattern usage
     *       </ul>
     * </ol>
     *
     * @param validatable The Wicket validatable object containing the URI string to validate
     */
    @Override
    public void validate(IValidatable<String> validatable) {
        URI uri;
        try {
            String value = validatable.getValue();
            uri = new URI(value);
        } catch (Exception e) {
            validatable.error(error("URIValidator.invalidURI", Map.of("uri", validatable.getValue())));
            return;
        }

        // Check scheme validity first, regardless of whether it's a file or other URI
        String scheme = uri.getScheme();
        if (!StringUtils.isEmpty(scheme) && !allowedSchemes.contains("*") && !allowedSchemes.contains(scheme)) {
            ValidationError error = error(
                    "URIValidator.schemeNotAllowed",
                    Map.of("scheme", scheme, "allowed", allowedSchemes.stream().collect(Collectors.joining(","))));
            validatable.error(error);
            return;
        }

        // Then proceed with specific validation based on scheme
        if (StringUtils.isEmpty(scheme) || "file".equals(scheme)) {
            validateFile(validatable, uri);
        } else {
            validateUri(validatable, uri);
        }
    }

    /**
     * Configures the validator to check that referenced files actually exist.
     *
     * <p>When this option is enabled, the validator will verify that:
     *
     * <ul>
     *   <li>For simple file paths: the file exists in the filesystem
     *   <li>For glob patterns: at least one matching file exists
     * </ul>
     *
     * <p><strong>Note:</strong> To enable validation of file URIs, the "file" scheme must be included in the allowed
     * schemes (via {@link #allowedSchemes(String...)}), or all schemes must be allowed by default. Otherwise, URIs with
     * the "file:" scheme will be rejected regardless of file existence.
     *
     * @return This validator instance for method chaining
     * @see #allowedSchemes(String...)
     */
    public URIValidator fileMustExist() {
        this.fileMustExist = true;
        return this;
    }

    /**
     * Configures the validator to accept glob patterns in file paths.
     *
     * <p>Glob patterns allow matching multiple files with wildcards and special characters:
     *
     * <ul>
     *   <li>* - matches any sequence of characters within a path component
     *   <li>** - matches any sequence of characters across multiple path components
     *   <li>? - matches a single character
     *   <li>[] - matches a single character from a character class
     *   <li>{} - matches a sequence from a group of patterns
     * </ul>
     *
     * @return This validator instance for method chaining
     */
    public URIValidator allowGlob() {
        this.allowGlob = true;
        return this;
    }

    /**
     * Configures the validator to accept file URIs without an explicit "file:" scheme.
     *
     * <p>When enabled, paths like "/data/file.parquet" will be treated as file URIs without requiring
     * "file:/data/file.parquet".
     *
     * <p><strong>Note:</strong> To enable validation of paths without the "file:" scheme, the "file" scheme must be
     * included in the allowed schemes (via {@link #allowedSchemes(String...)}), or all schemes must be allowed by
     * default. Even though these paths don't explicitly have a scheme, they will be treated as file URIs internally.
     *
     * @return This validator instance for method chaining
     * @see #allowedSchemes(String...)
     */
    public URIValidator allowNullSchemeFileURI() {
        this.allowNullSchemeFileURI = true;
        return this;
    }

    /**
     * Configures the validator to limit allowed URI schemes.
     *
     * <p>This method defines which URI protocols are acceptable in the input field. Common examples include "file",
     * "http", "https", "s3", etc.
     *
     * <p><strong>Important:</strong> If you want to validate file paths (with or without the "file:" scheme prefix),
     * you MUST include "file" in the list of allowed schemes. Otherwise, all file URIs will be rejected regardless of
     * other validation settings.
     *
     * <p>Example for allowing only file URIs and HTTP(S):
     *
     * <pre>
     * validator.allowedSchemes("file", "http", "https");
     * </pre>
     *
     * @param uriSchemes Zero or more allowed scheme strings. If none provided, all schemes are allowed.
     * @return This validator instance for method chaining
     * @see #allowNullSchemeFileURI()
     * @see #fileMustExist()
     */
    public URIValidator allowedSchemes(String... uriSchemes) {
        this.allowedSchemes.clear();
        if (uriSchemes == null || uriSchemes.length == 0) {
            this.allowedSchemes.add("*");
        } else {
            for (String scheme : uriSchemes) {
                Objects.requireNonNull(scheme);
                this.allowedSchemes.add(scheme);
            }
        }
        return this;
    }

    /**
     * Validates non-file URIs (http, https, s3, etc.).
     *
     * <p>This method performs two validations:
     *
     * <ol>
     *   <li>Checks if the URI contains glob patterns (and if they're allowed)
     *   <li>Verifies that the URI scheme is in the allowed schemes list
     * </ol>
     *
     * @param validatable The Wicket validatable object containing the URI string
     * @param uri The parsed URI object
     */
    private void validateUri(IValidatable<String> validatable, URI uri) {
        // Check for glob patterns in non-file URIs
        if (containsGlobPattern(uri.toString()) && !allowGlob) {
            validatable.error(error("URIValidator.globPatternsNotAllowed", Map.of("uri", uri)));
        }
        // Note: Scheme validation has already been performed in the validate() method
    }

    /**
     * Validates file URIs with additional file system checks.
     *
     * <p>This method performs several validations for file URIs:
     *
     * <ol>
     *   <li>Verifies the URI has a scheme if required
     *   <li>Converts the URI to an absolute file path
     *   <li>Optionally checks if the file exists
     *   <li>Handles glob patterns for matching multiple files
     * </ol>
     *
     * @param validatable The Wicket validatable object containing the URI string
     * @param uri The parsed URI object representing a file path
     */
    private void validateFile(IValidatable<String> validatable, URI uri) {

        if (StringUtils.isEmpty(uri.getScheme()) && !allowNullSchemeFileURI) {
            validatable.error(error("URIValidator.nullSchemeForbidden"));
            return;
        }
        try {
            Path file = toAbsolutePath(uri);
            boolean containsGlobPattern = containsGlobPattern(file.toString());
            if (containsGlobPattern && !allowGlob) {
                validatable.error(error("URIValidator.globPatternsNotAllowed", Map.of("uri", uri)));
                return;
            }
            if (fileMustExist) {
                boolean anyMatch;
                if (containsGlobPattern) {
                    anyMatch = anyFileMatchesGlob(validatable, file);
                } else {
                    anyMatch = Files.exists(file);
                }
                if (!anyMatch) {
                    validatable.error(error("URIValidator.fileNotFound", Map.of("uri", uri)));
                }
            }
        } catch (Exception e) {
            validatable.error(error("URIValidator.invalidFile", Map.of("error", e.getMessage())));
        }
    }

    /**
     * Checks whether any files in the filesystem match the given glob pattern.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Determines the base directory from which to start the search
     *   <li>Creates a path matcher for the glob pattern
     *   <li>Recursively walks the file tree from the base directory
     *   <li>Returns true if any file matches the pattern, false otherwise
     * </ol>
     *
     * @param validatable The Wicket validatable object for error reporting
     * @param file The path containing glob patterns to match
     * @return True if at least one file matches the glob pattern, false otherwise
     * @throws IOException If an I/O error occurs while traversing the file tree
     */
    private boolean anyFileMatchesGlob(IValidatable<String> validatable, Path file) throws IOException {
        // Get the file system but don't close it since it's the default file system
        // which should not be closed (FileSystems.getDefault() doesn't need to be closed)
        String syntaxAndPattern = "glob:" + file.toString();
        PathMatcher pathMatcher = file.getFileSystem().getPathMatcher(syntaxAndPattern);
        Path startDir = getBaseDir(file);
        if (!Files.isDirectory(startDir)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(startDir)) {
            return paths.anyMatch(pathMatcher::matches);
        }
    }

    /**
     * Determines the base directory to use for glob pattern matching. If the path contains a glob pattern, returns the
     * directory path up to the first component containing a glob pattern. Otherwise, returns a sensible directory to
     * start from.
     *
     * @param file The path that may contain glob patterns
     * @return The base directory from which to start glob matching
     */
    Path getBaseDir(Path file) {
        // If it doesn't contain a glob pattern, use the file as is if it's a directory
        if (!containsGlobPattern(file.toString())) {
            try {
                if (Files.isDirectory(file)) {
                    return file;
                }
            } catch (Exception e) {
                // File might not exist, continue to use parent
            }
            // Not a directory or doesn't exist, use parent
            return file.getParent() != null
                    ? file.getParent()
                    : file.getFileSystem().getPath(".");
        }

        // Split the path into its components and find first glob pattern
        String pathStr = file.toString();
        String separator = file.getFileSystem().getSeparator();
        String[] parts = pathStr.split(separator.equals("\\") ? "\\\\" : separator);

        // Find the first component containing a glob pattern
        int globIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].matches(".*" + GLOB_REGEX + ".*")) {
                globIndex = i;
                break;
            }
        }

        // Build the path up to but not including the component with the glob pattern
        if (globIndex > 0) {
            StringBuilder base = new StringBuilder();
            for (int i = 0; i < globIndex; i++) {
                if (base.length() > 0) {
                    base.append(separator);
                }
                base.append(parts[i]);
            }

            // Ensure we have an absolute path if the original was absolute
            String basePath = base.toString();
            if (file.isAbsolute() && !basePath.startsWith(separator)) {
                basePath = separator + basePath;
            }

            try {
                Path result = file.getFileSystem().getPath(basePath);
                return result;
            } catch (Exception e) {
                // Unable to create path, fall back to default
            }
        }

        // If we couldn't determine a better path, use the parent directory or current
        // directory
        return file.getParent() != null
                ? file.getParent()
                : file.getFileSystem().getPath(".");
    }

    private ValidationError error(String key) {
        return new ValidationError().addKey(key);
    }

    private ValidationError error(String key, Map<String, Object> variables) {
        ValidationError error = error(key);
        variables.forEach((variable, value) -> error.setVariable(variable, String.valueOf(value)));
        return error;
    }

    /**
     * Converts a URI to an absolute file path.
     *
     * <p>If the URI doesn't have a scheme, it's assumed to be a file path and a "file:" scheme is added before
     * conversion.
     *
     * @param uri The URI to convert
     * @return An absolute path corresponding to the URI
     */
    private static Path toAbsolutePath(URI uri) {
        if (StringUtils.isEmpty(uri.getScheme())) {
            uri = URI.create("file:" + uri);
        }
        return Path.of(uri).toAbsolutePath();
    }

    /**
     * Checks if a path string contains any glob pattern characters.
     *
     * <p>Looks for common glob wildcards and special characters like:
     *
     * <ul>
     *   <li>* - wildcard for any characters within a path segment
     *   <li>** - wildcard for any characters across multiple path segments
     *   <li>? - wildcard for a single character
     *   <li>[] - character class
     *   <li>{} - pattern alternatives
     * </ul>
     *
     * Ignores characters that are escaped with a backslash.
     *
     * @param pathString The path string to check
     * @return True if the path contains glob patterns, false otherwise
     */
    static boolean containsGlobPattern(String pathString) {
        try {
            // For URLs with schemes, only check the path part, not query parameters
            if (pathString.contains("://")) {
                java.net.URI uri = new java.net.URI(pathString);
                String path = uri.getPath();
                // If there's no path or it's just "/", return false
                if (path == null || path.isEmpty() || path.equals("/")) {
                    return false;
                }
                // Only check the path portion for glob patterns
                pathString = path;
            }

            // The matches() method checks if the entire string matches the pattern
            // We need to use find() through a Matcher to check if the pattern exists anywhere
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(GLOB_REGEX);
            java.util.regex.Matcher matcher = pattern.matcher(pathString);
            return matcher.find();
        } catch (java.net.URISyntaxException e) {
            // If the string isn't a valid URI, just use the regular pattern matching approach
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(GLOB_REGEX);
            java.util.regex.Matcher matcher = pattern.matcher(pathString);
            return matcher.find();
        }
    }
}
