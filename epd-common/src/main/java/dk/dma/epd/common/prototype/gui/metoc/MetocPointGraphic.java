/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.epd.common.prototype.gui.metoc;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.bbn.openmap.omGraphics.OMGraphicList;

import dk.dma.epd.common.prototype.model.route.RouteMetocSettings;
import dk.dma.epd.common.prototype.settings.EnavSettings;
import dk.frv.enav.common.xml.metoc.MetocDataTypes;
import dk.frv.enav.common.xml.metoc.MetocForecastPoint;
import dk.frv.enav.common.xml.metoc.MetocForecastTriplet;
/**
 * Graphic for a metoc point. Combining graphics for different metocs. 
 */
public class MetocPointGraphic extends OMGraphicList {

    private static final long serialVersionUID = 1L;
    
    private MetocWindGraphic     windMarker;
    private MetocCurrentGraphic currentMarker;
    private MetocWaveGraphic    waveMarker;
    private MetocForecastPoint    metocPoint;
    private MetocGraphic metocGraphic;
    private double lat;
    private double lon;
    EnavSettings eNavSettings;
    
    public MetocPointGraphic(MetocForecastPoint metocPoint, MetocGraphic metocGraphic, EnavSettings eNavSettings) {
        this.eNavSettings = eNavSettings;
        this.metocGraphic = metocGraphic;
        this.metocPoint = metocPoint;
        RouteMetocSettings metocSettings = metocGraphic.getRoute().getRouteMetocSettings();
        this.setVague(true);
        double lat = metocPoint.getLat();
        this.lat = lat;
        double lon = metocPoint.getLon();
        this.lon = lon;
        // Get wind speed in m/s
        MetocForecastTriplet windSpeed = metocPoint.getWindSpeed();
        // Wind from direction in degrees clockwise from north
        MetocForecastTriplet windDirection = metocPoint.getWindDirection();
        // Add wind marker
        if(windSpeed != null && windDirection != null && metocSettings.getDataTypes().contains(MetocDataTypes.WI)){
            double windForecastDirection = windDirection.getForecast();
            double windForecastMs = windSpeed.getForecast();
            double windForecastDirectionRadian = Math.toRadians(windForecastDirection);
            windMarker = new MetocWindGraphic(lat, lon, windForecastDirectionRadian, windForecastMs, metocSettings.getWindWarnLimit());
            add(windMarker);
        }
        
        // Current speed in m/s
        MetocForecastTriplet currentSpeed = metocPoint.getCurrentSpeed();
        // Current towards direction in degrees from north
        MetocForecastTriplet currentDirection = metocPoint.getCurrentDirection();
        // Add current marker
        if(currentSpeed != null && currentDirection != null && metocSettings.getDataTypes().contains(MetocDataTypes.CU)){
            double currentForecastMs = currentSpeed.getForecast();
            double currentForecastDirection = currentDirection.getForecast();
            double currentForecastDirectionRadian = Math.toRadians(currentForecastDirection);
            currentMarker = new MetocCurrentGraphic(lat, lon, currentForecastDirectionRadian, currentForecastMs, metocSettings.getCurrentWarnLimit(), eNavSettings);
            add(currentMarker);
        }
        
        // Mean wave height in meters
        MetocForecastTriplet waveHeight = metocPoint.getMeanWaveHeight();
        // Mean wave from direction in degrees from north
        MetocForecastTriplet waveDirection = metocPoint.getMeanWaveDirection();
        // Add wave marker
        if(waveHeight != null && waveDirection != null && metocSettings.getDataTypes().contains(MetocDataTypes.WA)){
            double waveForecastDirection = waveDirection.getForecast();
            waveForecastDirection += 180;
            double waveForecastHeight = waveHeight.getForecast();
            double waveForecastDirectionRadian = Math.toRadians(waveForecastDirection);
            waveMarker = new MetocWaveGraphic(lat, lon, waveForecastDirectionRadian, waveForecastHeight, metocSettings.getWaveWarnLimit(), eNavSettings);
            add(waveMarker);
        }
    }
    
    public MetocForecastPoint getMetocPoint() {
        return metocPoint;
    }
    
    @Override
    public void render(Graphics gr) {
        Graphics2D image = (Graphics2D) gr;
        image.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        image.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.render(image);
    }
    
    public MetocGraphic getMetocGraphic() {
        return metocGraphic;
    }
    
    public double getLat() {
        return lat;
    }
    
    public double getLon() {
        return lon;
    }
    
}
