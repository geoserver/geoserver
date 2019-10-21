.. _source:

Source Code
===========

The GeoServer source code is located on GitHub at https://github.com/geoserver/geoserver.

To clone the repository::

  % git clone git://github.com/geoserver/geoserver.git geoserver
  
To list available branches in the repository::

  % git branch
     2.15.x
     2.16.x
   * master

To switch to the 2.16.x branch above::

  % git checkout 2.16.x
  
Git
---

Git is a distributed version control system with a steep learning curve.
Luckily there is lots of great documentation around. Before continuing developers should take the 
time to educate themselves about git. The following are good references:

* `The Git Book <http://git-scm.com/book/>`__
* `A nice introduction <http://www.sbf5.com/~cduan/technical/git/>`__
* `Git Pull Requests <https://help.github.com/en/articles/about-pull-requests>`__

.. _gitconfig:

Git client configuration
------------------------

To review global settings:

.. code-block:: bash

   $ git config --global --get-regexp core.*

On Linux and Windows machines:

::

   core.autocrlf input
   core.safecrlf true
   
On macOS using decomposed unicode paths, and a default APFS case-insensitive file system:

::

   core.autocrlf input
   core.safecrlf true
   core.ignorecase false
   core.precomposeunicode true

We recommend making these changes to ``--gloabl`` (or ``--system``) as they reflect the operating system and file system on your local machine.

Some useful reading on this subject:

* `git config <https://git-scm.com/docs/git-config>`__ (git)

Line endings
^^^^^^^^^^^^

When a repository is shared across different platforms it is necessary to have a 
strategy in place for dealing with file line endings. In general git is pretty good about
dealing this without explicit configuration but to be safe developers should set the 
``core.autocrlf`` setting to "input":

.. code-block:: bash

   $ git config --global core.autocrlf input

The value "input" respects the line ending form as present in the git repository.

.. note::

   It is also a good idea, especially for Windows users, to set the ``core.safecrlf`` 
   option to "true":

   .. code-block:: bash
   
      $ git config --global core.safecrlf true

   This will prevent commits that may potentially modify file line endings.

Some useful reading on this subject:

* `Configuring Git to handle line endings <https://help.github.com/articles/dealing-with-line-endings>`__ (GitHub)
* `What's the best CRLF (carriage return, line feed) handling strategy with Git? <http://stackoverflow.com/questions/170961/whats-the-best-crlf-handling-strategy-with-git>`__ (Stack Overflow)
* `Mind the end of the End of Your Line <https://adaptivepatchwork.com/2012/03/01/mind-the-end-of-your-line/>`__ (Tim Clem)

File paths
^^^^^^^^^^

For those working on non case-sensitive, please keep in mind that our repository is case-sensitive:

.. code-block:: bash
   
   $ git config --global core.ignorecase false

Take extra care when adding files to prevent problems for others. To correct a file added with the wrong case:

.. code-block:: bash
   
   $ git mv --cached HttpHandler.java HTTPHandler.java

.. note:: 
   
   File paths can use two different representations of select unicode characters:

   +-------------------------+---------------+--------------------------+
   | Representation          | Example       | Operating System Default |
   +=========================+===============+==========================+
   | Precomposed form        | ``Ü``         | Linux, Windows           |
   +-------------------------+---------------+--------------------------+
   | Decomposed form         | ``U`` + ``¨`` | macOS                    |
   +-------------------------+---------------+--------------------------+

   Files committed in decomposed form show up as untracked (even with no modification made).
   
   .. code-block:: bash
 
      $ git status
 
   ::

      Untracked files:
         (use "git add <file>..." to include in what will be committed)
    
         ...
    
         "Entit\303\251G\303\251n\303\251rique/"

   GeoServer requires macOS users to use the following setting:

   .. code-block:: bash

      $ git config --global core.precomposeunicode true
   
   This setting converts paths to precomposed form when adding files to the repository.
   
   To fix a file added in decomposed form it must be removed:
   
   .. code-block:: bash
   
      git config --global core.precomposeunicode false
      mv EntitéGénérique /tmp/EntitéGénérique
      git rm EntitéGénérique
      git commit -m "Remove EntitéGénérique with decomposed filename"
      
   And then added:
   
   .. code-block:: bash
     
      git config --global core.precomposeunicode true
      mv /tmp/EntitéGénérique EntitéGénérique
      git add EntitéGénérique
      git commit -m "Restore EntitéGénérique with precomposed filename"

