package com.fadikazoura.nextrep.progression;

public enum ProgressionAction {
    /** No history yet: the user picks a starting weight. */
    START,
    /** Hit the top of the rep range on every set: add weight. */
    INCREASE,
    /** Not there yet: repeat the same weight. */
    HOLD,
    /** Stalled several times in a row: drop the weight and build back up. */
    DELOAD
}
