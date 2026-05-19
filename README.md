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

---

## ✅ Aggiornamenti recenti (sessione)

* Fix compilazione: `edgeAheadHasground` e aggiunta `Enemy.isDead()` per filtrare danni da cadaveri.
* Player QoL: salto a altezza variabile con rilascio anticipato del tasto.
* Nemici: dissolvenza morte (3s + fade 1s) e niente danni da hitbox quando sono morti.
* FalseKnight: ora riceve danno dai colpi del player; colpi al player = 5 danni.
* Respawn: alla morte il player torna allo spawn (fuori boss fight).
* Boss fight: sequenza morte con schermo nero, testo ingrandito, `se.jpg` 1s prima dell’uscita.
* HuskWarrior: hitbox ridotta con boundary polygon dedicato.
* Debug mapping: tasto `9` dalla prima mappa → ultima mappa.
* UI prompt/dialogo boss: stile menu pausa, sfondo scuro, testo verde, prompt nascosto fuori trigger.

---

## 🐞 Bug Report (merge da BUG.md)

# Bug Report — Integrazione nuove mappe nel gioco HollowKnight-Pablo

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
Aggiungere in Tiled un **Object Layer** chiamato "Oggetti" contenente almeno:
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
```
[TilemapActor] Layer Oggetti — tileList:
  - [start]
  - [solido]
  - [Crawlid]
  - [Tiktik]
```

2. **Warning mirato** — se un nome non viene trovato, stampa un warning con la lista di nomi disponibili.

---

## 10. Tilemap con poligoni — collisioni non funzionanti

### Cos'era
Le collisioni con le forme poligonali create in Tiled non funzionavano, perché `LevelScreen` leggeva solo rettangoli con `getRectangleList("solido")`.

### Causa
Le forme poligonali vengono restituite da `getPolygonList()`, ma non erano gestite.

### Soluzione
In `LevelScreen.java` è stata aggiunta la creazione di `Object` solidi anche per i `PolygonMapObject`:
```java
if (obj instanceof PolygonMapObject)
    addSolidTriangles(((PolygonMapObject) obj).getPolygon());
```
Con triangolazione tramite `EarClippingTriangulator` per ottenere poligoni convexi.

---

## 11. Pablo rimane bloccato sui bordi con mappe multiple

### Cos'era
Pablo non poteva uscire dai bordi X della mappa per triggerare la transizione, perché `boundToWorld()` clampava sempre la posizione.

### Causa
La funzione `boundToWorld()` in `Pablo.java` non distingueva tra gameplay normale e transizione mappa.

### Soluzione
Aggiunto un flag `allowMapTransition` in `Pablo`:
```java
public void setAllowMapTransition(boolean allow)
```
E in `boundToWorld()`:
```java
if (!allowMapTransition) {
    if (getX() < 0) setX(0);
    if (getX() + getWidth() > worldBounds.width) setX(worldBounds.width - getWidth());
}
```
Durante le transizioni, `LevelScreen` abilita il flag.

---

## 12. HUD anima resetta durante cambio mappa

### Cos'era
Il ricettacolo dell’anima veniva resettato quando si entrava in una nuova mappa.

### Causa
`LevelScreen` ricreava il `Pablo` con `new Pablo(...)` e non reapplicava l’anima.

### Soluzione
Introdotta la classe `PlayerState` per salvare/anima/HP:
```java
PlayerState.saveFrom(pablo);
BaseGame.setActiveScreen(new LevelScreen(node.rightNeighbor, "left"));
```
E `PlayerState.applyTo(pablo)` dopo lo spawn.

---

## 13. Input tastiera non intercettato dal menu

### Cos'era
Nel menu principale, i tasti `W/S` e `ENTER` non funzionavano.

### Causa
`MenuScreen` non aggiungeva l’input processor corretto.

### Soluzione
In `MenuScreen`, aggiunto:
```java
BaseGame.setInputProcessor(uiStage);
```

---

## 14. Audio menu non si riattiva dopo ritorno

### Cos'era
Dopo essere entrati nel gioco e tornati al menu, la musica non ripartiva.

### Causa
`MenuScreen` non rilanciava `SoundManager` in `initialize()`.

### Soluzione
In `MenuScreen.initialize()`:
```java
SoundManager.get().playMusic(Music.MENU);
```

---

## 15. Errore “VideoPlayerMesh” all’avvio

### Cos'era
Crash immediato con:
```
NoClassDefFoundError: VideoPlayerMesh
```

### Causa
Le librerie `gdx-video` erano miste tra `gdx-video-1.0.0.jar` e `gdx-video-1.0.0-anonl.jar`.

### Soluzione
Usare solo la variante `anonl` per entrambe:
```
gdx-video-1.0.0-anonl.jar
gdx-video-desktop-1.0.0-anonl.jar
```

---

## 16. Collisione Pablo con poligoni — glitch a velocità alte

### Cos'era
Pablo attraversava i poligoni quando cadeva velocemente.

### Causa
La collisione veniva risolta solo con `preventOverlap`, che può fallire con alte velocità.

