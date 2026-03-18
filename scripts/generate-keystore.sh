#!/bin/bash

# Keystore Generation Script for Contacts+
# This script generates a signing key and outputs the configuration needed for GitHub Secrets

set -e

KEYSTORE_FILE="keystore.jks"
KEY_ALIAS="contactsplus"
KEYSTORE_PASSWORD=""
KEY_PASSWORD=""

echo "🔐 Contacts+ Keystore Generator"
echo "================================"
echo ""

# Generate random passwords
generate_password() {
    openssl rand -base64 32 | tr -dc 'a-zA-Z0-9' | head -c 32
}

KEYSTORE_PASSWORD=$(generate_password)
KEY_PASSWORD=$(generate_password)

echo "Generated passwords:"
echo "  Keystore Password: $KEYSTORE_PASSWORD"
echo "  Key Password: $KEY_PASSWORD"
echo ""

# Generate keystore
echo "Generating keystore..."
keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=ContactsPlus, OU=Development, O=ContactsPlus, L=Local, S=State, C=US"

echo "✅ Keystore generated successfully!"
echo ""

# Encode keystore to base64
KEYSTORE_BASE64=$(base64 -i "$KEYSTORE_FILE" | tr -d '\n')

echo "📋 GitHub Secrets Configuration"
echo "================================"
echo "Add these secrets to your GitHub repository:"
echo ""
echo "1. KEYSTORE_BASE64 (paste the entire block below):"
echo "---BEGIN KEYSTORE_BASE64---"
echo "$KEYSTORE_BASE64"
echo "---END KEYSTORE_BASE64---"
echo ""
echo "2. KEYSTORE_PASSWORD"
echo "   Value: $KEYSTORE_PASSWORD"
echo ""
echo "3. KEY_PASSWORD"
echo "   Value: $KEY_PASSWORD"
echo ""
echo "4. KEY_ALIAS"
echo "   Value: $KEY_ALIAS"
echo ""

# Create keystore.properties for local development
cat > keystore.properties << EOF
storePassword=$KEYSTORE_PASSWORD
keyPassword=$KEY_PASSWORD
keyAlias=$KEY_ALIAS
storeFile=$KEYSTORE_FILE
EOF

echo "✅ keystore.properties created for local development"
echo ""
echo "⚠️  IMPORTANT: Store these secrets safely!"
echo "   - The keystore is required for app updates"
echo "   - Losing the keystore means users must reinstall the app"
echo "   - Never commit keystore.properties to git"
echo ""
echo "📖 Setup Instructions:"
echo "   1. Go to https://github.com/YOUR_USERNAME/ContactsPlus/settings/secrets/actions"
echo "   2. Add each secret listed above"
echo "   3. The next release build will use these signing credentials"
