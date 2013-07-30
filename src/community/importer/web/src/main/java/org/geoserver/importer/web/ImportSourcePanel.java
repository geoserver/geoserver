package org.geoserver.importer.web;

import java.io.IOException;

import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.importer.ImportData;

/**
 * Abstract class for import source panels.
 */
public abstract class ImportSourcePanel extends Panel {

    public ImportSourcePanel(String id) {
        super(id);
    }

    public abstract ImportData createImportSource() throws IOException;

}
