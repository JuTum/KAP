/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2017 Fabian Prasser, Florian Kohlmayer and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.aggregates.quality;

import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.common.Groupify;
import org.deidentifier.arx.common.Groupify.Group;
import org.deidentifier.arx.common.TupleWrapper;
import org.deidentifier.arx.common.WrappedBoolean;

/**
 * Implementation of the AECS measure, as proposed in:<br>
 * <br>
 * K. LeFevre, D. DeWitt, R. Ramakrishnan: "Mondrian multidimensional k-anonymity"
 * Proc Int Conf Data Engineering, 2006.
 * 
 * @author Fabian Prasser
 */
public class QualityModelRowOrientedAECS extends QualityModel<QualityMeasureRowOriented> {

    /**
     * Creates a new instance
     * 
     * @param interrupt
     * @param input
     * @param output
     * @param groupedInput
     * @param groupedOutput
     * @param hierarchies
     * @param shares
     * @param indices
     * @param config
     */
    public QualityModelRowOrientedAECS(WrappedBoolean interrupt,
                                       DataHandle input,
                                       DataHandle output,
                                       Groupify<TupleWrapper> groupedInput,
                                       Groupify<TupleWrapper> groupedOutput,
                                       String[][][] hierarchies,
                                       QualityDomainShare[] shares,
                                       int[] indices,
                                       QualityConfiguration config) {
        super(interrupt,
              input,
              output,
              groupedInput,
              groupedOutput,
              hierarchies,
              shares,
              indices,
              config);
    }

    @Override
    public QualityMeasureRowOriented evaluate() {

        try {
            // Calculate
            double min = getAverageGroupSize(getGroupedInput());
            double max = getInput().getNumRows();
            double result = getAverageGroupSize(getGroupedOutput());
            return new QualityMeasureRowOriented(min, result, max);
        } catch (Exception e) {
            // Silently catch exceptions
            return new QualityMeasureRowOriented();
        }
    }

    /**
     * Returns the average group size for this groupify
     * @param groupify
     * @return
     */
    private double getAverageGroupSize(Groupify<TupleWrapper> groupify) {
        // Calculate
        Group<TupleWrapper> group = groupify.first();
        double count = 1d;
        double sum = group.getCount();
        while (group.hasNext()) {
            group = group.next();
            count++;
            sum += group.getCount();

            // Check
            checkInterrupt();
        }
        
        // Finalize
        return sum / count;
    }
}