Some useful reading on this subject:

* `Untracked filenames with unicode names <https://www.git-tower.com/help/mac/faq-and-tips/faq/unicode-filenames>`__

Committing
----------

In order to commit the following steps must be taken:

#. Configure your git client for cross platform projects. See :ref:`notes <gitconfig>` below.
#. Register for commit access as described :ref:`here <comitting>`.
#. Fork the canonical GeoServer repository into your github account.
#. Clone the forked repository to create a local repository 
#. Create a remote reference to the canonical repository using a non-read only URL (``git@github.com:geoserver/geoserver.git``).

.. note::

   The next section describes how the git repositories are distributed for the project and
   how to manage local repository remote references.
   

Repository distribution
-----------------------

Git is a distributed versioning system which means there is strictly no notion of a single 
central repository, but many distributed ones. For GeoServer these are:

* The **canonical** repository located on GitHub that serves as the official authoritative 
  copy of the source code for project
* Developers' **forked** repositories on GitHub. These repositories 
  generally contain everything in the canonical repository, as well any feature or
  topic branches a developer is working on and wishes to back up or share.
* Developers' **local** repositories on their own systems.  This is where development work is actually done.

Even though there are numerous copies of the repository they can all interoperate because
they share a common history. This is the magic of git!  

In order to interoperate with other repositories hosted on GitHub, 
a local repository must contain *remote references* to them. 
A local repository typically contains the following remote references:
  
* A remote called **origin** that points to the developers' forked GitHub repository.
* A remote called **upstream** that points to the canonical GitHub repository.
* Optionally, some remotes that point to other developers' forked repositories on GitHub. 

To set up a local repository in this manner:

#. Clone your fork of the canonical repository (where "bob" is replaced with your GitHub account name)::

     % git clone git@github.com:bob/geoserver.git geoserver
     % cd geoserver
   
#. Create the ``upstream`` remote pointing to the canonical repository::

     % git remote add upstream git@github.com:geoserver/geoserver.git
    
   Or if your account does not have push access to the canonical repository use the read-only url::
    
     % git remote add upstream git://github.com/geoserver/geoserver.git

#. Optionally, create remotes pointing to other developer's forks. These remotes are typically 
   read-only::
   
      % git remote add aaime git://github.com/aaime/geoserver.git
      % git remote add jdeolive git://github.com/jdeolive/geoserver.git


Repository structure
--------------------

A git repository contains a number of branches. These branches fall into three categories:

#. **Primary** branches that correspond to major versions of the software
#. **Release** branches that are used to manage releases of the primary branches
#. **Feature** or topic branches that developers do development on

Primary branches
^^^^^^^^^^^^^^^^

Primary branches are present in all repositories and correspond to the main release streams of the 
project. These branches consist of:

* The **master** branch that is the current unstable development version of the project
* The current **stable** branch that is the current stable development version of the project
* The branches for previous stable versions

For example at present these branches are:

* **master** - The 2.17.x release stream, where unstable development such as major new features take place
* **2.16.x** - The 2.16.x release stream, where stable development such as bug fixing and stable features take place
* **2.15.x** - The 2.15.x release stream, which is at end-of-life and has no active development

Release tags
^^^^^^^^^^^^

Release tags are used to mark releases from the stable or maintenance branches. These can be used to create a release branch if an emergency patch needs to be made:

* 2.15-M0
* 2.15-RC
* 2.15.0
* 2.15.1


Release tagas are only used during a versioned release of the software. At any given time a release branch
corresponds to the exact state of the last release from that branch. During release these branches are tagged.

Release branches are also present in all repositories.

Feature branches
^^^^^^^^^^^^^^^^

Feature branches are what developers use for day-to-day development. This can include small-scale bug fixes or 
major new features. Feature branches serve as a staging area for work that allows a developer to freely commit to
them without affecting the primary branches. For this reason feature branches generally only live
in a developer's local repository, and possibly their remote forked repository. Feature branches are never pushed
up into the canonical repository.

