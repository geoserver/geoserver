/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.web;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.python.Python;
import org.geoserver.web.GeoServerSecuredPage;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonConsolePage extends GeoServerSecuredPage {

    PythonInterpreterDetachableModel model;
    TextArea outputTextArea;
    String output = ">>> ";
    String input = "";
    
    public PythonConsolePage() {
        Python python = getGeoServerApplication().getBeanOfType(Python.class);
        python.interpreter();
        model = new PythonInterpreterDetachableModel();
        
        Form form = new Form("form");
        add(form);
        
        final AjaxButton execute = new AjaxButton("execute") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String[] lines = output.split("\n");
                String line = lines[lines.length-1];
                if (line.startsWith(">>> ")) {
                    line = line.substring(4);
                }
                PythonInterpreter pi = (PythonInterpreter) model.getObject();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                pi.setOut(out);
                try {
                    pi.exec(line);
               }
                catch( PyException pe ) {
                    pe.printStackTrace(new PrintWriter(out, true));
                }
                output += "\n";
                output += new String(out.toByteArray());
                output += ">>> ";
                    
                target.addComponent(outputTextArea);
                target.appendJavascript(
                    "var ta = document.getElementById('" + outputTextArea.getMarkupId() + "');" + 
                    "ta.scrollTop = ta.scrollHeight;"
                );
            }
        };
        form.add(execute);
        
        AjaxButton clear = new AjaxButton("clear") {
            
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                output = ">>> ";
                target.addComponent(outputTextArea);
            }
            
        };
        form.add(clear);
        outputTextArea = new TextArea("output", new PropertyModel(this, "output")) {
            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("onkeypress", "if (event.keyCode == 13) { " + 
                    "document.getElementById('" + execute.getMarkupId() + "').click();" + 
                    "return true;" + 
                "}");
            }
        };
        outputTextArea.setOutputMarkupId(true);
        
        //outputTextArea.setEnabled(false);
        
        form.add(outputTextArea);
        /*form.add(new Label("prompt", ">>> "));
        form.add(new TextField("input", new PropertyModel(this, "input")));*/
    }
    
    class PythonInterpreterDetachableModel extends LoadableDetachableModel {

        PySystemState state;
        
        public PythonInterpreterDetachableModel() {
        }
        
        @Override
        protected Object load() {
            return new PythonInterpreter(null, state);
        }
        
        @Override
        public void detach() {
            state = Py.getSystemState();
            super.detach();
        }
    }
}
