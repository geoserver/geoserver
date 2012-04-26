/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.service;

import java.util.List;

import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

/**
 * 
 * <!--
 * 
 * <code>
 * <pre>
 *    <xsd:element name="GetEntries">
 *       <xsd:complexType>
 *          <xsd:complexContent>
 *             <xsd:extension base="gss:BaseRequestType">
 *                <xsd:sequence>
 *                   <xsd:element name="SearchTerms" type="xsd:string" minOccurs="0" maxOccurs="unbounded"/>
 *                   <xsd:element ref="fes:Filter" minOccurs="0"/>
 *                </xsd:sequence>
 *                <xsd:attribute name="feed" type="xsd:anyURI"/>
 *                <xsd:attribute name="outputFormat" type="xsd:string" use="optional" default="application/atom+xml; type=entry"/>
 *                <xsd:attribute name="startPosition" type="xsd:nonNegativeInteger" use="optional" default="1"/>
 *                <xsd:attribute name="maxEntries" type="xsd:nonNegativeInteger" use="optional" default="25"/>
 *             </xsd:extension>
 *          </xsd:complexContent>
 *       </xsd:complexType>
 *    </xsd:element>
 * </pre>
 * </code> -->
 * 
 * @author groldan
 * 
 */
public class GetEntries extends BaseRequest {

    private FeedType feed;

    private String outputFormat;

    private Long startPosition;

    private Long maxEntries;

    private List<String> searchTerms;

    private Filter filter;

    private SortOrder sortOrder;

    public GetEntries() {
        super("GetEntries");
    }

    public FeedType getFeed() {
        return feed;
    }

    public void setFeed(final FeedType feed) {
        this.feed = feed;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(final String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(final Long startPosition) {
        this.startPosition = startPosition;
    }

    public Long getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(final Long maxEntries) {
        this.maxEntries = maxEntries;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(final List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(final Filter filter) {
        this.filter = filter;
    }

    /**
     * @return the sortOrder
     */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
     * @param sortOrder
     *            the sortOrder to set
     */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

}
