/**
 * 
 */
package org.geoserver.web;

import java.io.Serializable;

import org.apache.wicket.Component;

public interface ComponentBuilder extends Serializable {
    Component buildComponent(String id);
}