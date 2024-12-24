#!/bin/bash

# This script processes a Teamengine XML result file to generate a report of failed tests.
# It checks if 'xmlstarlet' is installed; if not, it prompts the user to install it.
# If 'xmlstarlet' is available, the script extracts all <test-method> elements with 
# status="FAIL" and prints a formatted report to the console, including the test method name, 
# description, dependent groups, status, and any associated exception text.
# Leading and trailing whitespace is removed from the exception text.
# If no failed tests are found, it prints a message indicating this.

# Check if xmlstarlet is installed
if ! command -v xmlstarlet &> /dev/null; then
    echo "xmlstarlet is not available. Please install it to get a report of test failures."
    exit 1
fi

# File to process (assumed as argument)
if [ -z "$1" ]; then
    echo "Usage: $0 <file-name>"
    exit 1
fi

file="$1"
if [ ! -f "$file" ]; then
    echo "Error: File '$file' not found!"
    exit 1
fi

# Count the number of passed and failed test-method elements
num_passed=$(grep "endtest result=\"1\"" "$file" | wc -l)
num_failed=$(xmlstarlet sel -t -c "count(//endtest[@result='6'])"  "$file")

if [ "$num_failed" -eq 0 ] && [ "$num_passed" -gt 0 ]; then
    echo "No failed tests found in $file, and $num_passed tests succeeded."
    exit 0
fi

# Use xmlstarlet to find and print failed test methods
echo "Report of Failed Test Methods:"
echo "------------------------------"

xmlstarlet sel -t \
  -m "//endtest[@result='6']" \
  -v "preceding-sibling::starttest[1]/assertion" -n \
  -m "preceding-sibling::message" \
  -o "  " -v "." -n \
  "$file"

# Exit with an error code if there are failed tests
exit 1