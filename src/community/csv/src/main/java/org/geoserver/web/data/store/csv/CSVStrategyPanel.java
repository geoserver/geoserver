package org.geoserver.web.data.store.csv;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.panel.Panel;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.data.csv.CSVDataStore;
import org.geotools.data.csv.CSVDataStoreFactory;


public class CSVStrategyPanel extends Panel {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CSVDataStore.class);
    private String strategy = CSVDataStoreFactory.GUESS_STRATEGY;

    public CSVStrategyPanel(String id, String strategy) {
        super(id);
        this.strategy = strategy;
        setOutputMarkupId(true);

    }

    protected void onBeforeRender(){
        //build form elements
        switch (strategy){
            case CSVDataStoreFactory.GUESS_STRATEGY:
                LOGGER.log(Level.FINE, "Guessing");
                break;
            case CSVDataStoreFactory.SPECIFC_STRATEGY:
                LOGGER.log(Level.FINE, "Specific");
                break;
            case CSVDataStoreFactory.WKT_STRATEGY:
                LOGGER.log(Level.FINE, "WKT");
                break;
            case CSVDataStoreFactory.ATTRIBUTES_ONLY_STRATEGY:
                LOGGER.log(Level.FINE, "No Geography");
                break;
        }
        super.onBeforeRender();
    }

    public void setStrategy(String strategy){
        this.strategy = strategy;
    }
}
