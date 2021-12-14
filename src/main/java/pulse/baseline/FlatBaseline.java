/*
 * Copyright 2021 Artem Lunev <artem.v.lunev@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pulse.baseline;

import static java.lang.String.format;
import java.util.List;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;

/**
 * A flat baseline.
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */

public class FlatBaseline extends AdjustableBaseline {
    
        /**
     * A primitive constructor, which initialises a {@code CONSTANT} baseline
     * with zero intercept and slope.
     */
    public FlatBaseline() {
        this(0.0);
    }

    /**
     * Creates a flat baseline equal to the argument.
     *
     * @param intercept the constant baseline value.
     */
    public FlatBaseline(double intercept) {
        super(intercept);
    }
    
    
    @Override
    protected void doFit(List<Double> x, List<Double> y, int size) {
        double intercept = mean(y);
        set(BASELINE_INTERCEPT, derive(BASELINE_INTERCEPT, intercept));
    }

    @Override
    public Baseline copy() {
        return new FlatBaseline((double)getIntercept().getValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " = " + format("%3.2f", getIntercept().getValue());
    }
    
}
