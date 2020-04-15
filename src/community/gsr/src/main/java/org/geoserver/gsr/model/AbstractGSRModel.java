/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2019 GeoSolutions - https://geo-solutions.it All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class AbstractGSRModel implements GSRModel {
    public static final class Link {
        private String path;
        private String title;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Link(String path, String title) {
            super();
            this.path = path;
            this.title = title;
        }
    }

    private List<Link> path = new ArrayList<>();
    private List<Link> interfaces = new ArrayList<>();

    public AbstractGSRModel() {
        super();
        this.path.add(new Link("", "Home"));
    }

    @JsonIgnore
    public List<Link> getPath() {
        return path;
    }

    @JsonIgnore
    public List<Link> getInterfaces() {
        return interfaces;
    }
}
