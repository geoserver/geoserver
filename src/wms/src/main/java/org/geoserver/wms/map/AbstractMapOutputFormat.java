/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.geoserver.wms.GetMapOutputFormat;
import org.springframework.util.Assert;

/**
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Gabriel Roldan
 */
public abstract class AbstractMapOutputFormat implements GetMapOutputFormat {

    private final String mime;

    private final Set<String> outputFormatNames;

    protected AbstractMapOutputFormat(final String mime) {
        this(mime, new String[] { mime });
    }

    @SuppressWarnings("unchecked")
    protected AbstractMapOutputFormat(final String mime, final String[] outputFormats) {
        this(mime, outputFormats == null ? Collections.EMPTY_SET : new HashSet<String>(
                Arrays.asList(outputFormats)));
    }

    protected AbstractMapOutputFormat(final String mime, Set<String> outputFormats) {
        Assert.notNull(mime);
        this.mime = mime;
        if (outputFormats == null) {
            outputFormats = Collections.emptySet();
        }

        Set<String> formats = caseInsensitiveOutputFormats(outputFormats);
        formats.add(mime);
        this.outputFormatNames = Collections.unmodifiableSet(formats);
    }

    private static Set<String> caseInsensitiveOutputFormats(Set<String> outputFormats) {
        Set<String> caseInsensitiveFormats = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveFormats.addAll(outputFormats);
        return caseInsensitiveFormats;
    }

    protected AbstractMapOutputFormat() {
        this(null, (String[]) null);
    }

    /**
     * @see GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return mime;
    }

    /**
     * @see GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return outputFormatNames;
    }

}
