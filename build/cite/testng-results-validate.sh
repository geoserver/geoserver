#!/bin/bash

# This script reads a testNG results XML file with a root element like:
# <testng-results skipped="96" failed="17" total="2228" passed="2115">
# It extracts the number of passed, failed, and skipped tests, and prints
# the results with ANSI colors: green for passed, red for failed, and no color for skipped.
# If there are any failed tests (failed > 0), the script exits with a status code of 1.
#
# This script is used in the Makefile to fail the test run if there are failures, since
# when using the teamengine REST API to produce the xml report, it will always return a 200 HTTP status code.
#
# Contrary to testng-results-report.sh, this script does not require xmlstarlet to be installed,
# and can be used to fail the build if there are test failures rergardless of whether a detailed
# report can be printed out.


# Check if a filename is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <file-name>"
    exit 1
fi

# Read the XML file
file="$1"
if [ ! -f "$file" ]; then
    echo "Error: File '$file' not found!"
    exit 1
fi

# Extract the values using sed
skipped=$(sed -n 's/.*skipped="\([0-9]*\)".*/\1/p' "$file")
failed=$(sed -n 's/.*failed="\([0-9]*\)".*/\1/p' "$file")
passed=$(sed -n 's/.*passed="\([0-9]*\)".*/\1/p' "$file")

# ANSI escape sequences for colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Print the results
echo -e "Passed: ${GREEN}${passed}${NC}"
echo -e "Failed: ${RED}${failed}${NC}"
echo -e "Skipped: ${skipped}"

# Exit with status 1 if there are any failed tests
if [ "$failed" -gt 0 ]; then
    exit 1
fi
