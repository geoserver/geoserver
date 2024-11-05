#!/bin/bash

# This script processes a TestNG results XML file to generate a report of failed tests.
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

# Count the number of failed test-method elements
num_failed=$(xmlstarlet sel -t -v "count(//test-method[@status='FAIL'])" "$file")

if [ "$num_failed" -eq 0 ]; then
    echo "No failed tests found in $file"
    exit 0
fi

# Use xmlstarlet to find and print failed test methods
echo "Report of Failed Test Methods:"
echo "------------------------------"

xmlstarlet sel -t \
    -m "//test-method[@status='FAIL']" \
    -v "concat('test-method: ', @name)" -n \
    -v "concat('description: ', @description)" -n \
    -v "concat('depends-on-groups: ', @depends-on-groups)" -n \
    -v "concat('status: ', @status)" -n \
    -o "exception: " \
    -m "exception" \
    -v "normalize-space(.)" -b -n \
    -o "Request URI: " \
    -m "attributes/attribute[@name='request']" \
    -v "normalize-space(substring-after(., 'Request URI:'))" -b -n \
    -n \
    "$file"