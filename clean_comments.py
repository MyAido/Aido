#!/usr/bin/env python3
"""
Script to remove Hinglish and unnecessary comments from Kotlin files
This will make the code look more professional and less AI-generated
"""

import os
import re
from pathlib import Path

# Hinglish words to detect
HINGLISH_PATTERNS = [
    r'\bkarne\b', r'\bkarta\b', r'\bkarti\b', r'\bkarte\b',
    r'\bhai\b', r'\bhain\b', r'\bho\b',
    r'\bke liye\b', r'\bko\b', r'\bmein\b', r'\bse\b',
    r'\baur\b', r'\bya\b', r'\bki\b', r'\bka\b',
    r'\bAbhi\b', r'\bYeh\b', r'\bVah\b',
    r'\bdekhne\b', r'\bbanata\b', r'\bbanati\b',
    r'\bhandle\b.*\bkarta\b', r'\bhandle\b.*\bkarti\b'
]

# TODO patterns to remove
TODO_PATTERNS = [
    r'//\s*TODO:.*$',
    r'/\*\*?\s*TODO:.*?\*/',
]

def is_hinglish_comment(line):
    """Check if a line contains Hinglish"""
    for pattern in HINGLISH_PATTERNS:
        if re.search(pattern, line, re.IGNORECASE):
            return True
    return False

def is_todo_comment(line):
    """Check if a line is a TODO comment"""
    for pattern in TODO_PATTERNS:
        if re.search(pattern, line, re.IGNORECASE):
            return True
    return False

def should_remove_line(line):
    """Determine if a line should be removed"""
    stripped = line.strip()
    
    # Remove empty comment lines
    if stripped in ['*', '/**', '*/']:
        return False
    
    # Remove Hinglish comments
    if is_hinglish_comment(line):
        return True
    
    # Remove TODO comments
    if is_todo_comment(line):
        return True
    
    # Remove overly generic comments
    generic_comments = [
        'Main entry point',
        'Main composable',
        'Main screen',
        'Handle',
        'Process',
        'Manage',
    ]
    
    for generic in generic_comments:
        if stripped.startswith('*') and generic in stripped and len(stripped) < 50:
            return True
    
    return False

def clean_kotlin_file(file_path):
    """Clean a single Kotlin file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        cleaned_lines = []
        in_comment_block = False
        comment_block = []
        skip_next_blank = False
        
        for i, line in enumerate(lines):
            stripped = line.strip()
            
            # Track comment blocks
            if stripped.startswith('/**'):
                in_comment_block = True
                comment_block = [line]
                continue
            elif in_comment_block:
                comment_block.append(line)
                if stripped.endswith('*/'):
                    in_comment_block = False
                    # Check if entire comment block should be removed
                    block_text = ''.join(comment_block)
                    if not any(is_hinglish_comment(l) for l in comment_block):
                        # Keep the block if it's not Hinglish
                        cleaned_lines.extend(comment_block)
                    else:
                        skip_next_blank = True
                    comment_block = []
                continue
            
            # Remove single-line comments
            if stripped.startswith('//'):
                if should_remove_line(line):
                    skip_next_blank = True
                    continue
                else:
                    cleaned_lines.append(line)
                    continue
            
            # Skip extra blank lines after removed comments
            if skip_next_blank and not stripped:
                skip_next_blank = False
                continue
            
            skip_next_blank = False
            cleaned_lines.append(line)
        
        # Remove duplicate blank lines
        final_lines = []
        prev_blank = False
        for line in cleaned_lines:
            is_blank = not line.strip()
            if is_blank and prev_blank:
                continue
            final_lines.append(line)
            prev_blank = is_blank
        
        # Write back
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(final_lines)
        
        return True
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def remove_duplicate_imports(file_path):
    """Remove duplicate import statements"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        seen_imports = set()
        cleaned_lines = []
        
        for line in lines:
            if line.strip().startswith('import '):
                if line in seen_imports:
                    continue
                seen_imports.add(line)
            cleaned_lines.append(line)
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(cleaned_lines)
        
        return True
    except Exception as e:
        print(f"Error removing duplicates from {file_path}: {e}")
        return False

def process_directory(directory):
    """Process all Kotlin files in directory"""
    kotlin_files = list(Path(directory).rglob('*.kt'))
    
    print(f"Found {len(kotlin_files)} Kotlin files")
    print("Processing...")
    
    success_count = 0
    for file_path in kotlin_files:
        print(f"  Cleaning: {file_path.name}")
        if clean_kotlin_file(file_path):
            remove_duplicate_imports(file_path)
            success_count += 1
    
    print(f"\n✅ Successfully cleaned {success_count}/{len(kotlin_files)} files")

if __name__ == "__main__":
    # Set the path to your source directory
    source_dir = r"C:\Users\admin\Pictures\aido\app\src\main\java\com\rr\aido"
    
    print("🧹 Kotlin Comment Cleaner")
    print("=" * 50)
    print(f"Target directory: {source_dir}")
    print("\nThis will:")
    print("  • Remove Hinglish comments")
    print("  • Remove TODO comments")
    print("  • Remove duplicate imports")
    print("  • Clean up unnecessary comments")
    print("\n⚠️  Make sure you have a backup!")
    
    response = input("\nProceed? (yes/no): ")
    
    if response.lower() in ['yes', 'y']:
        process_directory(source_dir)
        print("\n✨ Done! Your code is now cleaner and more professional.")
    else:
        print("Cancelled.")
