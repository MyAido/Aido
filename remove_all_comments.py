#!/usr/bin/env python3
"""
Script to remove ALL multi-line comment blocks from Kotlin files
Keeps only simple single-line comments like // Demo Screen
"""

import os
import re
from pathlib import Path

def clean_kotlin_file(file_path):
    """Clean a single Kotlin file - remove all /** */ comment blocks"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Remove all /** ... */ comment blocks (multi-line)
        # This regex matches /** followed by anything (including newlines) until */
        content = re.sub(r'/\*\*.*?\*/', '', content, flags=re.DOTALL)
        
        # Remove all /* ... */ comment blocks (single-line style)
        content = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
        
        # Clean up multiple consecutive blank lines (more than 2)
        content = re.sub(r'\n{3,}', '\n\n', content)
        
        # Remove trailing whitespace from each line
        lines = content.split('\n')
        lines = [line.rstrip() for line in lines]
        content = '\n'.join(lines)
        
        # Write back
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
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
    
    print("🧹 Kotlin Comment Remover (All Multi-line Comments)")
    print("=" * 60)
    print(f"Target directory: {source_dir}")
    print("\nThis will:")
    print("  • Remove ALL /** */ comment blocks")
    print("  • Remove ALL /* */ comment blocks")
    print("  • Keep simple // comments")
    print("  • Remove duplicate imports")
    print("  • Clean up extra blank lines")
    print("\n⚠️  Make sure you have a backup!")
    
    response = input("\nProceed? (yes/no): ")
    
    if response.lower() in ['yes', 'y']:
        process_directory(source_dir)
        print("\n✨ Done! All multi-line comments removed.")
        print("💡 Simple // comments are preserved.")
    else:
        print("Cancelled.")
