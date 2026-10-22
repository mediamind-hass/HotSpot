# 📶 MikroTik HotSpot Manager (Android)

Un'applicazione Android moderna scritta in **Kotlin** e **Jetpack Compose** (Material 3) per connettersi a router e antenne **MikroTik RouterOS** sulla **porta 8278** (o personalizzata) per gestire in tempo reale gli utenti dell'HotSpot.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![MikroTik](https://img.shields.io/badge/MikroTik-RouterOS-EF2D2D?style=for-the-badge)

---

## 🌟 Caratteristiche Principali

- **🔌 Connessione Flessibile sulla Porta 8278**:
  - Supporta sia la **Socket API binaria** sia la **REST API** di RouterOS v7.
  - **Modalità Rilevamento Automatico (`AUTO`)**: Tenta la connessione via Socket API ed esegue il fallback automatico su REST API.
  - **Connessione Automatica allo Start**: Si collega in automatico all'avvio con i parametri salvati.
  - **Supporto SSL/TLS**: Possibilità di abilitare la cifratura SSL/TLS.
- **👥 Gestione Completa Utenti HotSpot (CRUD)**:
  - 📖 **Lettura**: Elenco utenti con stato (*Attivo/Disabilitato*), profilo, tempo di utilizzo (*uptime*) e traffico dati.
  - ➕ **Creazione**: Inserimento nuovi utenti con Username, Password, Profilo, Server, Limite Tempo e Note.
  - ✏️ **Modifica**: Aggiornamento parametri utente e switch per abilitazione/disabilitazione istantanea.
  - 🗑️ **Eliminazione**: Rimozione sicura degli utenti.
- **🔍 Filtri e Ricerca**:
  - Ricerca rapida in tempo reale per Username o Note.
  - Filtro dinamico per Profili HotSpot.

---

## 📐 Specifiche Tecniche e Architettura del Codice

### 🏗️ Architettura Software (MVVM + Clean Architecture Pattern)
L'applicazione segue i principi di **Modern Android Development (MAD)**:

```
com.example.hotspot
├── data
│   ├── local
│   │   └── ConnectionPreferences.kt      # Salvataggio credenziali su SharedPreferences
│   ├── model
│   │   └── HotspotUser.kt                # Classi dati (HotspotUser, HotspotProfile, ConnectionConfig)
│   ├── remote
│   │   ├── MikrotikSocketClient.kt       # Client socket nativo protocollo API MikroTik
│   │   └── MikrotikRestClient.kt         # Client HTTP/REST per RouterOS v7
│   └── repository
│       └── HotspotRepository.kt          # Repository unico con gestione fallback AUTO
├── ui
│   ├── components
│   │   ├── ConnectionConfigDialog.kt     # Dialog configurazione IP/Porta/Auth
│   │   ├── DeleteConfirmDialog.kt        # Dialog conferma eliminazione
│   │   ├── UserCard.kt                   # Componente UI card utente
│   │   └── UserFormDialog.kt             # Dialog creazione/modifica utente
│   ├── screens
│   │   └── MainHotspotScreen.kt          # Schermata principale Dashboard & Lista
│   ├── theme
│   │   ├── Color.kt, Theme.kt, Type.kt   # Temi Material 3
│   └── viewmodel
│       └── HotspotViewModel.kt           # Management dello Stato UI (StateFlow + Coroutines)
└── MainActivity.kt                       # Activity di ingresso dell'applicazione
```

---

### 🌐 Specifiche di Rete e Protocolli MikroTik

#### 1. RouterOS Socket API (`MikrotikSocketClient.kt`)
- Connessione diretta via `java.net.Socket` sulla porta configurata (Default `8278`).
- **Codifica della lunghezza delle parole (*Sentence Length Encoding*)**:
  - $L < 128$: 1 byte
  - $L < 16384$: 2 byte (`0x80 | (L >> 8)`, `L & 0xFF`)
  - $L < 2097152$: 3 byte (`0xC0 | (L >> 16)`, ...)
  - $L < 268435456$: 4 byte (`0xE0 | (L >> 24)`, ...)
- **Autenticazione**:
  - *RouterOS >= 6.43*: Login diretto con parametri `=name=` e `=password=`.
  - *RouterOS legacy (< 6.43)*: Autenticazione a 2 fasi via challenge/response MD5 (`0x00 + password + challenge_hex`).
- **Comandi eseguiti**:
  - `/ip/hotspot/user/print`
  - `/ip/hotspot/user/profile/print`
  - `/ip/hotspot/user/add`
  - `/ip/hotspot/user/set`
  - `/ip/hotspot/user/remove`

#### 2. RouterOS v7 REST API (`MikrotikRestClient.kt`)
- Protocollo HTTP/HTTPS con **Basic Auth** (`Authorization: Basic base64(user:pass)`).
- Parsing payload JSON (`org.json.JSONObject` e `org.json.JSONArray`).
- **Endpoint HTTP**:
  - `GET /rest/ip/hotspot/user`
  - `GET /rest/ip/hotspot/user/profile`
  - `PUT /rest/ip/hotspot/user`
  - `PATCH /rest/ip/hotspot/user/{id}`
  - `DELETE /rest/ip/hotspot/user/{id}`

---

## 💻 Requisiti di Sistema e Compilazione

- **Min SDK**: `24` (Android 7.0 Nougat)
- **Target SDK**: `37` (Android 15)
- **JDK**: Java 11 / 17
- **Gradle**: Version Catalog (`libs.versions.toml`)

### Permessi Android (`AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<application android:usesCleartextTraffic="true" ... />
```

---

## 💡 Guida alla Connessione

1. **Rete Locale / Wi-Fi**:
   Inserisci l'indirizzo IP locale della MikroTik (es. `192.168.88.1`) e la porta `8278`.
2. **Emulatore Android Studio**:
   Se stai testando l'app sull'emulatore Android per connetterti alla MikroTik sulla rete locale del tuo PC, utilizza l'IP **`10.0.2.2`** anziché `127.0.0.1`.
3. **Configurazione MikroTik**:
   Assicurati che il servizio API o WWW sia attivo sulla porta 8278:
   ```routeros
   /ip service set api port=8278 disabled=no
   # oppur per la REST API di RouterOS v7:
   /ip service set www port=8278 disabled=no
   ```

---

## 🛠️ Come Compilare il Progetto

```bash
# Clona il repository
git clone https://github.com/mediamind-hass/HotSpot.git
cd HotSpot

# Compila l'APK Debug
./gradlew assembleDebug

# L'APK generato si troverà in:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licenza

Distribuito sotto licenza MIT. Per maggiori informazioni consulta il codice sorgente.
