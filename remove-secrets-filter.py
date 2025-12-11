#!/usr/bin/env python3
"""
Git filter script to remove files containing GitHub tokens
"""

import sys

def callback(blob_file, metadata):
    """Filter callback to check for GitHub tokens in files"""
    # Files that might contain tokens
    sensitive_files = [
        'deploy-foundation-domain.sh',
        'deploy-foundation.ps1',
        'deploy-fresh.sh',
        'AUTOMATED_DEPLOYMENT.ps1',
        'DEPLOY_FOUNDATION.bat',
        'DEPLOY_NOW.ps1',
        'Simple-Deploy.ps1'
    ]

    # Get the filename from blob path
    filename = blob_file.name.decode('utf-8')

    # Check if this is a file we want to remove
    for sensitive in sensitive_files:
        if sensitive in filename:
            print(f"Removing sensitive file: {filename}")
            return None  # This tells git-filter-repo to remove the file

    # Return the original blob unchanged
    return blob_file

if __name__ == '__main__':
    # Import here to avoid import issues
    from git_filter_repo import BlobCallback

    # Register the callback
    blob_callback = BlobCallback(callback)