package org.geoserver.catalog.hib.types;

import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.hibernate.types.EnumUserType;

public class ProjectionPolicyType extends EnumUserType<ProjectionPolicy> {

    public ProjectionPolicyType() {
        super(ProjectionPolicy.class);
    }

}
