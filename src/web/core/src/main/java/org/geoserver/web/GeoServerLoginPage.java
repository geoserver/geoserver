/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * This is a simple login form shown when the user tries to access a secured page directly 
 * @author aaime
 */
public class GeoServerLoginPage extends GeoServerBasePage {

    public GeoServerLoginPage(PageParameters parameters) {
        //avoid showing two login forms
        if ( get("loginform") != null ) {
            get("loginform").setVisible(false);
        }
        
        TextField field = new TextField("username");
        HttpSession session = ((HttpServletRequest) ((WebRequest) getRequest()).getContainerRequest()).getSession();
        String lastUserName = (String) session.getAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY);
        field.setModel(new Model(lastUserName));
        add(field);
        
        try {
            if(parameters.get("error").toBoolean()) {
                error(new ParamResourceModel("error", this).getString());
            }
        } catch(Exception e) {
            // ignore
        }
    }
    
}
