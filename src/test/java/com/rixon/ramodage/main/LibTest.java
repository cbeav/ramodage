package com.rixon.ramodage.main;

import com.rixon.ramodage.factory.RamodageFactory;
import com.rixon.ramodage.model.DataGenerationStatus;
import com.rixon.ramodage.model.RandomData;
import com.rixon.ramodage.util.Constants;
import com.rixon.ramodage.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * User: rixonmathew
 * Date: 15/06/14
 * Time: 4:07 PM
 *
 * This class is used for testing the API behavior of ramodage
 */
public class LibTest {

    private Ramodage ramodage;
    private int expectedRecords = 50000;
    private int expectedSplits=10;

    @Before
    public void setup() {
        ramodage = RamodageFactory.getWithDefaultOptions();
    }

    @After
    public void teardown() {
        ramodage = null;
    }

    @Test
    public void testAPIBehaviour() {
       Properties properties = createProperties();
       RandomData<DailyTrade> randomData = ramodage.generateData(properties);
       assertNotNull(randomData);
       List<DailyTrade> allRecords = randomData.getAllRecords();
       assertNotNull(allRecords);
       int expectedSize=500;
       assertThat("Size is not as expected",allRecords.size(),is(expectedSize));
       for (int i=0;i<100;i++) {
           DailyTrade dailyTrade = randomData.getRandomRecord();
           assertNotNull(dailyTrade);
       }
//       for (DailyTrade dailyTrade:allRecords) {
//           System.out.println("dailyTrade = " + dailyTrade);
//       }
    }

    @Test
    public void testAPIBehaviourForAsyncCall() {
        Properties properties = createProperties();
        long startTime = System.currentTimeMillis();
        DataGenerationStatus<DailyTrade> dataGenerationStatus = ramodage.generateDataAsynchronously(properties);

        long expectedTimeTaken=2000l;
        long timeTakenToReturnAsyncCall=System.currentTimeMillis()-startTime;
        System.out.println("timeTakenToReturnAsyncCall = " + timeTakenToReturnAsyncCall);
        assertNotNull(dataGenerationStatus);
        while (!dataGenerationStatus.isDataGenerationComplete()) {
            try {
                Thread.sleep(500l);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            System.out.println(dataGenerationStatus.getProgress());
        }
        long timeTaken=System.currentTimeMillis()-startTime;
        System.out.println("timeTaken = " + timeTaken);
        RandomData<DailyTrade> randomData = dataGenerationStatus.getRandomData();
        List<DailyTrade> allRecords = randomData.getAllRecords();
        assertNotNull(allRecords);
        int expectedSize=expectedRecords*expectedSplits;
        assertThat("Size is not as expected",allRecords.size(),is(expectedSize));
        for (int i=0;i<100;i++) {
            DailyTrade dailyTrade = randomData.getRandomRecord();
            assertNotNull(dailyTrade);
        }

    }

    private Properties createProperties() {
        Properties properties = new Properties();
        properties.setProperty(Constants.SCHEMA_CONTENT,getJSONRepresentationOfSchema());
        properties.setProperty(Constants.GENERATION_TYPE,"random");
        properties.setProperty(Constants.OBJECT_CLASS_NAME,DailyTrade.class.getName());
        properties.setProperty(Constants.DESTINATION_TYPE,"IN_MEMORY");
        properties.setProperty(Constants.RECORDS_PER_SPLIT,String.valueOf(expectedRecords));
        properties.setProperty(Constants.NUMBER_OF_SPLITS,String.valueOf(expectedSplits));
        return properties;
    }

    private String getJSONRepresentationOfSchema() {
        return TestUtil.getFileContents("daily_trades.json");
    }

}
