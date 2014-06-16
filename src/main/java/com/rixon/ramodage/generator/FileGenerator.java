package com.rixon.ramodage.generator;

import com.rixon.ramodage.configuration.Options;
import com.rixon.ramodage.configuration.Schema;
import com.rixon.ramodage.destination.DataDestination;
import com.rixon.ramodage.destination.DestinationType;
import com.rixon.ramodage.destination.FileBasedDataDestination;
import com.rixon.ramodage.model.RandomData;
import com.rixon.ramodage.strategy.DataGenerationStrategy;
import com.rixon.ramodage.strategy.DataGenerationStrategyContext;
import org.apache.log4j.Logger;

/**
 * This class is responsible for creating the actual output files based on options
 * User: rixonmathew
 * Date: 20/01/13
 * Time: 1:17 PM
 */
public class FileGenerator<TYPE> implements DataGenerator<TYPE>{

    private final Logger LOG = Logger.getLogger(FileGenerator.class);

    @Override
    public RandomData<TYPE> generateData(Schema schema, Options options) {
        DataDestination<TYPE> dataDestination = new FileBasedDataDestination(schema,options);
        DataGenerationStrategy<TYPE> dataGenerationStrategy;
        if (options.getGenerationType().equals("class")){
            dataGenerationStrategy = DataGenerationStrategyContext.strategyFromClass(options.getDataGenerationStrategyClassName());
        } else {
            dataGenerationStrategy = DataGenerationStrategyContext.strategyForType(options.getGenerationType(), DestinationType.FILE.getDescription());
        }
        dataGenerationStrategy.generateData(schema,options,dataDestination);
        return dataDestination.getRandomData();
    }
}
