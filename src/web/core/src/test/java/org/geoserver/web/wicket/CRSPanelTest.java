package org.geoserver.web.wicket;

import java.io.Serializable;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CRSPanelTest extends GeoServerWicketTestSupport {

    public void testStandloneUnset() throws Exception {
        tester.startPage( new CRSPanelTestPage() );
        
        tester.assertComponent( "form", Form.class );
        tester.assertComponent( "form:crs", CRSPanel.class );
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage( "form:crs");
        assertNull( crsPanel.getCRS() );
    }
    
    public void testStandaloneUnchanged() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage( new CRSPanelTestPage( crs ) );
        // print(new CRSPanelTestPage(crs), true, true);
        
        tester.assertComponent( "form", Form.class );
        tester.assertComponent( "form:crs", CRSPanel.class );
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage( "form:crs");
        assertEquals( DefaultGeographicCRS.WGS84, crsPanel.getCRS() );
    }
    
    public void testPopupWindow() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage( new CRSPanelTestPage( crs ) );
        
        ModalWindow window = (ModalWindow) tester.getComponentFromLastRenderedPage("form:crs:popup");
        assertFalse(window.isShown());
        
        tester.clickLink("form:crs:wkt", true);
        assertTrue(window.isShown());
        
        tester.assertModelValue("form:crs:popup:content:wkt", crs.toWKT());
    }
    
    public void testPopupWindowNoCRS() throws Exception {
        // see GEOS-3207
        tester.startPage( new CRSPanelTestPage() );
        
        ModalWindow window = (ModalWindow) tester.getComponentFromLastRenderedPage("form:crs:popup");
        assertFalse(window.isShown());
        
        GeoServerAjaxFormLink link = (GeoServerAjaxFormLink) tester.getComponentFromLastRenderedPage("form:crs:wkt");
        assertFalse(link.isEnabled());
    }
    
    public void testStandaloneChanged() throws Exception {
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        tester.startPage( new CRSPanelTestPage( crs ) );
        
        TextField srs = (TextField) tester.getComponentFromLastRenderedPage( "form:crs:srs");
        srs.setModelObject( "EPSG:3005");
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        CRSPanel crsPanel = (CRSPanel) tester.getComponentFromLastRenderedPage( "form:crs");
        assertEquals( CRS.decode("EPSG:3005"), crsPanel.getCRS() );
    }
    
    public void testRequired() throws Exception {
        tester.startPage( new CRSPanelTestPage( (CoordinateReferenceSystem) null ) );
        CRSPanel panel = (CRSPanel) tester.getComponentFromLastRenderedPage("form:crs");
        panel.setRequired(true);
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        assertEquals(1, Session.get().getFeedbackMessages().size());
        // System.out.println(Session.get().getFeedbackMessages().messageForComponent(panel));
    }
    
    public void testCompoundPropertyUnchanged() throws Exception {
        Foo foo = new Foo( DefaultGeographicCRS.WGS84 );
        tester.startPage( new CRSPanelTestPage( foo ));
        
        tester.assertComponent( "form", Form.class );
        tester.assertComponent( "form:crs", CRSPanel.class );
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        assertEquals( DefaultGeographicCRS.WGS84, foo.crs );
    }
    
    public void testCompoundPropertyChanged() throws Exception {
        Foo foo = new Foo( DefaultGeographicCRS.WGS84 );
        tester.startPage( new CRSPanelTestPage( foo ));
        
        TextField srs = (TextField) tester.getComponentFromLastRenderedPage( "form:crs:srs");
        srs.setModelObject( "EPSG:3005");
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
       
        assertEquals( CRS.decode("EPSG:3005"), foo.crs );
    }
    
    public void testPropertyUnchanged() throws Exception {
        Foo foo = new Foo( DefaultGeographicCRS.WGS84 );
        tester.startPage( new CRSPanelTestPage( new PropertyModel( foo, "crs") ));
        
        tester.assertComponent( "form", Form.class );
        tester.assertComponent( "form:crs", CRSPanel.class );
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
        
        assertEquals( DefaultGeographicCRS.WGS84, foo.crs );
    }
    
    public void testPropertyChanged() throws Exception {
        Foo foo = new Foo( DefaultGeographicCRS.WGS84 );
        tester.startPage( new CRSPanelTestPage( new PropertyModel( foo, "crs" ) ));
        
        TextField srs = (TextField) tester.getComponentFromLastRenderedPage( "form:crs:srs");
        srs.setModelObject( "EPSG:3005");
        
        FormTester ft = tester.newFormTester( "form");
        ft.submit();
       
        assertEquals( CRS.decode("EPSG:3005"), foo.crs );
    }
    
    static class Foo implements Serializable {
        public CoordinateReferenceSystem crs;
        
        Foo( CoordinateReferenceSystem crs ) {
            this.crs = crs;
        }
    }
}
