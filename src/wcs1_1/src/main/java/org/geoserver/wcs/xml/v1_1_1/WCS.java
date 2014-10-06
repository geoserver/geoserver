/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1;


import java.util.Set;

import javax.xml.namespace.QName;

import org.geotools.gml3.GML;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xml.XSD;

/**
 * This interface contains the qualified names of all the types,elements, and 
 * attributes in the http://www.opengis.net/wcs/1.1.1 schema.
 *
 * @generated
 */
public final class WCS extends XSD {

    /** singleton instance */
    private static final WCS instance = new WCS();
    
    /**
     * Returns the singleton instance.
     */
    public static final WCS getInstance() {
       return instance;
    }
    
    /**
     * private constructor
     */
    private WCS() {
    }
    
    protected void addDependencies(Set dependencies) {
       super.addDependencies(dependencies);
        
        dependencies.add( GML.getInstance() );
        dependencies.add( OWS.getInstance() );
    }
    
    /**
     * Returns 'http://www.opengis.net/wcs/1.1.1'.
     */
    public String getNamespaceURI() {
       return NAMESPACE;
    }
    
    /**
     * Returns the location of 'wcsAll.xsd.'.
     */
    public String getSchemaLocation() {
       return getClass().getResource("wcsAll.xsd").toString();
    }
    
    /** @generated */
    public static final String NAMESPACE = "http://www.opengis.net/wcs/1.1.1";
    
    /* Type Definitions */
    /** @generated */
    public static final QName AxisType = 
        new QName("http://www.opengis.net/wcs/1.1.1","AxisType");
    /** @generated */
    public static final QName CoverageDescriptionType = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoverageDescriptionType");
    /** @generated */
    public static final QName CoverageDomainType = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoverageDomainType");
    /** @generated */
    public static final QName CoveragesType = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoveragesType");
    /** @generated */
    public static final QName CoverageSummaryType = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoverageSummaryType");
    /** @generated */
    public static final QName DomainSubsetType = 
        new QName("http://www.opengis.net/wcs/1.1.1","DomainSubsetType");
    /** @generated */
    public static final QName FieldType = 
        new QName("http://www.opengis.net/wcs/1.1.1","FieldType");
    /** @generated */
    public static final QName GridCrsType = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridCrsType");
    /** @generated */
    /** @generated */
    public static final QName IdentifierType = 
        new QName("http://www.opengis.net/wcs/1.1.1","IdentifierType");
    /** @generated */
    public static final QName ImageCRSRefType = 
        new QName("http://www.opengis.net/wcs/1.1.1","ImageCRSRefType");
    /** @generated */
    public static final QName InterpolationMethodBaseType = 
        new QName("http://www.opengis.net/wcs/1.1.1","InterpolationMethodBaseType");
    /** @generated */
    public static final QName InterpolationMethodType = 
        new QName("http://www.opengis.net/wcs/1.1.1","InterpolationMethodType");
    /** @generated */
    public static final QName OutputType = 
        new QName("http://www.opengis.net/wcs/1.1.1","OutputType");
    /** @generated */
    public static final QName RangeSubsetType = 
        new QName("http://www.opengis.net/wcs/1.1.1","RangeSubsetType");
    /** @generated */
    public static final QName RangeType = 
        new QName("http://www.opengis.net/wcs/1.1.1","RangeType");
    /** @generated */
    public static final QName RequestBaseType = 
        new QName("http://www.opengis.net/wcs/1.1.1","RequestBaseType");
    /** @generated */
    public static final QName SpatialDomainType = 
        new QName("http://www.opengis.net/wcs/1.1.1","SpatialDomainType");
    public static final QName TimeDurationType = 
        new QName("http://www.opengis.net/wcs/1.1.1","TimeDurationType");
    public static final QName TimePeriodType = 
        new QName("http://www.opengis.net/wcs/1.1.1","TimePeriodType");
    /** @generated */
    public static final QName TimeSequenceType = 
        new QName("http://www.opengis.net/wcs/1.1.1","TimeSequenceType");
    /** @generated */
    public static final QName _AvailableKeys = 
        new QName("http://www.opengis.net/wcs/1.1.1","_AvailableKeys");
    /** @generated */
    public static final QName _AxisSubset = 
        new QName("http://www.opengis.net/wcs/1.1.1","_AxisSubset");
    /** @generated */
    public static final QName _Capabilities = 
        new QName("http://www.opengis.net/wcs/1.1.1","_Capabilities");
    /** @generated */
    public static final QName _Contents = 
        new QName("http://www.opengis.net/wcs/1.1.1","_Contents");
    /** @generated */
    public static final QName _CoverageDescriptions = 
        new QName("http://www.opengis.net/wcs/1.1.1","_CoverageDescriptions");
    /** @generated */
    public static final QName _DescribeCoverage = 
        new QName("http://www.opengis.net/wcs/1.1.1","_DescribeCoverage");
    /** @generated */
    public static final QName _GetCapabilities = 
        new QName("http://www.opengis.net/wcs/1.1.1","_GetCapabilities");
    /** @generated */
    public static final QName _GetCoverage = 
        new QName("http://www.opengis.net/wcs/1.1.1","_GetCoverage");
    /** @generated */
    public static final QName _InterpolationMethods = 
        new QName("http://www.opengis.net/wcs/1.1.1","_InterpolationMethods");
    /** @generated */
    public static final QName RangeSubsetType_FieldSubset = 
        new QName("http://www.opengis.net/wcs/1.1.1","RangeSubsetType_FieldSubset");

