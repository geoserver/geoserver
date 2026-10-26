# Release Guide

This guide details the process of performing a GeoServer release.

## Before you start

### SNAPSHOT release

For any release (including release candidates) a GeoServer release requires the corresponding GeoTools and GeoWebCache releases. Therefore, before you start you should coordinate a release with these projects, either performing the release yourself or asking a volunteer to perform the release.

- [GeoTools Release Guide](https://docs.geotools.org/latest/developer/procedures/release.html)
- [GeoWebCache Release repo README.md](https://github.com/GeoWebCache/gwc-release)

### Notify developer group

Post a message on the [GeoServer developer group](https://discourse.osgeo.org/c/geoserver/developer/63) a few days in advance, even though the release date has been agreed upon beforehand. It is a good idea to remind developers to get any fixes 24 hours prior to release day, and to start a team discussion to identify any known blockers.

## Prerequisites

The following are necessary to perform a GeoServer release:

1.  Commit access to the GeoServer [Git repository](https://github.com/geoserver/geoserver)
2.  Build access to [Jenkins](https://build.geoserver.org/jenkins/)
3.  Edit access to the GeoServer [Blog](https://blog.geoserver.org)
4.  Administration rights to GeoServer [Jira](https://osgeo-org.atlassian.net/projects/GEOS)
5.  Release/file management privileges in [SourceForge](https://sourceforge.net/projects/geoserver/)

## Versions and revisions

When performing a release, we don't require a "code freeze" in which no developers can commit to the repository. Instead we release from a revision that is known to pass all tests, including unit/integration tests from a nightly build.

To obtain the GeoServer, GWC and GeoTools revisions that have passed testing, navigate to [geoserver.org/download > Development](https://geoserver.org/download), find the correct series (e.g. 3.0.x) and download a "binary" nightly build. From the download check the **`src/target/VERSION.txt`** file. For example:

```none
version = 3.0.3-SNAPSHOT
git revision = 1ee183d9af205080f1543dc94616bbe3b3e4f890
git branch = origin/3.0.x
build date = 26-Oct-2026 04:41
geotools version = 35-SNAPSHOT
geotools revision = 3bde6940610d228e01aec9de7c222823a2638664
geowebcache version = 2.0-SNAPSHOT
geowebcache revision = 27eec3fb31b8b4064ce8cc0894fa84d0ff97be61/27eec
hudson build = -1
```

Since most GeoServer releases require official GeoTools and GeoWebCache releases, the GeoTools and GeoWebCache revisions are usually not needed - the version numbers are used instead.

The GeoServer version is defined once, as the `revision` property in `src/pom.xml`, and used by all modules as `${revision}` (see [Maven Guide](../../maven-guide/index.md#project-version)). Older branches (such as `2.28.x`) use a literal version in every `pom.xml`.

The release jobs run `build/build_release.sh` and `build/publish_release.sh` from the `main` branch for every `BRANCH` being released, so these scripts (and `build/functions`) must work for both styles. Changes to them are made on `main` and do not need to be backported.

## Release in Jira

1.  Navigate to the [GeoServer project page](https://osgeo-org.atlassian.net/projects/GEOS?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page&status=released-unreleased) in Jira.
2.  Add a new version for the next version to be released after the current release. For example, if you are releasing GeoServer 3.0.3, create version 3.0.4. Enter the current date as the Start Date and use the date from the [release schedule](https://github.com/geoserver/geoserver/wiki/Release-Schedule) for the Release Date. For the final scheduled (archive) release of a series (typically x.y.6), do still create the next version (e.g. x.y.7) with a Description of `End of life, may not be released ever`.
3.  Click in the Actions column for the version you are releasing and select Release. Update the Release Date to the current date when prompted. If there are still unsolved issues remaining in this release, you will be prompted to move them to an unreleased version. If so, choose the new version you created in step 2 above.
4.  Check all the issues in this release for any that do not have a component and rectify (before running the `announcement.py` utility below.)

## GeoServer release announcement

!!! note
    Start this activity early to work on while release jobs are running. This will also serve to review resolved Jira issues before release notes are created.

The [GeoServer website](https://geoserver.org/) is managed as a [GitHub Pages repository](https://github.com/geoserver/geoserver.github.io).

1.  Follow the [instructions](https://github.com/geoserver/geoserver.github.io#releases) in the repository to create a release announcement.:

        python3 announcement.py username password 3.0.2 --geotools 35.2 --geowebcache 2.0.2

    For the initial release in a series you will need to make:

    - A `_layout` html template updated for any new extensions
    - A `bin/templates/about31.md` markdown snippet linking to new features and highlights

    Please read the instructions or script help for additional options beyond the example above.

2.  The announcement page header fields include the information required to generate a download page for the release.

    The page is generated from resolved Jira tickets:

    - You may need to review closed pull requests and Jira tickets to double check what is being announced.

      `https://github.com/geoserver/geoserver/compare/3.0.1...3.0.2`

    - Check for Jira tickets without a component, often these are community modules.

      Tip: To determine the correct component check the pull-request for the ticket. The files changed indicate what component has been modified.

3.  Check the "Security Considerations" section, generated when resolved Jira issues are vulnerabilities (security level "Vulnerability", or the "Disclosure" field set).

    **Important:** Review [Security Procedure](../../policies/security.md) for expectations on what to include and write here.

    - The release announcement does not list CVE numbers or details, only the urgency and when details will be provided:

          ## Security Considerations

          This release addresses security vulnerabilities and is an important upgrade for production systems. Details about the fixes will be provided at the start of November.

          See project [security policy](https://github.com/geoserver/geoserver/blob/main/SECURITY.md) for more information on how security vulnerabilities are managed.

      It is your judgement call how to word this section, use `--urgency normal|important|urgent` (default `important`): every security fix is a recommended update, one with serious consequences could be considered "important", and an active exploit considered "urgent". Use `--concurrent 3.0.3,2.28.7` to mention security releases made at the same time.

    - When everyone has had an opportunity to update, the vulnerabilities are publicly disclosed, normally alongside the first release of the next series. Each vulnerability has its Jira "Disclosure" field set to that release, and was fixed in several prior releases whose announcements need updating:

          python3 announcement.py username password 2.24.0 --disclosure 2.24.0

      This lists the vulnerabilities scheduled for disclosure in `2.24.0`, the blog posts for each fix version (for example `2-22-5-released.md`, `2-23-2-released.md` and `2-24-0-released.md`), and the "Security Considerations" text to add to each of them:

          ## Security Considerations

          This release addresses security vulnerabilities and is considered an important upgrade for production systems.

          * [GEOS-11121](https://osgeo-org.atlassian.net/browse/GEOS-11121) CVE-2023-41339: Unsecured WMS dynamic styling sld=<url> parameter affords blind unauthenticated SSRF

          The use of the CVE system allows the GeoServer team to reach a wider audience than blog posts.

          See project [security policy](https://github.com/geoserver/geoserver/blob/main/SECURITY.md) for more information on how security vulnerabilities are managed.

      Update the prior blog posts with a single pull-request, merged when the security advisories are published.

4.  Review the new features, documenting each with a heading, screen snap, and thanking the appropriate developer and organization responsible. :

        ## File System Sandbox Isolation

        A file system sandbox is used to limit access for GeoServer Administrators and Workspace Administrators to specified file folders.

        * A system sandbox is established using `GEOSERVER_FILESYSTEM_SANDBOX` application property, and applies to the entire application, limiting GeoServer administrators to the `<sandbox>` folder, and individual workspace administrators into isolated `<sandbox>/<workspace>` folders.

        * A regular sandbox can be configured from the **Security > Data** screen, and is used to limit individual workspace administrators into `<sandbox>/<workspace>` folders to avoid accessing each other's files.

          ![](/img/posts/2.26/filesystem-sandbox.png)

        Thanks to Andrea (GeoSolutions) for this important improvement at the bequest of [Munich RE](https://www.munichre.com/en.md).

        - [GSIP 229 - File system access isolation](https://github.com/geoserver/geoserver/wiki/GSIP-229)
        - [File system sandboxing](https://docs.geoserver.org/2.26.x/en/user/security/sandbox.md) (User Manual)

    For the initial release in a series there may be several new features to document in this manner.

5.  Create a pull-request for the new website.

    For the initial release expect input from developers to highlight changes and work performed.

## When publishing the first release of a series, create the stable branch

After publishing the first release of a series from the main development branch (e.g. `3.1.0`), there are some extra steps to create the new stable branch and update the version on the main development branch. Release candidates and milestones do not create a stable branch.

1.  Checkout the main development branch and make sure it is up to date and that there are no changes in your local workspace:

        git checkout main
        git pull
        git status

2.  Create the new stable branch, advance it to the next patch version, and push it to GitHub; for example, if `3.0.0` was published from the main development branch and the remote for the official GeoServer is called `geoserver`:

        git checkout -b 3.0.x
        build/rename.sh advance 3.0.0 3.0.1
        git commit -am "Advance 3.0.x to 3.0.1-SNAPSHOT"
        git push geoserver 3.0.x

3.  Enable [GitHub branch protection](https://github.com/geoserver/geoserver/settings/branches) for the new stable branch: tick "Protect this branch" (only) and press "Save changes".

    Check: Branch protection is configured with a wild card, but you can confirm the pattern correctly protects the branch.

4.  Checkout the main development branch:

        git checkout main

5.  Update the version in all pom.xml files; for example, if changing the main development branch from `3.0.0-SNAPSHOT` to `3.1.0-SNAPSHOT`.

    Run **`build/rename.sh`**, passing the current and next GeoServer versions along with the corresponding GeoTools and GeoWebCache versions:

    ```shell
    build/rename.sh rename 3.0.0 3.1.0 35 36 2.0 2.1
    ```

    This updates the `<revision>` property in `src/pom.xml` (inherited by all modules as `${revision}`), the standalone documentation and archetype `pom.xml` files, the Windows installer configs, release artifact descriptors, and `doc/version.py` in one step. The script works on macOS and Linux, and only changes files tracked by git.

6.  Commit the changes and push to the main development branch on GitHub:

        git commit -am "Updated version to 3.1.0-SNAPSHOT, updated GeoTools dependency to 36-SNAPSHOT, updated GeoWebCache dependency to 2.1-SNAPSHOT"
        git push geoserver main

7.  Create the new RC version in [Jira](https://osgeo-org.atlassian.net/projects/GEOS) for issues on the main development branch; for example, if the main development branch is now `3.1.0-SNAPSHOT`, create a Jira version `3.1.0` for the first release of the `3.1.x` series

8.  Update the main, nightly and live-docs jobs on build.geoserver.org:

    1.  Disable the maintenance jobs, and remove them from the geoserver view.

        **Warning**: If you wish to keep the `geoserver-<version>-docs` job for emergencies be sure to edit the live-docs build to comment out publishing to `maintain` location.:

            # Change this when releasing
            # LINK=maintain 
            ...
            # echo "link $VER to $LINK_PATH"
            # ssh -oStrictHostKeyChecking=no -p 2223 $REMOTE "if [ -e $LINK_PATH ]; then rm $LINK_PATH; fi && ln -s $REMOTE_PATH $LINK_PATH"
            #
            # echo "docs published to https://docs.geoserver.org/$LINK/en/user"

    2.  Create new jobs, copying from the existing stable jobs, and edit the branch.

    3.  For the previously stable version, modify the last line of `geoserver-<version>-docs` job, changing `stable` to `maintain` so that it publishes to the `https://docs.geoserver.org/maintain` location.:

            # Change this when releasing
            LINK=maintain 

        The new job you created should publish to `stable`, and the main development branch will continue to publish to `latest`.

    4.  Update the **Dashboard \> Manage Jenkins \> System** global properties environmental variable used by the `geoserver-main-nightly` docker build step to have correct name for publishing `main` branch.

        - Name: `GEOSERVER_MAIN_DOCKER_NAME`
        - Value: `3.1.0-SNAPSHOT`

9.  Update the MAIN variable in Docker [release.sh](https://github.com/geoserver/docker/blob/master/build/release.sh#L6) to the new main branch version, and the `MAIN_VERSION`, `STABLE_VERSION` and `MAINTENANCE_VERSION` variables in Docker [publish.yml](https://github.com/geoserver/docker/blob/master/.github/workflows/publish.yml).

10. Announce on the developer group that the new stable branch has been created.

11. Switch to the new stable branch and update the documentation links, replacing `docs.geoserver.org/latest` with `docs.geoserver.org/3.0.x` (for example):

    - `README.md`

## Build the Release

Run the `geoserver-release` job in Jenkins:

- [geoserver-release-jdk17](https://build.geoserver.org/view/release/job/geoserver-release-jdk17/): GeoServer 2.28.x and newer
- [geoserver-release-jdk11](https://build.geoserver.org/view/release/job/geoserver-release-jdk11/): GeoServer 2.27.x

The job takes the following parameters:

### BRANCH

The branch to release from, "3.0.x", "2.28.x", etc... Release candidates, milestones and the first release of a series (e.g. `3.1.0`) are released from `main`.

### REV

The Git revision number to release from, e.g. "24ae10fe662c....". If left blank, the latest revision (i.e. HEAD) on the `BRANCH` being released is used.

### VERSION

The version/name of the release to build, "3.0.3", "2.28.7", etc...

### GT_VERSION

The GeoTools version to include in the release. This may be specified as a version number such as "35.2" or "34.6". Alternatively, the version may be specified as a Git branch/revision pair in the form `<branch>@<revision>`. For example "main@36ba65jg53.....". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again, this version must be a version number corresponding to an official GeoTools release.

### GWC_VERSION

The GeoWebCache version to include in the release. This may be specified as a version number such as "2.0.2". Alternatively, the version may be specified as a Git revision of the form `<branch>@<revision>` such as "main@1b3243jb...". Finally, this value may be left blank in which the version currently declared in the geoserver pom will be used (usually a SNAPSHOT). Again, this version must be a version number corresponding to an official GeoWebCache release.

### GIT_USER

The Git username to use for the release.

### GIT_EMAIL

The Git email to use for the release.

This job will checkout the specified branch/revision and build the GeoServer release artifacts against the GeoTools/GeoWebCache versions specified. When successfully complete all release artifacts will be listed under artifacts in the job summary.

Additionally, when the job completes, it fires off another job for a windows worker (on Azure, provided by GeoCat). When this job completes (est. 30 min) it will add the `.exe` artifacts. If the `.exe` artifacts do not appear, ask GeoCat to investigate the Azure worker.

!!! note
    Presently the Azure worker is unable to upload to build.geoserver.org, so a GeoCat employee uploads the `.exe` artifacts to SourceForge directly.

## Test the Artifacts

Download and try out some of the artifacts from the above location and do a quick smoke test that there are no issues. Engage other developers to help test on the developer group.

It is important to test the artifacts using the minimum supported version of Java (currently Java 17).

## Publish the Release

Run the `geoserver-release-publish` in Jenkins:

- [geoserver-release-publish-jdk17](https://build.geoserver.org/view/release/job/geoserver-release-publish-jdk17/): GeoServer 2.28.x and newer
- [geoserver-release-publish-jdk11](https://build.geoserver.org/view/release/job/geoserver-release-publish-jdk11/): GeoServer 2.27.x

The job takes the following parameters:

### VERSION

The version being released. The same value specified for `VERSION` when running the `geoserver-release` job.

### BRANCH

The branch being released from. The same value specified for `BRANCH` when running the `geoserver-release` job.

This job will rsync all the artifacts located at:

    https://build.geoserver.org/geoserver/release/<RELEASE>

to the SourceForge FRS server. Navigate to [SourceForge](https://sourceforge.net/projects/geoserver/files/GeoServer/) and verify that the artifacts have been uploaded properly. If this is the latest stable release, set the necessary flags (you will need to be logged in as a SourceForge admin user) on the `.exe` and `-bin.zip` artifacts so that they show up as the appropriate default for users downloading on the Windows and Linux platforms. This does not apply to maintenance or support releases.

Update `_config.yml` in the [website repository](https://github.com/geoserver/geoserver.github.io#releases) so the nightly build page follows the next release: set `stable_nightly` (or `maintain_nightly` for a maintenance release) to the Jira version id of the next release.

### Docker images

Once the release is available on SourceForge, run the Docker [publish.yml](https://github.com/geoserver/docker/actions/workflows/publish.yml) workflow to build multi-arch (`linux/amd64`, `linux/arm64`) images, with and without GDAL:

    gh workflow run publish.yml -R geoserver/docker --ref master -f version=3.0.2

The workflow pushes `<version>` and `<version>-gdal` to `docker.osgeo.org/geoserver`, along with the series tags `<major>.<minor>-latest` and `stable-latest` or `maintenance-latest`. Check that both architectures are listed:

    docker buildx imagetools inspect docker.osgeo.org/geoserver:3.0.2

If a build job fails while downloading from SourceForge, re-run only the failed jobs:

    gh run rerun <run-id> --failed -R geoserver/docker

For GeoServer 3.x the publish also reverts the release commit on the `<RELEASE>.x` branch and merges it back into `BRANCH`. When `BRANCH` is a stable branch (e.g. `3.0.x`) it is then advanced to the next patch version (e.g. releasing `3.0.2` advances `3.0.x` to `3.0.3-SNAPSHOT`). The `main` branch is not advanced by the job, see [create the stable branch](#when-publishing-the-first-release-of-a-series-create-the-stable-branch) after publishing the first release of a series.

To advance a stable branch by hand, run `build/rename.sh` from a `main` checkout inside a checkout of the stable branch; it updates the git checkout of the current directory:

    cd geoserver-3.0.x
    ../geoserver/build/rename.sh advance 3.0.2 3.0.3
    git commit -am "Advance 3.0.x to 3.0.3-SNAPSHOT"

## Cite Certification

For a major (3.x.0) release, follow the [instructions](https://docs.geoserver.org/main/en/developer/cite-test-guide/index#cite-certification) to obtain certification, and include it in the release notes below.

## Release notes

The `geoserver-release` job tags the release located in:

    https://github.com/geoserver/geoserver/tags/<RELEASE>

Publish Jira markdown release notes to GitHub tag:

1.  Select the correct release from [Jira Releases](https://osgeo-org.atlassian.net/projects/GEOS?orderField=RANK&selectedItem=com.atlassian.jira.jira-projects-plugin%3Arelease-page&status=released) page.

2.  From the release page, locate the **Release notes** button at the top of the page to open the release notes edit

3.  Generate release notes as markdown:

    - Select format `Markdown`
    - Layout: Issue key with link
    - Issue types: All

    Change the heading to `Release notes`, and apply the change with **Done**.

    Use **Copy to clipboard** to obtain the markdown, similar to the following:

    ```text
    # Release notes

    ### Bug

    [GEOS-10264](https://osgeo-org.atlassian.net/browse/GEOS-10264) Address startup warning File option not set for appender [geoserverlogfile]

    [GEOS-10263](https://osgeo-org.atlassian.net/browse/GEOS-10263) WPSRequestBuilderTest assumes that JTS:area is the first process in the list

    [GEOS-10255](https://osgeo-org.atlassian.net/browse/GEOS-10255) i18n user interface inconsistent layout with br tags used for layout

    [GEOS-10245](https://osgeo-org.atlassian.net/browse/GEOS-10245) jdbcconfig: prefixedName filter field not updated

    [GEOS-9950](https://osgeo-org.atlassian.net/browse/GEOS-9950) MapPreviewPage logs unable to find property: format.wfs.text/csv continuously

    ### Improvement

    [GEOS-10246](https://osgeo-org.atlassian.net/browse/GEOS-10246) jdbcconfig: performance slow-down from unnecessary transactions

    ### New Feature

    [GEOS-10223](https://osgeo-org.atlassian.net/browse/GEOS-10223) Support MBTiles in OGC Tiles API

    ### Task

    [GEOS-10247](https://osgeo-org.atlassian.net/browse/GEOS-10247) Reuse of service documentation references for workspace, metadata and default language
    ```

4.  Navigate to GitHub tags <https://github.com/geoserver/geoserver/tags>

    Locate the new tag from the list, and use **... --> Create release**

    - Release title: `GeoServer 3.0.2`
    - Write: Paste the markdown from Jira release notes editor
    - Set as the latest release: only tick this for stable releases, leave unticked for maintenance and support releases

    Use **Publish release** button to publish the release notes.

## Announce the Release

### Mailing lists

!!! note
    This announcement should be made for all releases, including release candidates.

Post an announcement on both the Discourse User and Developer groups announcing the release. The message should be relatively short. You can base it on the blog post headings which often indicate new features to highlight.

The following is an example:

```
Subject: GeoServer 3.0.2 Released

The GeoServer team is happy to announce the release of GeoServer 3.0.2.

The release is available for download from:

https://geoserver.org/release/3.0.2/

GeoServer 3.0.2 is the next stable release of GeoServer and is recommended for production deployment.

This release comes with some exciting new features. The new and noteworthy include:

* The OGC API Maps community module has been aligned with the final OGC API - Maps 1.0.0 standard
* GeoFence for GeoServer 3, completing crowdfunding milestone 4
* This release is made in conjunction with GeoTools 35.2 and GeoWebCache 2.0.2

Along with many other improvements and bug fixes:

* https://osgeo-org.atlassian.net/jira/secure/ReleaseNote.jspa?projectId=10000&version=18102

Thanks to Jody Garnett (GeoCat) and Andrea Aime (GeoSolutions) for publishing this release. A very special thanks to all those who contributed bug fixes, new
features, bug reports, and testing to this release.

--
The GeoServer Team
```

### OSGeo Announcement

For major releases OSGeo asks that a news item be submitted:

- Login to the osgeo.org website, create a news item using the release announcement text above.

And that an announcement is sent to discuss:

- Mail major release announcements to <discuss@osgeo.org> (you will need to [subscribe first](https://lists.osgeo.org/listinfo/discuss) ).
