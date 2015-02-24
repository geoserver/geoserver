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
 * @author christian
 *
 */
public abstract class RoleXMLXpath  extends XMLXpath {
       
    /**
     * @return absolute expression for {@link NodeList} for roles
     */
    abstract public XPathExpression getRoleListExpression();
    
    /**
     * @return relative expression for parent role
     */
    abstract public XPathExpression getParentExpression();
        
    /**
     * @return relative expression for username attribute
     */
    abstract public XPathExpression getRoleNameExpression();
    
        
    /**
     * @return relative expression for {@link NodeList} of role properties
     */
    abstract public XPathExpression getRolePropertiesExpression();
    
    /**
     * @return relative expression for property name attribute
     */
    abstract public XPathExpression getPropertyNameExpression();
    
    /**
     * @return relative expression for property value attribute
     */
    abstract public XPathExpression getPropertyValueExpression();

    /**
     * @return absolute expression for {@link NodeList} of user/role 
     * assignment nodes
     */
    abstract public XPathExpression getUserRolesExpression();   
    
    /**
     * @return relative expression for user name attribute
     */    
    abstract public XPathExpression getUserNameExpression();
    
    
    /**
     * @return relative expression for {@link NodeList} of roles for
     * a user
     */
    abstract public XPathExpression getUserRolRefsExpression();

    /**
     * @return relative expression for role name  of role reference element
     */
    abstract public XPathExpression getUserRolRefNameExpression();
    
    /**
     * @return absolute expression for {@link NodeList} of group/role 
     * assignment nodes
     */
    abstract public XPathExpression getGroupRolesExpression();   
    
    /**
     * @return relative expression for group name attribute
     */    
    abstract public XPathExpression getGroupNameExpression();
    
    
    /**
     * @return relative expression for {@link NodeList} of roles for
     * a group
     */
    abstract public XPathExpression getGroupRolRefsExpression();

    /**
     * @return relative expression for role name  of role reference element
     */
    abstract public XPathExpression getGroupRolRefNameExpression();

    
}
