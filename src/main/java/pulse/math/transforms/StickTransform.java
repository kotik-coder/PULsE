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
package pulse.math.transforms;

import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import pulse.math.Segment;

/**
 * A simple bounded transform which makes the parameter stick to the 
 * boundaries upon reaching them. For insatnce, when a parameter <math>x</math>
 * attempts to escape its bounds due to a larger increment then allowed, this
 * transform will return it directly to the respective boundary, where it will
 * "stick".
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */

public class StickTransform extends BoundedParameterTransform {

    /**
     * Only the upper bound of the argument is used.
     *
     * @param bounds the {@code bounda.getMaximum()} is used in the transforms
     */
    public StickTransform(Segment bounds) {
        super(bounds);
    }

    /**
     * @param a
     * @see pulse.math.MathUtils.atanh()
     * @see pulse.math.Segment.getBounds()
     */
    @Override
    public double transform(double a) {
        double max = getBounds().getMaximum();
        double min = getBounds().getMinimum();
        return a > max ? max : (a < min ? min : a);
    }

    /**
     * @see pulse.math.MathUtils.tanh()
     * @see pulse.math.Segment.getBounds()
     */
    @Override
    public double inverse(double t) {
        return transform(t);
    }
    
}
