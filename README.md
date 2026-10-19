# GhostPocket — Minimal Spooky Student Expense Tracker 👻💰

**GhostPocket** is a native Android expense tracker and shared debt ledger built for college students. Crafted with a monochrome **Bento Grid + Claymorphism** aesthetic, playful spooky charm, and an offline-first architecture powered by Jetpack Compose, Room, and Firebase.

---

## ✨ Features

- **🎨 Bento Grid + Claymorphism UI**: Tactile 3D soft clay surfaces, subtle specular bevels, fluid elevations, and a strict monochrome palette (pure black, crisp white, deep slate, and neutral grays).
- **👻 Animated Mascot & Expressions**: An original micro-mascot blob with dynamic moods (Rich Ghost 🎩, Happy, Thinking, Spooked 😱) reflecting your budget health in real time.
- **💰 Monthly Allowance / Budgeting**: Set your monthly gold budget with visual progress bars and warning alerts if you overspend.
- **🏷️ 100% Custom Category Control & Budgeting**:
  - Zero hardcoded defaults; create and delete custom categories (e.g. *Chai*, *Books*, *Hostel Rent*).
  - Allocate category-specific budgets with live progress tracking in the **Insights 🔮** dashboard.
- **👥 Squad Ledgers & Bill Splitting**:
  - Track bilateral debts ("They Owe You 📜" vs "You Owe Them 💸").
  - Fair bill splitting with remainder-distributed exact paise division (no lost 1-paisa rounding errors).
  - Clean debt exorcism (settle all balances with one tap).
- **💬 WhatsApp Card Payment Reminders**:
  - Store friends' WhatsApp numbers directly.
  - Automatically generates an aesthetic card image (with debtor name, ₹ amount, reason, and spooky disclaimer) and opens it directly in WhatsApp.
- **🔑 Flexible Authentication**:
  - Email & Password sign-in / registration.
  - **Sign In with Google** (`play-services-auth` + Firebase Auth).
  - **Ghost Walk (Continue as Guest)** for instant, private offline exploration.
- **☁️ Smart Offline-First Sync**:
  - **Room Database** acts as the single source of truth (0ms latency, works 100% offline).
  - **WorkManager SyncWorker** batches updates to Cloud Firestore.
  - **Operation Coalescing**: Items created and deleted locally before syncing are cleared without emitting billable writes.
  - **Account Isolation**: Local cache is purged on sign-out to prevent data leakage between user accounts.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100% Native) |
| **UI Framework** | Jetpack Compose & Material 3 |
| **Architecture** | Unidirectional Data Flow (UDF) + MVVM |
| **Local Storage** | Room Database (SQLite) + Coroutine Flows |
| **Calculations** | Integer Paise (`Long`) for exact currency precision |
| **Cloud Database** | Cloud Firestore |
| **Authentication** | Firebase Auth (Google Sign-In, Email/Password, Anonymous) |
| **Background Sync** | AndroidX WorkManager (Debounced, network-constrained, exponential backoff) |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) |
| **Testing** | JUnit 4 unit tests |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 21 (e.g. OpenJDK 21)
- Android device or emulator running Android 8.0 (API 26) or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/Pocket-Budget.git
   cd Pocket-Budget
   ```
2. Build the debug APK:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
3. Run unit tests:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
4. Install to an active emulator or device:
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.ghostpocket.app/.MainActivity
   ```

---

## ☁️ Firebase Configuration

GhostPocket runs completely offline out of the box. To enable Cloud Sync across devices:

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.ghostpocket.app`.
3. Download `google-services.json` into `app/google-services.json`.
4. In Firebase Console:
   - **Authentication**: Enable **Email/Password**, **Google**, and **Anonymous** sign-in methods.
   - **Cloud Firestore**: Create a database (test or production mode).
5. Deploy or paste the rules from [`firestore.rules`](file:///firestore.rules) into the **Firestore Rules** editor:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;

         match /transactions/{transactionId} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
         match /friends/{friendId} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
         match /friend_ledgers/{ledgerId} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
         match /categories/{categoryName} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
         match /settings/{settingId} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
       }
     }
   }
   ```

---

## 📜 License & Acknowledgments

Built with 👻 for college mortals tracking their coins.
Distributed under the MIT License.
