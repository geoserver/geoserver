# Contributing

Contributors are asked to provide a [Contributor Agreement](http://wiki.osgeo.org/wiki/Contributor_Agreement) for working on the project:

* [Corporate Contributor License Agreement (CCLA)](http://wiki.osgeo.org/wiki/Corporate_Contributor_License_Agreement_(CCLA))
* [Individual Contributor License Agreement (CLA)](http://wiki.osgeo.org/wiki/Individual_Contributor_License_Agreement_(CLA))

This agreement can be printed, signed, scanned, and emailed to [info@osgeo.org](mailto:info@osgeo.org) at the Open Source Geospatial Foundation (OSGeo). [OSGeo](http://www.osgeo.org/content/foundation/about.html)
is the non-profit which holds the GeoServer codebase for the community.

For more information, please review the section on  [submitting patches](http://docs.geoserver.org/latest/en/developer/policies/patches.html) and [making commits](http://docs.geoserver.org/latest/en/developer/policies/committing.html).

## Pull Requests

Issuing a pull request requires that you [fork the GeoServer git repository ](https://github.com/geoserver/geoserver/fork_select) into 
your own account.

Assuming that `origin` points to your GitHub repository then the workflow becomes:

1. Make the change.

`````
   git checkout -b my_bugfix master
   git add .
   git commit -m "fixed bug xyz"
````
2. Push the change up to your GitHub repository.
````
   git push origin my_bugfix
````
3. Visit your GitHub repository page and issue the pull request. 

4. At this point the core developers will be notified of the pull request and review it at the earliest convenience. Core developers will review the patch and might require changes or improvements to it; it will be up to the submitter to amend the pull request and keep it alive until it gets merged.

   Please be patient, pull requests are often reviewed in spare time so turn-around can be a little slow. If a pull request becomes stale with no feedback from the submitter for a couple of months, it will linked to from a JIRA issue (to avoid losing the partial work) and then be closed.

## Pull Request Guidelines

The following guidelines are meant to ensure that your pull request is as easy as possible to  review.

* Ensure your IDE/editor is properly configured

  Ensure that your development environment is properly configured for GeoServer development. A common issue is a text editor is configured to use tabs rather than spaces.

* Include only relevant changes

  Ensure the patch only contains changes relevant to the issue you are trying to fix. A common mistake is  to include whitespace and formatting changes along with the relevant changes. These changes, while they  may seem harmless, make the patch much harder to read.

* Fix one thing at a time

  Do not batch up multiple unrelated changes into a single patch. If you want to fix multiple issues work on them separately and submit separate patches for them.

* Be patient

  The core developers review community patches in spare time. Be cognizant of this and realize that just  as you are contributing your own free time to the project, so is the developer who is reviewing and applying your patch.

* Tips

  Include a test case that shows your patch fixes an issue (or adds new functionality). If you do not include a test case the developer reviewing your work will need to create one.

  [JIRA Issue](http://jira.codehaus.org/browse/GEOS) are used to list your fix in the release notes each release. You can link to the JIRA ticket in your pull request description.

## Commit Guidelines

There is not much in the way of strict commit policies when it comes to committing
in GeoServer. But over time some rules and conventions have emerged:

1. **Update copyright headers:** When adding new source files to the repository remember to add the standard copyright header:

   ```
   /* (c) 2014 Open Source Geospatial Foundation - all rights reserved
    * This code is licensed under the GPL 2.0 license, available at the root
    * application directory.
    */
   ```
   When updating a file .. update the header:

   ```
   /* (c) 2013-2014 Open Source Geospatial Foundation - all rights reserved
    * This code is licensed under the GPL 2.0 license, available at the root
    * application directory.
    */
   ```

   When adding content from another organisation maintain copyright history and original license. Only add (c) OSGeo if you have made modifications to the file for GeoServer:

   ```
   /* (c) 2014 Open Source Geospatial Foundation - all rights reserved
    * (c) 2014 OpenPlans
    * (c) 2008-2010 GeoSolutions
    * 
    * This code is licensed under the GPL 2.0 license, available at the root
    * application directory.
    * 
    * Original from GeoWebCache 1.5.1 under a LGPL license 
    */
   ```

   In a rare case (as when asking to migrate content from GeoTools) you can obtain permission to change the license to our GPL 2.0 license.


2. **Do not commit large amounts of binary data:** In general do not commit any binary data to the repository. There are cases where it is appropriate like some data for a test case, but in these cases the files should be kept as small as possible.

3. **Do not commit jars or libs, use Maven instead:** In general never commit a depending library directly into the repository, this is what we use Maven for. If you have a jar that is not present in any maven repositories, ask on the developer list to get it uploaded to one of the project maven repositories.

4. **Ensure code is properly formatted:** Ensure that the IDE or editor used to edit source files is setup with proper
   formatting rules. This means spaces instead of tabs, 100 character line break, etc...

   If using Eclipse ensure you have configured it with the [template and formatter ](http://docs.geotools.org/latest/developer/conventions/code/style.html#use-of-formatting-tools) used for GeoTools.

## Community commit access

The first stage of access allows a developer to commit only to the community
module or extension for which they are the maintainer. This stage of access can
be obtained quite easily.

The process of getting community commit access is as follows:

1. **Email the developer list:** This first step is all about communication. In order to grant commit access the other developers on the project must first know what the intention is. Therefore any developer looking for commit access must first describe what they want to commit (usually a community module), and what it does.

2. **Sign up for a GitHub account:** GeoServer source code is hosted on Github and you'll need an account in order to access it. You can sign-up [here](https://github.com/signup/).

3. **Print, sign, scan and send the contributor agreement:**

   * [corporate_contributor.txt](http://www.osgeo.org/sites/osgeo.org/files/Page/corporate_contributor.tx)
   * [individual_contributor.txt](http://www.osgeo.org/sites/osgeo.org/files/Page/individual_contributor.txt)

   Scanned assignment agreement can be emailed to [info@osgeo.org](mailto:info@osgeo.org) at OpenPlans.

   The [contribution licenses](http://www.osgeo.org/content/foundation/legal/licenses.html) are used by OSGeo projects seeking to assign copyright directly to the foundation. These licenses are directly derived from the Apache code contribution licenses (CLA V2.0 and CCLA v r190612).

4. **Notify the developer list:** After a developer has signed up on Github they must notify the developer list. A project despot will then add them to the group of GeoServer committers and grant write access to the canonical repository.

5. **Fork the canonical GeoServer repository:** All committers maintain a fork of the GeoServer repository that they work from. Fork the canonical repository into your own account.

6. **Configure your local setup:** Follow this [guide](http://docs.geoserver.org/latest/en/developer/source.html#source) in the developer manual.

### Core commit access

The second allows a developer to make commits to the core modules of geoserver.
Being granted this stage of access takes time, and is obtained only after the
developer has gained the trust of the other core committers.

The process of obtaining core commit access is far less mechanical than the one
to obtain community commit access. It is based solely on trust. To obtain core
commit access a developer must first obtain the trust of the other core
committers.

The way this is typically done is through continuous code review of patches.
After a developer has submitted enough patches that have been met with a
positive response, and those patches require little modifications, the
developer will be nominated for core commit access.

There is no magic number of patches that make the criteria, it is based mostly
on the nature of the patches, how in depth the they are, etc... Basically it
boils down to the developer being able to show that they understand the code base
well enough to not seriously break anything.
