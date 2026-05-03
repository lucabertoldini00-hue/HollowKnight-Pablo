// HuskBullyState.java

package pablo.entities.enemies.huskBully;

public enum HuskBullyState
{
    IDLE,             // heavy breathing, no threat nearby
    PATROL,           // slow walk back and forth
    TURNING,          // turn1 + turn2, then flip and resume
    ATTACK_ANTICIPATE,// crouch wind-up → hold peak frame → leap
    ATTACK_LUNGE,     // airborne: lunge1 (ascending) / lunge2 (descending)
    ATTACK_COOLDOWN,  // punishable recovery on landing
    HIT_STOP,         // brief freeze before death launch
    DEAD_AIR,         // knocked into air, spinning
    DEAD_LAND         // tumble frames 1-7, then hold corpse frame 8
}