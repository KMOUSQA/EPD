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
package dk.dma.epd.common.prototype.model.voct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import com.bbn.openmap.geo.Geo;
import com.bbn.openmap.geo.Intersection;

import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.epd.common.Heading;
import dk.dma.epd.common.prototype.model.voct.sardata.DatumLineData;
import dk.dma.epd.common.prototype.model.voct.sardata.DatumPointData;
import dk.dma.epd.common.prototype.model.voct.sardata.EffortAllocationData;
import dk.dma.epd.common.prototype.model.voct.sardata.RapidResponseData;
import dk.dma.epd.common.prototype.model.voct.sardata.SARData;
import dk.dma.epd.common.prototype.model.voct.sardata.SARWeatherData;
import dk.dma.epd.common.prototype.model.voct.sardata.SimpleSAR;
import dk.dma.epd.common.util.Calculator;
import dk.dma.epd.common.util.Converter;
import dk.dma.epd.common.util.Ellipsoid;
import dk.dma.epd.common.util.ParseUtils;

/**
 * Where all SAR Calculations are performed
 * 
 * Has a type and the type determines which options are available
 * 
 * @author David
 * 
 */
public class SAROperation {

    SAR_TYPE operationType;

    // // Time of Last known position
    // DateTime TLKP;
    //
    // // Commence Search Start
    // DateTime CSS;
    //
    // // Commence time of surface Drift - When is this used?
    // Date CTSD;
    //
    // // Last known Position
    // Position LKP;
    //
    // // COmmence Start Point
    // Position CSP;
    //
    // // SURFACE DRIFT VARIABLES
    //
    // // Observed Total Water Current (WTC, knots)
    // double TWCknots;
    //
    // // TWC Vector //either a direction or heading, or degrees
    // double TWCHeading;
    //
    // // Leeway (LW), knots
    // double LWknots;
    //
    // // Leeway Vector, heading or degrees
    // double LWHeading;
    //
    // // The way the wind is blow means the object is pushed in the opposite
    // // direction
    // double downWind;
    //
    // // Initial Position Error X, nm
    // double x;
    //
    // // SRU Navigational Error Y, (GPS = 0.1 nm), nm
    // double y;
    //
    // // Safety Factor, FS
    // double SF;

    // VOCTManager voctManager;

    public SAROperation(SAR_TYPE operationType) {
        this.operationType = operationType;
        // this.voctManager = voctManager;
    }

    public DatumLineData startDatumLineCalculations(DatumLineData data) {
//        System.out.println("Datum line");

        // Create a datumpoint for each
        List<DatumPointData> datumPoints = data.getDatumPointDataSets();

        for (int i = 0; i < datumPoints.size(); i++) {

            DatumPointData datumPointData = datumPoints.get(i);

            double difference = (double) (datumPointData.getCSSDate().getMillis() - datumPointData.getLKPDate().getMillis()) / 60 / 60 / 1000;

            datumPointData.setTimeElasped(difference);

            datumPoint(datumPointData);
        }

        // System.out.println("Did we get something calculated?");

//        System.out.println("Different: ");
        for (int i = 0; i < datumPoints.size(); i++) {
//            System.out.println("Time elapsed " + datumPoints.get(i).getTimeElasped());
        }

        // We have to find the box around all circles
        data = findDatumLineSquare(data);

        // voctManager.setSarData(data);
        return data;

    }

    public RapidResponseData startRapidResponseCalculations(RapidResponseData data) {

//        System.out.println("Starting Rapid Response with the following parameters");
//        System.out.println("Time of Last known position: " + data.getLKPDate());
//        System.out.println("Commence Search Start time: " + data.getCSSDate());

        double difference = (double) (data.getCSSDate().getMillis() - data.getLKPDate().getMillis()) / 60 / 60 / 1000;

        data.setTimeElasped(difference);

        // System.out.println("Hours since started: " + difference);

        return rapidResponse(data);

    }

    public DatumPointData startDatumPointCalculations(DatumPointData data) {
//        System.out.println("Starting Datum Point with the following parameters");
//        System.out.println("Time of Last known position: " + data.getLKPDate());
//        System.out.println("Commence Search Start time: " + data.getCSSDate());

        double difference = (double) (data.getCSSDate().getMillis() - data.getLKPDate().getMillis()) / 60 / 60 / 1000;

        data.setTimeElasped(difference);

        // System.out.println("Hours since started: " + difference);

        return datumPoint(data);
    }

    public List<SARData> sarFutureCalculations(SARData data) {
        List<SARData> futureDataList = new ArrayList<SARData>();

        if (operationType == SAR_TYPE.RAPID_RESPONSE) {

            for (int i = 1; i < 9; i++) {

                int additionalTime = i * 30;

                futureDataList.add(rapidResponse(new RapidResponseData((RapidResponseData) data, additionalTime)));

//                System.out.println("Additional Time: " + additionalTime + " minutes");

            }

        }

        if (operationType == SAR_TYPE.DATUM_POINT) {

            for (int i = 1; i < 9; i++) {

                int additionalTime = i * 30;

                futureDataList.add(datumPoint(new DatumPointData((DatumPointData) data, additionalTime)));

//                System.out.println("Additional Time: " + additionalTime + " minutes");

            }

        }

        return futureDataList;
    }

    private double searchObjectValue(int searchObject, double LWKnots) {

        switch (searchObject) {
        case 0:
            return LeewayValues.personInWater(LWKnots);
        case 1:
            return LeewayValues.raftFourToSix(LWKnots);
        case 2:
            return LeewayValues.raftFourToSixWithDriftAnker(LWKnots);
        case 3:
            return LeewayValues.raftFourToSixWithoutDriftAnker(LWKnots);
        case 4:
            return LeewayValues.raftFifteenToTwentyFive(LWKnots);
        case 5:
            return LeewayValues.raftFifteenToTwentyFiveWithDriftAnker(LWKnots);
        case 6:
            return LeewayValues.raftFifteenToTwentyFiveWitouthDriftAnker(LWKnots);
        case 7:
            return LeewayValues.dinghyFlatBottom(LWKnots);
        case 8:
            return LeewayValues.dinghyWithKeel(LWKnots);
        case 9:
            return LeewayValues.dinghyCapsized(LWKnots);
        case 10:
            return LeewayValues.kayakWithPerson(LWKnots);
        case 11:
            return LeewayValues.surfboardWithPerson(LWKnots);
        case 12:
            return LeewayValues.windsurferWithPersonMastAndSailInWater(LWKnots);
        case 13:
            return LeewayValues.sailboatLongKeel(LWKnots);
        case 14:
            return LeewayValues.sailboatFinKeel(LWKnots);
        case 15:
            return LeewayValues.motorboat(LWKnots);
        case 16:
            return LeewayValues.fishingVessel(LWKnots);
        case 17:
            return LeewayValues.trawler(LWKnots);
        case 18:
            return LeewayValues.coaster(LWKnots);
        case 19:
            return LeewayValues.wreckage(LWKnots);

        }

        return -9999.9;
    }

    private int searchObjectValue(int searchObject) {

        switch (searchObject) {
        case 0:
            return LeewayValues.personInWater();
        case 1:
            return LeewayValues.raftFourToSix();
        case 2:
            return LeewayValues.raftFourToSixWithDriftAnker();
        case 3:
            return LeewayValues.raftFourToSixWithoutDriftAnker();
        case 4:
            return LeewayValues.raftFifteenToTwentyFive();
        case 5:
            return LeewayValues.raftFifteenToTwentyFiveWithDriftAnker();
        case 6:
            return LeewayValues.raftFifteenToTwentyFiveWitouthDriftAnker();
        case 7:
            return LeewayValues.dinghyFlatBottom();
        case 8:
            return LeewayValues.dinghyWithKeel();
        case 9:
            return LeewayValues.dinghyCapsized();
        case 10:
            return LeewayValues.kayakWithPerson();
        case 11:
            return LeewayValues.surfboardWithPerson();
        case 12:
            return LeewayValues.windsurferWithPersonMastAndSailInWater();
        case 13:
            return LeewayValues.sailboatLongKeel();
        case 14:
            return LeewayValues.sailboatFinKeel();
        case 15:
            return LeewayValues.motorboat();
        case 16:
            return LeewayValues.fishingVessel();
        case 17:
            return LeewayValues.trawler();
        case 18:
            return LeewayValues.coaster();
        case 19:
            return LeewayValues.wreckage();

        }

        return -9999;
    }

