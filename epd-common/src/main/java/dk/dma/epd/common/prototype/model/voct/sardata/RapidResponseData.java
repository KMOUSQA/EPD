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
package dk.dma.epd.common.prototype.model.voct.sardata;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.voct.RapidResponseDTO;
import dk.dma.epd.common.prototype.model.voct.LeewayValues;
import dk.dma.epd.common.text.Formatter;
import dk.dma.epd.common.util.MCTypeConverter;
import dma.voct.RapidResponse;

public class RapidResponseData extends SARData {

    private static final long serialVersionUID = 1L;
    private List<Position> currentList;
    private List<Position> windList;

    private Position datum;
    // private Position wtc;

    private double radius;

    private double timeElasped;

    private double rdvDirection;
    private double rdvDistance;
    private double rdvSpeed;

    private double rdvDirectionLast;
    private double rdvSpeedLast;

    private Position A;
    private Position B;
    private Position C;
    private Position D;

    public RapidResponseData(RapidResponseData data, int additionalTime) {

        super(data.getSarID(), data.getLKPDate(), data.getCSSDate()
                .plusMinutes(additionalTime), data.getLKP(), data.getX(), data
                .getY(), data.getSafetyFactor(), data.getSearchObject());

        currentList = data.getCurrentList();
        windList = data.getWindList();

        datum = data.getDatum();

        radius = data.getRadius();

        rdvDirection = data.getRdvDirection();
        rdvDistance = data.getRdvDistance();
        rdvSpeed = data.getRdvSpeed();

        rdvDirectionLast = data.getRdvDirectionLast();
        rdvSpeedLast = data.getRdvSpeedLast();

        timeElasped = data.getTimeElasped() + additionalTime;

        A = data.getA();
        B = data.getB();
        C = data.getC();
        D = data.getD();

        this.setWeatherPoints(data.getWeatherPoints());

    }

    // Init data
    public RapidResponseData(String sarID, DateTime TLKP, DateTime CSS,
            Position LKP, double x, double y, double SF, int searchObject) {

        super(sarID, TLKP, CSS, LKP, x, y, SF, searchObject);
    }

    public RapidResponseData(RapidResponse rapidResponse) {
        super(rapidResponse.getSarID(), 
                new DateTime(rapidResponse.getLKPDate().getTime()), 
                new DateTime(
                rapidResponse.getCSSDate().getTime()), Position.create(rapidResponse
                .getLkp().getLatitude(), rapidResponse.getLkp().getLongitude()),
                rapidResponse.getX(), rapidResponse.getY(), rapidResponse.getSafetyFactor(), rapidResponse
                        .getSearchObject());

        this.datum = Position.create(rapidResponse.getDatum().getLatitude(), rapidResponse
                .getDatum().getLongitude());
        this.radius = rapidResponse.getRadius();
        this.timeElasped = rapidResponse.getTimeElapsed();
        this.rdvDirection = rapidResponse.getRdvDirection();
        this.rdvDistance = rapidResponse.getRdvDistance();
        this.rdvSpeed = rapidResponse.getRdvSpeed();
        this.rdvDirectionLast = rapidResponse.getRdvSpeedLast();
        this.A = Position.create(rapidResponse.getA().getLatitude(), rapidResponse.getA()
                .getLongitude());
        this.B = Position.create(rapidResponse.getB().getLatitude(), rapidResponse.getB()
                .getLongitude());
        this.C = Position.create(rapidResponse.getC().getLatitude(), rapidResponse.getC()
                .getLongitude());
        this.D = Position.create(rapidResponse.getD().getLatitude(), rapidResponse.getD()
                .getLongitude());

        currentList = new ArrayList<Position>();
        windList = new ArrayList<Position>();

        for (int i = 0; i < rapidResponse.getCurrentList().size(); i++) {
            currentList.add(Position
                    .create(rapidResponse.getCurrentList().get(i).getLatitude(), rapidResponse
                            .getCurrentList().get(i).getLongitude()));
        }

        for (int i = 0; i < rapidResponse.getWindList().size(); i++) {
            windList.add(Position.create(rapidResponse.getWindList().get(i)
                    .getLatitude(), rapidResponse.getWindList().get(i).getLongitude()));
        }

        List<SARWeatherData> weatherPoints = new ArrayList<SARWeatherData>();

        for (int i = 0; i < rapidResponse.getWeatherData().size(); i++) {
            weatherPoints.add(new SARWeatherData(rapidResponse.getWeatherData().get(i)));
        }

        this.setWeatherPoints(weatherPoints);
    }

