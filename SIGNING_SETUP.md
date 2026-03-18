# Contacts+ Signing Setup

## 🚀 Quick Start (Automated Keystore Generation)

Run this script to generate a new keystore and get GitHub Secrets configuration:

```bash
./scripts/generate-keystore.sh
```

This will:
1. Generate a new keystore with secure random passwords
2. Output the GitHub Secrets you need to configure
3. Create `keystore.properties` for local development

## 📋 GitHub Secrets Configuration

After running the script, add these secrets to your GitHub repository:

1. Go to: `https://github.com/YOUR_USERNAME/ContactsPlus/settings/secrets/actions`
2. Add these 4 secrets:

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | The long base64 string from script output |
| `KEYSTORE_PASSWORD` | Password from script output |
| `KEY_PASSWORD` | Password from script output |
| `KEY_ALIAS` | `contactsplus` |

## ✅ Verification

After setup, the workflows will:
- ✅ Sign all release builds with the same key
- ✅ Allow app updates without reinstalling
- ✅ Show only ONE app icon (Green launcher icon)

## 📁 Files Modified

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | Fixed duplicate launcher icons (only 1 LAUNCHER category now) |
| `release.yml` | Added keystore support from GitHub Secrets |
| `dev-build.yml` | Added optional signing for debug builds |
| `scripts/generate-keystore.sh` | Automated keystore generation |
| `docs/GITHUB_SECRETS_SETUP.md` | Detailed setup documentation |

## 🔒 Security

- Keystore is stored encrypted in GitHub Secrets
- Only decrypted during build time
- Build runners are ephemeral (no persistent storage)
- Passwords are auto-generated with 32 characters of entropy

## ⚠️ Important

**BACKUP YOUR KEYSTORE!** Losing it means all users must reinstall the app.

Store backups in:
- Password manager (as base64)
- Encrypted cloud storage
- Physical backup (USB in safe)

## 🛠️ Local Development

For signed local builds, create `keystore.properties`:

```properties
storePassword=your_password
keyPassword=your_key_password
keyAlias=contactsplus
storeFile=../keystore.jks
```

This file is in `.gitignore` - never commit it!

## 📖 Full Documentation

See `docs/GITHUB_SECRETS_SETUP.md` for detailed instructions.