    public DatumPointData datumPoint(DatumPointData data) {

        // We need to calculate for each weather point

        List<SARWeatherData> weatherPoints = data.getWeatherPoints();
        DateTime startTime = data.getLKPDate();

        double leewayDivergence = searchObjectValue(data.getSearchObject());

        List<Double> weatherPointsValidFor = new ArrayList<Double>();

        List<Position> datumPositionsDownWind = new ArrayList<Position>();
        List<Position> datumPositionsMin = new ArrayList<Position>();
        List<Position> datumPositionsMax = new ArrayList<Position>();

        List<Position> currentPositions = new ArrayList<Position>();

        for (int i = 0; i < weatherPoints.size(); i++) {

            // Do we have a next?

            // How long is the data point valid for?

            // Is it the last one?

            if (i == weatherPoints.size() - 1) {
                // It's the last one - let it last the remainder
                double validFor = (double) (data.getCSSDate().getMillis() - startTime.getMillis()) / 60 / 60 / 1000;
                weatherPointsValidFor.add(validFor);
            } else {

                DateTime current = weatherPoints.get(i).getDateTime();

                if (current.isBefore(data.getLKPDate())) {
                    current = data.getLKPDate();
                }

                startTime = weatherPoints.get(i + 1).getDateTime();

                double validFor = (double) (startTime.getMillis() - current.getMillis()) / 60 / 60 / 1000;
                weatherPointsValidFor.add(validFor);
            }

            // How long is this data point valid for

        }

        for (int i = 0; i < weatherPoints.size(); i++) {
            SARWeatherData weatherObject = weatherPoints.get(i);
            double validFor = weatherPointsValidFor.get(i);

//            System.out.println("Valid for : " + validFor);

            double currentTWC = weatherObject.getTWCknots() * validFor;

//            System.out.println("Current TWC: " + currentTWC);
//            System.out.println("Heading TWC: " + weatherObject.getLWHeading());

            double leewayspeed = searchObjectValue(data.getSearchObject(), weatherObject.getLWknots());
            double leeway = leewayspeed * validFor;

            Position startingLocation = null;

            if (i == 0) {
                startingLocation = data.getLKP();
            } else {
                startingLocation = datumPositionsDownWind.get(i - 1);
            }

            Position currentPos = Calculator.findPosition(startingLocation, weatherObject.getTWCHeading(),
                    Converter.nmToMeters(currentTWC));

            currentPositions.add(currentPos);

//            System.out.println("Current is: " + currentPos.getLatitude());
//            System.out.println("Current is: " + currentPos.getLongitude());

            // Temp
            data.setWtc(currentPos);

            Position windPosDownWind = Calculator.findPosition(currentPos, weatherObject.getDownWind(),
                    Converter.nmToMeters(leeway));

            datumPositionsDownWind.add(windPosDownWind);

            // data.setDatumDownWind(windPosDownWind);

            Position windPosMin = Calculator.findPosition(currentPos, weatherObject.getDownWind() - leewayDivergence,
                    Converter.nmToMeters(leeway));

            datumPositionsMin.add(windPosMin);

            // data.setDatumMin(windPosMin);

            Position windPosMax = Calculator.findPosition(currentPos, weatherObject.getDownWind() + leewayDivergence,
                    Converter.nmToMeters(leeway));

            datumPositionsMax.add(windPosMax);

            // data.setDatumMax(windPosMax);

            // Position datumDownWind =
            // Calculator.calculateEndingGlobalCoordinates(
            // reference, currentPos, data.getWeatherPoints().get(0)
            // .getDownWind(), Converter.nmToMeters(leeway),
            // endBearing);
            //
            // endBearing = new double[1];
            //
            // Position datumMin = Calculator.calculateEndingGlobalCoordinates(
            // reference, currentPos, data.getWeatherPoints().get(0)
            // .getDownWind()
            // - leewayDivergence, Converter.nmToMeters(leeway),
            // endBearing);
            //
            // endBearing = new double[1];
            //
            // Position datumMax = Calculator.calculateEndingGlobalCoordinates(
            // reference, currentPos, data.getWeatherPoints().get(0)
            // .getDownWind()
            // + leewayDivergence, Converter.nmToMeters(leeway),
            // endBearing);

        }

        // Only apply divergence on last?

        Position datumDownWind = datumPositionsDownWind.get(datumPositionsDownWind.size() - 1);
        Position datumMin = datumPositionsMin.get(datumPositionsMin.size() - 1);
        Position datumMax = datumPositionsMax.get(datumPositionsMax.size() - 1);
        //
        //
        //
        // Position lastCurrentPositon =
        // currentPositions.get(currentPositions.size() - 1);
        // SARWeatherData lastWeatherPoint =
        // weatherPoints.get(weatherPoints.size() - 1);
        // double leewayspeed = searchObjectValue(data.getSearchObject(),
        // lastWeatherPoint.getLWknots());
        // double validFor =
        // weatherPointsValidFor.get(weatherPointsValidFor.size()-1);
        // double leeway = leewayspeed * validFor;
        //
        // Position datumMin = Calculator.findPosition(lastCurrentPositon,
        // lastWeatherPoint.getDownWind() - leewayDivergence,
        // Converter.nmToMeters(leeway));
        //
        //
        // Position datumMax = Calculator.findPosition(lastCurrentPositon,
        // lastWeatherPoint.getDownWind() + leewayDivergence,
        // Converter.nmToMeters(leeway));
        //
        // data.setWtc(lastCurrentPositon);

        //
        data.setDatumDownWind(datumDownWind);
        data.setDatumMin(datumMin);
        data.setDatumMax(datumMax);
        //

        data.setWindListDownWind(datumPositionsDownWind);
        data.setWindListMax(datumPositionsMax);
        data.setWindListMin(datumPositionsMin);

        data.setCurrentListDownWind(currentPositions);
        data.setCurrentListMax(currentPositions);
        data.setCurrentListMin(currentPositions);

        // Rapid Response

        // // datumPositions.remove(datumPositions.size()-1);
        //
        // Position datumPosition = data.getDatum();
        //
        // // datumPositions.remove(datumPositions.size()-1);
        //
        // data.setWindList(datumPositions);
        // data.setCurrentList(currentPositions);
        //
        // // RDV Direction
        // double rdvDirection = Calculator.bearing(data.getLKP(),
        // datumPosition,
        // Heading.RL);
        //
        // data.setRdvDirection(rdvDirection);
        // System.out.println("RDV Direction: " + rdvDirection);
        //
        // // RDV Distance
        // double rdvDistance = Calculator.range(data.getLKP(), datumPosition,
        // Heading.RL);
        //
        // data.setRdvDistance(rdvDistance);
        // System.out.println("RDV Distance: " + rdvDistance);
        //
        // // RDV Speed
        // double rdvSpeed = rdvDistance / data.getTimeElasped();
        //
        // data.setRdvSpeed(rdvSpeed);
        // System.out.println("RDV Speed: " + rdvSpeed);
        //
        // // Radius:
        // double radius = ((data.getX() + data.getY()) + 0.3 * rdvDistance)
        // * data.getSafetyFactor();
        //
        // data.setRadius(radius);

        // double timeElapsed = data.getTimeElasped();
        //
        // double currentTWC = data.getWeatherPoints().get(0).getTWCknots()
        // * timeElapsed;
        //
        // double leewayspeed = searchObjectValue(data.getSearchObject(), data
        // .getWeatherPoints().get(0).getLWknots());
        //
        // double leeway = leewayspeed * timeElapsed;
        //

        // RDV Direction
        double rdvDirectionDownWind = Calculator.bearing(data.getLKP(), datumDownWind, Heading.RL);

        data.setRdvDirectionDownWind(rdvDirectionDownWind);

        double rdvDirectionMin = Calculator.bearing(data.getLKP(), datumMin, Heading.RL);

        data.setRdvDirectionMin(rdvDirectionMin);

        double rdvDirectionMax = Calculator.bearing(data.getLKP(), datumMax, Heading.RL);

        data.setRdvDirectionMax(rdvDirectionMax);

        // RDV Distance
        double rdvDistanceDownWind = Calculator.range(data.getLKP(), datumDownWind, Heading.RL);

        data.setRdvDistanceDownWind(rdvDistanceDownWind);

        double rdvDistanceMin = Calculator.range(data.getLKP(), datumMin, Heading.RL);

        data.setRdvDistanceMin(rdvDistanceMin);

        double rdvDistanceMax = Calculator.range(data.getLKP(), datumMax, Heading.RL);

        data.setRdvDistanceMax(rdvDistanceMax);

        // RDV Speed
        double rdvSpeedDownWind = rdvDistanceDownWind / data.getTimeElasped();
        data.setRdvSpeedDownWind(rdvSpeedDownWind);

        double rdvSpeedMin = rdvDistanceMin / data.getTimeElasped();
        data.setRdvSpeedMin(rdvSpeedMin);

        double rdvSpeedMax = rdvDistanceMax / data.getTimeElasped();
        data.setRdvSpeedMax(rdvSpeedMax);

        // Radius:
        double radiusDownWind = ((data.getX() + data.getY()) + 0.3 * rdvDistanceDownWind) * data.getSafetyFactor();
        data.setRadiusDownWind(radiusDownWind);

        double radiusMin = ((data.getX() + data.getY()) + 0.3 * rdvDistanceMin) * data.getSafetyFactor();
        data.setRadiusMin(radiusMin);

        double radiusMax = ((data.getX() + data.getY()) + 0.3 * rdvDistanceMax) * data.getSafetyFactor();

        data.setRadiusMax(radiusMax);

        findSmallestSquare(data);

        return data;

    }

