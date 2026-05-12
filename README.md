# 📘 README.md — HollowKnight-Pablo (LibGDX)

## 🎯 Obiettivo del progetto

Replica di un sistema di gioco in stile *Hollow Knight* utilizzando **LibGDX (LWJGL2)** con:

* Menu interattivo stile HK
* Intro video
* Sistema HP a maschere
* Sistema anima (Soul Vessel)
* Meccaniche avanzate player (cura, dash, doppio salto, invulnerabilità)

---

## ⚙️ Setup Tecnico

### 🔧 Ambiente

* Java 8
* LibGDX 1.9.7
* Backend: **LWJGL2**
* Gestione dipendenze: **JAR manuali (NO Gradle)**

### 📦 Librerie aggiunte

* `gdx.jar`
* `gdx-backend-lwjgl.jar`
* `gdx-freetype.jar`
* `gdx-video-1.0.0-anonl.jar`
* `gdx-video-desktop-1.0.0-anonl.jar`
* `slf4j-api-1.7.36.jar`
* `slf4j-simple-1.7.36.jar`

⚠️ **Nota importante**
Le librerie `gdx-video` devono essere della **stessa famiglia (anonl)** per evitare errori:

```
NoClassDefFoundError: VideoPlayerMesh
```

---

## 📁 Struttura Assets

```
assets/
│
├── intro.mp4
├── menu_bg.png
├── font.ttf
│
├── Pablo/
│   ├── Dash/
│   │   └── dash1.png ... dash5.png
│   ├── Cura/
│   │   └── heal1.png ... heal6.png
│
├── soul/
│   ├── 9.png                (vuoto)
│   ├── 1_1.png ... 1_6.png
│   ├── ...
│   └── 8_1.png ... 8_6.png
│
├── mask.png
```

---

## 🎬 Intro Video

### ✔ Implementazione

* `IntroScreen.java`
* Usa `VideoPlayerDesktop`
* Skip con input utente
* Transizione automatica al menu

### ⚠ Problemi comuni

| Errore                 | Causa           |
| ---------------------- | --------------- |
| `Could not find file`  | Path sbagliato  |
| `UnsatisfiedLinkError` | FFmpeg mancante |

---

## 🧭 Menu System

### ✔ Features

* Layout stile Hollow Knight
* Pulsanti:

  * **Start Game**
  * **Options (placeholder)**
  * **Exit**

### ✔ Navigazione

* Mouse ✔
* Tastiera ✔ (W/S o ↑/↓ + ENTER)

### ✔ UI Behavior

* Pulsante selezionato evidenziato
* “Start Game” più grande
* Posizionamento bottom-right

---

## 🖥️ Viewport & Fullscreen Fix

### ✔ Soluzione finale

| Stage     | Viewport                | Scopo    |
| --------- | ----------------------- | -------- |
| mainStage | `FitViewport(800, 640)` | Gameplay |
| uiStage   | `ScreenViewport()`      | UI       |

### ✔ Resize fix

```java
public void resize(int width, int height) {
    mainStage.getViewport().update(width, height, true);
    uiStage.getViewport().update(width, height, true);
}
```

---

## ❤️ Sistema HP (Maschere)

### ✔ Implementazione

* 5 maschere (configurabili)
* Stato:

  * piena → bianca
  * vuota → scura

### ✔ Regole

* 1 maschera = 1 HP
* HP = 0 → morte

---

## 🔮 Sistema Anima (Soul Vessel)

### ✔ Meccanica

* Max: 99 anima
* Guadagno:

  * colpendo nemici (+33)

### ✔ Cura

* Costo: 33 anima
* Tasto: `F`

---

## 🎨 Vessel Animato

### ✔ Struttura

* 9 stati:

  * `0` → vuoto (`9.png`)
  * `1-8` → animazioni (6 frame ciascuna)

### ✔ Mapping anima → stato

```
0       → stato 0
1-12    → stato 1
...
87-99   → stato 8
```

