/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * A simple bookmarkable link with a label inside. This is a utility component,
 * avoid some boilerplate code in case the link is really just pointing to a bookmarkable
 * page without side effects
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class SimpleBookmarkableLink extends Panel {
    BookmarkablePageLink link;
    Label label;
    
    public SimpleBookmarkableLink(String id, Class pageClass, IModel labelModel, String... pageParams) {
        this(id, pageClass, labelModel, toPageParameters(pageParams));
    }
    
    private static PageParameters toPageParameters(String[] pageParams) {
        if(pageParams.length % 2 == 1)
            throw new IllegalArgumentException("The page parameters array should contain an even number of elements");
        
        Map<String, String> paramMap = new HashMap<String, String>();
        for (int i = 0; i < pageParams.length; i += 2) {
            paramMap.put(pageParams[i], pageParams[i+1]);
        }
        
        return new PageParameters(paramMap);
            
    }

    public SimpleBookmarkableLink(String id, Class pageClass, IModel labelModel, PageParameters params) {
        super(id, labelModel);
        
        add(link = new BookmarkablePageLink("link", pageClass, params));
        link.add(label = new Label("label", labelModel));
    }

    
    public BookmarkablePageLink getLink() {
        return link;
    }
    
    public Label getLabel() {
        return label;
    }
    
}