    private DatumLineData findDatumLineSquare(DatumLineData data) {

        List<Position> datumPolygon = new ArrayList<Position>();

        DatumPointData dst1 = data.getDatumPointDataSets().get(0);

        DatumPointData dst2 = data.getDatumPointDataSets().get(1);

        DatumPointData dst3 = data.getDatumPointDataSets().get(2);

        // A connects to B
        // C connects to D

        // DST1
        double width = dst1.getA().distanceTo(dst1.getB(), CoordinateSystem.CARTESIAN);

        double height = dst1.getA().distanceTo(dst1.getD(), CoordinateSystem.CARTESIAN);

        double bearingwidth = Calculator.bearing(dst1.getA(), dst1.getB(), Heading.RL);
        double bearingheight = Calculator.bearing(dst1.getA(), dst1.getD(), Heading.RL);

        Position widthCenter = Calculator.findPosition(dst1.getA(), bearingwidth, width / 2);

        // Center of dst1
        Position dst1Center = Calculator.findPosition(widthCenter, bearingheight, height / 2);

        // DST2
        width = dst2.getA().distanceTo(dst2.getB(), CoordinateSystem.CARTESIAN);

        height = dst2.getA().distanceTo(dst2.getD(), CoordinateSystem.CARTESIAN);

        bearingwidth = Calculator.bearing(dst2.getA(), dst2.getB(), Heading.RL);
        bearingheight = Calculator.bearing(dst2.getA(), dst2.getD(), Heading.RL);
        widthCenter = Calculator.findPosition(dst2.getA(), bearingwidth, width / 2);

        Position dst2Center = Calculator.findPosition(widthCenter, bearingheight, height / 2);

        // DST3
        width = dst3.getA().distanceTo(dst3.getB(), CoordinateSystem.CARTESIAN);

        height = dst3.getA().distanceTo(dst3.getD(), CoordinateSystem.CARTESIAN);

        bearingwidth = Calculator.bearing(dst3.getA(), dst3.getB(), Heading.RL);
        bearingheight = Calculator.bearing(dst3.getA(), dst3.getD(), Heading.RL);
        widthCenter = Calculator.findPosition(dst3.getA(), bearingwidth, width / 2);

        Position dst3Center = Calculator.findPosition(widthCenter, bearingheight, height / 2);

        // DST1 and DST2
        double bearingDst1Dst2 = Calculator.bearing(dst1Center, dst2Center, Heading.RL);

        // Vinkelret 90 grader til venstre for vinklen mellem dst1 og dst2 center
        double angleToDSt1Dst2Minus90 = Calculator.turn90Minus(bearingDst1Dst2);
        double angleToDSt1Dst2Plus90 = Calculator.turn90Plus(bearingDst1Dst2);

        Geo intersectionPoint = null;

        int lengthMax = 500000;

        // Create a line going from dst1 center thats perpendicule to angle between dst1 and dst2
        Position angleEndPosition = Calculator.findPosition(dst1Center, angleToDSt1Dst2Minus90, lengthMax);

        Geo a1 = new Geo(dst1.getA().getLatitude(), dst1.getA().getLongitude());
        Geo a2 = new Geo(dst1.getB().getLatitude(), dst1.getB().getLongitude());

        Geo b1 = new Geo(dst1Center.getLatitude(), dst1Center.getLongitude());
        Geo b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top left
        // Top punkt for DST1, placeret på kanten af cirklen
        Position ABCenter = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Bottom left for DST1, på kanten af cirklen
        angleEndPosition = Calculator.findPosition(dst1Center, angleToDSt1Dst2Plus90, lengthMax);

        a1 = new Geo(dst1.getC().getLatitude(), dst1.getC().getLongitude());
        a2 = new Geo(dst1.getD().getLatitude(), dst1.getD().getLongitude());

        b1 = new Geo(dst1Center.getLatitude(), dst1Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        // Top punkt på dst2, på kanten af cirklen
        Position CDCenter = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create a line going from dst2 center thats perpendicule to angle between dst1 and dst2
        angleEndPosition = Calculator.findPosition(dst2Center, angleToDSt1Dst2Plus90, lengthMax);

        a1 = new Geo(dst2.getC().getLatitude(), dst2.getC().getLongitude());
        a2 = new Geo(dst2.getD().getLatitude(), dst2.getD().getLongitude());

        b1 = new Geo(dst2Center.getLatitude(), dst2Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        // Top punkt på dst2, på kanten af cirklen
        Position C2D2Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create a line going from dst1 center thats perpendicule to angle between dst1 and dst2
        angleEndPosition = Calculator.findPosition(dst2Center, angleToDSt1Dst2Minus90, lengthMax);

        a1 = new Geo(dst2.getA().getLatitude(), dst2.getA().getLongitude());
        a2 = new Geo(dst2.getB().getLatitude(), dst2.getB().getLongitude());

        b1 = new Geo(dst2Center.getLatitude(), dst2Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        // Top punkt på dst2, på kanten af cirklen
        Position A2B2Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Top point A:
        // Top Left of DST1 will be:
        // Go from center to opposite of dst1 to dst2

        double reverseDst1ToDst2 = Calculator.reverseDirection(bearingDst1Dst2);

        // Find intersection

        // Create line from center of dst1 in opposite direction
        angleEndPosition = Calculator.findPosition(dst1Center, reverseDst1ToDst2, lengthMax);

        // Find intersection with A and D
        a1 = new Geo(dst1.getA().getLatitude(), dst1.getA().getLongitude());
        a2 = new Geo(dst1.getD().getLatitude(), dst1.getD().getLongitude());

        b1 = new Geo(dst1Center.getLatitude(), dst1Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        Position ADCenter = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create line going from topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(ADCenter, angleToDSt1Dst2Minus90, lengthMax);

        // Find how this line intersects with the line parallel with dst1 to dst2 line
        Position angleEndPosition2 = Calculator.findPosition(ABCenter, Calculator.bearing(A2B2Center, ABCenter, Heading.RL),
                lengthMax);

        a1 = new Geo(ADCenter.getLatitude(), ADCenter.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        b1 = new Geo(ABCenter.getLatitude(), ABCenter.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        Position finalA = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Bottom point D:
        // Bottom Left of DST1 will be:
        // Go from center to opposite of dst1 to dst2

        //
        // //Create line going fro topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(ADCenter, angleToDSt1Dst2Plus90, lengthMax);
        //
        // //Find how this line intersects with the line parallel with dst1 to dst2 line
        angleEndPosition2 = Calculator.findPosition(C2D2Center, Calculator.bearing(C2D2Center, CDCenter, Heading.RL), lengthMax);

        a1 = new Geo(ADCenter.getLatitude(), ADCenter.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        b1 = new Geo(CDCenter.getLatitude(), CDCenter.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());
        //
        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        Position finalD = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // DST2 and DST3
        double bearingDst2Dst3 = Calculator.bearing(dst2Center, dst3Center, Heading.RL);

        // Vinkelret 90 grader til venstre for vinklen mellem dst1 og dst2 center
        double angleToDSt2Dst3Minus90 = Calculator.turn90Minus(bearingDst2Dst3);
        double angleToDSt2Dst3Plus90 = Calculator.turn90Plus(bearingDst2Dst3);

        // Create a line going from dst2 center thats perpendicule to angle between dst2 and dst3
        angleEndPosition = Calculator.findPosition(dst2Center, angleToDSt2Dst3Minus90, lengthMax);

        a1 = new Geo(dst2.getA().getLatitude(), dst2.getA().getLongitude());
        a2 = new Geo(dst2.getB().getLatitude(), dst2.getB().getLongitude());

        b1 = new Geo(dst2Center.getLatitude(), dst2Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top left
        // Top punkt for DST2, placeret på kanten af cirklen
        Position A2B2Center2 = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Bottom left for DST2, på kanten af cirklen
        angleEndPosition = Calculator.findPosition(dst2Center, angleToDSt2Dst3Plus90, lengthMax);

        a1 = new Geo(dst2.getC().getLatitude(), dst2.getC().getLongitude());
        a2 = new Geo(dst2.getD().getLatitude(), dst2.getD().getLongitude());

        b1 = new Geo(dst2Center.getLatitude(), dst2Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Bottom punkt på dst2, på kanten af cirklen
        Position C2D2Center2 = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create a line going from dst2 center thats perpendicule to angle between dst1 and dst2
        angleEndPosition = Calculator.findPosition(dst3Center, angleToDSt2Dst3Plus90, lengthMax);

        a1 = new Geo(dst3.getC().getLatitude(), dst3.getC().getLongitude());
        a2 = new Geo(dst3.getD().getLatitude(), dst3.getD().getLongitude());

        b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Bottom punkt på dst3, på kanten af cirklen
        Position C3D3Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create a line going from dst1 center thats perpendicule to angle between dst1 and dst2
        angleEndPosition = Calculator.findPosition(dst3Center, angleToDSt2Dst3Minus90, lengthMax);

        a1 = new Geo(dst3.getA().getLatitude(), dst3.getA().getLongitude());
        a2 = new Geo(dst3.getB().getLatitude(), dst3.getB().getLongitude());

        b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top punkt på dst3, på kanten af cirklen
        Position A3B3Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Top point A2:
        double reverseDst2ToDst3 = Calculator.reverseDirection(bearingDst2Dst3);

        // Find intersection

        // Create line from center of dst1 in opposite direction
        angleEndPosition = Calculator.findPosition(dst2Center, reverseDst2ToDst3, lengthMax);

        // Find intersection with A and D
        a1 = new Geo(dst2.getA().getLatitude(), dst2.getA().getLongitude());
        a2 = new Geo(dst2.getD().getLatitude(), dst2.getD().getLongitude());

        b1 = new Geo(dst2Center.getLatitude(), dst2Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        Position A2D2Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create line going from topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(A2D2Center, angleToDSt2Dst3Minus90, lengthMax);

        // Find how this line intersects with the line parallel with dst1 to dst2 line
        angleEndPosition2 = Calculator
                .findPosition(A2B2Center2, Calculator.bearing(A3B3Center, A2B2Center2, Heading.RL), lengthMax);

        a1 = new Geo(A2D2Center.getLatitude(), A2D2Center.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        b1 = new Geo(A2B2Center2.getLatitude(), A2B2Center2.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Position finalA2 = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Bottom point D:
        // Bottom Left of DST1 will be:
        // Go from center to opposite of dst1 to dst2

        //
        // //Create line going from topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(A2D2Center, angleToDSt2Dst3Plus90, lengthMax);
        //
        // //Find how this line intersects with the line parallel with dst1 to dst2 line
        angleEndPosition2 = Calculator.findPosition(C3D3Center, Calculator.bearing(C3D3Center, C2D2Center2, Heading.RL), lengthMax);

        a1 = new Geo(A2D2Center.getLatitude(), A2D2Center.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        b1 = new Geo(C2D2Center2.getLatitude(), C2D2Center2.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());
        //
        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        // Position finalD2 = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Find intersection

        // Create line from center of dst1 in opposite direction
        angleEndPosition = Calculator.findPosition(dst3Center, bearingDst2Dst3, lengthMax);

        // Find intersection with B and C
        a1 = new Geo(dst3.getB().getLatitude(), dst3.getB().getLongitude());
        a2 = new Geo(dst3.getC().getLatitude(), dst3.getC().getLongitude());

        b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Top Middle
        Position B3C3Center = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Create line going from topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(B3C3Center, angleToDSt2Dst3Minus90, lengthMax);

        // Find how this line intersects with the line parallel with dst1 to dst2 line
        angleEndPosition2 = Calculator.findPosition(A3B3Center, Calculator.bearing(A2B2Center, A3B3Center, Heading.RL), lengthMax);

        a1 = new Geo(B3C3Center.getLatitude(), B3C3Center.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());

        b1 = new Geo(A3B3Center.getLatitude(), A3B3Center.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());

        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        Position finalB = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // Bottom point D:
        // Bottom Left of DST1 will be:
        // Go from center to opposite of dst1 to dst2

        //
        // //Create line going fro topLeftIntersect, going right, towards the top line
        angleEndPosition = Calculator.findPosition(B3C3Center, angleToDSt2Dst3Plus90, lengthMax);
        //
        // //Find how this line intersects with the line parallel with dst1 to dst2 line
        angleEndPosition2 = Calculator.findPosition(C2D2Center, Calculator.bearing(C2D2Center, C3D3Center, Heading.RL), lengthMax);

        a1 = new Geo(B3C3Center.getLatitude(), B3C3Center.getLongitude());
        a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        b1 = new Geo(C3D3Center.getLatitude(), C3D3Center.getLongitude());
        b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());
        //
        intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        Position finalC = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // datumPolygon.add(angleEndPosition2);
        datumPolygon.add(finalA);

        datumPolygon.add(finalD);

        // datumPolygon.add(topLeftIntersect);

        // datumPolygon.add(CDCenter);

        // datumPolygon.add(finalD2);

        datumPolygon.add(C2D2Center);

        // datumPolygon.add(C2D2Center2);

        datumPolygon.add(C3D3Center);

        datumPolygon.add(finalC);

        datumPolygon.add(finalB);

        datumPolygon.add(A3B3Center);

        // datumPolygon.add(finalA2);

        // datumPolygon.add(topIntersect);

        // datumPolygon.add(dst2Center);

        // datumPolygon.add(bottomDst3Intersect);
        // datumPolygon.add(topDst3IntersectPoint);

        datumPolygon.add(A2B2Center);
        // datumPolygon.add(A2B2Center2);

        datumPolygon.add(ABCenter);
        datumPolygon.add(finalA);

        //
        //
        //
        //
        // double bearingBetweenDSt2Dst3 = Calculator.bearing(dst2Center, dst3Center, Heading.RL);
        //
        // // Vinkelret 90 grader til venstre for vinklen mellem dst2 og dst3 center
        // double angleToDSt3Dst2Minus90 = Calculator.turn90Minus(bearingBetweenDSt2Dst3);
        // double angleToDSt3Dst2Plus90 = Calculator.turn90Plus(bearingBetweenDSt2Dst3);
        //
        //
        // // Create a line going from dst3 center thats perpendicule to angle between dst2 and dst3
        // angleEndPosition = Calculator.findPosition(dst3Center, angleToDSt3Dst2Plus90, lengthMax);
        //
        // a1 = new Geo(dst3.getC().getLatitude(), dst3.getC().getLongitude());
        // a2 = new Geo(dst3.getD().getLatitude(), dst3.getD().getLongitude());
        //
        // b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        // b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        // intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        // Position bottomDst3Intersect = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());
        //
        //
        // // Create a line going from dst1 center thats perpendicule to angle between dst1 and dst2
        // angleEndPosition = Calculator.findPosition(dst2Center, angleToDSt3Dst2Minus90, lengthMax);
        //
        // a1 = new Geo(dst3.getA().getLatitude(), dst3.getA().getLongitude());
        // a2 = new Geo(dst3.getB().getLatitude(), dst3.getB().getLongitude());
        //
        // b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        // b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        // intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        // // Top Middle
        // // Top punkt på dst2, på kanten af cirklen
        // Position topDst3IntersectPoint = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());
        //
        //
        //
        //
        //
        //
        //
        //
        //
        // // Bottom point C:
        // // Top Right of DST3 will be:
        // // Go from center to direction of dst2 and 3
        //
        // // Find intersection
        //
        // // Create line from center of dst1 in opposite direction
        // angleEndPosition = Calculator.findPosition(dst3Center, bearingBetweenDSt2Dst3, lengthMax);
        //
        // // Find intersection with B and C
        //
        // a1 = new Geo(dst3.getB().getLatitude(), dst3.getB().getLongitude());
        // a2 = new Geo(dst3.getC().getLatitude(), dst3.getC().getLongitude());
        //
        // b1 = new Geo(dst3Center.getLatitude(), dst3Center.getLongitude());
        // b2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        // intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);
        //
        // // Bottom right center
        // Position dst3RightCenterIntersect = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());
        //
        //
        //
        //
        // // Create line going from dst3RightCenterIntersect, going left, towards the bottom line
        // angleEndPosition = Calculator.findPosition(dst3RightCenterIntersect, angleToDSt3Dst2Minus90, lengthMax);
        //
        // // Find how this line intersects with the line parallel with dst2 to dst3 line
        // angleEndPosition2 = Calculator.findPosition(C2D2Center,
        // Calculator.bearing(C2D2Center, bottomDst3Intersect, Heading.RL), lengthMax);
        //
        // a1 = new Geo(dst3RightCenterIntersect.getLatitude(), dst3RightCenterIntersect.getLongitude());
        // a2 = new Geo(angleEndPosition.getLatitude(), angleEndPosition.getLongitude());
        //
        // b1 = new Geo(ABCenter.getLatitude(), ABCenter.getLongitude());
        // b2 = new Geo(angleEndPosition2.getLatitude(), angleEndPosition2.getLongitude());
        //
        // intersectionPoint = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // Position topA = Position.create(intersectionPoint.getLatitude(), intersectionPoint.getLongitude());

        // datumPolygon.add(dst3Center);

        // datumPolygon.add(dst1.getA());
        // // datumPolygon.add(dst1.getB());
        //
        // // datumPolygon.add(dst2.getA());
        // // datumPolygon.add(dst2.getB());
        //
        // datumPolygon.add(dst3.getA());
        // datumPolygon.add(dst3.getB());
        //
        // datumPolygon.add(dst3.getC());
        // // datumPolygon.add(dst3.getD());
        //
        // datumPolygon.add(dst2.getC());
        // // datumPolygon.add(dst2.getD());
        //
        // datumPolygon.add(dst1.getC());
        // datumPolygon.add(dst1.getD());

        // datumPolygon.add(dst3.getA());
        // datumPolygon.add(dst3.getB());
        // datumPolygon.add(dst3.getC());
        // datumPolygon.add(dst3.getD());
        //
        //
        //
        //
        // datumPolygon.add(dst2.getC());
        //
        //
        // datumPolygon.add(dst2.getD());
        //
        //
        //
        // datumPolygon.add(dst1.getC());
        // datumPolygon.add(dst1.getD());
        //
        //

        // datumPolygon.add(dst1.getA());
        // datumPolygon.add(dst1.getB());
        //
        // datumPolygon.add(dst2.getA());
        // datumPolygon.add(dst2.getB());
        //
        // datumPolygon.add(dst3.getA());
        // datumPolygon.add(dst3.getB());
        // datumPolygon.add(dst3.getC());
        // datumPolygon.add(dst3.getD());
        //
        //
        //
        //
        // datumPolygon.add(dst2.getC());
        //
        //
        // datumPolygon.add(dst2.getD());
        //
        //
        //
        // datumPolygon.add(dst1.getC());
        // datumPolygon.add(dst1.getD());
        //

        //
        // for (int i = 0; i < data.getDatumPointDataSets().size(); i++) {
        //
        // datumPolygon.add(data.getDatumPointDataSets().get(i).getA());
        // datumPolygon.add(data.getDatumPointDataSets().get(i).getB());
        // datumPolygon.add(data.getDatumPointDataSets().get(i).getC());
        // datumPolygon.add(data.getDatumPointDataSets().get(i).getD());
        //
        // }

        data.setDatumLinePolygon(datumPolygon);

        return data;

    }

    private void findSmallestSquare(DatumPointData data) {

        // Variable used to create intersection lines
        int directionLength = 500000;

        // Find circles furthest from each other - that will be the encompassing
        // box - after this box has been decided grow the box so it encompasses
        // the last circle.

        // Will always be max and min?

        Position datumMin = data.getDatumMin();
        Position datumMax = data.getDatumMax();
        Position datumDownWind = data.getDatumDownWind();

        double radiusMin = data.getRadiusMin();
        double radiusMax = data.getRadiusMax();
        double radiusDownWind = data.getRadiusDownWind();

        // Start by calculating on the largest circle, will set the first
        // bounds.
        Position startPos = datumMax;
        Position endPos = datumMin;
        double startRadius = radiusMax;
        double endRadius = radiusMax;

        if (radiusMin > radiusMax) {
//            System.out.println("Starting with min");
            startPos = datumMin;
            startRadius = radiusMin;
            endPos = datumMax;
            endRadius = radiusMax;
        } else {
//            System.out.println("Starting with max");
            startPos = datumMax;
            startRadius = radiusMax;
            endPos = datumMin;
            endRadius = radiusMin;
        }

        // Bearing between the two points - this will be the direction of the
        // box.
        double lengthBearing = Calculator.bearing(startPos, endPos, Heading.RL);

        Position TopPointStart = Calculator.findPosition(startPos, Calculator.turn90Minus(lengthBearing),
                Converter.nmToMeters(startRadius));
        Position BottomPointStart = Calculator.findPosition(startPos, Calculator.turn90Plus(lengthBearing),
                Converter.nmToMeters(startRadius));

        Position internalA = Calculator.findPosition(TopPointStart, Calculator.reverseDirection(lengthBearing),
                Converter.nmToMeters(startRadius));
        Position internalC = Calculator.findPosition(BottomPointStart, Calculator.reverseDirection(lengthBearing),
                Converter.nmToMeters(startRadius));

        // System.out.println("Length bearing is " + lengthBearing);
        // System.out.println("Reversed its " +
        // Calculator.reverseDirection(lengthBearing));
        Position endDirectionPoint = Calculator.findPosition(endPos, lengthBearing, Converter.nmToMeters(endRadius));

        Position endDirectionPointMinus90 = Calculator.findPosition(endDirectionPoint, Calculator.turn90Minus(lengthBearing),
                directionLength);
        Position endDirectionPointPlus90 = Calculator.findPosition(endDirectionPoint, Calculator.turn90Plus(lengthBearing),
                directionLength);

        Geo a1 = new Geo(endDirectionPoint.getLatitude(), endDirectionPoint.getLongitude());
        Geo a2 = new Geo(endDirectionPointMinus90.getLatitude(), endDirectionPointMinus90.getLongitude());

        Position directionFromA = Calculator.findPosition(internalA, lengthBearing, directionLength);
        Position directionFromC = Calculator.findPosition(internalC, lengthBearing, directionLength);

        Geo b1 = new Geo(internalA.getLatitude(), internalA.getLongitude());
        Geo b2 = new Geo(directionFromA.getLatitude(), directionFromA.getLongitude());

        Geo intersection = Intersection.segmentsIntersect(a1, a2, b1, b2);

        // System.out.println(a1);
        // System.out.println(a2);
        // System.out.println(b1);
        // System.out.println(b2);
        //
        // System.out.println("Intersectin at : " + intersection);

        Position internalB = Position.create(intersection.getLatitude(), intersection.getLongitude());

        a1 = new Geo(endDirectionPoint.getLatitude(), endDirectionPoint.getLongitude());
        a2 = new Geo(endDirectionPointPlus90.getLatitude(), endDirectionPointPlus90.getLongitude());

        b1 = new Geo(internalC.getLatitude(), internalC.getLongitude());
        b2 = new Geo(directionFromC.getLatitude(), directionFromC.getLongitude());

//        System.out.println(a1);
//        System.out.println(a2);
//        System.out.println(b1);
//        System.out.println(b2);
//
//        System.out.println("Internal A is: " + internalA);
//        System.out.println("Internal B is: " + internalB);
//        System.out.println("Internal C is: " + internalC);

        intersection = Intersection.segmentsIntersect(a1, a2, b1, b2);
        // System.out.println("Intersectin at : " + intersection);

        Position internalD = Position.create(intersection.getLatitude(), intersection.getLongitude());

        // Now we must encompass the DW circle.

        // Which direction?
        // Check for intersection

        // Check one direction first
        Position downWindParallelMinus = Calculator.findPosition(datumDownWind, Calculator.turn90Minus(lengthBearing),
                Converter.nmToMeters(radiusDownWind));

        // Which one intersects?

        a1 = ParseUtils.PositionToGeo(datumDownWind);
        a2 = ParseUtils.PositionToGeo(downWindParallelMinus);

        b1 = ParseUtils.PositionToGeo(internalA);
        b2 = ParseUtils.PositionToGeo(internalB);

        if (Intersection.segIntersects(a1, a2, b1, b2)) {

            // System.out.println("Modify in direction " +
            // datumDownWind.rhumbLineBearingTo(downWindParallelMinus));

            double direction = datumDownWind.rhumbLineBearingTo(downWindParallelMinus);

            Position downWindGrowCenter = Calculator.findPosition(datumDownWind, direction, Converter.nmToMeters(radiusDownWind));

            Position downWindLeft = Calculator.findPosition(downWindGrowCenter, Calculator.reverseDirection(lengthBearing),
                    directionLength);
            Position downWindRight = Calculator.findPosition(downWindGrowCenter, lengthBearing, directionLength);

            Position AGrow = Calculator.findPosition(internalA, direction, directionLength);
            Position BGrow = Calculator.findPosition(internalB, direction, directionLength);

            Geo newA = Intersection.segmentsIntersect(ParseUtils.PositionToGeo(downWindGrowCenter),
                    ParseUtils.PositionToGeo(downWindLeft), ParseUtils.PositionToGeo(internalA), ParseUtils.PositionToGeo(AGrow));

            Geo newB = Intersection.segmentsIntersect(ParseUtils.PositionToGeo(downWindGrowCenter),
                    ParseUtils.PositionToGeo(downWindRight), ParseUtils.PositionToGeo(internalB), ParseUtils.PositionToGeo(BGrow));

            if (newA != null) {
                internalA = ParseUtils.GeoToPosition(newA);

            } else {
                System.out.println("ERROR Internal A");
            }

            if (newB != null) {
                internalB = ParseUtils.GeoToPosition(newB);

            } else {
                System.out.println("ERROR Internal B");
            }

            data.setA(internalA);
            data.setB(internalB);

            // Modify A - find intersection

            // Modify A and B

        } else {
//            System.out.println("Modify in direction "
//                    + Calculator.reverseDirection(datumDownWind.rhumbLineBearingTo(downWindParallelMinus)));

            double direction = Calculator.reverseDirection(datumDownWind.rhumbLineBearingTo(downWindParallelMinus));

            Position downWindGrowCenter = Calculator.findPosition(datumDownWind, direction, Converter.nmToMeters(radiusDownWind));

            Position downWindLeft = Calculator.findPosition(downWindGrowCenter, Calculator.reverseDirection(lengthBearing),
                    directionLength);
            Position downWindRight = Calculator.findPosition(downWindGrowCenter, lengthBearing, directionLength);

            Position CGrow = Calculator.findPosition(internalC, direction, directionLength);
            Position DGrow = Calculator.findPosition(internalD, direction, directionLength);

            Geo newC = Intersection.segmentsIntersect(ParseUtils.PositionToGeo(downWindGrowCenter),
                    ParseUtils.PositionToGeo(downWindLeft), ParseUtils.PositionToGeo(internalC), ParseUtils.PositionToGeo(CGrow));

            Geo newD = Intersection.segmentsIntersect(ParseUtils.PositionToGeo(downWindGrowCenter),
                    ParseUtils.PositionToGeo(downWindRight), ParseUtils.PositionToGeo(internalD), ParseUtils.PositionToGeo(DGrow));

            if (newC != null && newD != null) {

                internalC = ParseUtils.GeoToPosition(newC);
                internalD = ParseUtils.GeoToPosition(newD);

                data.setD(internalC);
                data.setC(internalD);
            } else {
//                System.out.println("is already inside");
            }
            // Modify C and D
        }

        //
        // System.out.println("Does downwind parallele minus intersect with A to B: "
        // + Intersection.segIntersects(a1, a2, b1, b2));
        // //if yes then we modify A and B
        //
        // b1 = ParseUtils.PositionToGeo(internalC);
        // b2 = ParseUtils.PositionToGeo(internalD);
        //
        // System.out.println("Does downwind parallele minus intersect with C to D: "
        // + Intersection.segIntersects(a1, a2, b1, b2));
        // if yes then we modify C and D

        // A to B
        // C to D

        // //Bearing from largest circle ie. startPos to downwind will tell us
        // which direction it should grow
        // double startPosToDownwind =
        // startPos.rhumbLineBearingTo(datumDownWind);
        // System.out.println("downwind bearing to start " +
        // startPosToDownwind);
        //

        // if (startPosToDownwind < 180){
        // //grow in direction from minus bearing
        // double growDirection = Calculator.turn90Minus(lengthBearing);
        // System.out.println("Grow in direction " + growDirection);
        // }else{
        // //grow in direction from plus bearing
        //
        // double growDirection = Calculator.turn90Plus(lengthBearing);
        // System.out.println("Grow in direction " + growDirection);
        // }

        // 56°33,5 N 12°01,1 E
        // 56°33,5 N 11°54,5 E
        // 56°30,3 N 11°54,5 E
        // 56°30,3 N 12°01,1 E

        data.setA(internalA);
        data.setD(internalC);

        data.setB(internalB);
        data.setC(internalD);

        // Test values
        // try {
        // double lat1 = ParseUtils.parseLatitude("56 33.5 N");
        // double lon1 = ParseUtils.parseLongitude("12 01.1 E");
        //
        // Position A = Position.create(lat1, lon1);
        //
        // double lat2 = ParseUtils.parseLatitude("56 33.5 N");
        // double lon2 = ParseUtils.parseLongitude("11 54.5 E");
        //
        // Position B = Position.create(lat2, lon2);
        //
        // double lat3 = ParseUtils.parseLatitude("56 30.3 N");
        // double lon3 = ParseUtils.parseLongitude("11 54.5 E");
        //
        // Position C = Position.create(lat3, lon3);
        //
        // double lat4 = ParseUtils.parseLatitude("56 30.3 N");
        // double lon4 = ParseUtils.parseLongitude("12 01.1 E");
        //
        // Position D = Position.create(lat4, lon4);
        //
        //
        // data.setA(A);
        // data.setD(B);
        //
        // data.setB(D);
        // data.setC(C);
        //
        // } catch (FormatException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //

        //
        // // Find A and D
        //
        // double direction = lengthBearing + 90;
        // if (direction > 360){
        // direction = direction - 360;
        // }
        //
        // // Find top Position from radius
        // Position TopPointMax = Calculator.calculateEndingGlobalCoordinates(
        // reference, datumMax, direction,
        // Converter.nmToMeters(radiusMax), endBearing);
        //
        // endBearing = new double[1];
        //
        //
        // direction = lengthBearing - 90;
        // if (direction < 0){
        // direction = direction + 360;
        // }
        //
        // Position BottomPointMax =
        // Calculator.calculateEndingGlobalCoordinates(
        // reference, datumMax, direction,
        // Converter.nmToMeters(radiusMax), endBearing);
        // endBearing = new double[1];
        //
        // Position topPointMaxInnerBox = Calculator
        // .calculateEndingGlobalCoordinates(reference, TopPointMax,
        // Calculator.reverseDirection(lengthBearing),
        // Converter.nmToMeters(radiusMax),
        // endBearing);
        //
        //
        //
        //
        // // data.setA(topPointMaxInnerBox);
        //
        // Position bottomPointMaxInnerBox = Calculator
        // .calculateEndingGlobalCoordinates(reference, BottomPointMax,
        // Calculator.reverseDirection(lengthBearing),
        // Converter.nmToMeters(radiusMax),
        // endBearing);
        //
        // // data.setD(bottomPointMaxInnerBox);
        //
        //
        // direction = lengthBearing + 90;
        // if (direction > 360){
        // direction = direction - 360;
        // }
        //
        //
        // // Find top Position from radius
        // Position TopPointMin = Calculator.calculateEndingGlobalCoordinates(
        // reference, datumMin, direction,
        // Converter.nmToMeters(radiusMin), endBearing);
        //
        // endBearing = new double[1];
        //
        //
        // direction = lengthBearing - 90;
        // if (direction < 0){
        // direction = direction + 360;
        // }
        //
        // Position BottomPointMin =
        // Calculator.calculateEndingGlobalCoordinates(
        // reference, datumMin, direction,
        // Converter.nmToMeters(radiusMin), endBearing);
        // endBearing = new double[1];
        //
        // // Position top of min circle, turn 180 around and go radius distance
        // to
        // // find a box point
        // Position topPointMinInnerBox = Calculator
        // .calculateEndingGlobalCoordinates(reference, TopPointMin,
        // lengthBearing, Converter.nmToMeters(radiusMin),
        // endBearing);
        //
        // // data.setB(topPointMinInnerBox);
        //
        // endBearing = new double[1];
        //
        // Position bottomPointMinInnerBox = Calculator
        // .calculateEndingGlobalCoordinates(reference, BottomPointMin,
        // lengthBearing, Converter.nmToMeters(radiusMin),
        // endBearing);
        // endBearing = new double[1];
        //
        // // data.setC(bottomPointMinInnerBox);
        //
        // System.out.println("Bearing from datum to max is");
        // double bearingFromDownwindToMax = Calculator.bearing(datumDownWind,
        // datumMax, Heading.RL);
        // System.out.println(bearingFromDownwindToMax);
        //
        // double growthBearing;
        //
        //
        // System.out.println("Length bearing of normal box is " +
        // lengthBearing);
        //
        // if (bearingFromDownwindToMax > 90 && bearingFromDownwindToMax < 270)
        // {
        // growthBearing = lengthBearing + 90;
        // }else{
        // growthBearing = -(lengthBearing + 90);
        // }
        //
        // // Going down - replace D, C
        // growthBearing = lengthBearing + 90;
        //
        //
        // System.out.println("Growing in plus 90 " + growthBearing);
        //
        // double boxLength = topPointMaxInnerBox.distanceTo(
        // topPointMinInnerBox, CoordinateSystem.GEODETIC);
        //
        // System.out.println("Length of Box: " + boxLength);
        //
        // Position growthCenterPosition = Calculator
        // .calculateEndingGlobalCoordinates(reference, datumDownWind,
        // growthBearing, Converter.nmToMeters(radiusDownWind),
        // endBearing);
        // endBearing = new double[1];
        //
        // Position A = Calculator.calculateEndingGlobalCoordinates(reference,
        // growthCenterPosition, Calculator.reverseDirection(lengthBearing),
        // boxLength/2, endBearing);
        // endBearing = new double[1];
        // // data.setA(A);
        //
        // Position B = Calculator.calculateEndingGlobalCoordinates(reference,
        // growthCenterPosition, lengthBearing,
        // boxLength/2, endBearing);
        // endBearing = new double[1];

        // data.setB(B);

        // Position An = data.getA();
        // Position Bn = data.getB();
        // Position Cn = data.getC();
        // Position Dn = data.getD();
        // //
        // //
        // data.setA(Dn);
        // data.setB(Cn);
        // //
        // data.setC(Bn);
        // data.setD(An);

    }

    public Position applyDriftToPoint(SARData data, Position point, double timeElapsed) {

        if (data instanceof SimpleSAR){
            return point;
        }
        double currentTWC = data.getWeatherPoints().get(0).getTWCknots() * timeElapsed;
        double leewayspeed = searchObjectValue(data.getSearchObject(), data.getWeatherPoints().get(0).getLWknots());
        double leeway = leewayspeed * timeElapsed;

        Ellipsoid reference = Ellipsoid.WGS84;
        double[] endBearing = new double[1];

        // Object starts at LKP, with TWCheading, drifting for currentWTC
        // knots where will it end up
        Position currentPos = Calculator.calculateEndingGlobalCoordinates(reference, point, data.getWeatherPoints().get(0)
                .getTWCHeading(), Converter.nmToMeters(currentTWC), endBearing);

        endBearing = new double[1];

        Position windPos = Calculator.calculateEndingGlobalCoordinates(reference, currentPos, data.getWeatherPoints().get(0)
                .getDownWind(), Converter.nmToMeters(leeway), endBearing);

        Position datum = windPos;

        return datum;
    }

    private RapidResponseData rapidResponse(RapidResponseData data) {

        // We need to calculate for each weather point

        List<SARWeatherData> weatherPoints = data.getWeatherPoints();

        DateTime startTime = data.getLKPDate();

        List<Double> weatherPointsValidFor = new ArrayList<Double>();

        List<Position> datumPositions = new ArrayList<Position>();

        List<Position> currentPositions = new ArrayList<Position>();

        for (int i = 0; i < weatherPoints.size(); i++) {

            // Do we have a next?

            // How long is the data point valid for?

            // Is it the last one?

            if (i == weatherPoints.size() - 1) {
                // It's the last one - let it last the remainder
                double validFor = (double) (data.getCSSDate().getMillis() - startTime.getMillis()) / 60 / 60 / 1000;
                weatherPointsValidFor.add(validFor);
            } else {

                DateTime current = weatherPoints.get(i).getDateTime();

                if (current.isBefore(data.getLKPDate())) {
                    current = data.getLKPDate();
                }

                startTime = weatherPoints.get(i + 1).getDateTime();

                double validFor = (double) (startTime.getMillis() - current.getMillis()) / 60 / 60 / 1000;
                weatherPointsValidFor.add(validFor);
            }

            // How long is this data point valid for

        }

        // for (int i = 0; i < weatherPointsValidFor.size(); i++) {
        // System.out.println("Weather point " + i + " is valid for " +
        // weatherPointsValidFor.get(i) + " hours");
        // }

        for (int i = 0; i < weatherPoints.size(); i++) {
            SARWeatherData weatherObject = weatherPoints.get(i);
            double validFor = weatherPointsValidFor.get(i);

//            System.out.println("Valid for : " + validFor);

            double currentTWC = weatherObject.getTWCknots() * validFor;

//            System.out.println("Current TWC: " + currentTWC);
//            System.out.println("Heading TWC: " + weatherObject.getLWHeading());

            double leewayspeed = searchObjectValue(data.getSearchObject(), weatherObject.getLWknots());
            double leeway = leewayspeed * validFor;

            Position startingLocation = null;

            if (i == 0) {
                startingLocation = data.getLKP();
            } else {
                startingLocation = datumPositions.get(i - 1);
            }

            Position currentPos = Calculator.findPosition(startingLocation, weatherObject.getTWCHeading(),
                    Converter.nmToMeters(currentTWC));

            currentPositions.add(currentPos);

//            System.out.println("Current is: " + currentPos.getLatitude());
//            System.out.println("Current is: " + currentPos.getLongitude());

            Position windPos = Calculator.findPosition(currentPos, weatherObject.getDownWind(), Converter.nmToMeters(leeway));

            datumPositions.add(windPos);

            data.setDatum(windPos);

        }

        // datumPositions.remove(datumPositions.size()-1);

        Position datumPosition = data.getDatum();

        // datumPositions.remove(datumPositions.size()-1);

        data.setWindList(datumPositions);
        data.setCurrentList(currentPositions);

        // RDV Direction

        double rdvDirection;

        // RDV Distance
        double rdvDistance;

        // RDV Speed
        double rdvSpeed;
        if (datumPositions.size() > 1) {
            rdvDirection = Calculator.bearing(datumPositions.get(datumPositions.size() - 2), datumPosition, Heading.RL);

            rdvDistance = Calculator.range(datumPositions.get(datumPositions.size() - 2), datumPosition, Heading.RL);

            // RDV Speed
            rdvSpeed = rdvDistance / weatherPointsValidFor.get(weatherPointsValidFor.size() - 1);

        } else {
            rdvDirection = Calculator.bearing(data.getLKP(), datumPosition, Heading.RL);

            rdvDistance = Calculator.range(data.getLKP(), datumPosition, Heading.RL);

            // RDV Speed
            rdvSpeed = rdvDistance / data.getTimeElasped();
        }

        data.setRdvDirection(rdvDirection);
        data.setRdvDistance(rdvDistance);
        data.setRdvSpeed(rdvSpeed);

        //
        // // double rdvDirection = Calculator.bearing(data.getLKP(),
        // datumPosition,
        // // Heading.RL);
        //
        // data.setRdvDirection(rdvDirection);
        // System.out.println("RDV Direction: " + rdvDirection);
        //
        // if (datumPositions.size() > 1){
        // double rdvDirectionLast =
        // Calculator.bearing(datumPositions.get(datumPositions.size()-2),
        // datumPosition,
        // Heading.RL);
        // data.setRdvDirectionLast(rdvDirectionLast);
        // }else{
        // data.setRdvDirectionLast(rdvDirection);
        // }
        //
        //
        //
        //
        // // RDV Distance
        // double rdvDistance = Calculator.range(data.getLKP(), datumPosition,
        // Heading.RL);
        //
        // data.setRdvDistance(rdvDistance);
        // System.out.println("RDV Distance: " + rdvDistance);
        //
        // // RDV Speed
        // double rdvSpeed = rdvDistance / data.getTimeElasped();
        //
        // data.setRdvSpeed(rdvSpeed);
        // System.out.println("RDV Speed: " + rdvSpeed);

        // Radius:
        double radius = ((data.getX() + data.getY()) + 0.3 * rdvDistance) * data.getSafetyFactor();

        data.setRadius(radius);

//        System.out.println("Radius is: " + radius);

        findRapidResponseBox(datumPosition, radius, data);

        // voctManager.setSarData(data);
        return data;
    }

    public static void findRapidResponseBox(Position datum, double radius, RapidResponseData data) {
        // Search box
        // The box is square around the circle, with center point at datum
        // Radius is the calculated Radius
        // data.getRdvDirection()
        double verticalDirection = data.getRdvDirection();
        double horizontalDirection = verticalDirection + 90;

        if (horizontalDirection > 360) {
            horizontalDirection = horizontalDirection - 360;
        }

        // First top side of the box
        Position topCenter = Calculator.findPosition(datum, verticalDirection, Converter.nmToMeters(radius));

        // Bottom side of the box
        Position bottomCenter = Calculator.findPosition(datum, Calculator.reverseDirection(verticalDirection),
                Converter.nmToMeters(radius));

        // Go left radius length
        Position A = Calculator.findPosition(topCenter, Calculator.reverseDirection(horizontalDirection),
                Converter.nmToMeters(radius));
        Position B = Calculator.findPosition(topCenter, horizontalDirection, Converter.nmToMeters(radius));
        Position C = Calculator.findPosition(bottomCenter, horizontalDirection, Converter.nmToMeters(radius));
        Position D = Calculator.findPosition(bottomCenter, Calculator.reverseDirection(horizontalDirection),
                Converter.nmToMeters(radius));

//        System.out.println("Final box parameters:");
//        System.out.println("A: " + A.getLatitude());
//        System.out.println("A: " + A.getLongitude());
//
//        System.out.println("B: " + B.getLatitude());
//        System.out.println("B: " + B.getLongitude());
//
//        System.out.println("C: " + C.getLatitude());
//        System.out.println("C: " + C.getLongitude());
//
//        System.out.println("D: " + D.getLatitude());
//        System.out.println("D: " + D.getLongitude());

//        double area = radius * 2 * radius * 2;

//        System.out.println("Area in nm2 is: " + area);

        // Effort Allocation /// wait

        // // desired pod, in decimals
        // double pod = 0.79;
        //
        // // Swep Width
        // double W = 0.5;
        //
        // // System.out.println("W : 0.8");
        // // System.out.println("S : 0.7875");
        //
        // // System.out.println("POD is " + findPoD(0.8, 0.7875));
        // System.out.println("POD is " + findPoD(1.1, 0.8));

        data.setBox(A, B, C, D);

    }

    public SAR_TYPE getOperationType() {
        return this.operationType;
    }

    public void calculateEffortAllocation(SARData data) {

        for (Entry<Long, EffortAllocationData> entry : data.getEffortAllocationData().entrySet()) {
            EffortAllocationData effortAllocationData = entry.getValue();

            double trackSpacing = findS(effortAllocationData.getW(), effortAllocationData.getPod());

            effortAllocationData.setTrackSpacing(trackSpacing);

            double groundSpeed = effortAllocationData.getGroundSpeed();
            double timeSearching = effortAllocationData.getSearchTime();

//            System.out.println("Track Spacing is: " + trackSpacing);
//            System.out.println("Ground speed is: " + groundSpeed);
//            System.out.println("Time searching is: " + timeSearching);

            double areaSize = trackSpacing * groundSpeed * timeSearching;

            effortAllocationData.setEffectiveAreaSize(areaSize);

        }

    }

    private double findS(double W, double PoD) {
        // S = W*(-5/8*ln(1-x))^(-5/7)

        double val1 = (-5.0 / 8.0) * Math.log(1 - PoD);
        double val2 = Math.pow(val1, -5.0 / 7.0);

        // System.out.println("Val 1 is " + val1);
        // System.out.println("Val 2 is " + val2);

        return W * val2;
    }

}
