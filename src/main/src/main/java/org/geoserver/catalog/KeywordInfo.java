/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A keyword used for service and layer metadata.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface KeywordInfo {

    /** Regular expression for valid keyword and vocabulary. */
    static Pattern isValidPattern = Pattern.compile("[^\\\\]+");

    /**
     * Set the keyword value.
     *
     * @param keyword The keyword value
     */
    void setValue(String keyword);

    /** The keyword value. */
    String getValue();

    /** The language of the keyword, or {@code null} if no language specified. */
    String getLanguage();

    /**
     * Sets the language of the keyword.
     *
     * @param language Language of they keyword, or {@code null} if no language specified.
     */
    void setLanguage(String language);

    /**
     * The vocabulary of the keyword, {@code null} if no vocabulary.
     *
     * <p>Vocabulary is a URI that identifies the controlled vocabulary, providing a formal definition of they keyword
     * in context. Vocabulary is not a human readable string, but rather a unique identifier
     */
    String getVocabulary();

    /**
     * Sets the vocabulary of the keyword.
     *
     * @param vocabulary Vocabulary of the keyword, or {@code null} if no vocabulary specified.
     */
    void setVocabulary(String vocabulary);

    /**
     * Checks if keyword is valid.
     *
     * @throws IllegalArgumentException If value, language, or vocabulary is invalid.
     */
    public static void checkValid(KeywordInfo keyword) throws IllegalArgumentException {
        if (keyword.getValue() != null
                && !isValidPattern.matcher(keyword.getValue()).matches()) {
            throw new IllegalArgumentException("Illegal keyword '"
                    + keyword
                    + "'. "
                    + "Keywords must not be empty and must not contain the '\\' character");
        }
        if (keyword.getVocabulary() != null) {
            Matcher vocabMatcher = KeywordInfo.isValidPattern.matcher(keyword.getVocabulary());
            if (!vocabMatcher.matches()) {
                throw new IllegalArgumentException("Keyword vocabulary must not contain the '\\' character");
            }
        }
    }
}
