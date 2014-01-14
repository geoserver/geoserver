.. _patches:

Submitting Patches
==================

As with most open source project GeoServer is very happy to accept patches from the community. Patches
from the community have been a source of some of the best bug fixes and improvements in GeoServer and
is a great way to give back to the project.

This document is a guide designed to help users through the process of successfully submitting a patch.

Source code
-----------

Before one can generate a patch source code is needed. See the :ref:`developer quickstart <quickstart>`
for information about obtaining a copy of the GeoServer sources.

In general developers will only apply patches that apply cleanly against the latest versions of the stable
and master branches. Therefore before you generate a patch it is important that you update your checkout to 
the latest for the branch you are working on.

Generating a patch
------------------

There are a number of valid ways to generate a patch.

GitHub pull requests
^^^^^^^^^^^^^^^^^^^^

The GeoServer git repository is hosted on github, which provides a very nice way to manage patches in the 
way of `pull requests <https://help.github.com/articles/using-pull-requests/>`_. To issue a pull request 
requires that you `fork the GeoServer git repo <https://github.com/geoserver/geoserver/fork_select>`_ into 
your own account.

Assuming that ``origin`` points to your github repo the the patch workflow then becomes:

#. Make the change.

   ::

     git checkout -b my_bugfix master
     git add .
     git commit-m "fixed bug xyz"

#. Push the change up to your github repository.

   ::

     git push origin my_bugfix

#. Visit your github repo page and issue the pull request. 

At this point the core developers will be notified of the pull request and review it at the earliest 
convenience. Core developers will review the patch and might require changes or improvements to it, it
will be up to the submitter to amend the pull request and keep it alive until it gets merged.
Please be patient, pull requests are often reviewed in spare time so turn-around can be a little slow.
If a pull request becomes stale with no feedback from the submitter for a couple of months long, it will linked 
form a JIRA issue (to avoid losing the partial work) and then be closed.

Git diff
^^^^^^^^

All git clients provide an easy way to generate a patch. Assuming you are using the command line tools
a simple workflow for generating a patch is as follows.

#. Make the change as above.

#. Generate the patch.

   ::

     git diff master > my_bugfix.patch
     
#. Open a `JIRA <https://jira.codehaus.org/browse/GEOS>`_ ticket and attach the patch file to the ticket. 

At this point the core developers will be notified of the ticket t and review it at the earliest 
convenience. 

Unix diff
^^^^^^^^^

If you are not working from git, perhaps working from a source archive directly, it is always possible to 
manually generate a patch with unix diff tools.

#. Back up the source tree.
 
   ::

     cp geoserver geoserver.orig

#. Make the change.

#. Generate the diff.

   ::

     diff -ru geoserver.orig geoserver > my_bugfix.patch

#. Open a `JIRA <https://jira.codehaus.org/browse/GEOS>`_ ticket and attach the patch file to the ticket. 

At this point the core developers will be notified of the ticket t and review it at the earliest 
convenience. 

Patch guidelines
----------------

The following guidelines are meant to ensure that changes submitted via patch be as easy as possible to 
review. The easier a patch is to review the easier it is to apply.

Ensure your IDE/editor is properly configured
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ensure that your development environment is properly configured for GeoServer development. A common issue 
with patches from newcomers is that their text editor is configured to use tabs rather than spaces.

See the the :ref:`eclipse_guide` for general information about formatting and ide setup. 

Include only relevant changes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ensure the patch only contains changes relevant to the issue you are trying to fix. A common mistake is 
to include whitespace and formatting changes along with the relevant changes. These changes, while they 
may seem harmless, make the patch much harder to read.

Fix one thing at a time
^^^^^^^^^^^^^^^^^^^^^^^

Do not batch up multiple unrelated changes into a single patch. If you want to fix multiple issues work
on them separately and submit separate patches for them.

Be patient
^^^^^^^^^^

The core developers review community patches in spare time. Be cognizant of this and realize that just 
as you are contributing your own free time to the project, so is the developer who is reviewing and 
applying your patch.
