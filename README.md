# 📶 MikroTik HotSpot Manager (Android)

Un'applicazione Android moderna scritta in **Kotlin** e **Jetpack Compose** (Material 3) per connettersi a router e antenne **MikroTik RouterOS** (via Socket API binaria e REST API v7) per gestire in tempo reale gli utenti dell'HotSpot.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![MikroTik](https://img.shields.io/badge/MikroTik-RouterOS-EF2D2D?style=for-the-badge)

---

## 🌟 Caratteristiche Principali

- **🔌 Connessione Flessibile e Rilevamento Automatico (`AUTO`)**:
  - Supporta sia la **Socket API binaria** (Porta `8728` / `8729` SSL / `8278`) sia la **REST API** di RouterOS v7 (Porta `80` / `443` HTTPS).
  - **Modalità Rilevamento Automatico (`AUTO`)**: Prova intelligentemente e velocemente la porta personalizzata e le porte standard `80`, `8728`, `8729` (SSL) e `443` (HTTPS) con timeout ottimizzati.
  - **Auto-Reconnect su "Broken Pipe"**: Gestione automatica della riconnessione trasparente in caso di timeout di inattività del socket sul router.
  - **Compatibilità Estesa per Smartphone (Samsung One UI 6 / 7 / 9 / Android 14/15/16)**:
    - Binding automatico e fallback ibrido sull'interfaccia Wi-Fi per reti locali senza connessione Internet.
    - Sblocco dei vincoli sandbox per le chiamate HTTP/REST chiare e rilevamento preciso degli errori di autenticazione `HTTP 401`.
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
│   │   └── ConnectionPreferences.kt      # Salvataggio credenziali e porta attiva su SharedPreferences
│   ├── model
│   │   └── HotspotUser.kt                # Classi dati e AuthenticationException
│   ├── remote
│   │   ├── MikrotikSocketClient.kt       # Client socket nativo protocollo API MikroTik con auto-retry
│   │   └── MikrotikRestClient.kt         # Client HTTP/REST per RouterOS v7 con fix PATCH e SSL TrustAll
│   └── repository
│       └── HotspotRepository.kt          # Repository unico con fallback avanzato AUTO su porte standard
├── ui
│   ├── components
│   │   ├── ConnectionConfigDialog.kt     # Dialog configurazione IP/Porta/Auth con chip porte veloci
│   │   ├── DeleteConfirmDialog.kt        # Dialog conferma eliminazione
│   │   ├── UserCard.kt                   # Componente UI card utente
│   │   └── UserFormDialog.kt             # Dialog creazione/modifica utente
│   ├── screens
│   │   └── MainHotspotScreen.kt          # Schermata principale Dashboard & Lista
│   ├── theme
│   │   ├── Color.kt, Theme.kt, Type.kt   # Temi Material 3
│   └── viewmodel
│       └── HotspotViewModel.kt           # Gestione dello Stato UI (StateFlow + Coroutines + Routing Ibrido)
├── util
│   └── NetworkUtils.kt                   # Forzatura interfaccia Wi-Fi e gestione unbind per Android 14+
└── MainActivity.kt                       # Activity principale con prompt autorizzazioni runtime
```

---

### 🌐 Specifiche di Rete e Protocolli MikroTik

#### 1. RouterOS Socket API (`MikrotikSocketClient.kt`)
- Connessione diretta via `java.net.Socket` sulla porta configurata (Standard `8728`, SSL `8729`, Custom `8278`).
- **Gestione Trasparente "Broken Pipe" (`executeWithRetry`)**:
  - Intercetta i socket chiusi dal router per inattività e riapre la connessione in ~50ms.
  - Utilizza `readAllSentencesUntilDone()` per svuotare completamente il buffer del socket.
- **Autenticazione**:
  - *RouterOS >= 6.43*: Login diretto con parametri `=name=` e `=password=`.
  - *RouterOS legacy (< 6.43)*: Challenge/Response MD5.

#### 2. RouterOS v7 REST API (`MikrotikRestClient.kt`)
- Protocollo HTTP/HTTPS con **Basic Auth** (`Authorization: Basic base64(user:pass)`).
- **Fix HTTP Method `PATCH`**: Bypass delle limitazioni di `HttpURLConnection` per l'aggiornamento/disabilitazione utenti.
- **Riconoscimento HTTP 401**: Notifica istantanea in caso di credenziali invalide senza scartare la porta.
- **Endpoint HTTP**:
  - `GET /rest/ip/hotspot/user`
  - `GET /rest/ip/hotspot/user/profile`
  - `PUT /rest/ip/hotspot/user`
  - `PATCH /rest/ip/hotspot/user/{id}`
  - `DELETE /rest/ip/hotspot/user/{id}`

---

## 💻 Requisiti di Sistema e Compilazione

- **Min SDK**: `24` (Android 7.0 Nougat)
- **Target SDK**: `35` (Android 15 / One UI 9 Compatible)
- **Compile SDK**: `37`
- **JDK**: Java 11 / 17
- **Gradle**: Version Catalog (`libs.versions.toml`)

### Permessi Android (`AndroidManifest.xml`)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

---

## 💡 Guida alla Connessione

1. **Rete Locale / Wi-Fi**:
   Inserisci l'indirizzo IP locale della MikroTik (es. `192.168.88.1` o `192.168.16.120`).
2. **Emulatore Android Studio**:
   Utilizza l'IP **`10.0.2.2`** per connetterti al router sulla rete del PC.
3. **Porte Standard MikroTik**:
   - **`80`**: REST API HTTP (Consigliata per RouterOS v7)
   - **`8728`**: Socket API Plain
   - **`8729`**: Socket API SSL
   - **`8278`**: Porta personalizzata
4. **Configurazione Servizi in RouterOS**:
   ```routeros
   /ip service set api port=8728 disabled=no
   /ip service set www port=80 disabled=no
   ```

---

## 🛠️ Come Compilare il Progetto

```bash
# Clona il repository
git clone https://github.com/mediamind-hass/HotSpot.git
cd HotSpot

# Compila l'APK Debug
./gradlew assembleDebug

# Installa direttamente sul dispositivo collegato
./gradlew :app:installDebug
```

---

## 📄 Licenza

Distribuito sotto licenza MIT. Per maggiori informazioni consulta il codice sorgente.
