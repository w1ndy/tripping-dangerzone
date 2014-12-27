package autograder;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

import org.junit.runner.*;
import org.junit.runner.notification.*;

import autograder.AGCategories.AGTestDetails;


public class SuccessListener extends RunListener {

    private static final int MAX_CAUSE_DEPTH = 5;

    private final PrintStream psLogPrivate, psLogPublic;
    private final Map<Description, Failure> tests;

    private Description activeTest;
    private Failure activeFailure;

    public float pointsPossible = 0;
    public float pointsLost = 0;
    public float pointsEarned = 0;
    public int testsPossible = 0;
    public int testsEarned = 0;
    public int testsLost = 0; // Silly, but maybe handy for redundancy?
    public int extraPossible = 0;
    public int extraEarned = 0;

    public SuccessListener(Request request, PrintStream psPriv, PrintStream psPub) {
        psLogPrivate = psPriv;
        psLogPublic = psPub;
        // psLogPrivate.format("LISTENER: %d %s%n", request.getRunner()
        //     .testCount(), request.getRunner().getDescription());
        // psLogPrivate.format("LISTENER: %s%n", request.getRunner().getDescription());

        tests = Collections
        .synchronizedMap(new LinkedHashMap<Description, Failure>(
            request.getRunner().testCount()));
    }

    public static final Pattern[] classesAllowed = {
        Pattern.compile("kvstore.*"),
        Pattern.compile("java.*"), Pattern.compile("javax.*"),
        Pattern.compile("com.sun.org.*"),
    }; // Must match one of the Allowed but none of the Banned

    public static final Pattern[] classesBanned = {
        Pattern.compile("autograder.*"),
        Pattern.compile("java.security.*"),
        Pattern.compile("junit.*"), Pattern.compile("org.junit.*"),
        Pattern.compile("sun.reflect.*"), Pattern.compile("java.lang.reflect.*"),
    };

    public static boolean isVisibleClass(String fullName) {
        boolean maybe = false;
        for (Pattern pat: classesAllowed) {
            if (pat.matcher(fullName).matches()) {
                maybe = true;
                break;
            }
        }
        if (!maybe) return false;

        for (Pattern pat: classesBanned) {
            if (pat.matcher(fullName).matches()) return false;
        }
        return true;
    }

    public static String standardKVMessageFields(kvstore.KVMessage kve) {
        return String.format("[KVMessage: T=%s M=%s K=%s V=%s]",
            kve.getMsgType(), kve.getMessage(),
            kve.getKey(), kve.getValue());
    }

    public static void streamFlush(PrintStream ps) {
        ps.flush();
        try {
            Thread.sleep(1); // Workaround for eclipse output blending err/out
        } catch (InterruptedException e1) {}
    }

    public static void logError(PrintStream ps, boolean fullTrace, Throwable ex) {
        logError(ps, fullTrace, ex, 0);
    }

    protected static void logError(PrintStream ps, boolean fullTrace,
        Throwable ex, int causeDepth) {
        if ((ps != null) && (ex != null)) {
            if (ex instanceof AGException) {
                // Custom failure shows friendly name (perhaps more stuff later)
                ps.format("%s%n", ex);
            } else if (ex instanceof java.lang.AssertionError) {
                // A regular style JUnit error, show friendly message
                ps.format("ASSERT-FAILED: %s%n", ex.getMessage());
            } else {
                // Log exception basics, plus message in case toString() doesn't have it
                ps.format("EXCEPTION: %s [%s]%n", ex, ex.getMessage());
            }

            try { // Dump the visible pieces of the stack trace
                for (StackTraceElement ste: ex.getStackTrace()) {
                    if (fullTrace) {
                        ps.format("\t%s%n", ste);
                    } else if (isVisibleClass(ste.getClassName())) {
                        ps.format("  %s.%s(%s:%d)%n",
                            ste.getClassName(), ste.getMethodName(), ste.getFileName(),
                            ste.getLineNumber());
                    }
                    ps.flush();
                }
            } catch (Exception exIgnore) {}

            try { // Trace back the cause chain
                Throwable ecause = ex.getCause();
                if (ecause != null) {
                    if (causeDepth < MAX_CAUSE_DEPTH) {
                        ps.format(" CAUSE-%d: ", causeDepth + 1);
                        logError(ps, fullTrace, ecause, causeDepth + 1);
                    } else {
                        ps.println(" CAUSE CHAIN ABORTED.");
                    }
                }
            } catch (Exception exIgnore) {}
        }
        streamFlush(ps);
    }

