# GitHub Secrets Setup for Contacts+

## Overview

To ensure the app can be updated without requiring users to reinstall, all releases must be signed with the **same keystore**. This document explains how to set up signing for GitHub Actions builds.

## Quick Setup (Automated)

1. **Run the keystore generation script:**
   ```bash
   chmod +x scripts/generate-keystore.sh
   ./scripts/generate-keystore.sh
   ```

2. **Copy the output secrets** to GitHub:
   - Go to: `https://github.com/YOUR_USERNAME/ContactsPlus/settings/secrets/actions`
   - Add these 4 secrets:
     - `KEYSTORE_BASE64` (the long base64 string)
     - `KEYSTORE_PASSWORD`
     - `KEY_PASSWORD`
     - `KEY_ALIAS`

3. **Save the keystore file locally:**
   ```bash
   cp keystore.jks ~/secure-backup/
   ```

## Manual Setup

If you prefer to create your own keystore:

1. **Generate a keystore:**
   ```bash
   keytool -genkey -v \
     -keystore keystore.jks \
     -alias contactsplus \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000
   ```

2. **Encode to base64:**
   ```bash
   base64 -i keystore.jks | tr -d '\n' | pbcopy  # macOS
   # or
   base64 -w 0 keystore.jks | xclip -selection clipboard  # Linux
   ```

3. **Add secrets to GitHub** (same as above)

## Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `KEYSTORE_BASE64` | Base64-encoded keystore file | `UEsDBBQAAAgI...` (long string) |
| `KEYSTORE_PASSWORD` | Keystore password | `my_secure_password_123` |
| `KEY_PASSWORD` | Key password | `my_key_password_456` |
| `KEY_ALIAS` | Key alias name | `contactsplus` |

## Local Development

For local builds, create `keystore.properties` in the project root:

```properties
storePassword=your_password
keyPassword=your_key_password
keyAlias=contactsplus
storeFile=../keystore.jks
```

⚠️ **Never commit `keystore.properties` to git!** It's in `.gitignore`.

## Security Notes

- The keystore password is stored in GitHub Secrets (encrypted)
- The keystore is only decrypted during build time
- The build runner is ephemeral (no persistent storage)
- Consider using environment-specific keystores for dev/staging/production

## Troubleshooting

### Build fails with "Keystore was tampered with, or password was incorrect"
- Verify `KEYSTORE_PASSWORD` and `KEY_PASSWORD` secrets are correct
- Ensure `KEYSTORE_BASE64` is complete (no truncation)

### "Alias does not exist" error
- Check `KEY_ALIAS` secret matches the alias in your keystore
- List keystore contents: `keytool -list -keystore keystore.jks`

### App won't update over existing installation
- **This is critical!** The keystore must match the previous release
- If you lost the keystore, you must:
  1. Uninstall the old app
  2. Install the new version
  3. Users will lose data unless backed up

## Backup Your Keystore

**CRITICAL:** Store your keystore in multiple secure locations:
- Password manager (as base64)
- Encrypted cloud storage
- Physical backup (USB drive in safe)

Losing the keystore means **all users must reinstall the app**.
