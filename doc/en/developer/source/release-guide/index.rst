.. _release_guide:

Release Guide
=============

This guide details the process of performing a GeoServer release.

Before you start
----------------

SNAPSHOT release
^^^^^^^^^^^^^^^^

For any non-beta release (including release candidates) a GeoServer release requires an
corresponding GeoTools and GeoWebCache release. Therefore before you start you should
coordinate a release with these projects. Either performing the release yourself or
asking a volunteer to perform the release.

* `GeoTools Release Guide <http://docs.geotools.org/latest/developer/procedures/release.html>`_
* `GeoWebCache RELEASE_GUIDE.txt <https://github.com/GeoWebCache/geowebcache/blob/master/geowebcache/release/RELEASE_GUIDE.txt>`_

Notify developer list
^^^^^^^^^^^^^^^^^^^^^

It is good practice to notify the `GeoServer developer list <https://lists.sourceforge.net/lists/listinfo/geoserver-devel>`_ of the intention to make the release a few days in advance, even though the release date has been agreed upon before hand.

Prerequisites
-------------

The following are necessary to perform a GeoServer release:

#. Commit access to the GeoServer `Git repository <https://Github.com/geoserver/geoserver>`_
#. Build access to `Jenkins <http://ares.boundlessgeo.com/jenkins/>`_
#. Edit access to the GeoServer `Blog <http://blog.geoserver.org>`_
#. Administration rights to GeoServer `JIRA <https://jira.codehaus.org/browse/GEOS>`__
#. Release/file management privileges in `SourceForge <https://sourceforge.net/projects/geoserver/>`_

Versions and revisions
----------------------

When performing a release we don't require a "code freeze" in which no developers can commit to the repository. Instead we release from a revision that is known to pass all tests, including unit/integration tests as well as CITE tests.

To obtain the GeoServer and Geotools revisions that have passed the `CITE test <http://ares.boundlessgeo.com/jenkins/view/geoserver-cite/>`_, navigate to the latest Hudson run of the CITE test  and view it's console output and select to view its full log. For example::

    http://ares.boundlessgeo.com/jenkins/view/geoserver-cite/job/cite-wfs-1.1/9/consoleText

Perform a search on the log for 'Git revision' and you should obtain the following.::

	version = 2.2-SNAPSHOT
	Git revision = 4ea8d3fdcdbb130892a03f27ab086068b95a3b01
	Git branch = 4ea8d3fdcdbb130892a03f27ab086068b95a3b01
	build date = 03-Aug-2012 03:39
	geotools version = 8-SNAPSHOT
	geotools revision = 73e8d0746a4527e46a46e5e8bc778ca92ca89130

Since we don't make any release from master, ensure you select the right CITE test that passed to obtain the right revision.

Since most GeoServer releases require an official GeoTools release the GeoTools revision is usually not needed. But if performing a beta release it is
allowed to release directly from a specific GeoTools revision.

Release in JIRA
---------------

Run the `geoserver-release-jira <http://ares.boundlessgeo.com/jenkins/job/geoserver-release-jira/>`_ job in Jenkins. The job takes the following parameters:

**VERSION**

  The version to release, same as in the previous section. This version must match a version in JIRA.

**NEXT_VERSION**

  The next version in the series. All unresolved issues currently fils against ``VERSION`` will be transitioned to this version.

**JIRA_USER**

  A JIRA user name that has release privileges. This user  will be used to perform the release in JIRA, via the SOAP api.

**JIRA_PASSWD**

  The password for the ``JIRA_USER``.

This job will perform the tasks in JIRA to release ``VERSION``. Navigate to `JIRA <http://jira.codehaus.org/browse/GEOS>`__ and verify that the version has actually been released.

Build the Release
-----------------

Run the `geoserver-release <http://ares.boundlessgeo.com/jenkins/job/geoserver-release/>`_ job in Jenkins. The job takes the following parameters:

**BRANCH**

  The branch to release from, "2.2.x", "2.1.x", etc... This must be a stable branch. Releases are not performed from master.

**REV**

  The Git revision number to release from. eg, "24ae10fe662c....". If left blank the latest revision (ie HEAD) on the ``BRANCH`` being released is used.

**VERSION**

  The version/name of the release to build, "2.1.4", "2.2", etc...

**GT_VERSION**

  The GeoTools version to include in the release. This may be specified as a version number such as "8.0" or "2.7.5". Alternatively the version may be specified as a Git branch/revision pair in the form ``<branch>@<revision>``. For example "master@36ba65jg53.....". Finally this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again if performing a non-beta release this version must be a version number corresponding to an official GeoTools release.

