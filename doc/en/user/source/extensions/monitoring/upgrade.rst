.. _monitor_upgrade:

Upgrading
=========

The monitoring extension uses Hibernate to persist request data. Changes to the extension over time affect the structure of 
the underlying database, which should be taken into consideration before performing an upgrade. Depending on the nature of
changes in an upgrade, it may involve manually making changes to the underlying database before deploying a new version of 
the extension.

The sections below provides a history of such changes, and recommended actions that should be taken as part of the upgrade. 
Upgrades are grouped into two categories:

* **minor** upgrades that occur during a minor GeoServer version change, for example going from 2.1.2 to 
  2.1.3. These changes are backward compatible in that no action is specifically required but potentially
  recommended. In these cases performing an upgrade without any action still result in the monitoring
  extension continuing to function.

* **major** upgrades that occur during a major GeoServer version change, for example going from 2.1.2 to 
  2.2.0. These changes *may* be backward compatible, but not necessarily. In these cases performing an upgrade
  without any action could potentially result in the monitoring extension ceasing to function, and may result 
  in significant changes to the underlying database.

For each change the following information is maintained:

* The released version containing the change
* The date of the change
* The subversion revision of the change
* The jira issue referring to the change
  
The date and subversion revision are especially useful if a nightly build of the extension is being used.
  
Minor upgrades
--------------

Column ``resource`` renamed to ``name`` in ``request_resources`` table
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* *Version*: n/a, extension still community status
* *Date*: Dec 09, 2011
* *Subversion revision*: 16632
* *Reference*: :geos:`4871`

Upgrading without performing any action will result in the ``name`` column being added to the ``request_resources`` table, 
leaving the ``resource`` column intact. From that point forward the ``resource`` column will essentially be ignored.
However no data from the ``resource`` column will be migrated, which will throw off reports, resource access statistics, 
etc... If you wish to migrate the data perform one of the following actions two actions.

The first is a *pre* upgrade action that involves simply renaming the column before deploying the new monitoring
extension::
     
    ALTER TABLE request_resources RENAME COLUMN resource to name;

Alternatively the migration may occur *post* upgrade::
     
    UPDATE TABLE request_resources SET name = resource where name is NULL;
    ALTER TABLE request_resources DROP COLUMN resource;

Column ``remote_user_agent`` added to ``request`` table
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* *Version*: n/a, extension still community status
* *Date*: Dec 09, 2011
* *Subversion revision*: 16634
* *Reference*: :geos:`4872`

No action should be required here as Hibernate will simply append the new column to the table. If for some reason this does
not happen the column can be added manually::

    ALTER TABLE request ADD COLUMN remote_user_agent VARCHAR(1024);

Major upgrades
--------------

