/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import static org.geoserver.ftp.CallbackAction.CONTINUE;

import java.io.File;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Default empty implementation of {@link FTPCallback} that acts as base class for subclasses
 * interested in certain events only.
 * 
 * @author groldan
 */
public class DefaultFTPCallback implements FTPCallback {

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onDeleteStart
     */
    public CallbackAction onDeleteStart(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onDeleteEnd
     */
    public CallbackAction onDeleteEnd(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onUploadStart
     */
    public CallbackAction onUploadStart(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onUploadEnd
     */
    public CallbackAction onUploadEnd(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onDownloadStart
     */
    public CallbackAction onDownloadStart(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onDownloadEnd
     */
    public CallbackAction onDownloadEnd(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onRemoveDirStart
     */
    public CallbackAction onRemoveDirStart(UserDetails user, File workingDir, String dirName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onRemoveDirEnd
     */
    public CallbackAction onRemoveDirEnd(UserDetails user, File workingDir, String dirName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onMakeDirStart
     */
    public CallbackAction onMakeDirStart(UserDetails user, File workingDir, String dirName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onMakeDirEnd
     */
    public CallbackAction onMakeDirEnd(UserDetails user, File workingDir, String dirName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onAppendStart
     */
    public CallbackAction onAppendStart(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onAppendEnd
     */
    public CallbackAction onAppendEnd(UserDetails user, File workingDir, String fileName) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onRenameStart
     */
    public CallbackAction onRenameStart(UserDetails user, File workingDir, File renameFrom,
            File renameTo) {
        return CONTINUE;
    }

    /**
     * Empty implementation; override to take action
     * 
     * @return {@link CallbackAction#CONTINUE}
     * @see org.geoserver.ftp.FTPCallback#onRenameEnd
     */
    public CallbackAction onRenameEnd(UserDetails user, File workingDir, File renameFrom,
            File renameTo) {
        return CONTINUE;
    }

}
