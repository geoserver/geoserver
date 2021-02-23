/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * S3 remote file storage.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class S3FileServiceImpl extends AbstractS3FileServiceImpl {

    private static final long serialVersionUID = -5960841858385823283L;

    private String alias;

    private String endpoint;

    private String user;

    private String password;

    private static String S3_NAME_PREFIX = "s3-";

    public static String name(String prefix, String bucket) {
        return S3_NAME_PREFIX + prefix + "-" + bucket;
    }

    public S3FileServiceImpl() {}

    public S3FileServiceImpl(
            String endpoint, String user, String password, String alias, String rootFolder) {
        this.endpoint = endpoint;
        this.user = user;
        this.password = password;
        this.alias = alias;
        this.rootFolder = rootFolder;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getName() {
        return name(alias, rootFolder);
    }

    @Override
    public String getDescription() {
        return "S3 Service: " + alias + "/" + rootFolder;
    }

    @Override
    public URI getURI(String filePath) {
        try {
            return new URI(
                    alias
                            + "://"
                            + rootFolder
                            + "/"
                            + URLEncoder.encode(filePath.toString(), "UTF-8")
                                    .replaceAll("%2F", "/"));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AmazonS3 getS3Client() {
        if (user == null) {
            throw new IllegalArgumentException(
                    "The user is required, add a property: alias.s3.user");
        }
        if (password == null) {
            throw new IllegalArgumentException(
                    "The password is required, add a property: alias.s3.password");
        }
        if (rootFolder == null) {
            throw new IllegalStateException(
                    "The rootfolder is required, add a property: alias.s3.rootfolder");
        }

        AmazonS3 s3;
        // custom endpoint

        s3 = new AmazonS3Client(new BasicAWSCredentials(user, password));

        final S3ClientOptions clientOptions =
                S3ClientOptions.builder().setPathStyleAccess(true).build();
        s3.setS3ClientOptions(clientOptions);
        if (endpoint != null) {
            String endpoint = this.endpoint;
            if (!endpoint.endsWith("/")) {
                endpoint = endpoint + "/";
            }
            s3.setEndpoint(endpoint);
        }

        return s3;
    }
}
