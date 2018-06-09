/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import javax.xml.xpath.XPathExpression;
import org.w3c.dom.NodeList;

/**
 * Abstract base class declaring abstract methods for needed XPath expressions
 *
 * @author christian
 */
public abstract class RoleXMLXpath extends XMLXpath {

    /** @return absolute expression for {@link NodeList} for roles */
    public abstract XPathExpression getRoleListExpression();

    /** @return relative expression for parent role */
    public abstract XPathExpression getParentExpression();

    /** @return relative expression for username attribute */
    public abstract XPathExpression getRoleNameExpression();

    /** @return relative expression for {@link NodeList} of role properties */
    public abstract XPathExpression getRolePropertiesExpression();

    /** @return relative expression for property name attribute */
    public abstract XPathExpression getPropertyNameExpression();

    /** @return relative expression for property value attribute */
    public abstract XPathExpression getPropertyValueExpression();

    /** @return absolute expression for {@link NodeList} of user/role assignment nodes */
    public abstract XPathExpression getUserRolesExpression();

    /** @return relative expression for user name attribute */
    public abstract XPathExpression getUserNameExpression();

    /** @return relative expression for {@link NodeList} of roles for a user */
    public abstract XPathExpression getUserRolRefsExpression();

    /** @return relative expression for role name of role reference element */
    public abstract XPathExpression getUserRolRefNameExpression();

    /** @return absolute expression for {@link NodeList} of group/role assignment nodes */
    public abstract XPathExpression getGroupRolesExpression();

    /** @return relative expression for group name attribute */
    public abstract XPathExpression getGroupNameExpression();

    /** @return relative expression for {@link NodeList} of roles for a group */
    public abstract XPathExpression getGroupRolRefsExpression();

    /** @return relative expression for role name of role reference element */
    public abstract XPathExpression getGroupRolRefNameExpression();
}
