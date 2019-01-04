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
public abstract class UserGroupXMLXpath extends XMLXpath {

    /** @return absolute expression for {@link NodeList} of User nodes */
    public abstract XPathExpression getUserListExpression();

    /** @return relative expression for user enabled attribute */
    public abstract XPathExpression getUserEnabledExpression();

    /** @return relative expression for username attribute */
    public abstract XPathExpression getUserNameExpression();

    /** @return relative expression for user password attribute */
    public abstract XPathExpression getUserPasswordExpression();

    /** @return relative expression for {@link NodeList} of User properties */
    public abstract XPathExpression getUserPropertiesExpression();

    /** @return relative expression for property name attribute */
    public abstract XPathExpression getPropertyNameExpression();

    /** @return relative expression for property value attribute */
    public abstract XPathExpression getPropertyValueExpression();

    /** @return absolute expression for {@link NodeList} of group nodes */
    public abstract XPathExpression getGroupListExpression();

    /** @return relative expression for group name attribute */
    public abstract XPathExpression getGroupNameExpression();

    /** @return relative expression for group enabled attribute */
    public abstract XPathExpression getGroupEnabledExpression();

    /** @return relative expression for {@link NodeList} of group members */
    public abstract XPathExpression getGroupMemberListExpression();

    /** @return relative expression for user name of group member element */
    public abstract XPathExpression getGroupMemberNameExpression();
}
