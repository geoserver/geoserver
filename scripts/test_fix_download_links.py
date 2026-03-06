#!/usr/bin/env python3
"""
Unit Tests for Download Link Fixer

Tests the fix_download_links.py script to ensure it correctly:
1. Detects buggy download links
2. Reconstructs link text with full filenames
3. Preserves non-buggy links unchanged
4. Handles edge cases
"""

import pytest
from fix_download_links import DownloadLinkFixer


class TestDownloadLinkFixer:
    """Test suite for DownloadLinkFixer class."""
    
    def setup_method(self):
        """Set up test fixtures."""
        self.fixer = DownloadLinkFixer(dry_run=True)
    
    # Test: is_download_link
    
    def test_is_download_link_with_release_path(self):
        """Test detection of release download URLs."""
        url = "https://sourceforge.net/projects/geoserver/files/GeoServer/3.0.0/release/geoserver-3.0.0-mbstyle-plugin.zip"
        assert self.fixer.is_download_link(url) is True
    
    def test_is_download_link_with_nightly_path(self):
        """Test detection of nightly download URLs."""
        url = "https://build.geoserver.org/geoserver/main/nightly/geoserver-main-SNAPSHOT-css-plugin.zip"
        assert self.fixer.is_download_link(url) is True
    
    def test_is_download_link_with_community_path(self):
        """Test detection of community module URLs."""
        url = "https://sourceforge.net/projects/geoserver/files/GeoServer/3.0.0/community/geoserver-3.0.0-wps-download-plugin.zip"
        assert self.fixer.is_download_link(url) is True
    
    def test_is_not_download_link_with_regular_url(self):
        """Test that regular URLs are not detected as download links."""
        url = "https://docs.geoserver.org/latest/en/user/configuration.html"
        assert self.fixer.is_download_link(url) is False
    
    # Test: is_buggy_link
    
    def test_is_buggy_link_missing_prefix_and_extension(self):
        """Test detection of buggy links missing both prefix and extension."""
        assert self.fixer.is_buggy_link("mbstyle") is True
        assert self.fixer.is_buggy_link("css") is True
        assert self.fixer.is_buggy_link("wps-download") is True
    
    def test_is_not_buggy_link_with_prefix(self):
        """Test that links with geoserver- prefix are not buggy."""
        assert self.fixer.is_buggy_link("geoserver-3.0.0-mbstyle-plugin.zip") is False
    
    def test_is_not_buggy_link_with_extension(self):
        """Test that links with file extensions are not buggy."""
        assert self.fixer.is_buggy_link("mbstyle-plugin.zip") is False
        assert self.fixer.is_buggy_link("geoserver.war") is False
    
    # Test: classify_link_type
    
    def test_classify_link_type_war(self):
        """Test classification of WAR binary links."""
        assert self.fixer.classify_link_type("war", "https://example.com/geoserver.war") == "war"
        assert self.fixer.classify_link_type("WAR", "https://example.com/release/") == "war"
    
    def test_classify_link_type_bin(self):
        """Test classification of bin binary links."""
        assert self.fixer.classify_link_type("bin", "https://example.com/geoserver-bin.zip") == "bin"
        assert self.fixer.classify_link_type("binary", "https://example.com/geoserver-bin.zip") == "bin"
    
    def test_classify_link_type_community(self):
        """Test classification of community module links."""
        assert self.fixer.classify_link_type("wps-download", "https://example.com/community/plugin.zip") == "community"
    
    def test_classify_link_type_extension(self):
        """Test classification of extension links."""
        assert self.fixer.classify_link_type("mbstyle", "https://example.com/release/plugin.zip") == "extension"
        assert self.fixer.classify_link_type("css", "https://example.com/nightly/plugin.zip") == "extension"
    
    # Test: reconstruct_link_text
    
    def test_reconstruct_link_text_extension(self):
        """Test reconstruction of extension link text."""
        result = self.fixer.reconstruct_link_text("release", "mbstyle", "https://example.com/release/plugin.zip")
        assert result == "geoserver-{{ release }}-mbstyle-plugin.zip"
    
    def test_reconstruct_link_text_community(self):
        """Test reconstruction of community module link text."""
        result = self.fixer.reconstruct_link_text("release", "wps-download", "https://example.com/community/plugin.zip")
        assert result == "geoserver-{{ release }}-wps-download-plugin.zip"
    
    def test_reconstruct_link_text_war(self):
        """Test reconstruction of WAR binary link text."""
        result = self.fixer.reconstruct_link_text("release", "war", "https://example.com/geoserver.war")
        assert result == "geoserver-{{ release }}.war"
    
    def test_reconstruct_link_text_bin(self):
        """Test reconstruction of bin binary link text."""
        result = self.fixer.reconstruct_link_text("release", "bin", "https://example.com/geoserver-bin.zip")
        assert result == "geoserver-{{ release }}-bin.zip"
    
    def test_reconstruct_link_text_with_snapshot_macro(self):
        """Test reconstruction with snapshot macro variable."""
        result = self.fixer.reconstruct_link_text("snapshot", "css", "https://example.com/nightly/plugin.zip")
        assert result == "geoserver-{{ snapshot }}-css-plugin.zip"
    
    def test_reconstruct_link_text_with_version_macro(self):
        """Test reconstruction with version macro variable."""
        result = self.fixer.reconstruct_link_text("version", "importer", "https://example.com/release/plugin.zip")
        assert result == "geoserver-{{ version }}-importer-plugin.zip"
    
    # Test: fix_line
    
    def test_fix_line_with_buggy_extension_link(self):
        """Test fixing a line with buggy extension link."""
        line = "Download {{ release }} [mbstyle](https://sourceforge.net/release/plugin.zip) here.\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 1
        assert "geoserver-{{ release }}-mbstyle-plugin.zip" in fixed_line
        assert "https://sourceforge.net/release/plugin.zip" in fixed_line
    
    def test_fix_line_with_buggy_war_link(self):
        """Test fixing a line with buggy WAR link."""
        line = "Download {{ release }} [war](https://sourceforge.net/release/geoserver.war) here.\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 1
        assert "geoserver-{{ release }}.war" in fixed_line
    
    def test_fix_line_with_multiple_buggy_links(self):
        """Test fixing a line with multiple buggy links."""
        line = "Get {{ release }} [mbstyle](https://example.com/release/a.zip) or {{ release }} [css](https://example.com/release/b.zip)\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 2
        assert "geoserver-{{ release }}-mbstyle-plugin.zip" in fixed_line
        assert "geoserver-{{ release }}-css-plugin.zip" in fixed_line
    
    def test_fix_line_preserves_non_buggy_links(self):
        """Test that non-buggy links are preserved unchanged."""
        line = "See [configuration](../config.md) for details.\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 0
        assert fixed_line == line
    
    def test_fix_line_preserves_already_correct_download_links(self):
        """Test that already-correct download links are preserved."""
        line = "Download {{ release }} [geoserver-{{ release }}-mbstyle-plugin.zip](https://example.com/release/plugin.zip)\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 0
        assert fixed_line == line
    
    def test_fix_line_preserves_external_links(self):
        """Test that external reference links are preserved."""
        line = "See [OGC WMS](https://www.ogc.org/standards/wms) specification.\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 0
        assert fixed_line == line
    
    # Test: Edge cases
    
    def test_fix_line_with_special_characters_in_plugin_name(self):
        """Test handling of plugin names with special characters."""
        line = "Download {{ release }} [wps-download](https://example.com/release/plugin.zip)\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 1
        assert "geoserver-{{ release }}-wps-download-plugin.zip" in fixed_line
    
    def test_fix_line_with_whitespace_variations(self):
        """Test handling of various whitespace patterns in macros."""
        line = "Download {{release}} [mbstyle](https://example.com/release/plugin.zip)\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 1
        assert "geoserver-{{ release }}-mbstyle-plugin.zip" in fixed_line
    
    def test_fix_line_preserves_url_unchanged(self):
        """Test that URLs are never modified, only link text."""
        original_url = "https://sourceforge.net/projects/geoserver/files/GeoServer/3.0.0/release/geoserver-3.0.0-mbstyle-plugin.zip"
        line = f"Download {{{{ release }}}} [mbstyle]({original_url})\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 1
        assert original_url in fixed_line
    
    def test_fix_line_with_no_links(self):
        """Test that lines without links are unchanged."""
        line = "This is a regular paragraph with no links.\n"
        fixed_line, count = self.fixer.fix_line(line)
        
        assert count == 0
        assert fixed_line == line


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
