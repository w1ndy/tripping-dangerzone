package autograder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.concurrent.locks.Lock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import kvstore.KVCache;
import kvstore.KVException;
import kvstore.KeyValueInterface;

import org.junit.runner.Request;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import autograder.AGCategories.AGSuite;
import autograder.AGException.ErrCat;


public class TestUtils {

    public static final int kTimeoutQuick = 3 * 1000;
    public static final int kTimeoutDefault = 10 * 1000;
    public static final int kTimeoutSlow = 25 * 1000;

    // TODO: Use dynamic port within a range for all tests
    public static final int basePort = getEnvInt("AG_PORT_BASE", 8100);
    public static final String testHost = getEnvString("AG_HOST", "localhost");

    // Try to avoid colision (separate ports just in case one dangles)
    public static final int PORT_Client = basePort + 0;
    public static final int PORT_Server = basePort + 1;
    public static final int PORT_StudentEnds = basePort + 2;
    public static final int PORT_STU = basePort + 3;
    public static final int PORT_REF = basePort + 4;
    public static final int PORT_MSG = basePort + 5;

    // Ideally would impose "localhost"...but might require submission hacking.
    public static final int PORT_MasterServer = 8080;
    public static final int PORT_Registration = 9090;

    public static String getEnvString(String name, String def) {
        String ret = null;
        try {
            ret = System.getProperty(name);
        } catch (Exception e) {}
        if (ret == null) {
            try {
                ret = System.getenv(name);
            } catch (Exception e) {
                System.err.format("EXCEPTION reading env %s: %s%n", name, e);
            }
        }
        return (ret == null) ? def : ret;
    }

    public static int getEnvInt(String name, int def) {
        String s = null;
        try {
            s = getEnvString(name, null);
            if (s != null) return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.err.format("ERROR parsing value of env %s='%s'%n", name, s);
        }
        return def;
    }

    public static String readWholeFile(String fileName) {
        BufferedReader br = null;
        try {
            String curr;
            String ret = "";

            br = new BufferedReader(new FileReader(fileName));
            while ((curr = br.readLine()) != null) {
                ret += curr;
            }
            return ret;
        } catch (IOException ioe) {
            throw new AGException(ErrCat.INTERNAL, "Unable to read a test file", ioe);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Element parseXMLFile(String fileName) throws FileNotFoundException {
        InputStream istream = new FileInputStream(fileName);
        try {
            InputSource isource = new InputSource(istream);
            // TODO: Grab into string first...or add a filter to capture contents in parallel
            return parseXMLSource(isource);
        } finally {
            try {
                istream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static Element parseXMLString(String inXML) {
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(inXML));
        return parseXMLSource(is);
    }

    public static Element parseXMLSource(InputSource is) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setCoalescing(true);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true); // Useless (only active in validating mode)

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            return doc.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new AGException(ErrCat.INTERNAL, "Unable to create XML parser", e);
        } catch (IOException e) {
            throw new AGException(ErrCat.INTERNAL, "Unable to read XML string", e);
        } catch (SAXException e) {
            throw new AGException(ErrCat.INTERNAL, "Unable to parse XML string", e);
        }
    }

    public static String getTextValue(Element ele, String tagName) {
        NodeList nl = ele.getElementsByTagName(tagName);
        if ((nl != null) && (nl.getLength() == 1)) return nl.item(0).getTextContent();
        return null;
    }


    public static class KVCacheLockWrapper extends KVCache {

        public KVCacheLockWrapper(int numSets, int maxElemsPerSet) {
            super(numSets, maxElemsPerSet);
            // System.out.printf("new KVCacheLockWrapper(%d, %d)%n", numSets, maxElemsPerSet);
        }

        @Override
        public void put(String key, String value) {
            Lock lock = getLock(key);
            if (lock != null) {
                lock.lock();
            }
            try {
                super.put(key, value);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        @Override
        public String get(String key) {
            Lock lock = getLock(key);
            if (lock != null) {
                lock.lock();
            }
            try {
                return super.get(key);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        @Override
        public void del(String key) {
            Lock lock = getLock(key);
            if (lock != null) {
                lock.lock();
            }
            try {
                super.del(key);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

    }

    // This can help precipitate failures in == vs String.equals()
    public static class KVInternHurter implements KeyValueInterface {

        KeyValueInterface delegate;

        public KVInternHurter(KeyValueInterface d) {
            delegate = d;
            System.out.printf("new KVInternHurter(%s)%n", d);
        }

        @Override
        public void put(String key, String value) throws KVException {
            delegate.put(internifunker(key), internifunker(value));
        }

        @Override
        public String get(String key) throws KVException {
            return internifunker(delegate.get(internifunker(key)));
        }

        @Override
        public void del(String key) throws KVException {
            delegate.del(internifunker(key));
        }

        public static String internifunker(String s) {
            return (s == null) ? null : new String(s); // Hopefully reliable
        }
    }

    // This may or may-not be reliable (hard to say)
    public static class KVInternHelper implements KeyValueInterface {

        KeyValueInterface delegate;

        public KVInternHelper(KeyValueInterface d) {
            delegate = d;
            System.out.printf("new KVInternHelper(%s)%n", d);
        }

        @Override
        public void put(String key, String value) throws KVException {
            delegate.put(internifier(key), internifier(value));
        }

        @Override
        public String get(String key) throws KVException {
            return internifier(delegate.get(internifier(key)));
        }

        @Override
        public void del(String key) throws KVException {
            delegate.del(internifier(key));
        }

        public static String internifier(String s) {
            return (s == null) ? null : s.intern(); // Uncertain reliability
        }
    }

    public static Calendar elapsedTime(Calendar start, Calendar finish) {
        int elapsedMS = (int) (finish.getTimeInMillis() - start.getTimeInMillis());
        Calendar elapsedCal = Calendar.getInstance();
        elapsedCal.set(Calendar.HOUR_OF_DAY, 0);
        elapsedCal.set(Calendar.MINUTE, 0);
        elapsedCal.set(Calendar.SECOND, 0);
        elapsedCal.set(Calendar.MILLISECOND, 0);
        elapsedCal.add(Calendar.MILLISECOND, elapsedMS);
        return elapsedCal;
    }

    public static String indentStr(int num, String ident) {
        return new String(new char[num]).replace("\0", ident);
    }

    public static void main(String[] args) {
        int status = 1;
        AGSuite testSuite = null;
        if (args.length >= 1) {
            try {
                testSuite = AGSuite.valueOf(args[0].replace("-", "_"));
            } catch (Exception e) {
                System.err.format("Unknown test suite: %s%n", args[0]);
                System.exit(status);
                return;
            }
        } else {
            System.err.println("Specify test suite as argument.");
            System.exit(status);
         }
        PrintStream origErr = System.err;
        try {
            Request req = testSuite.getJUnitRequest();
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("list")) {
                    AGCategories.listTests(req.getRunner().getDescription(), -1);
                } else if (args[1].equalsIgnoreCase("tree")) {
                    AGCategories.listTests(req.getRunner().getDescription(), 1);
                } else if (args[1].equalsIgnoreCase("weights")) {
                    AGCategories.listTests(req.getRunner().getDescription(), 0);
                } else {
                    origErr.format("Unrecognized command: %s%n", args[1]);
                    System.exit(status);
                }
                System.exit(0);
            }
            boolean perfect = AGCategories.runJUnitTests(req);
            status = perfect ? 0 : 1;
        } catch (Throwable e) {
            e.printStackTrace(origErr);
        }
        System.exit(status);
    }

}
