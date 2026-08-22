# NetQuota Gateway

نظام مستقل لتحديد حصة إنترنت يومية لكل جهاز على الشبكة من غير تثبيت تطبيق على أجهزة المستخدمين.

يتكون المشروع من جزأين:

- `gateway-core`: المحرك الدائم داخل جهاز Linux/OpenWrt، ويحفظ الحصص والعدادات ويطبق القطع.
- `gateway-admin`: تطبيق Android عربي لإدارة الأجهزة والحصص، ويمكن بناؤه كـAPK بواسطة GitHub Actions.

## تنزيل APK من GitHub

1. افتح تبويب **Actions**.
2. اختر **NetQuota Gateway Admin APK**.
3. افتح آخر تشغيل ناجح بعلامة خضراء.
4. نزّل الملف من قسم **Artifacts** باسم `NetQuota-Gateway-Admin-APK-...`.
5. فك الضغط وثبت `NetQuota-Gateway-Admin.apk`.

## تجربة المحرك على الكمبيوتر

```powershell
cd gateway-core
$env:NETQUOTA_ADMIN_TOKEN = "اكتب-مفتاح-تجريبي"
npm test
npm start
```

بعد تشغيله، افتح إعدادات تطبيق Android واكتب عنوان الكمبيوتر على الشبكة مع المنفذ `8787`، ثم أدخل نفس مفتاح الإدارة.

الوضع الافتراضي محاكاة آمنة ولا يغير إعدادات الشبكة. راجع [المخطط الهندسي](NETQUOTA_GATEWAY_BLUEPRINT_AR.md) قبل تشغيل وضع `nftables` على جهاز Gateway حقيقي.
