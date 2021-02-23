/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * Amazon S3 remote file storage.
 *
 * @author Niels Charlier
 */
public class AWSFileServiceImpl extends AbstractS3FileServiceImpl {

    private static final long serialVersionUID = -5960841858385823283L;

    private boolean anonymous;

    private String awsRegion;

    private static String AWS_PREFIX = "aws-";

    public static String name(String bucket) {
        return AWS_PREFIX + bucket;
    }

    public AWSFileServiceImpl() {}

    public AWSFileServiceImpl(String rootFolder, boolean anonymous, String awsRegion) {
        this.rootFolder = rootFolder;
        this.anonymous = anonymous;
        this.awsRegion = awsRegion;
    }

    @Override
    public String getName() {
        return name(rootFolder);
    }

    @Override
    public String getDescription() {
        return "AWS Service: " + rootFolder;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @Override
    public URI getURI(String filePath) {
        try {
            String uri =
                    "s3://"
                            + rootFolder
                            + "/"
                            + URLEncoder.encode(filePath.toString(), "UTF-8").replaceAll("%2F", "/")
                            + "?useAnon="
                            + anonymous;
            if (awsRegion != null) {
                uri += "&awsRegion=" + awsRegion;
            }
            return new URI(uri);
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AmazonS3 getS3Client() {
        if (rootFolder == null) {
            throw new IllegalStateException(
                    "The rootfolder is required, add a property: alias.s3.rootfolder");
        }
        Regions region;
        if (awsRegion != null) {
            try {
                region = Regions.valueOf(awsRegion);
            } catch (IllegalArgumentException e) {
                // probably not great to have a default, but we can't just blow up if this
                // property isn't set
                LOGGER.warning(
                        "AWS_REGION property is set, but not set correctly. "
                                + "Check that the AWS_REGION property matches the Regions enum");
                region = Regions.US_EAST_1;
            }
        } else {
            LOGGER.warning("No AWS_REGION property set, defaulting to US_EAST_1");
            region = Regions.US_EAST_1;
        }

        AmazonS3 s3;
        if (anonymous) {
            s3 = new AmazonS3Client(new AnonymousAWSCredentials());
            s3.setRegion(Region.getRegion(region));
        } else {
            s3 = new AmazonS3Client();
            s3.setRegion(Region.getRegion(region));
        }
        final S3ClientOptions clientOptions =
                S3ClientOptions.builder().setPayloadSigningEnabled(true).build();
        s3.setS3ClientOptions(clientOptions);

        return s3;
    }
}