**GWC_VERSION**

  The GeoWebCache version to include in the release. This may be specified as a version number such as "1.3-RC3". Alternatively the version may be specified as a Git revision of the form ``<branch>@<revision>`` such as "master@1b3243jb...". Finally this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT).Git Again if performing a non-beta release this version must be a version number corresponding to an official GeoTools release.

**GIT_USER**

  The Git username to use for the release.

**GIT_EMAIL**

  The Git email to use for the release.

This job will checkout the specified branch/revision and build the GeoServer
release artifacts against the GeoTools/GeoWebCache versions specified. When
successfully complete all release artifacts will be uploaded to the following
location::

   http://ares.boundlessgeo.com/geoserver/release/<RELEASE>

Additionally when the job completes it fires off two jobs for building the
Windows and OSX installers. These jobs run on different hudson instances.
When those jobs complete the ``.exe`` and ``.dmg`` artifacts will be uploaded
to the location referenced above.

Test the Artifacts
------------------

Download and try out some of the artifacts from the above location and do a
quick smoke test that there are no issues. Engage other developers to help
test on the developer list.

Publish the Release
-------------------

Run the `geoserver-release-publish <http://ares.boundlessgeo.com/jenkins/job/geoserver-release-publish/>`_ in Jenkins. The job takes the following parameters:

**VERSION**

  The version being released. The same value s specified for ``VERSION`` when running the ``geoserver-release`` job.

**BRANCH**

  The branch being released from.  The same value specified for ``BRANCH`` when running the ``geoserver-release`` job.

This job will rsync all the artifacts located at::

     http://ares.boundlessgeo.com/geoserver/release/<RELEASE>

to the SourceForge FRS server. Navigate to `Sourceforge <http://sourceforge.net/projects/geoserver/>`__ and verify that the artifacts have been uploaded properly. If this is the latest stable release, set the necessary flags on the ``.exe``, ``.dmg`` and ``.bin`` artifacts so that they show up as the appropriate default for users downloading on the Windows, OSX, and Linux platforms.

Create the download page
------------------------

Get the JIRA version for this release:

#. Go to `JIRA <https://jira.codehaus.org/browse/GEOS/>`__
#. Select "Change log"
#. Open the release notes for the version being released
#. The version will be in the url, e.g. ``http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&version=18700`` -> 18700

Create the new download page:

#. Go to `GeoServer web site <http://geoserver.org/>`_ and make sure you are logged in.
#. Select the "Add Page" link in the menu
#. Click "select a page template to start from"
#. Choose the "Download" template
#. Fill in the version, release date (e.g., May 17th, 2012) and the jira version
#. Set the page title to the version being released (e.g. "GeoServer 2.2-RC3")
#. Save and check all the links are working. The template includes links for all plugins; for a stable or maintenance release, remove links to plugins that are not available for that branch.

Update the download short cuts:

#. If you are releasing a beta/RC, edit the `dev version` setting for the website in `_config.yml <https://github.com/geoserver/geoserver.github.io/blob/master/_config.yml>`_.

   Edit the details for `release/dev/index.html https://github.com/geoserver/geoserver.github.io/blob/master/release/dev/index.html>`_
   
   ```
   layout: release
   title: GeoServer
   version: 2.6-RC2
   jira_version: 20356
   release_date: August 18th, 2014
   ```
   
   We do not keep landing pages for beta releases so there is no need to create a copy of this file.
   
#. If you are releasing a stable version, edit the `stable_version` setting for the website in `_config.yml <https://github.com/geoserver/geoserver.github.io/blob/master/_config.yml>`_. If this is the first stable release you will want to remove the `dev version` setting (as we no longer need to offer a release candidate download).
   
   Edit the details for `release/stable/index.html <https://github.com/geoserver/geoserver.github.io/blob/master/release/stable/index.html>`_
   
   ```
   layout: release
   title: GeoServer
   version: 2.6.0
   jira_version: 20402
   release_date:  Sep 7, 2014
   ```
   
   And copy this page when you are done:
   
   ```
   cp -R stable 2.6.0
   ```
   
   Finally update the `download/index.html <https://github.com/geoserver/geoserver.github.io/blob/master/download/index.html>`_ page adding a link to 2.6.0 to the list.
   
