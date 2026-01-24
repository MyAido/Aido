# GitHub Release Signing Setup Script
# This script helps you set up signing for GitHub releases

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Aido GitHub Release Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check for existing keystore
Write-Host "Step 1: Checking for existing keystore..." -ForegroundColor Yellow
Write-Host ""

$keystorePath = Read-Host "Enter the FULL path to your existing Play Store keystore file (or press Enter to create new)"

if ([string]::IsNullOrWhiteSpace($keystorePath) -or -not (Test-Path $keystorePath)) {
    Write-Host ""
    Write-Host "WARNING: You should use your EXISTING Play Store keystore!" -ForegroundColor Red
    Write-Host "Using a different key will prevent users from updating between Play Store and GitHub releases." -ForegroundColor Red
    Write-Host ""
    
    $createNew = Read-Host "Do you want to create a NEW keystore anyway? (yes/no)"
    
    if ($createNew -eq "yes") {
        Write-Host ""
        Write-Host "Creating new keystore..." -ForegroundColor Yellow
        
        $alias = Read-Host "Enter key alias (e.g., aido)"
        $keystorePassword = Read-Host "Enter keystore password" -AsSecureString
        $keyPassword = Read-Host "Enter key password" -AsSecureString
        
        $keystorePath = Join-Path $PSScriptRoot "..\..\aido-release.keystore"
        
        # Convert secure strings to plain text for keytool
        $keystorePwd = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keystorePassword))
        $keyPwd = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPassword))
        
        Write-Host ""
        Write-Host "Generating keystore at: $keystorePath" -ForegroundColor Green
        
        keytool -genkey -v -keystore "$keystorePath" -alias "$alias" -keyalg RSA -keysize 2048 -validity 10000 -storepass "$keystorePwd" -keypass "$keyPwd"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Keystore created successfully!" -ForegroundColor Green
        } else {
            Write-Host "Failed to create keystore. Please install Java JDK." -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "Please locate your existing Play Store keystore and run this script again." -ForegroundColor Yellow
        exit 0
    }
} else {
    Write-Host "Found keystore: $keystorePath" -ForegroundColor Green
}

Write-Host ""
Write-Host "Step 2: Getting keystore details..." -ForegroundColor Yellow
$alias = Read-Host "Enter your key alias"
$keystorePassword = Read-Host "Enter keystore password"
$keyPassword = Read-Host "Enter key password"

Write-Host ""
Write-Host "Step 3: Encoding keystore to Base64..." -ForegroundColor Yellow

$encodedPath = Join-Path $env:TEMP "keystore.b64"
certutil -encode "$keystorePath" "$encodedPath" | Out-Null

# Remove header and footer from certutil output
$base64Content = Get-Content $encodedPath | Where-Object { $_ -notmatch "-----" } | Out-String
$base64Content = $base64Content.Trim()

Write-Host "Keystore encoded successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  GitHub Secrets Configuration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now add these secrets to your GitHub repository:" -ForegroundColor Yellow
Write-Host "Go to: Settings → Secrets and variables → Actions → New repository secret" -ForegroundColor White
Write-Host ""

Write-Host "1. Secret Name: " -NoNewline -ForegroundColor Cyan
Write-Host "SIGNING_KEY" -ForegroundColor White
Write-Host "   Value (copy this):" -ForegroundColor Cyan
Write-Host "   ----------------------------------------"
Write-Host $base64Content
Write-Host "   ----------------------------------------"
Write-Host ""

Write-Host "2. Secret Name: " -NoNewline -ForegroundColor Cyan
Write-Host "KEY_ALIAS" -ForegroundColor White
Write-Host "   Value: " -NoNewline -ForegroundColor Cyan
Write-Host $alias -ForegroundColor White
Write-Host ""

Write-Host "3. Secret Name: " -NoNewline -ForegroundColor Cyan
Write-Host "KEY_STORE_PASSWORD" -ForegroundColor White
Write-Host "   Value: " -NoNewline -ForegroundColor Cyan
Write-Host $keystorePassword -ForegroundColor White
Write-Host ""

Write-Host "4. Secret Name: " -NoNewline -ForegroundColor Cyan
Write-Host "KEY_PASSWORD" -ForegroundColor White
Write-Host "   Value: " -NoNewline -ForegroundColor Cyan
Write-Host $keyPassword -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Encoded keystore also saved to: $encodedPath" -ForegroundColor Green
Write-Host ""
Write-Host "IMPORTANT: Keep your keystore file SAFE and BACKED UP!" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Cyan

# Save to a file for easy copying
$secretsFile = Join-Path $PSScriptRoot "..\..\github-secrets.txt"
@"
GitHub Secrets for Aido Release Signing
========================================

1. SIGNING_KEY:
$base64Content

2. KEY_ALIAS: $alias

3. KEY_STORE_PASSWORD: $keystorePassword

4. KEY_PASSWORD: $keyPassword

========================================
Add these at: https://github.com/YOUR_USERNAME/aido/settings/secrets/actions
"@ | Out-File -FilePath $secretsFile -Encoding UTF8

Write-Host ""
Write-Host "All details also saved to: $secretsFile" -ForegroundColor Green
Write-Host "You can copy from there to GitHub." -ForegroundColor Green
Write-Host ""
Write-Host "Setup complete! [SUCCESS]" -ForegroundColor Green
