/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.proxy;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.geoserver.proxy.ProxyConfig;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class ProxyAdminPage extends GeoServerSecuredPage {
    /*NOTE & DANGER:
     * There is nothing here that will guarantee any form of consistency in case of multiple people
     * editing the proxy's configuration. Probably don't do that.
     */
    
    GeoServerTablePanel<String> hostnameFilterTable;
    GeoServerTablePanel<String> mimetypeFilterTable;
    HostRemovalLink hostRemoval;
    MimetypeRemovalLink mimetypeRemoval;


    @SuppressWarnings("serial")
    /*
     * Provides a webpage for editing proxy settings
     */
    public ProxyAdminPage() {
        
        //Grab configuration data
        HostnameProvider hostnameProvider = new HostnameProvider();
        MimetypeProvider mimetypeProvider = new MimetypeProvider(); 
       
        //
        //HOSTNAME
        //
        //Put together a table for editing what hostnames can go through the proxy
        hostnameFilterTable = 
            new GeoServerTablePanel<String>("hostnameTable", hostnameProvider, true) {
            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<String> property) {
                return new Label(id, property.getModel(itemModel));
            }
            //tell the table to enable the remove button when items are selected
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                hostRemoval.setEnabled(hostnameFilterTable.getSelection().size() > 0);
                target.addComponent(hostRemoval);
            }  
        };
        hostnameFilterTable.setOutputMarkupId(true);
        add(hostnameFilterTable);
        
        
        // the add button
        add(new BookmarkablePageLink("addNewHost", HostnameNewPage.class));
        // the removal button
        hostRemoval = new HostRemovalLink("removeSelectedHost", hostnameFilterTable);
        add(hostRemoval);        
        hostRemoval.setOutputMarkupId(true);
        hostRemoval.setEnabled(false);
        
        //
        //MIMETYPE
        //
        //Put together a table for editing what MIMETypes can go through the proxy
        mimetypeFilterTable = 
            new GeoServerTablePanel<String>("mimetypeTable", mimetypeProvider, true) {
            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<String> property) {
                return new Label(id, property.getModel(itemModel));
            }
            //tell the table to enable the remove button when items are selected
            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                mimetypeRemoval.setEnabled(mimetypeFilterTable.getSelection().size() > 0);
                target.addComponent(mimetypeRemoval);
            }  
        };
        mimetypeFilterTable.setOutputMarkupId(true);
        add(mimetypeFilterTable);
        
        // the add button
        add(new BookmarkablePageLink("addNewMimetype", MimetypeNewPage.class));
        // the removal button
        mimetypeRemoval = new MimetypeRemovalLink("removeSelectedMimetype", mimetypeFilterTable);
        add(mimetypeRemoval);        
        mimetypeRemoval.setOutputMarkupId(true);
        mimetypeRemoval.setEnabled(false);

    }
    
    /*unneeded*/
//    @SuppressWarnings("serial")
//    public final class ProxyForm extends Form{
//        public ProxyForm(final String componentName)
//        {
//            super(componentName);
//        }
//        
//        public void onSubmit()
//        {
//            // TODO: Add a submit button, and make it somehow call this vvv.
//            
//            //ProxyConfig.writeConfigToDisk(config);
//        }
//    }
 
    /*
     * An AJAX link to get rid of hostnames
     * @param id A Wicket id
     * @param tableObjects a GeoServerTablePanel to remove hostnames from
     */
    @SuppressWarnings("serial")
    private class HostRemovalLink extends AjaxLink {    
        GeoServerTablePanel<String> tableObjects;

        public HostRemovalLink(String id, GeoServerTablePanel<String> tableObjects) {
            super(id);
            this.tableObjects = tableObjects;
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            // see if the user selected anything
            ProxyConfig config = ProxyConfig.loadConfFromDisk();
            final List<String> selection = tableObjects.getSelection();
            if(selection.size() == 0)
                return;
            
            //remove selected hostnames from list
            for (String hostname : selection) {
                config.hostnameWhitelist.remove(hostname);
            }
            //write changes to disk
            ProxyConfig.writeConfigToDisk(config);
            
            //disable the removal link, since nothing is selected any more
            setEnabled(false);
            target.addComponent(HostRemovalLink.this);
            target.addComponent(tableObjects);
        }
    }
    
    @SuppressWarnings("serial")
    private class MimetypeRemovalLink extends AjaxLink {    
        GeoServerTablePanel<String> tableObjects;

        /*
         * An AJAX link to get rid of hostnames
         * @param id A Wicket id
         * @param tableObjects a GeoServerTablePanel to remove hostnames from
         */
        public MimetypeRemovalLink(String id, GeoServerTablePanel<String> tableObjects) {
            super(id);
            this.tableObjects = tableObjects;
        }

        
        @Override
        public void onClick(AjaxRequestTarget target) {
            // see if the user selected anything
            ProxyConfig config = ProxyConfig.loadConfFromDisk();
            final List<String> selection = tableObjects.getSelection();
            if(selection.size() == 0)
                return;
            
            //remove selected hostnames from list
            for (String hostname : selection) {
                config.mimetypeWhitelist.remove(hostname);
            }
            //write changes to disk
            ProxyConfig.writeConfigToDisk(config);
            
            //disable the removal link, since nothing is selected any more
            setEnabled(false);
            target.addComponent(MimetypeRemovalLink.this);
            target.addComponent(tableObjects);
        }
    }
}
