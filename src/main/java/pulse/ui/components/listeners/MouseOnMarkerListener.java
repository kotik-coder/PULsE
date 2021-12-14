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
package pulse.ui.components.listeners;

import java.awt.Cursor;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import pulse.ui.components.Chart;
import pulse.ui.components.MovableValueMarker;

/**
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class MouseOnMarkerListener implements ChartMouseListener {

    private final MovableValueMarker marker;
    private final Chart chart;
    private final double margin;
    
    private final static Cursor CROSSHAIR = new Cursor(Cursor.CROSSHAIR_CURSOR);
    private final static Cursor RESIZE = new Cursor(Cursor.E_RESIZE_CURSOR);    

    public MouseOnMarkerListener(Chart chart, MovableValueMarker marker, double margin) {
        this.chart = chart;
        this.marker = marker;
        this.margin = margin;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent arg0) {
        //blank
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent arg0) {
        double xCoord = chart.xCoord(arg0.getTrigger());
        highlightMarker(xCoord, marker);        
    }

    private void highlightMarker(double xCoord, MovableValueMarker marker) {

        if (xCoord > (marker.getValue() - margin)
                & xCoord < (marker.getValue() + margin)) {
            
            marker.setState(MovableValueMarker.State.SELECTED);
            chart.getChartPanel().setCursor(RESIZE);
            
        } else {
            
            marker.setState(MovableValueMarker.State.IDLE);
            chart.getChartPanel().setCursor(CROSSHAIR);
            
        }

    }

}