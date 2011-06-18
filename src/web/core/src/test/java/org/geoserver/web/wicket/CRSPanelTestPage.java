package org.geoserver.web.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CRSPanelTestPage extends WebPage {

    public CRSPanelTestPage() {
        Form form = new Form("form" );
        add(form);
        
        form.add( new CRSPanel("crs", new Model()) );
    }
    
    public CRSPanelTestPage(Object o) {
        Form form = new Form("form", new CompoundPropertyModel( o ) );
        add(form);
        
        form.add( new CRSPanel("crs") );
    }
    
    public CRSPanelTestPage(IModel model) {
        Form form = new Form("form");
        add(form);
        
        form.add( new CRSPanel("crs", model) );
    }
    
    public CRSPanelTestPage(CoordinateReferenceSystem crs) {
        Form form = new Form("form");
        add(form);
        
        form.add( new CRSPanel( "crs", crs ) );
    }
}