When a developer feels a particular feature is complete enough the feature branch is merged into a primary branch,
usually ``master``. If the work is suitable for the current stable branch the changeset can be ported back to the
stable branch as well. This is explained in greater detail in the :ref:`source_workflow` section.

Codebase structure
------------------

Each branch has the following structure::
  
     build/
     doc/
     src/
     data/
     

* ``build`` - release and continuous integration scripts
* ``doc`` - sources for the user and developer guides 
* ``src`` - java sources for GeoServer itself
* ``data`` - a variety of GeoServer data directories / configurations

.. _source_workflow:

Development workflow
--------------------

This section contains examples of workflows a developer will typically use on a daily basis. 
To follow these examples it is crucial to understand the phases that a changeset goes though in the git
workflow. The lifecycle of a single changeset is:

#. The change is made in a developer's local repository.
#. The change is **staged** for commit. 
#. The staged change is **committed**.
#. The committed changed is **pushed** up to a remote repository

There are many variations on this general workflow. 
For instance, it is common to make many local commits and then push them all up in batch to a remote repository.
Also, for brevity multiple local commits may be *squashed* into a single final commit.

Updating from canonical
^^^^^^^^^^^^^^^^^^^^^^^

Generally developers always work on a recent version of the official source code. The following example 
shows how to pull down the latest changes for the master branch from the canonical repository::

  % git checkout master
  % git pull upstream master
  
Similarly for the stable branch::

  % git checkout 2.2.x
  % git pull upstream 2.2.x

Making local changes
^^^^^^^^^^^^^^^^^^^^

As mentioned above, git has a two-phase workflow in which changes are first staged and then committed 
locally. For example, to change, stage and commit a single file::

  % git checkout master
  # do some work on file x
  % git add x
  % git commit -m "commit message" x
  
Again there are many 
variations but generally the staging process involves using ``git add`` to stage files that have been added 
or modified, and ``git rm`` to stage files that have been deleted. ``git mv`` is used to move files and
stage the changes in one step.

At any time you can run ``git status`` to check what files have been changed in the working area
and what has been staged for commit. It also shows the current branch, which is useful when 
switching frequently between branches.
  
Pushing changes to canonical
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once a developer has made some local commits they generally will want to push them up to a remote repository.
For the primary branches these commits should always be pushed up to the canonical repository. If they are for
some reason not suitable to be pushed to the canonical repository then the work should not be done on a primary
branch, but on a feature branch. 

For example, to push a local bug fix up to the canonical ``master`` branch::
  
  % git checkout master
  # make a change
  % git add/rm/mv ...
  % git commit -m "making change x"
  % git pull upstream master
  % git push upstream master
  
The example shows the practice of first pulling from canonical before pushing to it. Developers should **always** do 
this. In fact, if there are commits in canonical that have not been pulled down, by default git will not allow 
you to push the change until you have pulled those commits.

.. note:: 
   
   A **merge commit** may occur when one branch is merged with another. 
   A merge commit occurs when two branches are merged and the merge is not a "fast-forward" merge.
   This happens when the target branch has changed since the commits were created.
   Fast-forward merges are worth `reading about <http://git-scm.com/book/en/Git-Branching-Basic-Branching-and-Merging>`_. 
   
   An easy way to avoid merge commits is to do a "rebase" when pulling down changes::
   
     % git pull --rebase upstream master
     
   The rebase makes local changes appear in git history after the changes that are pulled down.
   This allows the following merge to be fast-forward. This is not a required practice since merge commits are fairly harmless, 
   but they should be avoided where possible since they clutter up the commit history and make the git log harder to read.
   
Working with feature branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As mentioned before, it is always a good idea to work on a feature branch rather than directly on a primary branch. 
A classic problem every developer who has used a version control system has run into is when they have 
worked on a feature locally and made a ton of changes, but then need to switch context to work on some other feature or 
bug fix. The developer tries to make the fix in the midst of the other changes 
and ends up committing a file that should not have been changed. 
Feature branches are the remedy for this problem.

To create a new feature branch off the master branch::

  % git checkout -b my_feature master
  % # make some changes
  % git add/rm, etc...
  % git commit -m "first part of my_feature"
  
Rinse, wash, repeat. The nice about thing about using a feature branch is that it is easy to switch context
to work on something else. Just ``git checkout`` whatever other branch you need to work on,
and then return to the feature branch when ready.

