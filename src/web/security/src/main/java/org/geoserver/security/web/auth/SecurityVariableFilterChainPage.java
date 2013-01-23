/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.exception.GeoServerExceptions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;
import org.geotools.util.logging.Logging;

/**
 * Class for configuration panels of {@link VariableFilterChain} objects
 * 
 * @author christan
 *
 */
public  class SecurityVariableFilterChainPage 
    extends SecurityFilterChainPage {

    private static final long serialVersionUID = 1L;

    /**
     * logger
     */
    

    protected AuthFilterChainPalette palette;
    
           

    public SecurityVariableFilterChainPage( VariableFilterChain chain, 
            SecurityManagerConfig secMgrConfig,
            boolean isNew) {
        
        VariableFilterChainWrapper wrapper = new VariableFilterChainWrapper(chain);
        
        Form<VariableFilterChainWrapper> theForm  = new Form<VariableFilterChainWrapper>("form",new 
                CompoundPropertyModel<VariableFilterChainWrapper>(wrapper)); 
                
        super.initialize(chain, secMgrConfig, isNew, theForm, wrapper);

                
        
        List<String> filterNames=new ArrayList<String>();
        try {  
            filterNames.addAll(getSecurityManager().listFilters(GeoServerSecurityInterceptorFilter.class));
            for (GeoServerSecurityInterceptorFilter filter :GeoServerExtensions.extensions(GeoServerSecurityInterceptorFilter.class)){
                filterNames.add(filter.getName());
            }
            form.add(new DropDownChoice<String>("interceptorName",
                    new PropertyModel<String>(chainWrapper.getChain(), "interceptorName"),
                    filterNames));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        form.add(palette=new AuthFilterChainPalette("authFilterChain", new AuthFilterNamesModel(getVariableFilterChainWrapper())));            
        palette.setOutputMarkupId(true);
        palette.setChain(getVariableFilterChainWrapper().getVariableFilterChain());

        
        form.add(new HelpLink("chainConfigFilterHelp").setDialog(dialog));
        
        
    }

    VariableFilterChainWrapper getVariableFilterChainWrapper() {
        return (VariableFilterChainWrapper) chainWrapper;
    }

            
    class AuthFilterNamesModel implements IModel<List<String>> {

        private static final long serialVersionUID = 1L;
        VariableFilterChainWrapper chainModel;

        AuthFilterNamesModel(VariableFilterChainWrapper chainModel) {
            this.chainModel = chainModel;
        }

        @Override
        public List<String> getObject() {
            
            GeoServerSecurityManager secMgr = getSecurityManager();
            List<String> filters = new ArrayList<String>(chainModel.getChain().getFilterNames());
            try {
                filters.retainAll(chainModel.getVariableFilterChain().listFilterCandidates(secMgr));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return filters;
        }

        @Override
        public void setObject(List<String> object) {
            chainModel.getChain().setFilterNames(object);
        }
        
        @Override
        public void detach() {
            //chainModel.detach();
        }
    }

}
