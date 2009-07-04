package hideftvads.android;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class hideftvads.android.AgentTest \
 * hideftvads.android.tests/android.test.InstrumentationTestRunner
 */
public class AgentTest extends ActivityInstrumentationTestCase<Agent> {

    public AgentTest() {
        super("hideftvads.android", Agent.class);
    }

}
