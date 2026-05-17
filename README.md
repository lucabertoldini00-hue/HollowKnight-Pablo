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

* # Bug Report — Integrazione nuove mappe nel gioco HollowKnight-Pablo

Questo documento descrive tutti i problemi riscontrati e risolti durante la sessione di debugging, in ordine cronologico.

---

## 1. Mappa incompatibile con il renderer — mappa infinita

### Cos'era
La prima mappa caricata in Tiled era stata creata con la modalità **Infinite** attiva. Questo produce un file TMX dove i dati del layer grafico sono divisi in **chunk** separati con encoding CSV, invece di un singolo blocco continuo.

### Causa
`OrthoCachedTiledMapRenderer` (e in generale il sistema di rendering di LibGDX) si aspetta una mappa a dimensione fissa con dati continui. La struttura a chunk della mappa infinita è incompatibile con questo renderer.

### Soluzione
In Tiled: `Map → Map Properties → togliere la spunta su "Infinite"`. Tiled converte automaticamente i chunk in un layer continuo compatibile con LibGDX.

---

## 2. Path assoluto nel tileset — portabilità zero

### Cos'era
Il file TMX referenziava l'immagine del tileset con un path assoluto del tipo:
```
../../Documenti/Code/Java/HollowKnightPabloDefV/assets/Maps/1.png
```

### Causa
Tiled, in alcune configurazioni, salva il path dell'immagine come percorso assoluto relativo alla posizione del file sul disco. Questo funziona solo sul computer dell'autore e rompe il progetto su qualsiasi altra macchina o in qualsiasi altra cartella.

### Soluzione
Spostare il file immagine del tileset (`1.png`) nella stessa cartella del file `.tmx` (`assets/Maps/`) e aggiornare il path nel TMX a semplicemente `1.png`. In Tiled questo si fa rimuovendo e reimportando il tileset con il file nella posizione corretta.

---

## 3. Layer Oggetti assente — crash immediato all'avvio del livello

### Cos'era
La prima versione della mappa non conteneva alcun layer di tipo **Object Layer**. Il codice in `LevelScreen.initialize()` chiamava:
```java
tma.getRectangleList("start").get(0);
```
che restituiva una lista vuota, causando un `IndexOutOfBoundsException`.

### Causa
Senza il layer Oggetti non esistono né l'oggetto `start` (posizione di spawn di Pablo) né gli oggetti `solido` (collisioni). Il gioco non può partire senza sapere dove piazzare il giocatore.

### Soluzione
Aggiungere in Tiled un **Object Layer** chiamato `"Oggetti"` contenente almeno:
- Un rettangolo `start` (con custom property `name = "start"`) per lo spawn del giocatore
- Rettangoli `solido` (con custom property `name = "solido"`) per pavimento, soffitto e muri

---

## 4. Oggetto `start` non trovato — `IndexOutOfBoundsException`

### Cos'era
Anche dopo aver aggiunto il layer Oggetti, il gioco continuava a crashare con:
```
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
    at pablo.LevelScreen.initialize(LevelScreen.java:201)
```

### Causa
Il layer Oggetti aveva un **offset applicato** (`offsetx="272"`) e gli oggetti avevano coordinate negative per compensarlo (es. `x="-96"` per `start`). LibGDX legge le coordinate degli oggetti così come sono scritte nel TMX, senza applicare l'offset del layer. Quindi l'oggetto risultava fuori dai bounds attesi o non veniva riconosciuto correttamente.

In aggiunta, il messaggio di errore originale (`IndexOutOfBoundsException`) era generico e non indicava chiaramente la causa.

### Soluzione
**In Tiled:** rimuovere l'offset dal layer Oggetti (`offsetx` riportato a 0) e correggere le coordinate degli oggetti usando valori assoluti positivi.

**Nel codice (`LevelScreen.java`):** sostituire la chiamata raw con una versione robusta che lancia un errore esplicito:
```java
ArrayList<MapObject> startPoints = tma.getRectangleList("start");
if (startPoints.isEmpty())
    throw new IllegalStateException(
        "[LevelScreen] Nessun oggetto 'start' trovato in: " + mapPath);
MapObject startPoint = startPoints.get(0);
```

---

## 5. FalseKnight obbligatorio — crash su mappe senza boss

### Cos'era
In `LevelScreen.initialize()` era presente questo codice:
```java
if (falseKnightPoints.isEmpty())
    throw new IllegalStateException("Missing FalseKnight spawn object in map.");
```
Questo rendeva impossibile caricare qualsiasi mappa che non contenesse il boss FalseKnight.

### Causa
Il codice originale era scritto per la sola `mapPablo2.tmx`, l'unica mappa del gioco che contiene il boss. Non era stato progettato per essere riutilizzato su mappe diverse.

### Soluzione
In `LevelScreen.java` il FalseKnight è stato reso **opzionale**:
```java
if (!falseKnightPoints.isEmpty()) {
    falseKnight = new FalseKnight(...);
    falseKnight.setScreenShakeCallback(() -> shakeDuration = 0.15f);
} else {
    Gdx.app.log("LevelScreen", "FalseKnight non presente — skip.");
}
```
Tutte le righe di `update()` che usano `falseKnight` sono state protette con un controllo `if (falseKnight != null && !falseKnight.isDead())`.

---

