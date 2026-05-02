# 🧠 DOCUMENTAZIONE TECNICA — Evoluzione del Progetto HollowKnight-Pablo

## 🎯 Obiettivo iniziale

Il progetto nasce con due richieste principali:

* Migliorare il **menu principale** in stile *Hollow Knight*
* Aggiungere un **video intro** prima del menu

Queste due feature hanno portato a modifiche strutturali profonde nel progetto.

---

## 🎬 Sistema Intro Video

### Problema

LibGDX non supporta facilmente la riproduzione video, soprattutto:

* senza Gradle
* con LWJGL2
* usando JAR manuali

### Soluzione

Utilizzo della libreria `gdx-video`

### Issue critico

```java
NoClassDefFoundError: VideoPlayerMesh
```

### Causa

Mismatch tra:

* core library
* desktop backend

### Fix

Allineamento completo su:

* `gdx-video-1.0.0-anonl`
* `gdx-video-desktop-1.0.0-anonl`

### Risultato

* Video riprodotto correttamente
* Skip con input
* Transizione automatica al menu

---

## 🧭 Menu System

### Problema iniziale

* Nessuna navigazione da tastiera

### Soluzione

Implementazione manuale:

* `selectedIndex`
* gestione `keyDown()`

### Controlli

* W / ↑ → su
* S / ↓ → giù
* ENTER → selezione

### Risultato

Sistema di navigazione completo e controllabile

---

## 🖥️ Fullscreen & Viewport

### Problema

* UI deformata
* Gameplay scalato male
* Risoluzione non adattiva

### Root cause

Uso di:

```java
new Stage()
```

### Soluzione architetturale

#### Gameplay

```java
FitViewport(800, 640)
```

#### UI

```java
ScreenViewport()
```

### Risultato

* Gameplay scalato correttamente
* UI sempre precisa
* Supporto fullscreen stabile

---

## 🎮 Input System

### Problema

* Input tastiera ignorato

### Causa

`InputMultiplexer` con ordine errato

### Fix

* Gestione input in `BaseScreen`
* Uso diretto di `keyDown()`

---

## ❤️ Sistema HP (Maschere)

### Implementazione

* 5 maschere
* 1 maschera = 1 HP

### Stati

* piena → bianca
* vuota → scura

### Regola

HP = 0 → morte

---

## 🔮 Sistema Anima

### Meccanica

* Max: 99
* Guadagno: +33 colpendo nemici

### Cura

* Costo: 33
* Input: `F`

### Loop gameplay

* attacco → accumulo
* rischio → cura

---

## 🎨 Soul Vessel Animato

### Evoluzione

1. ProgressBar → scartata
2. Immagini statiche → migliorate
3. Animazioni multi-stato → soluzione finale

---

### Struttura

* 9 stati:

  * 0 → vuoto
  * 1–8 → livelli anima

* Ogni stato:

  * 6 frame animati

---

### Logica

1. anima → stato
2. cambio stato → reset animazione
3. animazione loop nel tempo

---

## 🛠 Debug System

### Tasto

* `L` → +33 anima

### Scopo

* testing rapido
* bilanciamento

---

## 🧠 Sistema Cura

### Caratteristiche

* non istantanea
* animazione dedicata
* effetto a fine animazione

### Regole

* anima consumata subito
* HP aggiunto alla fine
* interruzione possibile

---

## 🧍 Player State Machine

### Stati

* IDLE
* WALK
* JUMP
* HEALING
* DASH

### Ruolo

Gestione centralizzata del comportamento del player

---

## ⚡ Dash

### Parametri

* veloce
* breve
* cooldown

### Feature

* invulnerabilità
* direzione bloccata

---

## 🦘 Doppio Salto

* 1 salto extra
* reset a terra

---

## ⛔ Invulnerabilità

### Dopo danno

* durata: 1.5s
* lampeggio
* no danni multipli

---

## ❄ Hitstop

* durata: 0.12s
* blocca solo il player

---

## 🔧 Problemi Risolti

### ✔ Dependency mismatch

→ fix gdx-video

### ✔ Fullscreen

→ separazione viewport

### ✔ Input

→ gestione manuale

### ✔ Assets

→ fix working directory

---

## 📊 Stato Attuale

### ✔ Sistema completo

* movimento avanzato
* cura
* risorse
* UI dinamica

### ✔ Architettura solida

* Screen system
* FSM
* Viewport separati

---

## 🧠 Conclusione

Il progetto ha evoluto da semplice UI a:

* sistema gameplay completo
* architettura scalabile
* base per action platformer

---

## 🚧 Prossimi Step Consigliati

* Combat system avanzato (knockback, feedback)
* AI nemici
* Camera dinamica
* Sistema morte/respawn
* Effetti visivi e audio

---
