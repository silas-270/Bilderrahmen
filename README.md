# 🖼️ Bilderrahmen | The Smart Digital Photo Frame

**Bilderrahmen** is a high-performance, responsive digital photo frame application built with Java. It transforms any display—from a minimalist Raspberry Pi setup to a high-resolution 4K monitor—into a sleek, interactive dashboard that blends personal memories with real-time smart home data.

---

## 🎯 Project Overview

The project is designed to be "set and forget": it handles resolution scaling automatically, detects home presence, and dims itself during night hours, all while maintaining a buttery-smooth 60fps during UI transitions without taxing the system CPU.

---

## ✨ Key Features

### 📡 Smart Home & IoT Integration

The application features a modular widget system. Each widget is designed to be unobtrusive while providing high-value information at a glance:

*   **🕒 Clock Widget**: The core display showing current time and date in a bold, readable format.
    *   *Data Source*: Local system time, updated once per minute.
*   **🌦️ Weather Widget**: Displays the current temperature and a context-aware weather icon.
    *   *Data Source*: **Open-Meteo API**. To maintain high performance, data is cached and updated in the background only at specific intervals, ensuring no network dependency on the main UI thread.
*   **☀️ Solar Widget**: A minimalist energy dashboard for solar power producers. It shows current power production (W), today's total yield (kWh), and a sparkline chart of the last 8 hours.
    *   *Data Source*: **Home Assistant REST API**. Historical data is fetched asynchronously to populate the chart without blocking the rendering of the next photo.
*   **👥 Presence Widget (Network)**: Shows which family members or roommates are currently "at home."
    *   *Data Source*: **Background ARP Scanning**. The application utilizes an `ExecutorService` to parallelize pings to known device IPs, filling the system's ARP cache (`/proc/net/arp`). This multi-threaded approach ensures that local network latency never impacts the smoothness of the slideshow.

---

### 🎨 Premium User Experience
*   **Responsive Engine**: A custom-built scaling utility (`util.Scale`) ensures the UI remains perfectly proportioned whether running on a 7-inch touchscreen or a full-sized television.
*   **Smooth Transitions**: A decoupled `RenderLoop` architecture allows for resource-efficient performance (0.2 FPS during idle) while ramping up to 60 FPS for fluid cross-fading and UI animations.
*   **Gesture Control**: Supports touch swipes for manual photo navigation and intuitive click-interactions for widget overlays.

### 🛠️ Technical Robustness
*   **Persistent Configuration**: A JSON-based configuration management system that remembers user preferences, API keys, and image directories.
*   **Metadata Awareness**: Built-in EXIF support to respect image orientation and provide technical insights into the displayed photography.
*   **Modern Aesthetics**: Styled with a customized **FlatLaf** Dark Theme, featuring clean typography and a minimalist widget layout.

---

## 🏗️ Technical Architecture

### **Adaptive Render Loop**
To balance visual fidelity with low power consumption (specifically for ARM-based systems like the Raspberry Pi), the architecture utilizes a state-based loop:
*   **`IDLE` Mode**: Drops refresh rates to ~0.2 FPS during stagnant photo display. This drastically reduces CPU overhead and heat generation compared to traditional 60Hz rendering.
*   **`TRANSITION` Mode**: Automatically ramps up to 60 FPS only during cross-fading and UI animations for a premium, fluid feel.
*   **`MENU` Mode**: High-priority interaction mode ensuring sub-16ms touch/click response.

### **Thread-Safe Data Pipeline**
A major performance hurdle in Java Swing is the blocking of the Event Dispatch Thread (EDT). This project solves this by offloading all IO-bound tasks:
1.  **Image Loading**: High-resolution photos are pre-loaded and decoded in a background thread to prevent "stuttering" during transitions.
2.  **API Decoupling**: All IoT data (Weather/Solar) is fetched via a non-blocking HTTP client and stored in thread-safe caches.
3.  **Parallel Scans**: The Network presence detection handles up to 254 pings simultaneously using a managed thread pool, keeping the system responsive regardless of network size.

### **Resolution Independence**
Instead of relying on standard OS scaling—which often results in blurred Swing components on HiDPI displays—the project features a custom-built rendering engine. By treating `1080p` as a reference height and mathematically recalculating every coordinate, font size, and padding in real-time, the UI remains perfectly sharp and proportioned across any output resolution.

---

## 💻 Tech Stack

*   **Language**: Java 17+
*   **UI Framework**: Java Swing + FlatLaf (Modern Look & Feel)
*   **Integration**: Home Assistant REST API, OpenWeatherMap API
*   **Data Handling**: JSON for configuration and state persistence
*   **Libraries**: 
    *   `com.formdev.flatlaf` for advanced styling.
    *   Custom implementation for gesture detection and image transition shaders.

---

## 🚀 How to Run

### **Prerequisites**
*   JDK 17 or higher
*   (Optional) Home Assistant instance for full IoT features

### **Quick Start**
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/Bilderrahmen.git
   ```
2. Build the project (using your IDE or Maven/Gradle if implemented).
3. Run the application:
   ```bash
   java -jar Bilderrahmen.jar /path/to/your/photos
   ```