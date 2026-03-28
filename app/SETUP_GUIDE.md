# 🗡 IQAVE Android — دليل البناء الكامل

## المتطلبات
- Android Studio Hedgehog أو أحدث
- JDK 11+
- Android SDK (API 21-34)

---

## 📁 هيكل المشروع

```
IQAVE-Android/
├── android/
│   ├── app/
│   │   ├── build.gradle
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/iqave/app/
│   │       │   └── MainActivity.java
│   │       ├── res/
│   │       │   ├── drawable/   ← الأيقونات
│   │       │   ├── layout/     ← activity_main.xml
│   │       │   ├── values/     ← styles.xml
│   │       │   └── xml/        ← network_security_config.xml
│   │       └── assets/
│   │           └── www/        ← ← ← ضع هنا ملفاتك
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle.properties
└── www/
    ├── index.html              ← الصفحة الرئيسية
    ├── offline.html            ← صفحة بدون نت
    ├── samurai.gif             ← خلفية GIF
    └── music.mp3               ← الصوت
```

---

## 🚀 خطوات البناء

### 1. إعداد المشروع
```bash
# افتح Android Studio
# File → Open → اختر مجلد android/
```

### 2. نسخ ملفات الويب
```
انسخ المجلد www/ كاملاً إلى:
android/app/src/main/assets/www/
```

> **مهم:** إذا مجلد `assets` غير موجود، اصنعه يدوياً أو من Android Studio:
> Right-click على `main` → New → Directory → اكتب `assets/www`

### 3. تعديل محرك البحث (اختياري)
في `index.html` ابحث عن:
```js
location.href = q.startsWith('http') ? q : `https://www.google.com/search?q=${encodeURIComponent(q)}`;
```
غيّرها إلى Brave:
```js
location.href = q.startsWith('http') ? q : `https://search.brave.com/search?q=${encodeURIComponent(q)}`;
```

### 4. بناء APK Debug (للتجربة)
```
Build → Build Bundle(s)/APK(s) → Build APK(s)
```
الملف في: `app/build/outputs/apk/debug/app-debug.apk`

### 5. بناء APK Release (للنشر)
```
Build → Generate Signed Bundle/APK → APK
```
- اصنع Keystore جديد أو استخدم موجود
- اختر `release`
- ✅ V1 و V2 Signature

---

## 🔧 إعدادات مهمة

### تغيير اسم الحزمة
في `build.gradle`:
```groovy
applicationId "com.iqave.app"  // غيّر للاسم اللي تريده
```
وفي `AndroidManifest.xml`:
```xml
package="com.iqave.app"
```

### تغيير الأيقونة
ضع أيقونتك في `res/drawable/` بالأسماء:
- `ic_launcher.png` (512×512)
- `ic_launcher_round.png` (512×512)

أو استخدم: Android Studio → Right-click res → New → Image Asset

### JavaScript Bridge
من الـ HTML تقدر تستدعي Native Android:
```js
// التحقق هل التطبيق يعمل على أندرويد
if(window.AndroidBridge && AndroidBridge.isAndroid()){
    AndroidBridge.showToast('مرحباً من JS!');
}
```

---

## 📱 الصلاحيات المطلوبة فقط

| الصلاحية | السبب |
|----------|-------|
| INTERNET | البحث والطقس والاقتراحات |
| READ_MEDIA_* | رفع GIF/MP4/صوت من الهاتف |

**لا يوجد:** كاميرا / ميكروفون / موقع GPS تلقائي

---

## ⚡ تحسينات الأداء المفعّلة

- `Hardware Acceleration` → ✅ (Custom Cursor سلس)
- `DOM Storage` → ✅ (localStorage يعمل)
- `IndexedDB` → ✅ (ملفات 1GB+)
- `Media Autoplay` → ✅ (الصوت يشتغل تلقائياً)
- `File Chooser` → ✅ (رفع ملفات من المعرض)

---

## 🐛 مشاكل شائعة وحلولها

**المشكلة:** `INSTALL_FAILED_INSUFFICIENT_STORAGE`
**الحل:** حذف APK القديم من الجهاز أولاً

**المشكلة:** الصوت ما يشتغل
**الحل:** تأكد `setMediaPlaybackRequiresUserGesture(false)` موجود في MainActivity

**المشكلة:** ملفات GIF/MP4 كبيرة ما ترفع
**الحل:** IndexedDB محدود في بعض الأجهزة — استخدم الملفات من نفس مجلد assets

**المشكلة:** خطأ `net::ERR_CLEARTEXT_NOT_PERMITTED`
**الحل:** تأكد `network_security_config.xml` مضاف في Manifest

---

## 📦 تقليل حجم APK

```groovy
// في build.gradle
buildTypes {
    release {
        minifyEnabled true      // يحذف كود JS/Java غير المستخدم
        shrinkResources true    // يحذف صور/ملفات غير مستخدمة
    }
}
```

الحجم المتوقع: **3-8 MB** (بدون ملفات الوسائط)

---

*IQAVE v1.0 — مبني بـ ❤️*
