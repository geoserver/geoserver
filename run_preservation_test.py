#!/usr/bin/env python3
import sys
import traceback

try:
    print("Importing test module...")
    sys.stdout.flush()
    
    from test_preservation_property import test_preservation_baseline
    
    print("Running test...")
    sys.stdout.flush()
    
    test_preservation_baseline()
    
    print("Test completed successfully")
    sys.stdout.flush()
    
except Exception as e:
    print(f"ERROR: {e}")
    traceback.print_exc()
    sys.exit(1)
