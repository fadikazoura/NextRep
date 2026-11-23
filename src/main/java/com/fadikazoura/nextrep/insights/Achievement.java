package com.fadikazoura.nextrep.insights;

/**
 * The badges a user can earn. Each one is a threshold on a number that only ever grows
 * (workouts done, longest streak, heaviest set, ...), so a badge, once earned, stays earned.
 */
public enum Achievement {
    FIRST_WORKOUT(Metric.WORKOUTS, 1),
    WORKOUTS_10(Metric.WORKOUTS, 10),
    WORKOUTS_25(Metric.WORKOUTS, 25),
    WORKOUTS_50(Metric.WORKOUTS, 50),
    WORKOUTS_100(Metric.WORKOUTS, 100),
    PERFECT_WEEK(Metric.PERFECT_WEEKS, 1),
    STREAK_4(Metric.LONGEST_STREAK, 4),
    STREAK_12(Metric.LONGEST_STREAK, 12),
    SETS_500(Metric.SETS, 500),
    CLUB_100(Metric.HEAVIEST_SET_KG, 100),
    BIG_SESSION(Metric.BEST_SESSION_VOLUME_KG, 10_000);

    enum Metric { WORKOUTS, PERFECT_WEEKS, LONGEST_STREAK, SETS, HEAVIEST_SET_KG, BEST_SESSION_VOLUME_KG }

    final Metric metric;
    final int target;

    Achievement(Metric metric, int target) {
        this.metric = metric;
        this.target = target;
    }
}