#. If you are releasing a maintenance version, edit the `maintenance_version` setting for the website in `_config.yml <https://github.com/geoserver/geoserver.github.io/blob/master/_config.yml>`_.
   
   Edit the details for `release/maintenance/index.html <https://github.com/geoserver/geoserver.github.io/blob/master/release/stable/index.html>`_
   
   ```
   layout: release
   title: GeoServer
   version: 2.5.8
   jira_version: 20457
   release_date:  August 18, 2014
   ```
   
    And copy this page when you are done:
   
   ```
   cp -R maintenance 2.5.8
   ```
   
   Adding a link to the download page when you are done.

Post the Documentation
----------------------

.. note:: For now, this requires Boundless credentials; if you do not have them, please ask on the `GeoServer developer list <https://lists.sourceforge.net/lists/listinfo/geoserver-devel>`_ for someone to perform this step for you.

.. note:: This content will likely move to GitHub in the near future.

#. Log in to the server.

#. Create the following new directories::

     /var/www/docs.geoserver.org/htdocs/a.b.c
     /var/www/docs.geoserver.org/htdocs/a.b.c/developer
     /var/www/docs.geoserver.org/htdocs/a.b.c/user

   where ``a.b.c`` is the full release number.

#. Download the HTML documentation archive from the GeoServer download page, and extract the contents of both user manuals to the appropriate directory::

    cd /var/www/docs.geoserver.org/htdocs/a.b.c/
    sudo wget http://downloads.sourceforge.net/geoserver/geoserver-a.b.c-htmldoc.zip
    sudo unzip geoserver-a.b.c-htmldoc.zip
    sudo rm geoserver-a.b.c-htmldoc.zip

#. Open the file :file:`/var/www/docs.geoserver.org/htdocs/index.html` in a text editor.

#. Add a new entry in the table for the most recent release::

    <tr>
      <td><strong><a href="http://geoserver.org/release/a.b.c/">a.b.c</a></strong></td>
      <td><a href="a.b.c/user/">User Manual</a></td>
      <td><a href="a.b.c/developer/">Developer Manual</a></td>
    </tr>

#. Save and close this file.

Announce the Release
--------------------

GeoServer Blog
^^^^^^^^^^^^^^

.. note:: This announcement should be made for all releases, including betas and release candidates.

.. note::

   This step requires an account on http://blog.geoserver.org/

#. Log into the `GeoServer Blog <http://blog.geoserver.org/wp-login.php>`_.

#. Create a new post. The post should be more "colorful" than the average
   announcement. It is meant to market and show off any and all new
   features.

   .. code-block:: html

      The GeoServer team is pleased to announce the release of
      <a href="http://geoserver.org/release/2.5.1/">GeoServer 2.5.1</a>:
      <ul>
         <li>Downloads (<a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1-bin.zip/download">zip</a>,
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1-war.zip/download">war</a>,
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1.dmg/download">dmg</a> and
             <a href="http://sourceforge.net/projects/geoserver/files/GeoServer/2.5.1/geoserver-2.5.1.exe/download">exe</a>) are listed on the
             <a href="http://geoserver.org/release/2.5.1/">GeoServer 2.5.1</a> page
             along with documentation and extensions.
            <ul>
               <li>This release includes and is made in conjunction with
                 <a href="http://geotoolsnews.blogspot.com/2014/05/geotools-111-released.html">GeoTools 11.1</a>.</li>
            </ul>
         </li>
         <li>Thanks to <a href="http://www.warwickshire.gov.uk/">Warwickshire County Council</a>
             for some great GeoWebCache integration work:
            <ul>
               <li>GeoWebCache tile layer HTTP cache headers are now taken from GeoServer layer configration</li>
               <li>GeoWebCache settings are now correctly saved on Windows</li>
            </ul>
         </li>
         <li>A wide range of improvements provided by the community
            <ul>
               <li>Scale hints now exposed in WMS GetCapabilities document</li>
               <li>Fixed Symbology Encoding 1.1 encoding of relative external graphics</li>
               <li>Addressed axis order issues cascading WMS 1.3.0 services through GeoServer</li>
            </ul>
         </li>
      </ul>
      More details can be found in the
      <a href="http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&amp;version=19231">GeoServer 2.5.1 Release Notes</a>.

