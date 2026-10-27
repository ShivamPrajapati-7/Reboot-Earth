# Reboot Earth 🌱

> **Small steps for a greener planet**

**Reboot Earth** (EcoVision) is an Android application designed to help users identify and properly dispose of household waste using on-device machine learning. Powered by TensorFlow Lite, the app quickly classifies waste items into appropriate categories and presents tailored disposal guidance and step-by-step recycling instructions.

---

## 🌟 Key Features

- **📸 Image Input (Gallery & Camera)**:
  - Choose existing photos from your device gallery using the System Photo Picker.
  - Capture real-time photos of waste items using your device camera with `FileProvider` integration.

- **🤖 On-Device Waste Classification**:
  - Runs local, private, and instant machine learning inference via **TensorFlow Lite** (`waste_classifier.tflite`).
  - Operates completely offline with zero server latency or data usage.

- **📊 Model Confidence & Warning System**:
  - Displays explicit confidence scores and visual progress indicators.
  - Highlights low-confidence predictions (< 50%) with warnings to encourage accurate waste sorting.

- **♻️ Actionable Guidance & Recycling Steps**:
  - **Disposal Guidance**: Specific instructions on how and where to dispose of the item (e.g., general residual waste, compost, specialized hazardous collection).
  - **Recycling Steps**: Clear, numbered step-by-step instructions on cleaning, sorting, and placing items in designated bins.

- **🎨 Modern Eco-Inspired Material 3 UI**:
  - Designed with a soft green theme, rounded cards, intuitive layout, and custom vector icons.

---

## 📱 Screenshots

| Waste Identification Screen |
| :---: |
| Eco-friendly layout with camera, gallery, model score, guidance, and recycling steps. |

---

## 🛠️ Technology Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: Material Design 3 (`com.google.android.material`)
- **Machine Learning**: TensorFlow Lite (`org.tensorflow:tensorflow-lite:2.16.1`, `tensorflow-lite-task-vision:0.4.4`)
- **Concurrency**: Kotlin Coroutines (`lifecycleScope`, `Dispatchers.IO`)
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 37 (Android 15)
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)

---

## 📂 Project Architecture

```
Reboot-Earth/
├── Reboot_Earth.apk                          # Pre-built APK file for quick installation
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── waste_classifier.tflite   # Pre-trained TFLite model
│   │   │   ├── java/com/reboot/rebootearth/
│   │   │   │   ├── MainActivity.kt           # Main UI logic & result handling
│   │   │   │   └── WasteClassifier.kt        # TFLite inference & image pre-processing
│   │   │   └── res/
│   │   │       ├── drawable/                 # Custom vector drawables (leaf, gallery, camera, scan, etc.)
│   │   │       ├── layout/activity_main.xml  # Material 3 screen layout
│   │   │       └── values/colors.xml         # Eco color palette definitions
│   └── build.gradle.kts                      # App module build configuration
├── build.gradle.kts                          # Project root build script
├── settings.gradle.kts                       # Repository settings
└── README.md                                 # Project documentation
```

---

## 🚀 Getting Started & Installation

### Option 1: Quick Installation via Pre-built APK (`Reboot_Earth.apk`)

A pre-built APK file [`Reboot_Earth.apk`](Reboot_Earth.apk) is included directly in the root directory of this repository for instant testing.

#### Method A: Direct Install on Android Device
1. Transfer `Reboot_Earth.apk` to your Android phone or device.
2. Open the file manager on your phone and tap `Reboot_Earth.apk`.
3. Allow **"Install from Unknown Sources"** if prompted by Android.
4. Open **Reboot Earth** and start identifying waste!

#### Method B: Install via ADB (Android Debug Bridge)
If you have ADB installed on your computer and a connected Android device or emulator:
```bash
adb install Reboot_Earth.apk
```

---

### Option 2: Build & Run from Source (Android Studio)

#### Prerequisites
- **Android Studio**: Android Studio Ladybug (2024.2.1) or newer.
- **JDK**: Java Development Kit 11 or higher.
- **Android Device / Emulator**: Running Android 7.0 (API Level 24) or higher.

#### Steps
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/Reboot-Earth.git
   cd Reboot-Earth
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the `Reboot-Earth` directory.

3. **Sync & Run**:
   - Allow Gradle to sync dependencies.
   - Connect your device or emulator and press **Run** (`Shift + F10`).

---

## 🔬 How the Classifier Works

1. **Image Pre-processing**:
   - Converts the selected image into a `Bitmap` scaled to match the input tensor shape expected by `waste_classifier.tflite` (e.g. `224x224x3`).
   - Normalizes RGB pixel channels into direct float buffers.

2. **Tensor Inference**:
   - Passes the buffer into the TFLite `Interpreter`.
   - Obtains probability scores across waste categories (**Hazardous**, **Non-Recyclable**, **Organic**, **Recyclable**).

3. **UI Result Rendering**:
   - Maps the predicted category to category badge, item title, confidence percentage, progress bar value, disposal guidance, and recycling steps.

---

## 👥 Authors

- **Authors**: Shivam Prajapati, Rahul, Yash, Priyansh
- **Team**: Reboot
- **Project**: Reboot Earth
- **Contributions**: Pull requests, feature suggestions, and bug reports are welcome!

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
