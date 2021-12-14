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
package pulse.ui.components;

import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.Color.black;
import java.awt.Stroke;
import org.jfree.chart.plot.ValueMarker;

/**
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class MovableValueMarker extends ValueMarker {

    private State state = State.IDLE;

    public final static Stroke IDLE_STROKE = new BasicStroke(1.5f, CAP_BUTT, JOIN_MITER, 5.0f, new float[]{10f}, 0.0f);
    public final static Stroke SELECTED_STROKE = new BasicStroke(3.0f, CAP_BUTT, JOIN_MITER, 5.0f, new float[]{10f}, 0.0f);

    public MovableValueMarker(double value) {
        super(value);
        setPaint(black);
        setStroke(IDLE_STROKE);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        if (this.state != state) {
            //do only if state has changed
            this.state = state;
            super.setStroke(state == State.IDLE ? IDLE_STROKE : SELECTED_STROKE);
        }
    }

    public enum State {
        IDLE, SELECTED, MOVING;
    }

}
