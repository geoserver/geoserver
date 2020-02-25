# Contributing

When submitting pull request:

* **Small Contribution / Single Source Code File:** For a small change to a single source file a project committer can review and apply the change on your behalf. This is a quick workaround allowing us to correct spelling mistakes in the documentation, clarify a javadoc, or accept a very small fix.

  We understand that fixing a single source file may require changes to several test case files to verify the fix addresses its intended problem.

* **Large Contributions / Multiple Files / New Files:** To  contribute a new file, or if your change effects several files, sign a [Code Contribution License]( http://docs.geoserver.org/latest/en/developer/policies/committing.html). It does not take long and you can send it via email.
   * [Corporate contributor license](https://www.osgeo.org/resources/corporate-contributor-license/)
   * [Individual contributor license](https://www.osgeo.org/resources/individual-contributor-license/)

This agreement can be printed, signed, scanned, and emailed to [info@osgeo.org](mailto:info@osgeo.org) at the Open Source Geospatial Foundation (OSGeo). [OSGeo](https://www.osgeo.org/about/)
is the non-profit which holds the GeoServer codebase for the community.

For more information, please review the section on  [submitting pull requests](http://docs.geoserver.org/latest/en/developer/policies/pull_request.html) and [making commits](http://docs.geoserver.org/latest/en/developer/policies/committing.html).

## Pull Requests

Issuing a pull request requires that you [fork the GeoServer git repository](https://github.com/geoserver/geoserver) into
your own account.

Assuming that `origin` points to your GitHub repository then the workflow becomes:

1. Make the change.

```
   git checkout -b my_bugfix master
   git add .
   git commit -m "fixed bug xyz"
```
2. Push the change up to your GitHub repository.
```
   git push origin my_bugfix
```
3. Visit your GitHub repository page and issue the pull request.

4. At this point the core developers will be notified of the pull request and review it at the earliest convenience. Core developers will review the patch and might require changes or improvements to it; it will be up to the submitter to amend the pull request and keep it alive until it gets merged.

> Please be patient, pull requests are often reviewed in spare time so turn-around can be a little slow. If a pull request becomes stale with no feedback from the submitter for a couple of months, it will linked to from a JIRA issue (to avoid losing the partial work) and then be closed.

## Pull Request Guidelines

The following guidelines are meant to ensure that your pull request is as easy as possible to  review.

* Ensure your IDE/editor is properly configured

  Ensure that your development environment is properly configured for GeoServer development. A common issue is a text editor is configured to use tabs rather than spaces.

* Include only relevant changes

  Ensure the patch only contains changes relevant to the issue you are trying to fix. A common mistake is  to include whitespace and formatting changes along with the relevant changes. These changes, while they  may seem harmless, make the patch much harder to read.

* Fix one thing at a time

  Do not batch up multiple unrelated changes into a single patch. If you want to fix multiple issues work on them separately and submit separate patches for them.

* Always add a test

  Given a large code base, the large number of external contributors, and the fast evolution of the code base, tests are really the only line of defense against accidental breakage of the contributed functionality. That is why we always demand to have at least one test, it's not a "punishment", but a basic guarantee your changes will still be there, and working, in future releases.

* Referer to a Jira ticket from the commit message

  Release managers generate a changelog by checking the tickets resolved for a given target version, if there is none, your contribution won't show up. So always create a ticket associated to your commits, and refer to it from your commit message.

* Be patient

  The core developers review community patches in spare time. Be cognizant of this and realize that just  as you are contributing your own free time to the project, so is the developer who is reviewing and applying your patch.

* Tips

  Include a test case that shows your patch fixes an issue (or adds new functionality). If you do not include a test case the developer reviewing your work will need to create one.

  [JIRA Issue](https://osgeo-org.atlassian.net/projects/GEOS/issues) are used to list your fix in the release notes each release. You can link to the JIRA ticket in your pull request description.

## Commit Guidelines

GeoServer does not have much in the way of strict commit policies. Our current conventions are:

1. **Add copyright headers:**
   * Remember to add a copyright header with the year of creation to any new file. As an example, if you are adding a file in 2018 the copyright header would be:

   ```
   /* (c) 2018 Open Source Geospatial Foundation - all rights reserved
    * This code is licensed under the GPL 2.0 license, available at the root
    * application directory.
    */
   ```

   * If you are modifying an existing file that does not have a copyright header, add one as above.

   * Updates to existing files with copyright headers do not require updates to the copyright year.

   * When adding content from another organisation maintain copyright history and original license. Only add Open Source Geospatial Foundation if you have made modifications to the file for GeoServer:

   ```
   /* (c) 2016 Open Source Geospatial Foundation - all rights reserved
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

4. **Ensure code is properly formatted:** We follow the [Google formatting conventions](https://google.github.io/styleguide/javaguide.html) with the AOSP variant (4 spaces indent instead of 2).
   
   The [google-java-format project](https://github.com/google/google-java-format) offers plugins for various IDEs. If your IDE is not supported, please just build once on the command line before committing.

## Community commit access

The first stage of access allows a developer to commit only to the community
module or extension for which they are the maintainer. This stage of access can
be obtained quite easily.

The process of getting community commit access is as follows:

1. **Email the developer list:** This first step is all about communication. In order to grant commit access the other developers on the project must first know what the intention is. Therefore any developer looking for commit access must first describe what they want to commit (usually a community module), and what it does.

2. **Sign up for a GitHub account:** GeoServer source code is hosted on Github and you'll need an account in order to access it. You can sign-up [here](https://github.com/signup/).

3. **Print, sign, scan and send the contributor agreement:**

   * [corporate_contributor.pdf](https://www.osgeo.org/wp-content/uploads/corporate_contributor.pdf)
   * [individual_contributor.pdf](https://www.osgeo.org/wp-content/uploads/individual_contributor.pdf)

   Scanned assignment agreement can be emailed to [info@osgeo.org](mailto:info@osgeo.org) at OpenPlans.

   The [contribution licenses](https://www.osgeo.org/about/licenses/) are used by OSGeo projects seeking to assign copyright directly to the foundation. These licenses are directly derived from the Apache code contribution licenses (CLA V2.0 and CCLA v r190612).

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