### ✔ Animazione

* Loop continuo (~12 FPS)
* Reset quando cambia stato

---

## 🛠 Debug

### ✔ Tasti

| Tasto | Azione    |
| ----- | --------- |
| L     | +33 anima |
| F     | Cura      |

---

## 🧠 Sistema Cura

### ✔ Comportamento

* NON istantanea
* Usa animazione (`heal1 → heal6`)
* HP aumentato **solo a fine animazione**

### ✔ Interruzioni

* Se colpito:

  * cura annullata
  * anima consumata comunque

---

## 🧍 Player Mechanics

### ✔ Stati (`PlayerState`)

* IDLE
* WALK
* JUMP
* HEALING
* DASH

---

## ⚡ Dash

* Tasto: `SHIFT`
* Durata: 0.18s
* Cooldown: 0.6s
* Invulnerabilità: ✔
* Direzione bloccata: ✔

---

## 🦘 Doppio Salto

* 1 extra salto in aria
* Reset su atterraggio

---

## ⛔ Invulnerabilità

### ✔ Dopo danno

* Durata: 1.5s
* Effetto:

  * lampeggio
  * niente danni multipli

---

## ❄ Hitstop

* Durata: 0.12s
* Congela solo il player

---

## ⚠ Problemi Risolti

### ❌ Input tastiera non funzionava

✔ Fix:

* Gestione in `keyDown()`
* InputMultiplexer corretto

---

### ❌ Fullscreen rotto

✔ Fix:

* Separazione viewport UI / game

---

### ❌ Asset non trovati

✔ Fix:

* Working directory corretta
* Path espliciti

---

### ❌ gdx-video errori

✔ Fix:

* Versioni compatibili (anonl)

---

## 🚧 TODO Futuri

* Options menu reale
* Sistema morte + respawn
* Nemici avanzati
* Effetti particellari (cura/danno)
* Audio system
* Salvataggi

---

## 🧩 Architettura

### File principali

| File               | Ruolo         |
| ------------------ | ------------- |
| `Pablo.java`       | Player logic  |
| `PlayerState.java` | FSM           |
| `MenuScreen.java`  | Menu          |
| `IntroScreen.java` | Video intro   |
| `LevelScreen.java` | UI + gameplay |
| `BaseScreen.java`  | Framework     |

---

## 🗺️ Sistema Multi‑Mappa

### ✔ Come funziona

* La logica delle connessioni è in `MapGraph.java`.
* `LevelScreen` legge il nodo corrente e, quando Pablo supera il bordo sinistro/destrro, carica la mappa vicina.
* Lo stato del player (HP/anima) viene salvato con `PlayerState.saveFrom()` e ripristinato con `PlayerState.applyTo()` durante la transizione.

### ✔ Aggiungere una nuova mappa

1. Crea il `.tmx` in `assets/Maps/` (es. `Mappa4.tmx`).
2. In Tiled, aggiungi nel layer “Oggetti”:
   * `start` (rettangolo spawn iniziale)
   * `solido` (rettangoli o poligoni per collisioni)
3. Registra la mappa in `MapGraph.java` e collega i vicini:

```java
MapNode map4 = new MapNode("assets/Maps/Mappa4.tmx");
map3.rightNeighbor = map4.path;
map4.leftNeighbor  = map3.path;
NODES.put(map4.path, map4);
```

### ✔ Oggetti opzionali per boss/teleport

* `tp` → trigger dialogo (interazione con `E`)
* `spawntp` → punto di teletrasporto del player
* `FalseKnight` → spawn del boss

### ✔ Note pratiche

* I nomi oggetto sono case‑insensitive (usa comunque gli stessi nomi per coerenza).
* Se una mappa non ha vicini registrati, non avviene transizione.
* Il ricettacolo (anima) non viene resettato al cambio mappa.

---

## 💡 Note Finali

* Stato progetto: **mid-prototype avanzato**
* Core loop già funzionante
* Base solida per espansione gameplay