.. note:: 
   
   When a branch is checked out, all the files in the working area are modified to reflect
   the current state of the branch.  When using development tools which cache the state of the
   project (such as Eclipse) it may be necessary to refresh their state to match the file system.
   If the branch is very different it may even be necessary to perform a rebuild so that 
   build artifacts match the modified source code.


Merging feature branches
^^^^^^^^^^^^^^^^^^^^^^^^

Once a developer is done with a feature branch it must be merged into one of the primary branches and pushed up
to the canonical repository. The way to do this is with the ``git merge`` command::

  % git checkout master
  % git merge my_feature

It's as easy as that. After the feature branch has been merged into the primary branch push it up as described before::

  % git pull --rebase upstream master
  % git push upstream master
  

Porting changes between primary branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Often a single change (such as a bug fix) has to be committed to multiple branches. Unfortunately primary
branches **cannot** be merged with the ``git merge`` command. Instead we use ``git cherry-pick``.

As an example consider making a change to master::

  % git checkout master
  % # make the change
  % git add/rm/etc... 
  % git commit -m "fixing bug GEOS-XYZ"
  % git pull --rebase upstream master
  % git push upstream master
  
We want to backport the bug fix to the stable branch as well. To do so we have to note the commit
id of the change we just made on master. The ``git log`` command will provide this. Let's assume the commit
id is "123". Backporting to the stable branch then becomes::

  % git checkout 2.2.x
  % git cherry-pick 123
  % git pull --rebase upstream 2.2.x
  % git push upstream 2.2.x

Cleaning up feature branches
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Consider the following situation. A developer has been working on a feature branch and has gone back 
and forth to and from it making commits here and there. The result is that the feature branch has accumulated
a number of commits on it. But all the commits are related, and what we want is really just one commit.

This is easy with git and you have two options:

#. Do an **interactive rebase** on the feature branch
#. Do a **merge with squash**

Interactive rebase
~~~~~~~~~~~~~~~~~~

Rebasing allows us to rewrite the commits on a branch, deleting commits we don't want, or merging commits that should
really be done. You can read more about interactive rebasing `here <http://git-scm.com/book/en/Git-Tools-Rewriting-History#Changing-Multiple-Commit-Messages>`_. 

.. warning::

   Much care should be taken with rebasing. You should **never** rebase commits that are public (that is, commits that have 
   been copied outside your local repository). Rebasing public commits changes branch history and results in the inability to merge
   with other repositories.
   

The following example shows an interactive rebase on a feature branch::

  % git checkout my_feature
  % git log

The git log shows the current commit on the branch is commit "123". 
We make some changes and commit the result::

  % git commit "fixing bug x" # results in commit 456

We realize we forgot to stage a change before committing, so we add the file and commit::

  % git commit -m "oops, forgot to commit that file" # results in commit 678

Then we notice a small mistake, so we fix and commit again::

  % git commit -m "darn, made a typo" # results in commit #910

At this point we have three commits when what we really want is one. So we rebase, 
specifying the revision immediately prior to the first commit::

  % git rebase -i 123
  
This invokes an editor that allows indicating which commits should be combined.
Git then *squashes* the commits into an equivalent single commit. 
After this we can merge the cleaned-up feature branch into master as usual::

  % git checkout master
  % git merge my_feature

Again, be sure to read up on this feature before attempting to use it. And again, **never rebase a public commit**.

Merge with squash
~~~~~~~~~~~~~~~~~

The ``git merge`` command takes an option ``--squash`` that performs the merge 
against the working area but does not commit the result to the target branch. 
This squashes all the commits from the feature branch into a single changeset that
is staged and ready to be committed::

  % git checkout master
  % git merge --squash my_feature
  % git commit -m "implemented feature x"
  
  
More useful reading
-------------------

The content in this section is not intended to be a comprehensive introduction to git. There are many things not covered
that are invaluable to day-to-day work with git. Some more useful info:

* `10 useful git commands <http://webdeveloperplus.com/general/10-useful-advanced-git-commands/>`_
* `Git stashing <http://git-scm.com/book/en/Git-Tools-Stashing>`_
* `GeoTools git primer <http://docs.geotools.org/latest/developer/procedures/git.html>`_

  



