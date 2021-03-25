/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/** Default implementation of a SupplementalFileExtensionsProvider */
public class DefaultSupplementalFileExtensionsProvider
        implements SupplementalFileExtensionsProvider {
    private Set<String> acceptedInputExtensions;
    private Set<String> supplementalExtensions;
    private Set<String> upperCaseSupplementalExtensions;

    public DefaultSupplementalFileExtensionsProvider(
            Set<String> acceptedInputExtensions, Set<String> supplementalExtensions) {
        this.acceptedInputExtensions = Collections.unmodifiableSet(acceptedInputExtensions);
        this.supplementalExtensions = Collections.unmodifiableSet(supplementalExtensions);
        Set<String> upperCase = new HashSet<>();
        supplementalExtensions.stream().forEach(e -> upperCase.add(e.toUpperCase()));
        upperCaseSupplementalExtensions = Collections.unmodifiableSet(upperCase);
    }

    private boolean isSupportedInputExtension(String extension) {
        return extension != null && acceptedInputExtensions.contains(extension.toLowerCase());
    }

    @Override
    public boolean canHandle(String baseExtension) {
        return baseExtension != null
                && acceptedInputExtensions.contains(baseExtension.toLowerCase());
    }

    @Override
    public Set<String> getExtensions(String baseExtension) {
        if (!isSupportedInputExtension(baseExtension)) return Collections.emptySet();
        // some data providers produce tiff files being stored as .TIF
        // we can reasonably suppose that supplemental files will be upper case too
        // i.e. .PRJ, .XML
        return StringUtils.isAllUpperCase(baseExtension)
                ? upperCaseSupplementalExtensions
                : supplementalExtensions;
    }
}
