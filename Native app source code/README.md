# BD Admission Android — 1.1.0

এই প্যাকেজে আপনার দেওয়া অ্যাপের WebView ও হোমস্ক্রিন উইজেট আপডেট করা হয়েছে। Java lambda, method reference, AndroidX বা Java Stream ব্যবহার করা হয়নি। ওয়েবসাইটের নতুন তথ্য অ্যাপে অনলাইনে লোড হবে; অ্যাপটি অফলাইন ভর্তি ডেটাবেস নয়।

## AIDE-তে সহজভাবে চালানো

1. পুরো ZIP extract করুন; ZIP-এর ভেতর থেকে একটি Java ফাইল আলাদা করে খুলবেন না।
2. পুরোনো AIDE হলে `AIDE-Classic/AndroidManifest.xml` খুলুন। এই প্রজেক্টে `src/`, `res/`, `project.properties` আছে; Gradle দরকার হয় না।
3. AIDE SDK manager-এ Android API 28 ইনস্টল থাকতে হবে। তারপর Run/Build APK চাপুন। অ্যাপের minimum Android API 24 (Android 7)।
4. আপনার AIDE যদি আধুনিক Gradle project সমর্থন করে, মূল ফোল্ডারের `app/build.gradle` খুলতে পারেন। এতে আপনার আগের প্রজেক্টের AGP 7.0.2, compile SDK 34, build tools 34.0.0 রাখা হয়েছে; AGP 7.0.2-এর জন্য Java 11 ও compatible Gradle 7 লাগে। সব AIDE সংস্করণ একই build engine দেয় না।
5. বিল্ড শেষে AIDE-এর Install prompt ব্যবহার করুন। আগের অ্যাপের সঙ্গে signing key না মিললে update install হবে না; আগের signing key ব্যবহার করুন। পুরোনো অ্যাপ মুছে দিলে তার পছন্দ ও উইজেট মুছে যাবে, তাই আগে প্রয়োজনীয় তথ্য রেখে নিন।

## দুটি build profile

| Profile | Source | SDK target | Intended use |
|---|---|---|---|
| `AIDE-Classic/` | Java 7-compatible syntax, platform Android APIs | 28 | Older AIDE / classic project support |
| Root `app/` | Same Java and resources | 34 | Newer AIDE / Android Studio |

Classic profile Google Play publishing-এর জন্য নয়। Gradle wrapper binaries included নয়; আপনার AIDE/IDE-র installed build engine ব্যবহার করতে হবে। ফোনে APK বানাতে প্রথমবার SDK/build dependencies নামানোর ইন্টারনেট লাগতে পারে।

## হোমস্ক্রিন উইজেট

অ্যাপ খুলে বিশ্ববিদ্যালয় ও ইউনিট নির্বাচন করুন। রাবির ক্ষেত্রে প্রবেশপত্র অনুযায়ী শিফট নির্বাচন করুন। “পরীক্ষা উইজেটে রাখুন” অথবা “আবেদনের সময় উইজেটে রাখুন” চাপুন। এরপর launcher-এর Widgets থেকে BD Admission যোগ করুন।

উইজেট Android-এর পর্যায়ক্রমিক update এবং refresh button-এ আপডেট হয়; প্রতি সেকেন্ডে চলে না। এজন্য seconds field-এ `—` থাকে। নির্দিষ্ট সময় প্রকাশিত না হলে উইজেট কোনো অনুমান করা সময় ধরে countdown দেখাবে না। নতুন ওয়েব ডেটা widget-এ আনতে অ্যাপ খুলতে হবে।

## পরিবর্তন

- একই WebView-তে responsive ওয়েবসাইট, official application/circular links external browser-এ।
- Exact HTTPS host allowlist; SSL error হলে cancel, mixed content/file access বন্ধ।
- লোডিং error-এর পর page-finished callback ভুল করে error screen সরাবে না।
- System bars-এর নিচে content ঢুকে না যাওয়ার জন্য immersive full screen সরানো হয়েছে।
- বাংলাদেশ সময়ের ISO offset parse; অজানা সময় ও আবেদন countdown support।
- পুরোনো selection-এর image download নতুন widget logo overwrite করতে পারবে না।
- `build/`, `bin/`, `gen/`, পুরোনো APK, signing key ও machine-specific local.properties বাদ দেওয়া হয়েছে।

## Verification / সীমাবদ্ধতা

Java source syntax parse, XML parse, lambda/method-reference scan ও দুটি profile-এর source parity পরীক্ষা করা হয়েছে। এখানে Android SDK বা AIDE device না থাকায় APK compile, install বা emulator পরীক্ষা করা হয়নি। তাই এই ZIP source project, পরীক্ষিত APK নয়।

Live URL: https://bd-admission.vercel.app/
Version code: 3; version name: 1.1.0.

Build or release করার পরে তবেই Vercel-এর `APK_VERSION_CODE`, `APK_VERSION_NAME`, `APK_URL` পরিবর্তন করুন। এই source update আগের APK download endpoint-এর release version নিজে থেকে বদলায় না।
