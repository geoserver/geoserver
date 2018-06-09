/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;

public class Keyword implements Serializable, KeywordInfo {

    String value;

    String language;

    String vocabulary;

    public Keyword(String value) {
        this.value = value;
        if (value == null) {
            throw new NullPointerException("value must be non-null");
        }
    }

    public Keyword(Keyword other) {
        this.value = other.value;
        this.language = other.language;
        this.vocabulary = other.vocabulary;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getVocabulary() {
        return vocabulary;
    }

    @Override
    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((vocabulary == null) ? 0 : vocabulary.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Keyword other = (Keyword) obj;
        if (language == null) {
            if (other.language != null) return false;
        } else if (!language.equals(other.language)) return false;
        if (value == null) {
            if (other.value != null) return false;
        } else if (!value.equals(other.value)) return false;
        if (vocabulary == null) {
            if (other.vocabulary != null) return false;
        } else if (!vocabulary.equals(other.vocabulary)) return false;
        return true;
    }
}