    /* Elements */
    /** @generated */
    public static final QName AvailableKeys = 
        new QName("http://www.opengis.net/wcs/1.1.1","AvailableKeys");
    /** @generated */
    public static final QName AxisSubset = 
        new QName("http://www.opengis.net/wcs/1.1.1","AxisSubset");
    /** @generated */
    public static final QName Capabilities = 
        new QName("http://www.opengis.net/wcs/1.1.1","Capabilities");
    /** @generated */
    public static final QName Contents = 
        new QName("http://www.opengis.net/wcs/1.1.1","Contents");
    /** @generated */
    public static final QName Coverage = 
        new QName("http://www.opengis.net/wcs/1.1.1","Coverage");
    /** @generated */
    public static final QName CoverageDescriptions = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoverageDescriptions");
    /** @generated */
    public static final QName Coverages = 
        new QName("http://www.opengis.net/wcs/1.1.1","Coverages");
    /** @generated */
    public static final QName CoverageSummary = 
        new QName("http://www.opengis.net/wcs/1.1.1","CoverageSummary");
    /** @generated */
    public static final QName DescribeCoverage = 
        new QName("http://www.opengis.net/wcs/1.1.1","DescribeCoverage");
    /** @generated */
    public static final QName GetCapabilities = 
        new QName("http://www.opengis.net/wcs/1.1.1","GetCapabilities");
    /** @generated */
    public static final QName GetCoverage = 
        new QName("http://www.opengis.net/wcs/1.1.1","GetCoverage");
    /** @generated */
    public static final QName GridBaseCRS = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridBaseCRS");
    /** @generated */
    public static final QName GridCS = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridCS");
    /** @generated */
    public static final QName GridOffsets = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridOffsets");
    /** @generated */
    public static final QName GridOrigin = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridOrigin");
    /** @generated */
    public static final QName GridType = 
        new QName("http://www.opengis.net/wcs/1.1.1","GridType");
    /** @generated */
    public static final QName Identifier = 
        new QName("http://www.opengis.net/wcs/1.1.1","Identifier");
    /** @generated */
    public static final QName InterpolationMethods = 
        new QName("http://www.opengis.net/wcs/1.1.1","InterpolationMethods");
    /** @generated */
    public static final QName TemporalDomain = 
        new QName("http://www.opengis.net/wcs/1.1.1","TemporalDomain");
    /** @generated */
    public static final QName TemporalSubset = 
        new QName("http://www.opengis.net/wcs/1.1.1","TemporalSubset");

    /* Attributes */

}
    