#. Examples of content:

   * Link to the **Download Page** in the wiki created above, and possibly to the
     installers for each platform.

     Example: `GeoServer 2.3.4 Released <http://blog.geoserver.org/2013/07/28/geoserver-2-3-4-released/>`_

   * Indicate which version of GeoTools is used, and thank your employer.

   * Link to completed pull requests and Jira tickets, looking for new features or
     important bug fixes to highlight. Make a point to thank new contributors
     and sponsors.

     Example: `GeoServer 2.3.1 released <http://blog.geoserver.org/2013/04/23/geoserver-2-3-1-released/>`_

   * For the run up to a major release you can build up a list of the new features and
     change requests.

     Example: `GeoServer 2.4 Beta Released <http://blog.geoserver.org/2013/07/22/geoserver-2-4-beta-released/>`_

   * For the major release you can spend a bit more time on the new features, linking
     to blog posts if they are available.

     Example: `GeoServer 2.3-beta released <http://blog.geoserver.org/2013/01/29/geoserver-2-3-beta-released/>`_

#. Do not publish the post right away. Instead ask the devel list for review.


Mailing lists
^^^^^^^^^^^^^

.. note:: This announcement should be made for all releases, including betas and release candidates.

Send an email to both the developers list and users list announcing the
release. The message should be relatively short. You can base it on the blog post.
The following is an example::

   Subject: GeoServer 2.5.1 Released

   The GeoServer team is happy to announce the release of GeoServer 2.5.1.
  
   The release is available for download from:

   http://geoserver.org/release/2.5.1/

   GeoServer 2.5.1 is the next the stable release of GeoServer and is recommended for production deployment.

   This release comes with some exciting new features. The new and
   noteworthy include:
   
   * By popular request Top/Bottom labels when configuring layer group order
   * You can now identify GeoServer “nodes” in a cluster by configuring a label and color in the UI. Documentation and example in the user guide.
   * Have you ever run GeoServer and not quite gotten your file permissions correct? GeoServer now has better logging when it cannot your data directory and is required to “fall back” to the embedded data directory during start up.
   * We have a new GRIB community module (community modules are not in the release until they pass a    QA check, but great to see new development taking shape)
   * Documentation on the jp2kak extension now in the user guide
   * Additional documentation for the image mosaic in the user guide with tutorials covering the plugin, raster time-series, time and elevation and footprint management.
   * WCS 2.0 support continues to improve with DescribeCoverage now supporting null values
   * Central Authentication Service (CAS) authentication has received a lot of QA this release and is now available in the GeoServer 2.5.x series.
   * This release is made in conjunction with GeoTools 11.1
   
   Along with many other improvements and bug fixes:
   
   * https://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&version=20202

   Thanks to Andrea and Jody (GeoSolutions and Boundless) for publishing this release. A very special thanks to all those who contributed bug fixes, new
   features, bug reports, and testing to this release.

   --
   The GeoServer Team


SlashGeo
^^^^^^^^

.. note:: This announcement should be made only for official releases. Not betas and release candidates.

.. note::

   This step requires an account on http://slashgeo.org

#. Go to http://slashgeo.org, and log in, creating an account if necessary.

#. Click the **Submit Story** link on the left hand side of the page.
   Examples of previous stories:

   * http://technology.slashgeo.org/technology/08/12/09/1745249.shtml
   * http://industry.slashgeo.org/article.pl?sid=08/10/27/137216

FreeGIS
^^^^^^^

.. note:: This announcement should be made only for official releases. Not betas and release candidates.

Send an email to ``bjoern dot broscheit at uni-osnabrueck dot de``.
Example::

  Subject: GeoServer update for freegis

  GeoServer 1.7.1 has been released with some exciting new features. The big
  push for this release has been improved KML support. The new and noteworthy
  include:

    * KML Super Overlay and Regionating Support
    * KML Extrude Support
    * KML Reflector Improvements
    * Mac OS X Installer
    * Dutch Translation
    * Improved Style for Web Admin Interface
    * New SQL Server DataStore Extension
    * Improved Oracle DataStore Extension
    * Default Templates per Namespace

  Along with many other improvements and bug fixes. The entire change log for
  the 1.7.1 series is available in the issue tracker:

  http://jira.codehaus.org/browse/GEOS/fixforversion/14502

FreshMeat
^^^^^^^^^

.. note:: This announcement should be made only for official rel-eases. Not betas and release candidates.

.. note::

   This step requires an account on http://freshmeat.net/

#. Go to http://freshmeat.net/ and log in.
#. Search for "geoserver" and click the resulting link.
#. Click the **add release** link at the top of the page.
#. Choose the **Default** branch
#. Enter the version and choose the appropriate **Release focus**.

   .. note::

      The release focus is usually 4,5,6, or 7. Choose which ever is
      appropriate.

#. Enter a succinct description (less than 600 characters) of the **Changes**.
#. Update the links to the following fields:

   * Zip
   * OS X package
   * Changelog

#. Click the **Step 3** button.
#. Click the **Finish** button.
