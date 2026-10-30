#!/usr/bin/env bash
# Copies this module to a GeoServer branch, applies the few edits that branch needs, and builds and tests it there.
# Run it before opening a pull request on main, to find a change that will not backport; run it with a checkout of the
# branch to make the backport itself.
#
# Usage: backport-checks.sh <branch> [<checkout>]
#   branch    3.0.x, 2.28.x or 2.27.x
#   checkout  a checkout of that branch: the module is copied into it and the changes are left there to commit.
#             Without it, a temporary worktree of the upstream branch is used and removed afterwards.
# Environment:
#   QA=1       also runs the QA checks (Error Prone, PMD, SpotBugs, Checkstyle), as the pull request builds do
#   OFFLINE=1  builds with -o, using only what is in the local Maven repository
#
# The edits, which are all a branch needs while the module follows CLAUDE.md:
#   all  the parent version in pom.xml
#   3.x  the user guide page and its screenshots, copied from this checkout
#   2.x  the test dependency on wicket-tester (Wicket 9 has the tester in wicket-core; 2.27.x needs junit-jupiter),
#        javax instead of jakarta for the servlet Filter in one test, no module category or contact in
#        applicationContext.xml, no PMD suppression on getOrder(), and the user guide page copied from
#        old-v2-docs/index.rst
set -euo pipefail

usage() {
    sed -n '6,12p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

[ $# -ge 1 ] && [ $# -le 2 ] || usage
branch=$1
case "$branch" in
    3.0.x | 2.28.x | 2.27.x) ;;
    *) usage ;;
esac

module_src=$(cd "$(dirname "$0")" && pwd)
repo=$(git -C "$module_src" rev-parse --show-toplevel)
module_path=${module_src#"$repo"/}

if [ $# -eq 2 ]; then
    target=$(cd "$2" && pwd)
else
    remote=$(git -C "$repo" remote -v | awk '$2 ~ /github\.com[:\/]geoserver\/geoserver(\.git)?$/ && $3 == "(fetch)" {print $1; exit}')
    [ -n "$remote" ] || { echo "No git remote points to geoserver/geoserver" >&2; exit 1; }
    git -C "$repo" fetch --quiet "$remote" "$branch"
    target=$(mktemp -d)
    trap 'git -C "$repo" worktree remove --force "$target"' EXIT
    git -C "$repo" worktree add --quiet --detach "$target" FETCH_HEAD
fi
module=$target/$module_path
if [ "$module" -ef "$module_src" ]; then
    echo "The target is the checkout this script is in; it would delete the module before copying it" >&2
    exit 1
fi

echo "Copying $module_path to $branch in $target"
rm -rf "$module/src" "$module/old-v2-docs"
mkdir -p "$module"
(cd "$module_src" && git ls-files --cached --others --exclude-standard . | while read -r file; do
    [ -e "$file" ] && mkdir -p "$module/$(dirname "$file")" && cp "$file" "$module/$file"
done)

version=$(sed -n '1,20p' "$target/src/community/pom.xml" | grep -m1 -o '<version>[^<]*</version>')
# the first version in the file is the parent's
perl -0pi -e "s{<version>[^<]*</version>}{$version}" "$module/pom.xml"

if [[ $branch == 3.* ]]; then
    docs_md=$target/doc/en/user/community/updates-checker
    mkdir -p "$docs_md"
    cp "$repo"/doc/en/user/community/updates-checker/* "$docs_md/"
fi

if [[ $branch == 2.* ]]; then
    if [ "$branch" = 2.27.x ]; then
        replacement='<groupId>org.junit.jupiter</groupId>\n      <artifactId>junit-jupiter</artifactId>'
        perl -0pi -e "s{<groupId>org.apache.wicket</groupId>\s*<artifactId>wicket-tester</artifactId>}{$replacement}" \
            "$module/pom.xml"
    else
        perl -0pi -e 's{\n\s*<dependency>\s*<groupId>org.apache.wicket</groupId>\s*<artifactId>wicket-tester</artifactId>.*?</dependency>}{}s' \
            "$module/pom.xml"
    fi
    sed -i '/backport-checks.sh removes these two lines/d; /<property name="category" value="COMMUNITY"\/>/d; /<property name="contact" /d' \
        "$module/src/main/resources/applicationContext.xml"
    grep -rl 'import jakarta.servlet.Filter;' "$module/src/test" | xargs -r sed -i 's/import jakarta.servlet.Filter;/import javax.servlet.Filter;/'
    # getOrder() overrides nothing on 2.x, where PMD reports the suppression as unnecessary
    sed -i 's/@SuppressWarnings({"MissingOverride", "PMD.MissingOverride"})/@SuppressWarnings("MissingOverride")/' \
        "$module/src/main/java/org/geoserver/updateschecker/web/HomePageContentProvider.java"

    docs_rst=$target/doc/en/user/source/community/updates-checker
    mkdir -p "$docs_rst"
    cp "$module_src/old-v2-docs/index.rst" "$docs_rst/index.rst"
    cp "$repo"/doc/en/user/community/updates-checker/*.png "$docs_rst/"
    grep -q 'updates-checker/index' "$target/doc/en/user/source/community/index.rst" ||
        echo "WARNING: doc/en/user/source/community/index.rst has no toctree entry for updates-checker/index" >&2
fi

grep -q '<module>updates-checker</module>' "$target/src/community/pom.xml" ||
    echo "WARNING: src/community/pom.xml does not list the module" >&2
grep -q 'gs-updates-checker' "$target/src/web/app/pom.xml" ||
    echo "WARNING: src/web/app/pom.xml has no updates-checker profile" >&2

args=(-nsu -Dmaven.gitcommitid.skip=true clean install)
[ "${QA:-0}" = 1 ] && args+=(-Dqa)
[ "${OFFLINE:-0}" = 1 ] && args+=(-o)
# 2.27.x has no .mvn/jvm.config, which on Java 16 or later Error Prone needs to reach the compiler internals
if [ "${QA:-0}" = 1 ] && [ ! -f "$target/.mvn/jvm.config" ]; then
    for package in api file main model parser processing tree util; do
        MAVEN_OPTS="${MAVEN_OPTS:-} --add-exports=jdk.compiler/com.sun.tools.javac.$package=ALL-UNNAMED"
    done
    export MAVEN_OPTS="$MAVEN_OPTS --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
fi

echo "Building on $branch: mvn ${args[*]}"
(cd "$module" && mvn "${args[@]}")
echo "$branch: build and tests passed"
