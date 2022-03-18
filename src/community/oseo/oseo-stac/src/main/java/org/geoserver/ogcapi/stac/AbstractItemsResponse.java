/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.math.BigInteger;
import java.util.Map;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geotools.feature.FeatureCollection;

/**
 * Wrapper for the returned FeatureCollection containing the STAC Items definitions. Base class for
 * both "items" and "search" resources result. Adds navigation and self links on top of what {@link
 * AbstractQueryResult} already provides.
 */
public abstract class AbstractItemsResponse extends AbstractQueryResult {

    private String next;
    private String previous;
    private String self;
    Map<String, Object> nextBody;
    Map<String, Object> previousBody;
    boolean post;
    private Map<String, RootBuilder> templateMap;

    public AbstractItemsResponse(FeatureCollection items, BigInteger numberMatched, int returned) {
        super(items, numberMatched, returned);
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public Map<String, Object> getNextBody() {
        return nextBody;
    }

    public void setNextBody(Map<String, Object> nextBody) {
        this.nextBody = nextBody;
    }

    public Map<String, Object> getPreviousBody() {
        return previousBody;
    }

    public void setPreviousBody(Map<String, Object> previousBody) {
        this.previousBody = previousBody;
    }

    public boolean isPost() {
        return post;
    }

    public void setPost(boolean post) {
        this.post = post;
    }

    @Override
    public Map<String, RootBuilder> getTemplateMap() {
        return templateMap;
    }

    @Override
    public void setTemplateMap(Map<String, RootBuilder> templateMap) {
        this.templateMap = templateMap;
    }
}
