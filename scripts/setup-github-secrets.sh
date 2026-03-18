#!/bin/bash

# GitHub Secrets Setup Script for Contacts+
# This script configures GitHub Actions secrets for app signing

set -e

# Configuration
KEYSTORE_BASE64=$(base64 -i keystore.jks | tr -d '\n')
KEYSTORE_PASSWORD="4adx9yd8acjw2PcRD1gCZREN9rl0Hqkw"
KEY_PASSWORD="0g6FhVb0IYkJ3DIhw3Qg5SbjBQqAXJ7p"
KEY_ALIAS="contactsplus"

echo "🔐 Setting up GitHub Secrets for Contacts+"
echo "=========================================="
echo ""

# Check if gh CLI is available
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) not found."
    echo ""
    echo "Please install it: https://cli.github.com/"
    echo "Or manually add these secrets to GitHub:"
    echo ""
    echo "URL: https://github.com/YOUR_USERNAME/ContactsPlus/settings/secrets/actions"
    echo ""
    echo "Secrets to add:"
    echo "  1. KEYSTORE_BASE64"
    echo "     Value: $KEYSTORE_BASE64"
    echo ""
    echo "  2. KEYSTORE_PASSWORD"
    echo "     Value: $KEYSTORE_PASSWORD"
    echo ""
    echo "  3. KEY_PASSWORD"
    echo "     Value: $KEY_PASSWORD"
    echo ""
    echo "  4. KEY_ALIAS"
    echo "     Value: $KEY_ALIAS"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "🔑 Authenticating with GitHub..."
    gh auth login
fi

# Get repository name
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner' 2>/dev/null || echo "")

if [ -z "$REPO" ]; then
    echo "❌ Not in a GitHub repository or repository not found."
    exit 1
fi

echo "✅ Found repository: $REPO"
echo ""

# Set secrets
echo "📝 Setting GitHub Secrets..."
echo ""

echo "  ✓ Setting KEYSTORE_BASE64..."
gh secret set KEYSTORE_BASE64 --body "$KEYSTORE_BASE64" --repo "$REPO"

echo "  ✓ Setting KEYSTORE_PASSWORD..."
gh secret set KEYSTORE_PASSWORD --body "$KEYSTORE_PASSWORD" --repo "$REPO"

echo "  ✓ Setting KEY_PASSWORD..."
gh secret set KEY_PASSWORD --body "$KEY_PASSWORD" --repo "$REPO"

echo "  ✓ Setting KEY_ALIAS..."
gh secret set KEY_ALIAS --body "$KEY_ALIAS" --repo "$REPO"

echo ""
echo "✅ All secrets configured successfully!"
echo ""
echo "📱 Next steps:"
echo "   1. Commit and push all changes to GitHub"
echo "   2. The next release build will be signed automatically"
echo "   3. Users can update without reinstalling"
echo ""
echo "🔒 Backup your keystore.jks file - it's critical for app updates!"
