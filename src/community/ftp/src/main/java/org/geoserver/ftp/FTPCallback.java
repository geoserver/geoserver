/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.io.File;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Interface defining a GeoServer extension point for FTP file activity event notification.
 * <p>
 * The FTP server will notify the implementations of this interface registered in the application
 * context of different file related activity being performed by a registered user through the FTP
 * service.
 * </p>
 * 
 * @author groldan
 */
public interface FTPCallback {

    /**
     * Notification of a delete file request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file about to be deleted if the request proceeds (i.e., if all the
     *            callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onDeleteStart(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of success to a delete file request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file just deleted
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onDeleteEnd(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of an upload file request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file about to be uploaded if the request proceeds (i.e., if all
     *            the callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onUploadStart(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of success to an upload file request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file just uploaded on the {@code workingDir}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onUploadEnd(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of a download file request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file about to be downloaded if the request proceeds (i.e., if all
     *            the callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onDownloadStart(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of success to a download file request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file just downloaded
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onDownloadEnd(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of a remove directory request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param dirName
     *            the name of the directory about to be deleted if the request proceeds (i.e., if
     *            all the callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onRemoveDirStart(UserDetails user, File workingDir, String dirName);

    /**
     * Notification of success to a delete directory request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param dirName
     *            the name of the directory just deleted
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onRemoveDirEnd(UserDetails user, File workingDir, String dirName);

    /**
     * Notification of a create directory request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param dirName
     *            the name of the directory about to be created if the request proceeds (i.e., if
     *            all the callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onMakeDirStart(UserDetails user, File workingDir, String dirName);

    /**
     * Notification of success to a create directory request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param dirName
     *            the name of the directory just created
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onMakeDirEnd(UserDetails user, File workingDir, String dirName);

    /**
     * Notification of a request to append content at the end of the given file by the given
     * {@code user}, on the given working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file to which content is to be appended if the request proceeds
     *            (i.e., if all the callbacks return {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onAppendStart(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of success to an append content to file request by the given {@code user}, on
     * the given working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param fileName
     *            the name of the file to which content has been appended
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onAppendEnd(UserDetails user, File workingDir, String fileName);

    /**
     * Notification of a rename file request by the given {@code user}, on the given working
     * directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param renameFrom
     *            the name of the original file that's about to be renamed if the request proceeds
     *            (i.e., if all the callbacks return {@link CallbackAction#CONTINUE}
     * @param renameTo
     *            the name of the target file that {@code renameFrom} is about to be renamed to if
     *            the request proceeds (i.e., if all the callbacks return
     *            {@link CallbackAction#CONTINUE}
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onRenameStart(UserDetails user, File workingDir, File renameFrom, File renameTo);

    /**
     * Notification of success to a rename file request by the given {@code user}, on the given
     * working directory.
     * 
     * @param user
     *            the GeoServer authenticated user performing the operation
     * @param workingDir
     *            the absolute path to the current working directory where the request is being
     *            processed
     * @param renameFrom
     *            the name of the original file that was renamed as {@code renameTo}
     * @param renameTo
     *            the name of the target file {@code renameFrom} has been renamed to
     * @return whether to continue with normal processing of the request, abort, or abort AND shut
     *         down the connection.
     */
    CallbackAction onRenameEnd(UserDetails user, File workingDir, File renameFrom, File renameTo);

}
