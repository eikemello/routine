# Agent Instructions & Product Vision (Routine)

## 🎯 Core Vision
**Routine** is an Android application built around the Android `NotificationListenerService` (NLS). The core objective of the product is to intercept, process, and potentially automate actions based on incoming device notifications. By leveraging the NLS API (as indicated by the package name `com.android.nls.Routine` and the README), the app aims to create smart "routines", filters, or logs that react contextually to the user's notifications.

## 📂 Project Structure
The project is a standard Android Gradle project currently being developed in Java.

```text
routine/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # Defines app permissions, configuration, and components.
│   │   ├── java/com/android/nls/Routine/
│   │   │   ├── HomeActivity.java  # Main entry point UI of the application.
│   │   │   └── utils/
│   │   │       ├── Common.java    # Utility methods (e.g., Tag generation).
│   │   │       └── Constants.java # Global constants (e.g., base TAG "RTN_").
│   │   └── res/
│   │       └── layout/
│   │           └── activity_home.xml # Layout for HomeActivity.
│   └── build.gradle               # App-level build configurations (minSdk 33, targetSdk 36).
├── README.md                      # Project overview ("# NotifyListenerService").
└── build.gradle                   # Root-level build configurations.
```

## ⚠️ Current Issues & Immediate Tasks
1. **Missing NLS Implementation**: There is currently no class extending `NotificationListenerService`. 
   - **Task**: Create a new service (e.g., `RoutineNotificationService`) extending `NotificationListenerService`.
   - **Task**: Add the service to the manifest with the `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` permission and the intent filter for `android.service.notification.NotificationListenerService`.
3. **Permission Prompting**: To enable NLS, the user must grant permission in system settings. The app needs a mechanism in `HomeActivity` to check if the permission is granted and prompt the user by launching the `android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` intent if it's not.

## 🧠 Guidelines for AI Agents
- **Language**: The project is using **Java 11**. Stick to Java for new implementations unless instructed to introduce Kotlin.
- **SDK Versions**: `minSdk 33` (Android 13) and `targetSdk 36`. Keep modern Android API restrictions in mind (especially regarding foreground services, notifications, and background execution limits).
- **Logging**: Use `Common.generateTag(YourClass.class)` and standard `Log` methods. The base tag is defined as `RTN_`.
- **UI/UX**: The app uses modern `EdgeToEdge` configurations and XML layouts with `ViewCompat` window insets. Keep UI additions compliant with these edge-to-edge practices.
