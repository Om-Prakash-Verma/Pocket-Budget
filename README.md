# GhostPocket — Minimal Spooky Student Expense Tracker 👻💰

**GhostPocket** is a native Android expense tracker and shared debt ledger designed for college students. Crafted with a monochrome **Bento Grid + Claymorphism** aesthetic, playful spooky charm, and an offline-first architecture powered by Jetpack Compose, Room, and a cost-optimized **Cloudflare D1 & Workers** backend.

---

## 📱 Screenshots

<div align="center">

| Home (Bento Dashboard) | Activity Log (Spells) | Ghost Squad (Shared Debts) | Spooky Insights (Alchemy) |
|:---:|:---:|:---:|:---:|
| <img src="screenshots/Screenshot_20261019_154927.png" width="200" alt="Home Dashboard" /> | <img src="screenshots/Screenshot_20261019_155011.png" width="200" alt="Activity Log" /> | <img src="screenshots/Screenshot_20261019_155023.png" width="200" alt="Ghost Squad" /> | <img src="screenshots/Screenshot_20261019_155041.png" width="200" alt="Spooky Insights" /> |

| Add Transaction | Split Bill Wizard | Friend Ledger & Exorcism | Spooky Settings |
|:---:|:---:|:---:|:---:|
| <img src="screenshots/Screenshot_20261019_155100.png" width="200" alt="Add Transaction" /> | <img src="screenshots/Screenshot_20261019_155110.png" width="200" alt="Split Bill Wizard" /> | <img src="screenshots/Screenshot_20261019_155152.png" width="200" alt="Friend Ledger" /> | <img src="screenshots/Screenshot_20261019_155054.png" width="200" alt="Spooky Settings" /> |

</div>

---

## ✨ Features

- **🎨 Bento Grid + Claymorphism UI**: 
  - Tactile 3D soft clay surfaces, specular borders, fluid elevations, and strict monochrome palette (pure black, crisp white, deep slate, and neutral grays).
  - Unified tactile Bento headers across every screen featuring custom `ClayIconButton` controls and zero redundant top bars.
- **👻 Animated Mascot & Expressions**: 
  - An original vector micro-mascot blob with dynamic moods (Rich Ghost 🎩, Happy, Thinking, Spooked 😱) that reacts in real time to your spending velocity and remaining budget.
- **📜 Animated Crypt Data Restoration**:
  - Polished loading screen with an animated Thinking Mascot and progress spinner (*"Conjuring your data from the crypt... 📜✨"*) when signing in on a new device.
- **💰 Monthly Allowance & Budgeting**: 
  - Set your monthly budget with live progress bars, danger zone alerts, and funny status messages.
- **🏷️ 100% Custom Category Control & Budgeting**:
  - Zero hardcoded defaults; create and delete custom categories (e.g., *Chai*, *Books*, *Hostel Rent*).
  - Allocate category-specific budgets with live progress tracking in the **Insights 🔮** dashboard.
- **👥 Squad Ledgers & Bill Splitting**:
  - Track bilateral debts (*"They Owe You 📜"* vs *"You Owe Them 💸"*).
  - Fair bill splitting with remainder-distributed exact paise division (zero 1-paisa rounding errors).
  - Debt exorcism (settle individual or all balances with one tap).
- **💬 WhatsApp Card Payment Reminders**:
  - Store friends' WhatsApp numbers directly.
  - Automatically renders an aesthetic 1080x1350 card image (with cartoon mascot, anime begging eyes, speech bubble, and meme friendship disclaimer) and opens it directly in WhatsApp with zero cloud storage costs.
- **🔑 Flexible Authentication**:
  - Email & Password with SHA-256 credential verification and friendly error messages.
  - **Sign In with Google** (`play-services-auth` + Firebase Auth).
  - **Ghost Walk (Continue as Guest)** for instant, private offline exploration.
- **⚡ Ultra-Optimized Cloudflare D1 Backend**:
  - **Single-Roundtrip 2-Way Sync** (`POST /api/sync`): Pushes local dirty records and pulls remote updates in one atomic HTTP transaction.
  - **Only-When-Dirty Gating**: Minimizes Worker invocations by only syncing when local mutations exist.
  - **Atomic Transactions**: Batch upsert with D1 SQLite avoids partial writes.
- **☁️ Multi-Cloud & Offline-First**:
  - **Room Database** acts as the single source of truth (0ms latency, works 100% offline).
  - Supports both Cloudflare D1 (Edge SQLite) and Firebase Firestore.
  - **Account Isolation**: Local cache is purged on sign-out to prevent data leakage between accounts.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100% Native) |
| **UI Framework** | Jetpack Compose & Material 3 |
| **Design System** | Bento Grid + Claymorphism (Strict Monochrome) |
| **Architecture** | Unidirectional Data Flow (UDF) + MVVM |
| **Local Storage** | Room Database (SQLite) + Coroutine Flows |
| **Currency Calculations** | Integer Paise (`Long`) for exact financial precision |
| **Backend & Sync** | Cloudflare Workers + Cloudflare D1 (Edge SQLite) via Hono v4 |
| **Alternative Cloud DB** | Cloud Firestore |
| **Authentication** | Cloudflare D1 Auth + Firebase Auth (Google Sign-In, Email/Password, Guest) |
| **Background Sync** | AndroidX WorkManager (Debounced, network-constrained, exponential backoff) |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) |
| **JDK Requirement** | JDK 21 (Temurin / OpenJDK 21) |
| **Testing** | JUnit 4 unit tests |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 21 (`java -version` should report 21.x)
- Android device or emulator running Android 8.0 (API 26) or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/Pocket-Budget.git
   cd Pocket-Budget
   ```
2. Build the debug APK:
   ```powershell
   $env:JAVA_HOME = "C:\path\to\jdk-21"
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

## ⚡ Cloudflare D1 Backend

The production-ready backend is located in [`/backend`](file:///backend) and deployed to Cloudflare Workers:

- **Live URL**: `https://ghostpocket-backend.omprakashverma-in-fda.workers.dev`
- **Database**: Cloudflare D1 (`ghostpocket-db`)
- **Key Endpoints**:
  - `POST /api/auth/register` — Register account
  - `POST /api/auth/login` — Sign in with credential verification
  - `POST /api/auth/google` — Sign in with Google ID token
  - `POST /api/sync` — Unified 2-way sync (push dirty records + pull updates in 1 roundtrip)

To run the backend locally or deploy your own:
```bash
cd backend
npm install
npx wrangler d1 execute ghostpocket-db --local --file=./schema.sql
npm run dev
```

For deployment to Cloudflare:
```bash
npm run deploy
```

---

## ☁️ Firebase Configuration (Optional)

GhostPocket can also be configured with Firebase:
1. Add `google-services.json` to the `app/` folder.
2. In Firebase Console, enable **Email/Password**, **Google**, and **Anonymous** sign-in methods.
3. Apply security rules from [`firestore.rules`](file:///firestore.rules).

---

## 📜 License & Acknowledgments

Built with 👻 for college mortals tracking their coins.
Distributed under the MIT License.
