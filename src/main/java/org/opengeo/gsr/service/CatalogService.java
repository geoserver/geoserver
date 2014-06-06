/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.service;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

@XStreamAlias(value = "")
public class CatalogService implements AbstractService {

    private String name;

    private ServiceType type;

    private String specVersion;

    private String productName;

    private double currentVersion;

    @XStreamImplicit(itemFieldName = "folders")
    private List<String> folders;

    @XStreamImplicit(itemFieldName = "services")
    private List<AbstractService> services;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType serviceType) {
        this.type = serviceType;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(double currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }

    public List<AbstractService> getServices() {
        return services;
    }

    public void setServices(List<AbstractService> services) {
        this.services = services;
    }

    public CatalogService(String name, String specVersion, String productName,
            double currentVersion, List<String> folders, List<AbstractService> services) {
        this.name = name;
        this.type = ServiceType.CatalogServer;
        this.specVersion = specVersion;
        this.productName = productName;
        this.currentVersion = currentVersion;
        this.folders = folders;
        this.services = services;
    }

}
