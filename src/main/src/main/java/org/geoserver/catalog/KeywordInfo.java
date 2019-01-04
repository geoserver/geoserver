/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.regex.Pattern;

/**
 * A keyword used for service and layer metadata.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface KeywordInfo {

    /** regular expression for valid keyword and vocabulary. */
    static Pattern RE = Pattern.compile("[^\\\\]+");

    /** The keyword value. */
    String getValue();

    /** The language of the keyword, <code>null</code> if no language. */
    String getLanguage();

    /** Sets the language of the keyword. */
    void setLanguage(String language);

    /** The vocabulary of the keyword, <code>null</code> if no vocabulary. */
    String getVocabulary();

    /** Sets the vocabulary of the keyword. */
    void setVocabulary(String vocabulary);
}
