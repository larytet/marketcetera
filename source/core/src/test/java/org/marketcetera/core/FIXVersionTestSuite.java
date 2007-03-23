package org.marketcetera.core;

import junit.framework.Assert;
import junit.framework.Test;
import org.marketcetera.quickfix.FIXVersion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Extends the {@link MarketceteraTestSuite} to run the unit test
 * through the set of all specified FIX versions as well
 *
 * You can specify an option set of "exception" methods that are only to be run
 * with a different set of versions. This is useful when some test cases are applicable
 * only under certain but not all versions.
 *
 * @author toli
 * @version $Id$
 */

@ClassVersion("$Id$")
public class FIXVersionTestSuite extends MarketceteraTestSuite {
    public static final FIXVersion[] ALL_VERSIONS =
            new FIXVersion[]{FIXVersion.FIX40, FIXVersion.FIX41, FIXVersion.FIX42, FIXVersion.FIX43, FIXVersion.FIX44};
    public static final FIXVersion[] FIX42_PLUS_VERSIONS =
            new FIXVersion[]{FIXVersion.FIX42, FIXVersion.FIX43, FIXVersion.FIX44};

    public FIXVersionTestSuite() {
    }

    public FIXVersionTestSuite(Class aClass, FIXVersion[] inVersions) {
        super();
        addTestForEachVersion(aClass, inVersions, new HashSet<String>(), new FIXVersion[0]);
    }

    public FIXVersionTestSuite(Class aClass, MessageBundleInfo extraBundle, FIXVersion[] inVersions) {
        super();
        init(new MessageBundleInfo[]{extraBundle});
        addTestForEachVersion(aClass, inVersions, new HashSet<String>(), new FIXVersion[0]);
    }

    public FIXVersionTestSuite(Class aClass, MessageBundleInfo[] extraBundles, FIXVersion[] inVersions) {
        super();
        init(extraBundles);
        addTestForEachVersion(aClass, inVersions, new HashSet<String>(), new FIXVersion[0]);
    }

    public FIXVersionTestSuite(Class aClass, MessageBundleInfo extraBundle, FIXVersion[] inVersions,
                               Set<String> exceptionMethods, FIXVersion[] exceptionVersions) {
        super();
        init(new MessageBundleInfo[]{extraBundle});
        addTestForEachVersion(aClass, inVersions, exceptionMethods, exceptionVersions);
    }

    public FIXVersionTestSuite(Class aClass, FIXVersion[] inVersions,
                               Set<String> exceptionMethods, FIXVersion[] exceptionVersions) {
        super();
        addTestForEachVersion(aClass, inVersions, exceptionMethods, exceptionVersions);
    }
    
    public FIXVersionTestSuite(Class aClass, MessageBundleInfo[] extraBundles, FIXVersion[] inVersions,
                               Set<String> exceptionMethods, FIXVersion[] exceptionVersions) {
        super();
        init(extraBundles);
        addTestForEachVersion(aClass, inVersions, exceptionMethods, exceptionVersions);
    }

    /** Class to introspect, and the set of versions to apply to all tests in that class
     * Can also have a set of excpetions and a subset of versions to apply to the exceptions
     * The exceptions should be used for when you have testXXX methods that are only applicable
     * to a subset of FIX versions, such as MARKET_DATA_REQUESTs
     */
    private void addTestForEachVersion(Class aClass, FIXVersion[] inVersions, Set<String> exceptionMethods, FIXVersion[] exceptionVersions) {
        String[] testNames = getTestNames(aClass);
        for (String name : testNames) {
            try {
                Constructor constructor = aClass.getConstructor(String.class, FIXVersion.class);
                if (exceptionMethods.contains(name)) {
                    addTestWithVersion(constructor, name, exceptionVersions);
                } else {
                    addTestWithVersion(constructor, name, inVersions);
                }
            } catch (Exception ex) {
                Assert.fail("Creation of test suite failed: " + ex.getMessage());
            }
        }
    }

    private void addTestWithVersion(Constructor cons, String testName, FIXVersion[] versions) throws Exception {
        for (FIXVersion version : versions) {
            addTest((Test) cons.newInstance(testName, version));
        }
    }

    private String[] getTestNames(Class inClass)
    {
        Vector<String> testNames = new Vector<String>();
        Method[] methods= inClass.getDeclaredMethods();
        for (Method method : methods) {
            if(method.getName().startsWith("test")) {
                testNames.add(method.getName());
            }
        }
        return testNames.toArray(new String[0]);
    }

}
