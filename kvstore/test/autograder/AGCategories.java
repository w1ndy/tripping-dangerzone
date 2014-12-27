package autograder;

import java.io.PrintStream;
import java.lang.annotation.*;
import java.util.*;

import kvstore.*;

import org.junit.FixMethodOrder;
import org.junit.experimental.categories.*;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.*;
import org.junit.runners.*;

public final class AGCategories {

    // JUnit Categories
    public interface AG_CS162 {}
    public interface AG_PROJ3 extends AG_CS162 {}
    public interface AG_PROJ3_TEST extends AG_PROJ3 {}
    public interface AG_PROJ3_CODE extends AG_PROJ3 {}
    public interface AG_PROJ4 extends AG_CS162 {}
    public interface AG_PROJ4_TEST extends AG_PROJ4 {}
    public interface AG_PROJ4_CODE extends AG_PROJ4 {}

    // JUnit Suites (defined by categories)
    @RunWith(Categories.class)
    @Suite.SuiteClasses({
        EndToEndTest.class,
        KVCacheTest.class,
        KVClientTest.class,
        KVMessageTest.class,
        KVStoreTest.class,
        SocketServerTest.class,
        ThreadPoolTest.class,
        KVServerTest.class
    })
    @IncludeCategory(AG_PROJ3_CODE.class)
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    public static class AGSuite_proj3_code {}

    @IncludeCategory(AG_PROJ3_TEST.class)
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    public static class AGSuite_proj3_test extends AGSuite_proj3_code {}

    @RunWith(Categories.class)
    @Suite.SuiteClasses({
    })
    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    @IncludeCategory(AG_PROJ4_CODE.class)
    public static class AGSuite_proj4_code {}

    @FixMethodOrder(MethodSorters.NAME_ASCENDING)
    @IncludeCategory(AG_PROJ4_TEST.class)
    public static class AGSuite_proj4_test extends AGSuite_proj4_code {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AGTestDetails {
        // int uid(); // Unique ID (manually chosen)
        float points() default Float.NaN; // Points during grading
        String desc(); // Public description for students
        String developNotes() default ""; // Private description for AG developers/TA's
        String testFamily() default ""; // If blank, just uses class name
        boolean isPublic() default true; // Made available in public auto-autograder
    }

    public static String getTestTitle(Description d) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        if (details == null) return d.getMethodName();
        return String.format("%s", d.getDisplayName());
    }

    public static String getTestDesc(Description d) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        if (details == null) return d.getDisplayName();
        return details.desc();
    }

    public static String getTestFamily(Description d) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        String family = (details == null) ? null : details.testFamily();
        if ((family == null) || family.isEmpty()) {
            family = d.getTestClass().getSimpleName();
        }
        return family;
    }

    public static String getTestWeights(Description d, boolean showAll) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        if (details == null) return (showAll) ? "??? 0 " + d.getDisplayName() : null;
        return (!((details.points() > 0.0f) || showAll)) ? null
                : String.format("%s %d %s", d.getDisplayName(),
                        Math.round(details.points()), getTestFamily(d));
    }

    public static String getTestDetailText(Description d) {
        AGTestDetails details = d.getAnnotation(AGTestDetails.class);
        if (details == null) return d.getDisplayName();
        return String.format("%s %.2f %s [%s] %s", d.getDisplayName(),
                details.points(), getTestFamily(d),
                details.isPublic() ? "public" : "private",
                details.desc());
    }


    static enum AGSuite {
        proj3_test(AGSuite_proj3_test.class),
        proj3_code(AGSuite_proj3_code.class),
        proj4_test(AGSuite_proj4_test.class),
        proj4_code(AGSuite_proj4_code.class);

        private Class<?> suiteClass;

        AGSuite(Class<?> suite) {
            suiteClass = suite;
        }

        public Request getJUnitRequest() {
            return Request.aClass(suiteClass);
        }
    }

    public static boolean runJUnitTests(final Request request) {
        // TODO: Explicitly take two streams, then redirect & restore stdout/err
        PrintStream psOut = System.err;
        System.setErr(System.out);

        Calendar startCal = Calendar.getInstance();
        System.out.format("AutoGrader Run: %1tc%n%n", startCal);
        System.out.println("========================== DISPLAYING FAILED TESTS ==========================");
        System.out.flush();
        // psOut.format("START-ALL: %1tc%n", startCal);
        // Perform the JUnit run proper
        SuccessListener jlistener = new SuccessListener(request, System.err, System.err);//psOut);
        JUnitCore jcore = new JUnitCore();
        jcore.addListener(jlistener);
        // Result result = jcore.run(request);
        jcore.run(request);
        boolean perfectOnRequired = (jlistener.testsLost == 0);

        // Report on the run in various summary forms
        // jlistener.logSummary(System.err, true);
        // jlistener.logSummary(System.err, false);
        // jlistener.logSummary(psOut, true);

        // Just encourage finalization of stuff (probably useless)
        // result = null;
        jlistener = null;
        jcore = null;
        System.gc();
        System.runFinalization();

        System.err.flush();
        psOut.flush();
        // Calendar finishCal = Calendar.getInstance();
        // System.out.format("%n%nFINISH-ALL: %1tc  [ ELAPSED: %2tT ]%n%n",
        //         finishCal, TestUtils.elapsedTime(startCal, finishCal));

        return perfectOnRequired;
    }

    public static void listTests(Description desc, int level) {
        int nextLevel = level + (int) Math.signum(level);
        List<Description> reqList = desc.getChildren();

        if ((level > 0) || desc.isTest()) {
            String line = null;
            if (level == 0) {
                line = getTestWeights(desc, false);
            } else if (level > 0) {
                line = getTestDetailText(desc);
                if (line != null) {
                    line = TestUtils.indentStr(level - 1, "\t") + line;
                }
            } else {
                line = getTestDetailText(desc);
            }
            if (line != null) {
                System.out.println(line);
            }
        }

        for (Description child: reqList) {
            listTests(child, nextLevel);
        }
    }

}
