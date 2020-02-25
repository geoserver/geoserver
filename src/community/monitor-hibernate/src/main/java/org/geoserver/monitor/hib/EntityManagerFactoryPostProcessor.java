/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class EntityManagerFactoryPostProcessor implements BeanPostProcessor {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    GeoServerDataDirectory data;

    public EntityManagerFactoryPostProcessor(GeoServerDataDirectory data) {
        this.data = data;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (bean instanceof AbstractEntityManagerFactoryBean) {
            init((AbstractEntityManagerFactoryBean) bean);
        }
        return bean;
    }

    void init(AbstractEntityManagerFactoryBean factory) {
        try {
            Resource f = data.get(Paths.path("monitoring", "hibernate.properties"));
            if (!Resources.exists(f)) {
                // copy one out from
                Properties props = new Properties();
                props.putAll(factory.getJpaVendorAdapter().getJpaPropertyMap());
                props.putAll(factory.getJpaPropertyMap());

                Resource monitoring = data.get("monitoring");
                Resource file = monitoring.get("hibernate.properties");
                OutputStream fout = file.out();

                props.store(fout, "hibernate configuration");
                fout.flush();
                fout.close();
            } else {
                // use config to overide
                Properties props = new Properties();
                InputStream fin = f.in();
                props.load(fin);
                fin.close();

                HibernateJpaVendorAdapter adapter =
                        (HibernateJpaVendorAdapter) factory.getJpaVendorAdapter();
                adapter.setDatabase(Database.valueOf((String) props.get("database")));
                adapter.setDatabasePlatform((String) props.get("databasePlatform"));
                adapter.setShowSql(Boolean.valueOf((String) props.getProperty("showSql")));
                adapter.setGenerateDdl(Boolean.valueOf((String) props.getProperty("generateDdl")));

                for (Map.Entry e : props.entrySet()) {
                    if (((String) e.getKey()).startsWith("hibernate")) {
                        factory.getJpaPropertyMap().put((String) e.getKey(), e.getValue());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (bean instanceof AbstractEntityManagerFactoryBean) {
            init((AbstractEntityManagerFactoryBean) bean);
        }
        return bean;
    }
}