### Soluzione
Aggiunto `snapToGroundIfOverlapping()` per i nemici e una logica simile per il player.

---

## 17. Mappa non loggata correttamente — confusione in debug

### Cos'era
I log mostrano solo il path del TMX, senza indicare il nome effettivo della mappa.

### Soluzione
Aggiunto un log più esplicito in `LevelScreen`:
```java
Gdx.app.log("LevelScreen", "Caricamento mappa: " + effectiveMapPath);
```

---

## 18. Input Interazione boss non chiude dialogo

### Cos'era
Il dialogo boss restava aperto anche dopo la scelta.

### Soluzione
`closeBossDialog()` viene chiamato in entrambe le scelte `SI/NO`.

---

## 19. Enemy AI non attiva off-screen ma cade nel vuoto

### Cos'era
I nemici fuori camera restavano congelati in aria e poi “cadevano” di colpo quando entravano in camera.

### Soluzione
In `Enemy`, è stata aggiunta la fase di **settle** con gravità anche off-screen per i primi 0.5s.

---

## 20. Nessun supporto mappe in `MapGraph`

### Cos'era
Non esisteva alcuna struttura formale per gestire mappe multiple.

### Soluzione
Aggiunto `MapGraph` come registro statico per tutte le mappe e i loro vicini.

---

## 21. Trigger boss in mappe senza boss

### Cos'era
Il trigger boss veniva creato anche in mappe non connesse al boss, generando prompt inutili.

### Soluzione
Se non esiste un oggetto `tp` o `FalseKnight`, il prompt non viene creato.

---

## 22. Errori di collisione su asset troppo grandi

### Cos'era
Gli sprite con dimensioni diverse causavano hitbox inconsistenti.

### Soluzione
`Enemy.setAnimation()` ora richiama `setBoundaryRectangle()` ad ogni cambio frame.

---

## 23. Hitbox attacco Pablo non corretta dopo loadTexture

### Cos'era
La hitbox del colpo di Pablo era troppo grande perché `loadTexture()` resettava la size del `Hitbox`.

### Soluzione
Dopo `loadTexture()` la size viene reimpostata manualmente nella classe `Hitbox`.

---

## 24. Sistema Void Respawn non consistente

### Cos'era
Cadendo nel vuoto, il respawn non sempre avveniva o avveniva subito.

### Soluzione
Aggiunto `voidTimer` con `VOID_RESPAWN_DELAY` e reset pulito.

---

## 25. Boss Fight: mancata animazione di morte nemici

### Cos'era
I nemici scomparivano immediatamente alla morte senza animazione di cadavere.

### Soluzione
Rimosso il `remove()` immediato in `Enemy.takeDamage()` e aggiunta dissolvenza dopo 3s.

---

## 26. AI: edge sensor non funzionante

### Cos'era
I nemici camminavano nel vuoto perché `edgeAheadHasground()` non veniva trovato (errore di compilazione).

### Soluzione
Corretto il nome del metodo (case sensitive) e uniformato in tutte le classi.

---

## 27. Pablo non torna alla mappa iniziale dopo morte

### Cos'era
La morte del player non resettava correttamente la posizione.

### Soluzione
Aggiunto `respawn()` in `Pablo` e chiamato in `LevelScreen`.

---

## 28. FalseKnight non riceveva danno

### Cos'era
Il boss non veniva colpito perché non estende `Enemy`.

### Soluzione
`Hitbox` ora gestisce anche `FalseKnight` separatamente.

---

## 29. HuskWarrior hitbox troppo grande

### Cos'era
Il player veniva colpito anche quando era visivamente distante.

### Soluzione
Ridotta hitbox con `setBoundaryPolygon()` e offset personalizzati.

---

## 30. Prompt “Premi E” sempre visibile

### Cos'era
Il pannello del prompt boss restava visibile anche fuori dal trigger.

### Soluzione
Toggle di visibilità sulla `Table` completa quando `pablo` esce dall’area.

---

## 31. Boss dialog e prompt con stile diverso dal menu

### Cos'era
Il prompt boss e il dialogo non erano in linea con lo stile del menu pausa.

### Soluzione
Introdotti font Trajan + colori verdi + pannello scuro e decorazioni.

---

## 32. Boss death sequence mancante

### Cos'era
In boss fight, la morte portava al respawn immediato.

### Soluzione
Sequenza: schermo nero → testo → `se.jpg` → uscita.

---

## 33. Danno del boss troppo basso

### Cos'era
Il FalseKnight infliggeva solo 1 danno.

### Soluzione
Danno impostato a 5.

---

## 34. Teleport debug tra mappe

### Cos'era
Nessun comando rapido per saltare all’ultima mappa.

### Soluzione
Tasto `9` dalla prima mappa → ultima mappa.

---

## 35. Jump variabile (QoL)

### Cos'era
Il salto aveva altezza fissa e poco responsiva.

### Soluzione
Rilascio anticipato del tasto riduce la velocità verticale per salti corti.

---

## 36. Danni da nemici morti

### Cos'era
I cadaveri continuavano a danneggiare il player.

### Soluzione
Blocco danni quando `Enemy.isDead()`.


