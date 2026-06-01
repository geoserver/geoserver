/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Source-scanning lint test that finds {@code new HashSet<>()} usage in persisted config model classes
 * ({@code *InfoImpl.java}). These fields should use {@code LinkedHashSet} to preserve insertion order and produce
 * deterministic XML output when serialized by XStreamPersister.
 *
 * <p>If this test fails, change the field initializer from {@code new HashSet<>()} to {@code new LinkedHashSet<>()}.
 *
 * <p>Fields that are genuinely transient/runtime (not serialized) can be added to {@link #EXCLUDED_PATTERNS}.
 */
public class LinkedHashSetLintTest {

    /**
     * Exclusion list for known non-persisted HashSet usages in *InfoImpl classes. Format:
     * "SimpleClassName.java:lineContent" substring that uniquely identifies the acceptable usage.
     */
    private static final Set<String> EXCLUDED_PATTERNS = Set.of(
            // Runtime cache invalidation tracking, not persisted
            "LegendSampleImpl.java",
            // Runtime authentication cache, not persisted
            "GuavaAuthenticationCacheImpl.java",
            "LRUAuthenticationCacheImpl.java",
            // Runtime role conversion, not persisted
            "GeoServerRoleConverterImpl.java",
            // Runtime password validation, not persisted
            "PasswordValidatorImpl.java",
            // Runtime utility/fallback returns, not field initializers
            "GeoServerTileLayerInfoImpl.java",
            // Error fallback returning mutable empty set, not a persisted field
            "WMSLayerInfoImpl.java");

    /** Pattern matching {@code new HashSet<>()} or {@code new HashSet<>(someArg)} in source. */
    private static final Pattern HASHSET_PATTERN = Pattern.compile("new\\s+HashSet<");

    @Test
    public void testNoHashSetInInfoImplClasses() throws IOException {
        // Find the GeoServer src/ root by walking up from the working directory
        Path srcRoot = findSrcRoot();
        if (srcRoot == null) {
            // If we can't find the source root (e.g. running from a packaged jar), skip
            org.junit.Assume.assumeTrue("Could not locate source root — skipping LinkedHashSetLintTest", false);
            return;
        }

        List<String> violations = new ArrayList<>();

        Files.walkFileTree(srcRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith("InfoImpl.java")) {
                    return FileVisitResult.CONTINUE;
                }
                // Skip test directories
                if (file.toString().contains("/test/") || file.toString().contains("\\test\\")) {
                    return FileVisitResult.CONTINUE;
                }
                // Skip excluded files
                for (String excluded : EXCLUDED_PATTERNS) {
                    if (fileName.equals(excluded)) {
                        return FileVisitResult.CONTINUE;
                    }
                }
                // Scan file for new HashSet<>
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    Matcher m = HASHSET_PATTERN.matcher(line);
                    if (m.find()) {
                        // Relative path for readable output
                        Path relative = srcRoot.relativize(file);
                        violations.add(relative + ":" + (i + 1) + ": " + line.trim());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                // Skip build output and hidden directories
                if (dirName.equals("target") || dirName.startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertTrue(
                "*InfoImpl classes should use LinkedHashSet (not HashSet) for deterministic serialization.\n"
                        + "Fix by changing 'new HashSet<>()' to 'new LinkedHashSet<>()'.\n"
                        + "If the field is genuinely non-persisted, add the file to EXCLUDED_PATTERNS in "
                        + "LinkedHashSetLintTest.java.\n\nViolations:\n"
                        + String.join("\n", violations),
                violations.isEmpty());
    }

    /**
     * Walks up from the current working directory to find the GeoServer {@code src/} root (the directory containing
     * {@code pom.xml} with subdirectories like {@code main/}, {@code wfs-core/}, etc.).
     */
    private static Path findSrcRoot() {
        // Maven runs tests with cwd = module directory (e.g. src/main/)
        // The src/ root is the parent of that
        Path cwd = Paths.get("").toAbsolutePath();
        // Walk up looking for the parent pom that contains the reactor modules
        Path candidate = cwd;
        for (int i = 0; i < 5; i++) {
            if (Files.exists(candidate.resolve("main"))
                    && Files.exists(candidate.resolve("pom.xml"))
                    && Files.exists(candidate.resolve("platform"))) {
                return candidate;
            }
            candidate = candidate.getParent();
            if (candidate == null) break;
        }
        return null;
    }
}
