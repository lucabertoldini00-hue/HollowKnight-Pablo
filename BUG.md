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