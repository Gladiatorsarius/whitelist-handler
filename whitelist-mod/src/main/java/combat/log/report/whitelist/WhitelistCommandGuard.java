package combat.log.report.whitelist;

/**
 * Guards against reacting to mod-initiated whitelist commands.
 */
public final class WhitelistCommandGuard {
    private static final ThreadLocal<Boolean> IGNORE_ADD = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> IGNORE_REMOVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WhitelistCommandGuard() {
    }

    public static boolean isIgnoringAdd() {
        return Boolean.TRUE.equals(IGNORE_ADD.get());
    }

    public static boolean isIgnoringRemove() {
        return Boolean.TRUE.equals(IGNORE_REMOVE.get());
    }

    public static void runIgnoringAdd(Runnable action) {
        IGNORE_ADD.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            IGNORE_ADD.set(Boolean.FALSE);
        }
    }

    public static void runIgnoringRemove(Runnable action) {
        IGNORE_REMOVE.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            IGNORE_REMOVE.set(Boolean.FALSE);
        }
    }
}