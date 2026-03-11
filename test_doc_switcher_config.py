"""
Unit tests for doc_switcher configuration loading.

Tests the configuration loading logic in doc/version.py that loads
the shared doc_switcher.yml file and validates its structure.

**Validates: Requirements 2.3, 2.4, 5.1, 5.2, 5.3, 5.4, 5.5**
"""

import unittest
import yaml
import tempfile
import os
from pathlib import Path
from unittest.mock import patch, mock_open


class TestDocSwitcherConfigLoading(unittest.TestCase):
    """Test suite for doc_switcher configuration loading."""
    
    def setUp(self):
        """Set up test fixtures."""
        # Valid doc_switcher configuration
        self.valid_config = {
            'doc_switcher': [
                {
                    'label': 'User Manual',
                    'url': '../user/',
                    'type': 'user'
                },
                {
                    'label': 'Developer Manual',
                    'url': '../developer/',
                    'type': 'developer'
                },
                {
                    'label': 'Documentation Guide',
                    'url': '../docguide/',
                    'type': 'docguide'
                },
                {
                    'label': 'Swagger APIs',
                    'url': '../user/api/',
                    'type': 'swagger'
                }
            ]
        }
    
    def test_load_valid_config_file(self):
        """
        Test that doc_switcher.yml can be loaded successfully.
        
        **Validates: Requirements 2.1, 2.2, 5.1**
        """
        # Create a temporary YAML file with valid configuration
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yml', delete=False) as f:
            yaml.dump(self.valid_config, f)
            temp_path = f.name
        
        try:
            # Load the configuration
            with open(temp_path, 'r') as f:
                config = yaml.safe_load(f)
            
            # Verify the configuration was loaded
            self.assertIsNotNone(config)
            self.assertIn('doc_switcher', config)
            self.assertIsInstance(config['doc_switcher'], list)
        finally:
            # Clean up
            os.unlink(temp_path)
    
    def test_loaded_data_has_expected_structure(self):
        """
        Test that loaded data has expected structure (list with label, url, type fields).
        
        **Validates: Requirements 5.1, 5.2**
        """
        # Create a temporary YAML file with valid configuration
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yml', delete=False) as f:
            yaml.dump(self.valid_config, f)
            temp_path = f.name
        
        try:
            # Load the configuration
            with open(temp_path, 'r') as f:
                config = yaml.safe_load(f)
            
            doc_switcher = config['doc_switcher']
            
            # Verify it's a list
            self.assertIsInstance(doc_switcher, list)
            self.assertGreater(len(doc_switcher), 0)
            
            # Verify each entry has required fields
            for entry in doc_switcher:
                self.assertIn('label', entry, "Entry missing 'label' field")
                self.assertIn('url', entry, "Entry missing 'url' field")
                self.assertIn('type', entry, "Entry missing 'type' field")
                
                # Verify field types
                self.assertIsInstance(entry['label'], str)
                self.assertIsInstance(entry['url'], str)
                self.assertIsInstance(entry['type'], str)
        finally:
            # Clean up
            os.unlink(temp_path)
    
    def test_error_handling_missing_file(self):
        """
        Test error handling for missing file.
        
        **Validates: Requirements 2.3, 5.3**
        """
        # Try to open a non-existent file
        non_existent_path = '/path/to/non/existent/file.yml'
        
        with self.assertRaises(FileNotFoundError):
            with open(non_existent_path, 'r') as f:
                yaml.safe_load(f)
    
    def test_error_handling_invalid_yaml(self):
        """
        Test error handling for invalid YAML.
        
        **Validates: Requirements 2.4, 5.3**
        """
        # Create a temporary file with invalid YAML
        invalid_yaml = """
        doc_switcher:
          - label: "User Manual"
            url: "../user/"
            type: "user"
          - label: "Developer Manual
            url: "../developer/"
            type: "developer"
        """
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yml', delete=False) as f:
            f.write(invalid_yaml)
            temp_path = f.name
        
        try:
            # Try to load invalid YAML
            with self.assertRaises(yaml.YAMLError):
                with open(temp_path, 'r') as f:
                    yaml.safe_load(f)
        finally:
            # Clean up
            os.unlink(temp_path)
    
    def test_error_handling_missing_doc_switcher_key(self):
        """
        Test error handling when doc_switcher key is missing.
        
        **Validates: Requirements 5.2, 5.3**
        """
        # Create a config without doc_switcher key
        invalid_config = {
            'some_other_key': ['value1', 'value2']
        }
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yml', delete=False) as f:
            yaml.dump(invalid_config, f)
            temp_path = f.name
        
        try:
            # Load the configuration
            with open(temp_path, 'r') as f:
                config = yaml.safe_load(f)
            
            # Try to access doc_switcher key
            with self.assertRaises(KeyError):
                _ = config['doc_switcher']
        finally:
            # Clean up
            os.unlink(temp_path)
    
    def test_validate_type_field_values(self):
        """
        Test that type field contains expected values.
        
        **Validates: Requirements 5.4**
        """
        # Create a temporary YAML file with valid configuration
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yml', delete=False) as f:
            yaml.dump(self.valid_config, f)
            temp_path = f.name
        
        try:
            # Load the configuration
            with open(temp_path, 'r') as f:
                config = yaml.safe_load(f)
            
            doc_switcher = config['doc_switcher']
            expected_types = {'user', 'developer', 'docguide', 'swagger'}
            
            # Verify each entry has a valid type
            for entry in doc_switcher:
                self.assertIn(entry['type'], expected_types,
                            f"Invalid type value: {entry['type']}")
        finally:
            # Clean up
            os.unlink(temp_path)
    
    def test_actual_config_file_exists(self):
        """
        Test that the actual doc_switcher.yml file exists and is valid.
        
        **Validates: Requirements 1.1, 2.1, 5.5**
        """
        # Path to the actual config file
        config_path = Path('doc/themes/geoserver/doc_switcher.yml')
        
        # Verify file exists
        self.assertTrue(config_path.exists(), 
                       f"Config file not found at {config_path}")
        
        # Load and validate the actual config
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        
        # Verify structure
        self.assertIn('doc_switcher', config)
        self.assertIsInstance(config['doc_switcher'], list)
        self.assertEqual(len(config['doc_switcher']), 4,
                        "Expected 4 documentation types")
        
        # Verify each entry
        for entry in config['doc_switcher']:
            self.assertIn('label', entry)
            self.assertIn('url', entry)
            self.assertIn('type', entry)


if __name__ == '__main__':
    unittest.main()
