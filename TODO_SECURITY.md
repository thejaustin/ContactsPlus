# TODO: Security Enhancements for Private Contacts

## Task: Re-enable Cross-App Private Contact Access
Currently, `MyContactsContentProvider` is set to `android:exported="false"` in `AndroidManifest.xml` to prevent malicious apps from scraping private contact data without permission.

### Goal
If a "Dialer+" or "SMS+" companion app is developed, they will need access to these private contacts to display names for incoming calls/messages.

### Implementation Plan
1. **Custom Permission**: Define a signature-level permission in the manifest:
   ```xml
   <permission 
       android:name="com.contactsplus.app.PERMISSION_READ_PRIVATE_CONTACTS" 
       android:protectionLevel="signature" />
   ```
2. **Secure Export**: Re-enable export but protect it with the new permission:
   ```xml
   <provider
       android:name=".contentproviders.MyContactsContentProvider"
       android:authorities="com.contactsplus.contactsprovider"
       android:exported="true"
       android:readPermission="com.contactsplus.app.PERMISSION_READ_PRIVATE_CONTACTS" />
   ```
3. **Companion Integration**: Grant the same permission to the Dialer+ app. Because it's a `signature` permission, Android will only grant it if both apps are signed with your same developer key.

---
*Created on: Sunday, February 8, 2026*
