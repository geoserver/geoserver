/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.dto;

import org.locationtech.geogig.spring.dto.RepositoryInitRepo;
import org.springframework.http.HttpStatus;

/** Mirror the output of init, but use OK status instead of CREATED. */
public class RepositoryImportRepo extends RepositoryInitRepo {
    @Override
    public HttpStatus getStatus() {
        return HttpStatus.OK;
    }
}
