# NetQuota Gateway Core

محرك محلي مستقل يحفظ الأجهزة والحصص في SQLite، ويقدم API لتطبيق NetQuota Admin، ويطبق القطع داخل جهاز الـGateway.

## تجربة آمنة على الكمبيوتر

يتطلب Node.js 22.5 أو أحدث:

```powershell
cd gateway-core
$env:NETQUOTA_ADMIN_TOKEN = "ضع-مفتاح-قوي-هنا"
npm start
```

ثم داخل تطبيق Android استخدم:

- العنوان: `IP-الكمبيوتر:8787`
- مفتاح الإدارة: نفس قيمة `NETQUOTA_ADMIN_TOKEN`

الوضع الافتراضي Simulator ويولد استهلاكًا تجريبيًا من غير تعديل الشبكة.

## الاختبارات

```powershell
npm test
```

## تشغيل Linux الفعلي

```bash
export NETQUOTA_MODE=nftables
export NETQUOTA_ADMIN_TOKEN='replace-with-a-long-random-secret'
export NETQUOTA_DATABASE=/var/lib/netquota/netquota.db
sudo --preserve-env=NETQUOTA_MODE,NETQUOTA_ADMIN_TOKEN,NETQUOTA_DATABASE node src/main.mjs
```

وضع `nftables` مخصص لجهاز Linux يمر من خلاله كل Traffic. لا تشغله على شبكة حقيقية قبل ضبط واجهات WAN/LAN وDHCP، واختبار الوصول الإداري من كابل منفصل. النسخة الحالية تنفذ مجموعة حظر IPv4؛ عدادات Linux الفعلية واكتشاف DHCP وIPv6 هي الخطوة التالية قبل الاعتماد الإنتاجي.
