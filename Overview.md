# 🖼️ Digitaler Bilderrahmen – Projektbeschreibung (Aktueller Stand)

## Überblick

Natives Java-Projekt zur Realisierung eines digitalen Bilderrahmens, basierend auf Java Swing / Graphics2D. Die Anwendung zeigt Bilder im Vollbild-Modus an und wechselt diese automatisch in einem konfigurierbaren Intervall. Neu hinzugekommen sind weiche Crossfade-Übergänge zwischen den Bildern. Darüber hinaus bietet die App Zusatz-Widgets (Uhrzeit, lokales Wetter, WLAN-Anwesenheitserkennung) sowie ein umfassendes Ring-Menü zur Steuerung.

---

## Funktionen

### 1. Bilderanzeige

- Vollbild-Anzeige von Bilddateien (`.jpg`, `.jpeg`, `.png`)
- Automatischer Bildwechsel nach konfigurierbarem Zeitintervall (Sekunden aus `config.json`)
- **Weicher Crossfade (Bildübergang):** Überblendet das alte in das neue Bild (Dauer über `config.json` anpassbar). Die unsichtbaren Ränder verblassen zu Schwarz.
- EXIF-Auswertung bei `.jpg`/`.jpeg`-Dateien → automatische und korrekte Rotation der Bilder
- Navigation: Vorwärts (`Swipe Left`) und Rückwärts (`Swipe Right`) manuell möglich
- Ordner werden rekursiv durchsucht, die Bilder zufällig sortiert und per intelligentem Pointer-Mechanismus iteriert

---

### 2. Dateiverwaltung & Setup

#### Ordnerstruktur (Auszug)

```
bilderrahmen/
├── src/
│   └── main/java/.../
│       ├── Main.java                  # Einstiegspunkt, Widget-Registrierung
│       ├── display/                   # GUI, Frame, Rendering, Game-Loop
│       ├── slideshow/                 # Bildladen, Pointer, Shuffle, Preloading
│       ├── menu/                      # Ringmenü, Ordner-Dialog
│       ├── widgets/                   # Anzeige-Overlays (Uhr, Wetter, Netzwerk)
│       ├── config/                    # JSON Manager, DTOs
│       └── util/                      # EXIF, Skalierung, Ping-Logic
│
├── resources/
│   ├── config.json                    # Persistente Laufzeit-Konfiguration
│   └── arp_mapping.json               # MAC/IP → Personen-Name
│
└── lib/                               # metadata-extractor (EXIF) etc.
```

#### Verhalten beim Laden

- Der **Root-Pfad** wird in der JSON-Config gespeichert (bleibt erhalten).
- Beim Start (oder wenn im Menü geändert) sammelt der Loader alle Bilder aus tiefen Ordner-Strukturen ein (außer Unterordner namens `/Personen/`).
- Die gefundenen Bilder werden *shuffled* und vorberechnet im Hintergrund geladen (`preloaded`), um Pausen beim Bildwechsel zu eliminieren.

---

### 3. Menü

Das Menü präsentiert sich als modernes Ringmenü und öffnet/schließt sich auf Bildschirm-Klick/Touch.

- Sucht automatisch nach einem `Personen`-Unterordner und listet dessen Unterordner als klickbare Buttons auf dem Ring auf.
- **Funktionen:**
  - `Neuen Ordner wählen`: Öffnet einen JFileChooser (OS-nativ) und setzt das Basisverzeichnis neu.
  - `Alle Bilder`: Zeigt die gigantische Standard-Auswahl.
  - `[Personen Name]`: Schränkt die Slideshow temporär nur auf diese Person ein.
  - `Exit`: Schließt die App (auch über 'ESC' Taste möglich).

---

### 4. Widgets

#### 4.1 Datum & Uhrzeit (`ClockWidget`)
- Anzeige von Wochentag, Datum und aktueller Uhrzeit.
- Visuell: Halbtransparentes, abgerundetes Rechteck unten links.

#### 4.2 Wetter (`WeatherWidget`)
- Großflächiges, modernes Design mit zentriertem Icon und Temperatur-Pille mittig links am Rand.
- Nutzt die OpenWeatherMap API (aktuell Dummy-Mode bis API-Key eingetragen wird).
- Aktualisierungsintervall flexibel in `config.json` definierbar (Standard: 30 Min).

#### 4.3 WLAN-Geräte (`NetworkWidget`)
- Zeigt Personen an, die gerade zuhause im WLAN eingeloggt sind.
- Im Gegensatz zu Uhr/Wetter **kein Rechteck**, sondern reiner, gestapelter weißer Text mit leichtem Dropshadow direkt über dem Wetter.
- **Logik:** Liest `arp_mapping.json` (MAC, Target IP, Personenname). Führt einen massiven parallelen Ping-Sweep (oder direkte Pings auf konfigurierte IPs) aus und liest dann den Kernel ARP-Cache (`/proc/net/arp`). Extrem performant und exakt. Aktualisierungsintervall in der Config definierbar.

---

### 5. Performance & Framerate (RenderLoop)

Um den Raspberry Pi vor Hitzetod und Systemauslastung zu bewahren, läuft die `BufferStrategy` in einem dynamischen State-Machine-Render-Loop:

| State | Framerate | Nutzung |
|---|---|---|
| **IDLE** | 0,2 fps | Standard-Betrieb. Das Bild liegt einfach statisch an. GPU/CPU schlafen fast komplett. |
| **MENU** | 30.0 fps | Ring-Menü ist ausgefahren. Schnelles Feedback für Clicks und Hover. |
| **TRANSITION** | 60.0 fps | Während eines Bildwechsels (Standard: 800ms) rechnet die App extrem schnell hoch, um den weichen Crossfade perfekt stufenlos darzustellen. Fällt danach direkt auf IDLE ab. |

---

### 6. Konfiguration (Persistenz)

JSON ist das Herzstück der Einstellungen. GSON erstellt fehlende Keys automatisch.

**`config.json`:**
- `rootPath`: Aktueller Bilder-Ordner. 
- `imageDurationSeconds`: Zeit pro Bild in der Slideshow (Standard 10).
- `weatherApiEndpoint` & `weatherApiKey`: Für OpenWeatherMap.
- `weatherRefreshIntervalSeconds`: z.B. 1800 (30 Min).
- `networkRefreshIntervalSeconds`: z.B. 60 (1 Min).
- `transitionDurationMs`: Zeit für den Crossfade in Millisekunden (z.B. 800).

**`arp_mapping.json`:**
Struktur für smarte Anwesenheitserkennung:
```json
{
  "D2:A2:BA:C6:2A:9A": {
    "name": "Silas Handy",
    "ip": "192.168.178.94"
  }
}
```

---

### Zusammenfassung
Ein minimalistischer, performanter Bilderrahmen in purem Java, gebaut für flüssigen 24/7 Betrieb auf Linux/Raspberry Pi Systemen.
