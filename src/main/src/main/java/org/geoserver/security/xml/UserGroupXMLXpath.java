/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.NodeList;


/**
 * Abstract base class declaring abstract methods for
 * needed XPath expressions 
 * 
 * 
 * @author christian
 *
 */
public abstract class UserGroupXMLXpath  extends XMLXpath {

    /**
     * @return absolute expression for {@link NodeList} of User nodes
     */
    abstract public XPathExpression getUserListExpression();
    
    /**
     * @return relative expression for user enabled attribute
     */
    abstract public XPathExpression getUserEnabledExpression();

    /**
     * @return relative expression for username attribute
     */
    abstract public XPathExpression getUserNameExpression();
    
    /**
     * @return relative expression for user password attribute
     */
    abstract public XPathExpression getUserPasswordExpression();
    
    /**
     * @return relative expression for {@link NodeList} of User properties
     */
    abstract public XPathExpression getUserPropertiesExpression();
    

    /**
     * @return relative expression for property name attribute
     */
    abstract public XPathExpression getPropertyNameExpression();
    
    /**
     * @return relative expression for property value attribute
     */
    abstract public XPathExpression getPropertyValueExpression();

    /**
     * @return absolute expression for {@link NodeList} of group nodes
     */
    abstract public XPathExpression getGroupListExpression();   
    
    /**
     * @return relative expression for group name attribute
     */    
    abstract public XPathExpression getGroupNameExpression();
    
    /**
     * @return relative expression for group enabled attribute
     */
    abstract public XPathExpression getGroupEnabledExpression();

    
    /**
     * @return relative expression for {@link NodeList} of group members
     */
    abstract public XPathExpression getGroupMemberListExpression();

    /**
     * @return relative expression for user name  of group member element
     */
    abstract public XPathExpression getGroupMemberNameExpression();
        
}
