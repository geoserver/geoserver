#!/bin/bash

# This script processes a TestNG results XML file to print the full contents of
# <test-method> elements with status="FAIL". It checks if 'xmlstarlet' is installed;
# if not, it prompts the user to install it.
# If 'bat' is available, the output is piped to 'bat' with XML syntax highlighting.
# If no failed tests are found, it prints a message indicating this.

# Check if xmlstarlet is installed
if ! command -v xmlstarlet &> /dev/null; then
    echo "xmlstarlet is not available. Please install it to process the XML file."
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

# Count the number of failed tests based on the header:
# some test method may FAIL and that will just disable an optional conformance class

num_failed=$(xmlstarlet sel -t -v "//testng-results/@failed" "$file")

if [ "$num_failed" -eq 0 ]; then
    echo "No failed tests found in $file"
    exit 0
fi

# Use xmlstarlet to find and print the full contents of failed test-method elements
echo "Full Contents of Failed Test Methods (some might just be disabling optional conformance classes):"
echo "-------------------------------------------------------------------------------------------------"

output=$(xmlstarlet sel -t \
    -m "//test-method[@status='FAIL']" \
    -c . -n \
    "$file")

# Check if 'batcat' or 'bat' is installed
# On linux it's now batcat, bat would install something different
# On macos brew install bat would do
if command -v batcat &> /dev/null; then
    echo "$output" | batcat --language=xml --theme=Coldark-Dark #--paging=never
elif command -v bat &> /dev/null; then
    echo "$output" | bat --language=xml --theme=Coldark-Dark #--paging=never
else
    echo "$output"
fi