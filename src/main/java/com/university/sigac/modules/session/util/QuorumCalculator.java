package com.university.sigac.modules.session.util;

/** RN-11: minimum deliberative quorum = (members × percentage / 100) + 1, never exceeding the total members. */
public final class QuorumCalculator {

    private QuorumCalculator() {
    }

    public static int required(int members, int percentage) {
        if (members <= 0) {
            return 0;
        }
        int r = (members * percentage) / 100 + 1;
        return Math.min(r, members);
    }

    public static boolean reached(int members, int present, int percentage) {
        return members > 0 && present >= required(members, percentage);
    }
}