    public static void logResult(PrintStream ps, boolean fullTrace, Description d, Failure f) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        if (details == null) {
            if (fullTrace) {
                ps.format("%nUNRECOGNIZED-%s: %s%n", (f == null)
                    ? "PASS"
                    : "FAIL", d.getDisplayName());
            }
            return;
        }

        if (f == null) {
            return;
        }
        ps.flush();
        ps.format("\n===> [FAILED] %s\n", d.getDisplayName());
        ps.flush();
        ps.format("TEST-DESCRIPTION: %s%n", details.desc());
        ps.flush();

        if (!(details.points() > 0)) {
            ps.format("*** This is an INFORMATIONAL test only; not included in points *** %n");
        }

        if (f != null) { // Was it a failed test???
            logError(ps, fullTrace, f.getException());
        }
    }

    public void logSummary(PrintStream ps, boolean fullTrace) {
        psLogPublic.flush();
        psLogPrivate.flush();
        String stats = String.format("Tests passed: %s", getStats(false));
        stats += String.format(" | Score: %s", getStats(true));

        for (Map.Entry<Description, Failure> e: tests.entrySet()) {
            Description d = e.getKey();
            Failure f = e.getValue();
            logResult(ps, fullTrace, d, f);
        }

        ps.println("\n=============================================================================\n\n" +
            "SUMMARY: " + stats + "\n");
    }

    @Override
    public void testRunStarted(Description d) {
        psLogPublic.flush();
        psLogPrivate.flush();
        activeTest = null;
    }

    @Override
    public void testRunFinished(Result result) {
        psLogPublic.flush();
        psLogPrivate.flush();

        try { Thread.sleep(100); } catch (InterruptedException ie) {  }

        logSummary(psLogPublic, false);
    }

    @Override
    public void testIgnored(Description d) {
        psLogPublic.flush();
        psLogPrivate.flush();
        psLogPrivate.format("TEST-SKIP: %s%n", d);
    }

    @Override
    public void testStarted(Description d) {
        psLogPublic.flush();
        psLogPrivate.flush();
        psLogPublic.format("Running test %s\n", d.getDisplayName());
        activeTest = d;
        activeFailure = null;
    }

    @Override
    public void testFailure(Failure f) {
        psLogPublic.flush();
        psLogPrivate.flush();
        activeFailure = f;
    }

    @Override
    public void testFinished(Description d) {
        psLogPublic.flush();
        psLogPrivate.flush();

        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        Float points = (details == null) ? null : details.points();

        if ((points != null) && !points.isNaN()) {
            if (points == 0.0f) { // TODO: Compare after rounding
                extraPossible++;
                if (activeFailure == null) {
                    extraEarned++;
                }
            } else {
                pointsPossible += points;
                testsPossible++;
                if (activeFailure == null) {
                    pointsEarned += points;
                    testsEarned++;
                } else {
                    pointsLost += points;
                    testsLost++;
                }
            }
        }

        if (activeFailure != null) {
            if (activeTest != activeFailure.getDescription()) {
                psLogPrivate.println("WARNING: CRAZY FAILURE MISMATCH");
            }
        }

        tests.put(activeTest, activeFailure);

        activeTest = null;
        activeFailure = null;
    }


    public String getStats(boolean getPoints) {
        if (getPoints) return String.format("%.2f/%.2f",
            pointsEarned, pointsPossible);
            return String.format("%d/%d", testsEarned, testsPossible);
    }

}
