# WorkBee - Home Service Booking Platform 🐝

WorkBee is a premium, state-of-the-art home service booking Android application built using **Java, XML, Material Design 3, and Firebase (Authentication, Firestore, and Storage)**. It connects local homeowners (Customers) with highly skilled, vetted local service professionals (Service Providers) like plumbers, electricians, cleaners, carpenters, painters, AC technicians, and appliance repair experts under a unified platform managed by a robust Admin panel.

---

## 🌟 Premium Features

### 👤 Customer Features
* **User Onboarding**: Modern ViewPager slide deck with beautiful, custom DOT indicators and high-contrast animations.
* **Smart Search & Filters**: Search services and browse categorized grids with real-time distance and rate sorting.
* **Flexible Bookings**: Interactive date and time slot calendar pickers with dynamic summary pricing calculators.
* **Real-time Tracking**: Real-time status trackers for scheduled, active, and completed bookings.
* **Ratings & Feedback**: Add star reviews and comments after job completion to build trusted provider portfolios.

### 🛠️ Service Provider Features
* **Dynamic Registrations**: Segmented controls expanding specialized provider input fields (business names, categories, experience, bio, and hourly rates) during register.
* **Online Switch**: High-contrast availability toggle switches. Turn "Online" to accept incoming service requests, or "Offline" to go off-duty.
* **Earnings Metrics Panel**: Tracking stats for net earnings (after 10% platform commission), total jobs completed, and average rating stars.
* **Work Order Manager**: Accept or decline new customer booking requests and easily advance work states with simple "Mark Completed" hooks.

### 👑 Admin Features
* **Live Analytics Board**: High-end statistic counts showing platform-wide members, active providers, booking transactions, and calculated platform commissions.
* **Provider Vetting Queue**: Segmented tabs to inspect new provider applicants, vet credentials, approve partners, or suspend/block active partners immediately.
* **Service Category CRUD**: In-app grid view of categories equipped with dynamic custom forms to register brand-new services, descriptions, prices, and vector icons.

---

## 🚀 Dual-Mode Database Architecture (Offline Mock Fallback)

To eliminate immediate remote database dependencies and make local compilation or runtime testing smooth:
* **Firebase Cloud Mode**: If a valid `google-services.json` is integrated in the app folder, the application utilizes live Firebase Authentication, Firestore databases, and Google Storage APIs.
* **Local Sim Fallback**: If compiled without a remote configuration, `FirebaseHelper.java` catches initialization exceptions automatically, switches to `useMockMode = true`, and boots a **dynamic, in-memory simulated database pre-seeded with custom users, categories, bookings, reviews, and notifications**!
* **Everything Works Instantly**: Users can log in, perform bookings, accept requests, complete jobs, and manage metrics offline without crashes or configuration hurdles.

---

## 🔑 Pre-seeded Testing Accounts (Simulated & Auth)

Log in directly using these pre-configured credentials to inspect features instantly:

| Role | Email Address | Password | Details & Pre-seeded Stats |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@workbee.com` | `password` | **Sarah Jenkins** (456 Blossom Lane). Active booking history. |
| **Service Provider** | `provider@workbee.com` | `password` | **Miller Plumbing Services** ($49/hr, 8 yrs exp). Completed 42 jobs, $2,058.00 net earnings, 4.8★. |
| **Admin** | `admin@workbee.com` | `password` | **WorkBee Admin** with complete platform control dashboard. |

*Additional pre-seeded providers in simulated database for browsing/filters:*
* `sparky@workbee.com` (Marcus Sparks - **Electrical**)
* `elena@workbee.com` (Elena Rostova - **Cleaning**)
* `jack@workbee.com` (Jack Higgins - **Carpentry** - *Awaiting Admin Vetting*)

---

## 🎨 Theme & Styling System

The application boasts a premium, high-contrast **Bee-themed** color palette:
* **Honey Gold (Primary)**: `#FFC107`
* **Deep Amber (Primary Variant)**: `#FF8F00`
* **Midnight Charcoal (Secondary/Blacks)**: `#212121`
* **Warm Sand (Tertiary Accent)**: `#FFF8E1`
* **Success/Active**: `#4CAF50` (Pills & Accept buttons)
* **Danger/Cancelled**: `#F44336` (Pills & Decline buttons)

The design incorporates Material 3 components, card elevations with custom borders, rounded layouts, and custom-styled status pills.

---

## 🛠️ Project Compilation & Installation Guidelines

### Requirements
* **Android Studio** (Koala or newer recommended)
* **Android SDK** (Min SDK 24, Target SDK 34)
* **Gradle Wrapper** (v8.0+)
* **JDK** (Java Development Kit 17)

### Step-by-Step Build Instructions

1. **Clone & Open Project**:
   Open Android Studio, select **File -> Open**, and choose the project root folder.
   
2. **Firebase Setup (Optional - for online mode)**:
   * Go to [Firebase Console](https://console.firebase.google.com/).
   * Register a new Android App with package name `com.workbee.app`.
   * Download the `google-services.json` file and place it under `app/` folder.
   * Enable **Email/Password sign-in** under Auth, and initialize **Cloud Firestore** and **Firebase Storage**.

3. **Build Code**:
   Build the project via terminal using:
   ```bash
   ./gradlew assembleDebug
   ```
   Or click **Build -> Make Project** inside Android Studio.

4. **Run Application**:
   Connect an Android Emulator or a physical testing device via USB debugging, and run:
   ```bash
   ./gradlew installDebug
   ```
   Or click the green **Run** icon in the toolbar.
