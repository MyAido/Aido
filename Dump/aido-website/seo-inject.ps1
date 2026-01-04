# SEO Injection Script for Remaining Pages
$favicon = '<link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>⚡</text></svg>">'

# Define pages and their metadata
$pages = @{
    "guides/index.html" = @{
        title = "User Guides - Aido AI Assistant"
        description = "Learn how to use Aido with our comprehensive guides. Setup instructions, troubleshooting, and tips."
        canonical = "https://myaido.web.app/guides/"
    }
    "guides/xiaomi-fix.html" = @{
        title = "Xiaomi Fix Guide - Aido"
        description = "Fix notification issues on Xiaomi devices. Step-by-step guide to enable Aido properly."
        canonical = "https://myaido.web.app/guides/xiaomi-fix.html"
    }
    "features/ai-writer.html" = @{
        title = "AI Writer Feature - Aido"
        description = "Transform your writing with Aido's AI Writer. Generate emails, essays, and creative content instantly."
        canonical = "https://myaido.web.app/features/ai-writer.html"
    }
    "features/customization.html" = @{
        title = "Customize Aido - Themes & Settings"
        description = "Personalize Aido with custom themes, animations, and settings."
        canonical = "https://myaido.web.app/features/customization.html"
    }
    "features/shortcuts.html" = @{
        title = "Smart Shortcuts - Aido"
        description = "Create text shortcuts for faster typing. Expand abbreviations instantly."
        canonical = "https://myaido.web.app/features/shortcuts.html"
    }
    "features/utility-commands.html" = @{
        title = "Utility Commands - Aido"
        description = "Use powerful utility commands for calculations, transformations, and more."
        canonical = "https://myaido.web.app/features/utility-commands.html"
    }
}

foreach ($page in $pages.Keys) {
    $filePath = "C:\Users\admin\Pictures\aido\docs\aido-website\$page"
    $meta = $pages[$page]
    
    if (Test-Path $filePath) {
        $content = Get-Content $filePath -Raw
        
        # Check if already has canonical (skip if already optimized)
        if ($content -notmatch 'rel="canonical"') {
            # Inject after <title> tag
            $seoBlock = @"
    <meta name="description" content="$($meta.description)">
    <link rel="canonical" href="$($meta.canonical)">
    $favicon
"@
            $content = $content -replace '(<title>[^<]+</title>)', "`$1`r`n$seoBlock"
            Set-Content $filePath $content -NoNewline
            Write-Host "✓ Optimized: $page"
        } else {
            Write-Host "⊘ Skipped (already optimized): $page"
        }
    }
}

Write-Host "`n✅ SEO injection complete!"