## 6. Path mappa hardcoded — impossibile caricare mappe diverse

### Cos'era
In `LevelScreen.initialize()` il percorso della mappa era scritto direttamente nel codice:
```java
TilemapActor tma = new TilemapActor("assets/Maps/mapPablo2.tmx", mainStage);
```
Cambiare mappa richiedeva di modificare il sorgente e ricompilare.

### Causa
Scelta di design originale pensata per un singolo livello.

### Soluzione
`LevelScreen` è stato reso **parametrico** aggiungendo un costruttore che accetta il path della mappa:
```java
public LevelScreen(String mapPath) {
    this.mapPath = mapPath;
}
```
Con un costruttore di default che punta a `mapPablo2.tmx` per retrocompatibilità. Da `MenuScreen` si chiama semplicemente:
```java
BaseGame.setActiveScreen(new LevelScreen("assets/Maps/miaMapNuova.tmx"));
```

---

## 7. `BufferOverflowException` — renderer non compatibile con mappe grandi

### Cos'era
Il gioco crashava al primo frame con:
```
java.nio.BufferOverflowException
    at com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer.renderTileLayer(...)
```

### Causa
`OrthoCachedTiledMapRenderer` pre-carica **tutta** la mappa in un buffer di vertici di dimensione fissa. La mappa da 1920×1072 px con molti tile supera la capacità del buffer, causandone il traboccamento.

### Soluzione
Sostituire in `TilemapActor.java` il renderer con `OrthogonalTiledMapRenderer`, che renderizza on-the-fly senza buffer fisso:
```java
// Prima
private OrthoCachedTiledMapRenderer tiledMapRenderer;
tiledMapRenderer = new OrthoCachedTiledMapRenderer(tiledMap);

// Dopo
private OrthogonalTiledMapRenderer tiledMapRenderer;
tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
```
`OrthogonalTiledMapRenderer` non ha limitazioni di dimensione e funziona con mappe di qualsiasi grandezza.

---

## 8. Sprite distorti orizzontalmente — viewport sbagliato

### Cos'era
Tutti gli sprite del gioco apparivano **schiacciati/allargati** orizzontalmente. Ad esempio Pablo, che ha sprite quadrati (121×121 px), appariva chiaramente più largo che alto.

### Causa
`BaseScreen` usava `StretchViewport(800, 640)`. Questo viewport forza il mondo di gioco (rapporto **5:4**) a occupare l'intero schermo (rapporto **16:9**), applicando scale diverse in X e in Y:
```
Scala X: 1920 / 800 = 2.40
Scala Y: 1080 / 640 = 1.69
```
La differenza tra i due fattori di scala distorce ogni sprite.

### Soluzione
Sostituire `StretchViewport` con `ExtendViewport` in `BaseScreen.java`:
```java
// Prima
mainStage = new Stage(new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT));

// Dopo
mainStage = new Stage(new ExtendViewport(WORLD_WIDTH, WORLD_HEIGHT));
```
`ExtendViewport` preserva le proporzioni originali degli sprite e, se lo schermo è più largo del mondo di gioco, **estende** la visuale orizzontalmente invece di deformare. Niente bande nere, niente distorsione.

---

## 9. Logging assente — debug impossibile

### Cos'era
Quando un oggetto non veniva trovato nella mappa, l'unico messaggio era un `IndexOutOfBoundsException` senza contesto. Non era possibile capire rapidamente quale oggetto mancasse o in quale mappa.

### Causa
`TilemapActor.getRectangleList()` restituiva silenziosamente una lista vuota senza alcuna segnalazione.

### Soluzione
In `TilemapActor.java` sono stati aggiunti due meccanismi di logging:

1. **Log al caricamento** — stampa tutti gli oggetti trovati in ogni layer:
```java
private void logAllObjects() {
    Gdx.app.log("TilemapActor", "--- Oggetti nella mappa " + loadedPath + " ---");
    for (MapLayer layer : tiledMap.getLayers()) { ... }
}
```

2. **Warning quando un tipo non viene trovato:**
```java
if (list.isEmpty())
    Gdx.app.log("TilemapActor", "ATTENZIONE: nessun oggetto '"
        + propertyName + "' trovato in " + loadedPath);
```

Questo ha permesso di identificare immediatamente che `start` e `solido` non venivano riconosciuti, e di ricondurre il problema all'offset del layer Oggetti.

---

## Riepilogo

| # | Bug | File modificato | Tipo |
|---|-----|-----------------|------|
| 1 | Mappa infinita incompatibile | `Senza_Titolo.tmx` (Tiled) | Configurazione |
| 2 | Path assoluto tileset | `Senza_Titolo.tmx` (Tiled) | Configurazione |
| 3 | Layer Oggetti assente | `Senza_Titolo.tmx` (Tiled) | Configurazione |
| 4 | Oggetto `start` non trovato | `LevelScreen.java`, `TilemapActor.java` | Bug + Configurazione |
| 5 | FalseKnight obbligatorio | `LevelScreen.java` | Design |
| 6 | Path mappa hardcoded | `LevelScreen.java` | Design |
| 7 | BufferOverflowException | `TilemapActor.java` | Bug |
| 8 | Sprite distorti | `BaseScreen.java` | Bug |
| 9 | Logging assente | `TilemapActor.java` | Miglioramento |