    public void setBox(Position A, Position B, Position C, Position D) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;
    }

    /**
     * @return the rdvDirectionLast
     */
    public double getRdvDirectionLast() {
        return rdvDirectionLast;
    }

    /**
     * @param rdvDirectionLast
     *            the rdvDirectionLast to set
     */
    public void setRdvDirectionLast(double rdvDirectionLast) {
        this.rdvDirectionLast = rdvDirectionLast;
    }

    /**
     * @return the rdvSpeedLast
     */
    public double getRdvSpeedLast() {
        return rdvSpeedLast;
    }

    /**
     * @param rdvSpeedLast
     *            the rdvSpeedLast to set
     */
    public void setRdvSpeedLast(double rdvSpeedLast) {
        this.rdvSpeedLast = rdvSpeedLast;
    }

    /**
     * @return the currentList
     */
    public List<Position> getCurrentList() {
        return currentList;
    }

    /**
     * @param currentList
     *            the currentList to set
     */
    public void setCurrentList(List<Position> currentList) {
        this.currentList = currentList;
    }

    /**
     * @return the windList
     */
    public List<Position> getWindList() {
        return windList;
    }

    /**
     * @param windList
     *            the windList to set
     */
    public void setWindList(List<Position> windList) {
        this.windList = windList;
    }

    /**
     * @return the rdvDirection
     */
    public double getRdvDirection() {
        return rdvDirection;
    }

    /**
     * @param rdvDirection
     *            the rdvDirection to set
     */
    public void setRdvDirection(double rdvDirection) {
        this.rdvDirection = rdvDirection;
    }

    /**
     * @return the rdvDistance
     */
    public double getRdvDistance() {
        return rdvDistance;
    }

    /**
     * @param rdvDistance
     *            the rdvDistance to set
     */
    public void setRdvDistance(double rdvDistance) {
        this.rdvDistance = rdvDistance;
    }

    /**
     * @return the rdvSpeed
     */
    public double getRdvSpeed() {
        return rdvSpeed;
    }

    /**
     * @param rdvSpeed
     *            the rdvSpeed to set
     */
    public void setRdvSpeed(double rdvSpeed) {
        this.rdvSpeed = rdvSpeed;
    }

    /**
     * @return the timeElasped
     */
    public double getTimeElasped() {
        return timeElasped;
    }

    /**
     * @param timeElasped
     *            the timeElasped to set
     */
    public void setTimeElasped(double timeElasped) {
        this.timeElasped = timeElasped;
    }

    /**
     * @param datum
     *            the datum to set
     */
    public void setDatum(Position datum) {
        this.datum = datum;
    }

    /**
     * @param radius
     *            the radius to set
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * @param a
     *            the a to set
     */
    public void setA(Position a) {
        A = a;
    }

    /**
     * @param b
     *            the b to set
     */
    public void setB(Position b) {
        B = b;
    }

    /**
     * @param c
     *            the c to set
     */
    public void setC(Position c) {
        C = c;
    }

    /**
     * @param d
     *            the d to set
     */
    public void setD(Position d) {
        D = d;
    }

    /**
     * @return the datum
     */
    public Position getDatum() {
        return datum;
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @return the a
     */
    public Position getA() {
        return A;
    }

    /**
     * @return the b
     */
    public Position getB() {
        return B;
    }

    /**
     * @return the c
     */
    public Position getC() {
        return C;
    }

    /**
     * @return the d
     */
    public Position getD() {
        return D;
    }

    @Override
    public String generateHTML() {

        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("dd MMMM, yyyy, 'at.' HH':'mm");

        // // Generate a html sheet of rapid response calculations
        StringBuilder str = new StringBuilder();
        // String name = "How does this look";
        //
        str.append("<html>");
        str.append("<table >");
        str.append("<tr>");
        str.append("<td align=\"left\" style=\"vertical-align: top;\">");
        str.append("<h1>Search and Rescue - Rapid Response</h1>");
        str.append("<hr>");
        str.append("<font size=\"4\">");
        str.append("Time of Last Known Position: "
                + fmt.print(this.getLKPDate()) + "");
        str.append("<br>Last Known Position: " + this.getLKP().toString()
                + "</br>");
        str.append("<br>Commence Search Start time: "
                + fmt.print(this.getCSSDate()) + "</br>");

        str.append(this.getWeatherPoints().get(0).generateHTML());

        str.append("<hr>");
        str.append("<font size=\"4\">");
        str.append("Initial Position Error, X in nautical miles: "
                + this.getX() + "");
        str.append("<br>SRU Navigational Error, Y in nautical miles: "
                + this.getY() + "</br>");
        str.append("<br>Safety Factor, Fs: " + this.getSafetyFactor() + "</br>");
        str.append("<hr>");
        str.append("<font size=\"4\">");
        str.append("Search Object: "
                + LeewayValues.getLeeWayTypes().get(this.getSearchObject())
                + "");
        str.append("<br>With value: "
                + LeewayValues.getLeeWayContent().get(this.getSearchObject())
                + "</br>");
        str.append("<hr>");
        str.append("<font size=\"4\">");
        str.append("Time Elapsed: " + Formatter.formatHours(timeElasped) + "");
        str.append("<br>Applying Leeway and TWC gives a datum of  "
                + datum.toString() + "</br>");
        str.append("<br>With the following Residual Drift Vector</br>");
        str.append("<br>RDV Direction: " + rdvDirection + "°</br>");
        str.append("<br>RDV Distance: "
                + Formatter.formatDouble(rdvDistance, 2) + " nm</br>");
        str.append("<br>RDV Speed: " + Formatter.formatDouble(rdvSpeed, 2)
                + " kn/h</br>");
        str.append("<br>With radius: " + Formatter.formatDouble(radius, 2)
                + "nm </br>");
        str.append("<hr>");
        str.append("<font size=\"4\">");
        str.append("Search Area:");
        str.append("<br>A: " + A.toString() + "</br>");
        str.append("<br>B: " + B.toString() + "</br>");
        str.append("<br>C: " + C.toString() + "</br>");
        str.append("<br>D: " + D.toString() + "</br>");
        str.append("<br>Total Size: "
                + Formatter.formatDouble(radius * 2 * radius * 2, 2)
                + " nm2</br>");

        str.append("</font>");
        str.append("</td>");
        str.append("</tr>");
        str.append("</table>");
        str.append("</html>");
        //
        // calculationsText.setText(str.toString());
        //
        //

        return str.toString();
    }

    public RapidResponse getModelData() {

        RapidResponse rapidResponseData = new RapidResponse();

        for (int i = 0; i < getWeatherPoints().size(); i++) {
            SARWeatherData weatherPoint = getWeatherPoints().get(i);
            rapidResponseData.addWeatherData(weatherPoint.getDTO());
        }

        for (int i = 0; i < windList.size(); i++) {

            rapidResponseData.addWindList(MCTypeConverter
                    .getMaritimeCloudPositin(windList.get(i)));
        }

        for (int i = 0; i < currentList.size(); i++) {
            rapidResponseData.addCurrentList(MCTypeConverter
                    .getMaritimeCloudPositin(currentList.get(i)));

        }

        net.maritimecloud.util.geometry.Position cSPPos = null;

        if (getCSP() != null) {
            cSPPos = MCTypeConverter.getMaritimeCloudPositin(getCSP());
            rapidResponseData.setC(cSPPos);

        }

        rapidResponseData.setA(MCTypeConverter.getMaritimeCloudPositin(A));
        rapidResponseData.setB(MCTypeConverter.getMaritimeCloudPositin(B));
        rapidResponseData.setC(MCTypeConverter.getMaritimeCloudPositin(C));
        rapidResponseData.setD(MCTypeConverter.getMaritimeCloudPositin(D));

        rapidResponseData.setCSSDate(MCTypeConverter
                .getMaritimeCloudTimeStamp(getCSSDate()));

        rapidResponseData.setDatum(MCTypeConverter
                .getMaritimeCloudPositin(datum));

        rapidResponseData.setLKPDate(MCTypeConverter
                .getMaritimeCloudTimeStamp(getLKPDate()));
        rapidResponseData.setRadius(radius);

        rapidResponseData.setRdvDirection(rdvDirection);

        rapidResponseData.setRdvDirectionLast(rdvDirectionLast);

        rapidResponseData.setRdvDistance(rdvDistance);
        rapidResponseData.setRdvSpeed(rdvSpeed);
        rapidResponseData.setRdvSpeedLast(rdvSpeedLast);
        rapidResponseData.setSafetyFactor(getSafetyFactor());
        rapidResponseData.setSarID(getSarID());
        rapidResponseData.setSearchObject(getSearchObject());
        rapidResponseData.setTimeElapsed(getTimeElasped());
        rapidResponseData.setX(getX());
        rapidResponseData.setY(getY());
        rapidResponseData.setSarID(getSarID());
        
        rapidResponseData.setLkp(MCTypeConverter.getMaritimeCloudPositin(this.getLKP()));
        
        return rapidResponseData;

    }
}
