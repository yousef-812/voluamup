# 📞 VolumeUp Dialer - تطبيق اتصالات أندرويد مضخم للصوت 200%

تطبيق مكالمات للهواتف الأندرويد مبني بلغة **Kotlin**، يهدف أساساً لمضاعفة صوت سماعة الأذن (Earpiece) أثناء المكالمات الهاتفية حتى **200% (+6dB)** باستخدام تقنيات المعالجة الرقمية للصوت (Audio DSP & LoudnessEnhancer)، مع توفير واجهة متكاملة للاتصال وحفظ جهات الاتصال والبريد وسجل المكالمات.

---

## 🚀 المميزات الرئيسية (Features)

- 🎧 **مضخم صوت سماعة الأذن (200% Digital Gain):** رفع وتوضيح نبرة الصوت في سماعة الأذن أثناء المكالمات الهاتفية مع شريط منزلق ديناميكي للتحكم بالدقة.
- 📱 **واجهة مكالمات جارية مخصصة (Custom InCallService):** إدارة كاملة للمكالمات الصادرة والواردة، كتم الصوت، والتبديل بين سماعة الأذن ومكبر الصوت (Speaker).
- 👥 **جهات الاتصال والبريد (Contacts & Email Sync):** مزامنة جلب الأرقام والإيميلات المسجلة من الجهاز وحسابات جوجل المربوطة.
- 📞 **سجل المكالمات (Call Log History):** عرض تفاصيل المكالمات الواردة، الصادرة، والمفقودة.
- 🔢 **لوحة طلب أرقام (Numeric Keypad Dialer):** واجهة اتصال سريعة تدعم البحث المباشر وإجراء المكالمات.
- ⚙️ **دعم البناء الآلي (GitHub Actions CI/CD):** بناء وتصدير ملف الـ APK تلقائياً عند رفع الكود على GitHub.

---

## 🛠️ التقنيات المستخدمة (Tech Stack)

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0+) | **Target SDK:** 34 (Android 14)
- **Frameworks & APIs:**
  - Android Telecom Framework (`InCallService`, `Call.Callback`)
  - Android Audio Effects (`LoudnessEnhancer`, `AudioManager`)
  - Android Providers (`ContactsContract`, `CallLog`)
  - Material Design 3 & ViewBinding
  - GitHub Actions CI/CD Workflow

---

## 💻 كيفية التشغيل والبناء (Building the Project)

```bash
# استنساخ المستودع
git clone https://github.com/YOUR_USERNAME/volumeup-dialer.git
cd volumeup-dialer

# البناء باستخدام Gradle
./gradlew assembleDebug
```

تجد ملف الـ APK الناتج في:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 الترخيص (License)
MIT License - مفتوح المصدر.
