package com.rixon.ramodage.strategy.marketdata;

import com.rixon.ramodage.configuration.Field;
import com.rixon.ramodage.strategy.AbstractDataGenerationStrategy;
import com.rixon.ramodage.strategy.record.RecordCreationContext;
import com.rixon.ramodage.strategy.record.RecordCreationStrategy;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * This is the base class for generating market data. Quotes and Trades have some amount of commonality.
 * To achieve code reuse this abstract class has been introduced
 * User: rixon
 * Date: 11/3/13
 * Time: 4:45 PM
 */
public abstract class AbstractMarketDataGenerationStrategy extends AbstractDataGenerationStrategy {

    protected final String SYMBOL = "Symbol";
    protected final String TIMESTAMP = "Timestamp";
    protected Map<String,List<PriceSet>> masterQuotes;
    protected Map<String,List<Long>> timestampsPerSplit;
    protected Map<String,Field> fieldMap;
    protected long stepValue;
    private long startTimestamp;


    @Override
    protected void populateDataForSplit(String split, String taskId) throws IOException {
        RecordCreationStrategy recordCreationStrategy = RecordCreationContext.strategyFor(schema.getType());
        List<Long> timestamps = timestampsPerSplit.get(split);
        Long startTime = timestamps.get(0);
        Long endTime = timestamps.get(1);
        while (startTime<=endTime){
            for (String symbolName:masterQuotes.keySet()){
                for (PriceSet priceSet:masterQuotes.get(symbolName)) {
                    Map<Field,String> overriddenValues = determineOverriddenValues(startTime,symbolName,priceSet);
                    String record = recordCreationStrategy.createRecordWithOverrides(schema,options,0,overriddenValues);
                    this.dataDestination.addRecord(split,record);
                }
            }
            startTime+=stepValue;
            progressReporter.updateThreadProgress(taskId, (endTime-startTime)*1.0f/endTime);
        }
    }


    @Override
    protected void prepareForDataGeneration() {
        super.prepareForDataGeneration();
        createMasterQuotes();
        initializeTimeStamps();
    }


    protected void createMasterQuotes() {
        populateFieldMap();
        masterQuotes = new HashMap<>();
        Field symbolField = fieldMap.get(SYMBOL);
        String[] symbols = symbolField.getRange().split(",");
        for (String symbolName:symbols) {
            masterQuotes.put(symbolName,createPriceSets());
        }
    }

    private void populateFieldMap() {
        fieldMap = new HashMap<>();
        for (Field field:schema.getFields()){
            fieldMap.put(field.getName(),field);
        }
    }

    protected void initializeTimeStamps() {
        Field timeStampField = fieldMap.get(TIMESTAMP);
        startTimestamp = Long.valueOf(timeStampField.getMinValue());
        long endTimestamp = Long.valueOf(timeStampField.getMaxValue());
        stepValue = Long.valueOf(timeStampField.getStepValue());
        populateTimestampsForSplits();
    }

    private void populateTimestampsForSplits() {
        timestampsPerSplit = new HashMap<>();
        for (int i=0;i<options.getNumberOfFileSplits();i++) {
            List<Long> timestamps = getTimestampsPerSplit(i);
            timestampsPerSplit.put(String.valueOf(i),timestamps);
        }
    }

    private List<Long> getTimestampsPerSplit(int i) {
        Long startTime = startTimestamp+i*options.getNumberOfRecordsPerSplit()*stepValue;
        Long endTime = startTime+stepValue*options.getNumberOfRecordsPerSplit()-stepValue;
        List<Long> timestamps = new ArrayList<>();
        timestamps.add(startTime);
        timestamps.add(endTime);
        return timestamps;
    }

    private List<PriceSet> createPriceSets() {
        List<PriceSet> priceSets = new ArrayList<>();
        PriceSet priceSet100 = new PriceSet();
        priceSet100.askPrice="00002000000";
        priceSet100.bidPrice="00001800000";
        priceSet100.askQuantity="0000100";
        priceSet100.bidQuantity="0000100";
        priceSets.add(priceSet100);
        PriceSet priceSet200 = new PriceSet();
        priceSet200.askPrice="00002010000";
        priceSet200.bidPrice="00001850000";
        priceSet200.askQuantity="0000200";
        priceSet200.bidQuantity="0000200";
        priceSets.add(priceSet200);
        return priceSets;
    }

    //TODO This methods should not be in strategy. Think about how this can be moved to ValueProvider
    protected String getRandomValue(String baseValue, double randomBound, Field field) {
        Random random = new Random();
        double multiplier = random.nextInt(1)==1?1.0d:-1.0d;
        BigDecimal randomValue  = BigDecimal.valueOf(Double.valueOf(baseValue)+randomBound*Math.random()*multiplier);
        randomValue =  randomValue.setScale(0, BigDecimal.ROUND_HALF_UP);
        return getValueWithPadding(randomValue.toPlainString(),field);
    }

    //TODO This methods should not be in strategy. Think about how this can be moved to ValueProvider
    protected String getValueWithPadding(String value, Field field) {
        int paddingLength = field.getFixedLength()-value.length();
        StringBuilder valueWithPadding = new StringBuilder();
        for (int i=0;i<paddingLength;i++) {
            valueWithPadding.append(field.getPadding());
        }
        return valueWithPadding.append(value).toString();
    }

    protected abstract Map<Field,String> determineOverriddenValues(Long startTime, String symbolName, PriceSet priceSet);

    class PriceSet {
        String askPrice;
        String askQuantity;
        String bidPrice;
        String bidQuantity;
    }
}