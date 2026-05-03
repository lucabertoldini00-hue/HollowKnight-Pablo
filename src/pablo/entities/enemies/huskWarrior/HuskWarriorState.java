// HuskWarriorState.java

package pablo.entities.enemies.huskWarrior;

public enum HuskWarriorState
{
    IDLE,
    PATROL,
    TURNING,
    SHIELD_ANTICIPATE,   // 40ms flash on block
    SHIELD_FRONT,        // impact hold + recoil → retaliate
    SHIELD_TOP,          // pogo raise shield overhead
    SHIELD_TOP_BUMP,     // impact + hold → return to patrol
    ATTACK_WINDUP,       // attack1-4 slow buildup
    ATTACKING,           // attack5-8 three lightning strikes
    ATTACK_COOLDOWN,     // punish window — no blocking
    HIT_STOP,
    DEAD_AIR,
    DEAD_LAND
}