.. _preflight_quickstart:

Preflight Checklist
===================

This quickstart walks through common configuration steps before sharing your GeoServer publicly.

.. note:: This tutorial assumes that GeoServer is running at ``http://localhost:8080/geoserver``.

Reference:

* :ref:`production`

Security
--------

There are several warnings shown when we first start up GeoServer and login as admin:


1. Change the default login from **admin/geoserver**.

   .. figure:: images/admin-password-warning.png
      
      Default admin password warning

   Click on the :guilabel:`change it` to open :guilabel:`Edit user` for the ``admin`` user.
   You may also reach this screen by navigate to :menuselection:`Security > Users, Groups, and Roles`.
   Changing the the :guilabel:`Users/Groups` tag, and selecting the ``admin`` user.
   
   .. figure:: images/admin-password.png
      
      Change Master Password
      
   User this screen to change the `admin` user password from the default:
   
   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - User name
        - :kbd:`admin`
      * - Password
        - (make up a new password)
      * - Confirm password:
        - (confirm new password)

3. Change master password:

   .. figure:: images/master-password-warning.png
      
      Master password warning
   
   Click on the :guilabel:`change it` to open :guilabel:`Change Master Password`. You may also reach this screen by navigate to :menuselection:`Security > Passwords`, and pressing :guilabel:`Change password`.
   
   .. figure:: images/master-password.png
      
      Change Master Password
   
   Use this screen to change the master or keystore password.
   
   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Current password
        - :kbd:`geoserver`
      * - New password
        - (make up a new password)
      * - Confirmation:
        - (confirm new password)
   
   If you do not know the current password, navigate to :menuselection:`Security > Passwords` and there is an option to recover the password (either to a local file or via REST API).
   
   For more information see :ref:`security_master_passwd`.
   
   .. note:: What is the keystore password or master password?
   
      * The keystore password used to :ref:`store security credentials and encryption keys <security_passwd_keystore>`.
      * Optional: When experimenting with security configuration, you can enable use of the :ref:`root account <security_master_passwd>`.

Global Settings
---------------

1. By default GeoServer logs provide a record of every interaction. 

   This is useful when initially configuring GeoServer, however once you are comfortable everything
   is working correctly you can configure GeoServer to only record warnings and errors.
   
   Navigate to :menuselection:`Settings > Global`. Locate the heading :menuselection:`Internal Settings` and adjust
   :guilabel:`Logging profile` to ``PRODUCTION_LOGGING``.
   
   .. figure:: images/logging-profile.png
       
      PRODUCTION_LOGGING profile
      
   For more information see :ref:`config_globalsettings_log_profile`.

Contact Information
-------------------

1. Navigate to :menuselection:`About & Status > Contact Information`.
   
   * Filling in this information is shown initial Welcome page.
   * This informaiton is included in web service description information.
   * Contact information may be provided for each workspace.
   
   For more information :ref:`config_contact`.
   
