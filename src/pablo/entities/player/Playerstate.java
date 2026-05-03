// Playerstate.java
package pablo.entities.player;

public enum Playerstate
{
    IDLE,
    WALKING,
    JUMPING,
    FALLING,
    LANDING,
    ATTACKING,
    DASHING,   // aggiunto: dash orizzontale con invulnerabilità
    HEALING    // aggiunto: animazione di cura (tasto F)
}