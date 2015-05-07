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
package dk.dma.epd.shore.layers.voct;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.bbn.openmap.event.MapMouseListener;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMList;
import com.bbn.openmap.proj.coords.LatLonPoint;

import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.voct.SARAreaData;
import dk.dma.epd.common.prototype.layers.voct.EffortAllocationAreaGraphics;
import dk.dma.epd.common.prototype.layers.voct.EffortAllocationAreaGraphics.LineType;
import dk.dma.epd.common.prototype.layers.voct.EffortAllocationInternalGraphics;
import dk.dma.epd.common.prototype.layers.voct.EffortAllocationLines;
import dk.dma.epd.common.prototype.layers.voct.SarAreaGraphic;
import dk.dma.epd.common.prototype.layers.voct.SarGraphics;
import dk.dma.epd.common.prototype.model.voct.SAR_TYPE;
import dk.dma.epd.common.prototype.model.voct.sardata.DatumLineData;
import dk.dma.epd.common.prototype.model.voct.sardata.DatumPointData;
import dk.dma.epd.common.prototype.model.voct.sardata.DatumPointDataSARIS;
import dk.dma.epd.common.prototype.model.voct.sardata.EffortAllocationData;
import dk.dma.epd.common.prototype.model.voct.sardata.RapidResponseData;
import dk.dma.epd.common.prototype.model.voct.sardata.SARData;
import dk.dma.epd.common.prototype.model.voct.sardata.SimpleSAR;
import dk.dma.epd.common.prototype.voct.VOCTUpdateEvent;
import dk.dma.epd.shore.EPDShore;
import dk.dma.epd.shore.voct.SRU;

public class VoctLayerPlanning extends VoctLayerCommon {

    private static final long serialVersionUID = 1L;

    private OMGraphicList graphics = new OMGraphicList();
    private OMGraphic selectedGraphic;

    private boolean dragging;

    private List<EffortAllocationAreaGraphics> effectiveSRUAreas = new ArrayList<EffortAllocationAreaGraphics>();

    private SarAreaGraphic sarArea;
    private SarGraphics sarGraphics;

    public VoctLayerPlanning() {

    }

    @Override
    public synchronized OMGraphicList prepare() {
        graphics.project(getProjection());
        return graphics;
    }

    @Override
    public MapMouseListener getMapMouseListener() {
        return this;
    }

    @Override
    public boolean mousePressed(MouseEvent paramMouseEvent) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mouseReleased(MouseEvent e) {
        if (dragging) {
            dragging = false;
            // doPrepare();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseEvent e) {
        // System.out.println("Mouse Clicked");
        if (e.getButton() != MouseEvent.BUTTON3) {
            return false;
        }

        selectedGraphic = null;
        OMList<OMGraphic> allClosest = graphics.findAll(e.getX(), e.getY(), 3.0f);

        for (OMGraphic omGraphic : allClosest) {
            if (omGraphic instanceof EffortAllocationInternalGraphics) {
                // System.out.println("Selected Effective Area");
                selectedGraphic = omGraphic;
                break;
            }
        }

        // if (selectedGraphic instanceof WaypointCircle) {
        // WaypointCircle wpc = (WaypointCircle) selectedGraphic;
        // // mainFrame.getGlassPane().setVisible(false);
        // waypointInfoPanel.setVisible(false);
        // routeMenu.routeWaypointMenu(wpc.getRouteIndex(), wpc.getWpIndex());
        // routeMenu.setVisible(true);
        // // routeMenu.show(this, e.getX() - 2, e.getY() - 2);
        // routeMenu(e);
        // return true;
        // }

        return false;
    }

    @Override
    public void mouseEntered(MouseEvent paramMouseEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent paramMouseEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean mouseDragged(MouseEvent e) {
        // System.out.println("Mouse dragged!");
        if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) {
            return false;
        }

        if (!dragging) {
            // mainFrame.getGlassPane().setVisible(false);
            selectedGraphic = null;
            OMList<OMGraphic> allClosest = graphics.findAll(e.getX(), e.getY(), 3.0f);
            for (OMGraphic omGraphic : allClosest) {
                if (omGraphic instanceof EffortAllocationLines) {
                    // System.out.println("selected something");
                    selectedGraphic = omGraphic;
                    break;
                } else {
                    if (omGraphic instanceof EffortAllocationInternalGraphics) {
                        // System.out.println("selected something");
                        selectedGraphic = omGraphic;
                        // break;
                    }
                    // if (|| omGraphic instanceof AreaInternalGraphics)
                }
            }
        }

        if (selectedGraphic instanceof EffortAllocationLines) {
            // System.out.println("Selected line");
            EffortAllocationLines selectedLine = (EffortAllocationLines) selectedGraphic;

            // If bottom or top we can only adjust latitude

            // If sides we can adjust longitude

            // New Position of line
            LatLonPoint newLatLon = mapBean.getProjection().inverse(e.getPoint());

            Position newPos = Position.create(newLatLon.getLatitude(), newLatLon.getLongitude());

            selectedLine.updateArea(newPos);

            doPrepare();
            dragging = true;
            updateEffectiveAreaLocation(voctManager.getSarData());
            return true;

        }

        if (selectedGraphic instanceof EffortAllocationInternalGraphics) {
            // System.out.println("Moving box");
            EffortAllocationInternalGraphics selectedArea = (EffortAllocationInternalGraphics) selectedGraphic;

            // New Center
            LatLonPoint newLatLon = mapBean.getProjection().inverse(e.getPoint());

            Position newPos = Position.create(newLatLon.getLatitude(), newLatLon.getLongitude());

            if (!dragging) {
                // System.out.println("only once? first time?");
                selectedArea.adjustInternalPosition(newPos);
            }

            // if (!(newPos == initialBoxRelativePosition)){
            selectedArea.moveRelative(newPos, voctManager.getSarData());
            // }

            doPrepare();
            dragging = true;
            updateEffectiveAreaLocation(voctManager.getSarData());
            return true;

        }

        return false;
    }

    @Override
    public boolean mouseMoved(MouseEvent e) {
        if (!dragging) {
            // mainFrame.getGlassPane().setVisible(false);
            selectedGraphic = null;
            OMList<OMGraphic> allClosest = graphics.findAll(e.getX(), e.getY(), 2.0f);
            for (OMGraphic omGraphic : allClosest) {
                if (omGraphic instanceof EffortAllocationLines) {
                    // System.out.println("selected something");
                    selectedGraphic = omGraphic;
                    break;
                } else {
                    if (omGraphic instanceof EffortAllocationInternalGraphics) {
                        // System.out.println("selected something");
                        selectedGraphic = omGraphic;
                        // break;
                    }
                    // if (|| omGraphic instanceof AreaInternalGraphics)
                }
            }
        }

        if (selectedGraphic instanceof EffortAllocationLines) {
            // System.out.println("Selected line");
            EffortAllocationLines selectedLine = (EffortAllocationLines) selectedGraphic;

            double bearing = selectedLine.getA().rhumbLineBearingTo(selectedLine.getB());
//            System.out.println(bearing);

            LineType type = selectedLine.getType();

            Cursor cursor = null;

            if (type == LineType.BOTTOM) {
                cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);

                // Straight line
                if (bearing > 80 && bearing < 100 || bearing > 260 && bearing < 280) {
                    cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                }

                // SE line
                if (bearing > 100 && bearing < 170 || bearing > 290 && bearing < 350) {
                    cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                }

                // SW line
                if (bearing > 0 && bearing < 80 || bearing > 190 && bearing < 270) {
                    cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                }

            }

            if (type == LineType.TOP) {
                cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);

                // Straight line
                if (bearing > 80 && bearing < 100 || bearing > 260 && bearing < 280) {
                    cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                }

                // NE line
                if (bearing > 100 && bearing < 170 || bearing > 290 && bearing < 350) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                }

                // NW line
                if (bearing > 0 && bearing < 80 || bearing > 190 && bearing < 270) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                }

            }

            if (type == LineType.LEFT) {
                cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

                // Straight line
                if (bearing > 170 && bearing < 190 || bearing < 10 && bearing < 350) {
                    cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                }

                // NE line
                if (bearing > 130 && bearing < 160 || bearing > 210 && bearing < 240) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                }

                // NW line
                if (bearing > 130 && bearing < 160 || bearing > 300 && bearing < 330) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                }

            }

            if (type == LineType.RIGHT) {
                cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);

                // Straight line
                if (bearing > 170 && bearing < 190 || bearing < 10 && bearing < 350) {
                    cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                }

                // NE line
                if (bearing > 130 && bearing < 160 || bearing > 210 && bearing < 240) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                }

                // NW line
                if (bearing > 130 && bearing < 160 || bearing > 300 && bearing < 330) {
                    cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                }

            }

            jMapFrame.getGlassPane().setVisible(true);
            jMapFrame.getGlassPane().setCursor(cursor);

            return true;

        }

        if (selectedGraphic != null && selectedGraphic instanceof EffortAllocationInternalGraphics) {
            jMapFrame.getGlassPane().setVisible(true);
            jMapFrame.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            return true;

        }

        jMapFrame.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        jMapFrame.getGlassPane().setVisible(false);

        return false;
    }

    @Override
    public void mouseMoved() {
        graphics.deselect();
        repaint();
    }

    @Override
    public void voctUpdated(VOCTUpdateEvent e) {

        if (e == VOCTUpdateEvent.SAR_CANCEL) {
            graphics.clear();
            this.setVisible(false);
        }

        if (e == VOCTUpdateEvent.SAR_DISPLAY) {

            if (voctManager.getSarType() == SAR_TYPE.RAPID_RESPONSE) {
                drawRapidResponse();
//                System.out.println("Painting Rapid Response");
            }
            if (voctManager.getSarType() == SAR_TYPE.DATUM_POINT) {
                drawDatumPoint();
            }
            if (voctManager.getSarType() == SAR_TYPE.DATUM_LINE) {
                drawDatumLine();
            }

            if (voctManager.getSarType() == SAR_TYPE.SARIS_DATUM_POINT) {
                drawSarisDatumPoint();
            }
            
            
            if (voctManager.getSarType() == SAR_TYPE.SIMPLE_SAR) {
                drawSimpleSar();
            }
            
            this.setVisible(true);
        }

        if (e == VOCTUpdateEvent.EFFORT_ALLOCATION_DISPLAY) {
            createEffectiveArea();
            this.setVisible(true);
        }

        if (e == VOCTUpdateEvent.EFFORT_ALLOCATION_SERIALIZED) {
//            System.out.println("Draw Effort Allocation Areas");
            drawEffortAllocationAreasFromSerialization();
        }

    }

    private void drawSimpleSar() {

        graphics.clear();
        
        SimpleSAR data = (SimpleSAR) voctManager
                .getSarData();

        
        
//        for (int i = 0; i < data.getSarAreaData().size(); i++) {
////
//            SARAreaData sarArea = data.getSarAreaData().get(i);
//
            SarGraphics sarAreaGraphic = new SarGraphics(data.getA(),
                    data.getB(), data.getC(), data.getD(),
                    data.getDatum(), "");

            graphics.add(sarAreaGraphic);
//        }

        doPrepare();
        this.setVisible(true);
    }
    
    private void drawEffortAllocationAreasFromSerialization() {

        // Probability of Detection Area - updateable

        // Remove all from graphics
        for (int i = 0; i < effectiveSRUAreas.size(); i++) {

            graphics.remove(effectiveSRUAreas.get(i));

        }

        SARData data = voctManager.getSarData();

        // for (int i = 0; i < sruManager.getSRUsAsList().length; i++) {

        for (Entry<Long, SRU> entry : EPDShore.getInstance().getSruManager().getSRUs().entrySet()) {
            SRU sru = entry.getValue();

            if (data.getEffortAllocationData().containsKey(sru.getMmsi())) {

                EffortAllocationData effortAllocationArea = data.getEffortAllocationData().get(sru.getMmsi());
                EffortAllocationAreaGraphics effectiveArea;

                // if (!effortAllocationArea.isNoRedraw()) {

                effectiveArea = new EffortAllocationAreaGraphics(effortAllocationArea.getEffectiveAreaA(),
                        effortAllocationArea.getEffectiveAreaB(), effortAllocationArea.getEffectiveAreaC(),
                        effortAllocationArea.getEffectiveAreaD(), sru.getMmsi(), sru.getName());

                // TEMP
                effectiveArea.setVisible(true);

                boolean exists = false;
                for (int i = 0; i < effectiveSRUAreas.size(); i++) {

                    if (effectiveSRUAreas.get(i).getId() == sru.getMmsi()) {
                        effectiveSRUAreas.set(i, effectiveArea);
                        exists = true;
                    }

                }

                if (!exists) {
                    effectiveSRUAreas.add(effectiveArea);
//                    System.out.println("Add stuff");
                } else {
//                    System.out.println("Why can\t I add?");
                }

                // }

            }

        }

//        System.out.println("Draw Serialized stuff? " + effectiveSRUAreas.size());

        for (int i = 0; i < effectiveSRUAreas.size(); i++) {
//            System.out.println("Adding graphics");
            graphics.add(effectiveSRUAreas.get(i));
        }

        // PoD for each SRU, initialized with an effective area? possibly a
        // unique ID

        doPrepare();

    }

    private void drawSarisDatumPoint() {

        graphics.clear();

        DatumPointDataSARIS data = (DatumPointDataSARIS) voctManager.getSarData();

//        System.out.println("Draw SARIS DATUM " + data.getSarisTarget().size());

        for (int i = 0; i < data.getSarAreaData().size(); i++) {

//            System.out.println("Adding graphics");
            SARAreaData sarArea = data.getSarAreaData().get(i);

            SarGraphics sarAreaGraphic = new SarGraphics(sarArea.getA(), sarArea.getB(), sarArea.getC(), sarArea.getD(),
                    sarArea.getCentre(), data.getSarisTarget().get(i).getName());

            graphics.add(sarAreaGraphic);
        }

        doPrepare();
        this.setVisible(true);
    }

    private void drawDatumLine() {

        // Create as many data objects as is contained

        // Clear all previous
        graphics.clear();

        DatumLineData datumLineData = (DatumLineData) voctManager.getSarData();

        for (int i = 0; i < datumLineData.getDatumPointDataSets().size(); i++) {

//            System.out.println("Creating area " + i);
            DatumPointData data = datumLineData.getDatumPointDataSets().get(i);

            Position datumDownWind = data.getDatumDownWind();
            Position datumMin = data.getDatumMin();
            Position datumMax = data.getDatumMax();

            double radiusDownWind = data.getRadiusDownWind();
            double radiusMin = data.getRadiusMin();
            double radiusMax = data.getRadiusMax();

            Position LKP = data.getLKP();
            Position WTCPoint = data.getWtc();

            sarGraphics = new SarGraphics(datumDownWind, datumMin, datumMax, radiusDownWind, radiusMin, radiusMax, LKP, WTCPoint,
                    i + 1);

            graphics.add(sarGraphics);
        }

        sarArea = new SarAreaGraphic(datumLineData.getDatumLinePolygon());
        graphics.add(sarArea);

        // public SarGraphics(Position datumDownWind, Position datumMin,
        // Position datumMax, double radiusDownWind, double radiusMin, double
        // radiusMax, Position LKP, Position current) {

        doPrepare();

    }

    private void drawDatumPoint() {

        DatumPointData data = (DatumPointData) voctManager.getSarData();

        Position A = data.getA();
        Position B = data.getB();
        Position C = data.getC();
        Position D = data.getD();

        sarArea = new SarAreaGraphic(A, B, C, D);
        

        Position datumDownWind = data.getDatumDownWind();
        Position datumMin = data.getDatumMin();
        Position datumMax = data.getDatumMax();

        double radiusDownWind = data.getRadiusDownWind();
        double radiusMin = data.getRadiusMin();
        double radiusMax = data.getRadiusMax();

        Position LKP = data.getLKP();
        Position WTCPoint = data.getWtc();

        graphics.clear();

        sarGraphics = new SarGraphics(datumDownWind, datumMin, datumMax, radiusDownWind, radiusMin, radiusMax, LKP, WTCPoint);
        graphics.add(sarArea);
        graphics.add(sarGraphics);

        doPrepare();
    }

    private void drawRapidResponse() {

        RapidResponseData data = (RapidResponseData) voctManager.getSarData();

        Position A = data.getA();
        Position B = data.getB();
        Position C = data.getC();
        Position D = data.getD();

        Position datum = data.getDatum();
        double radius = data.getRadius();

        Position LKP = data.getLKP();

        graphics.clear();

        sarArea = new SarAreaGraphic(A, B, C, D);
        graphics.add(sarArea);

        sarGraphics = new SarGraphics(datum, radius, LKP, data.getCurrentList(), data.getWindList());
        graphics.add(sarGraphics);

        doPrepare();
    }

    private void createEffectiveArea() {
        // Probability of Detection Area - updateable

        // Remove all from graphics
        for (int i = 0; i < effectiveSRUAreas.size(); i++) {

            // if
            // (voctManager.getSarData().getEffortAllocationData().get(i).getEffectiveAreaA()
            // == null){
            graphics.remove(effectiveSRUAreas.get(i));
            // }

        }

        SARData data = voctManager.getSarData();

        for (Entry<Long, EffortAllocationData> entry : data.getEffortAllocationData().entrySet()) {
            EffortAllocationData effortAllocationData = entry.getValue();

            EffortAllocationAreaGraphics effectiveArea;

            if (!effortAllocationData.isNoRedraw()) {
                double effectiveAreaSize = effortAllocationData.getEffectiveAreaSize();

//                System.out.println("EFFECTIVE AREA IS " + effectiveAreaSize);

                // Effective Area: 10 nm2 Initialize by creating box
                double width = Math.sqrt(effectiveAreaSize);
                double height = Math.sqrt(effectiveAreaSize);

                effectiveArea = new EffortAllocationAreaGraphics(width, height, data, entry.getKey(), EPDShore.getInstance()
                        .getSruManager().getSRUs().get(entry.getKey()).getName());

                effectiveArea.setVisible(voctManager.getSruManager().getSRUs().get(entry.getKey()).isVisible());

                boolean exists = false;
                for (int i = 0; i < effectiveSRUAreas.size(); i++) {

                    if (effectiveSRUAreas.get(i).getId() == entry.getKey()) {
                        effectiveSRUAreas.set(i, effectiveArea);
                        exists = true;
                    }

                }

                if (!exists) {
                    effectiveSRUAreas.add(effectiveArea);
                }
            }

        }

        for (int i = 0; i < effectiveSRUAreas.size(); i++) {
            graphics.add(effectiveSRUAreas.get(i));
        }

        // PoD for each SRU, initialized with an effective area? possibly a
        // unique ID

        doPrepare();
    }

    @Override
    public void updateEffectiveAreaLocation(SARData sarData) {

        for (int i = 0; i < effectiveSRUAreas.size(); i++) {
            effectiveSRUAreas.get(i).updateEffectiveAreaSize(sarData);
        }
        voctManager.saveToFile();
    }

    @Override
    public void toggleEffectiveAreaVisibility(int i, boolean visible) {
//        System.out.println("Toggle visibiity " + effectiveSRUAreas.size() + " and i " + i);
        if (effectiveSRUAreas.size() >= i + 1) {

            effectiveSRUAreas.get(i).setVisible(visible);

            doPrepare();
        }
    }

    @Override
    public void removeEffortAllocationArea(long id) {

        for (int j = 0; j < effectiveSRUAreas.size(); j++) {

            if (effectiveSRUAreas.get(j).getId() == id) {
                EffortAllocationAreaGraphics area = effectiveSRUAreas.get(j);
                effectiveSRUAreas.remove(j);

                graphics.remove(area);

                doPrepare();
            }

        }

    }

    public void showFutureData(SARData sarData) {

        if (sarData instanceof DatumPointData) {

            DatumPointData data = (DatumPointData) sarData;

            Position A = data.getA();
            Position B = data.getB();
            Position C = data.getC();
            Position D = data.getD();

            Position datumDownWind = data.getDatumDownWind();
            Position datumMin = data.getDatumMin();
            Position datumMax = data.getDatumMax();

            double radiusDownWind = data.getRadiusDownWind();
            double radiusMin = data.getRadiusMin();
            double radiusMax = data.getRadiusMax();

            Position LKP = data.getLKP();
            Position WTCPoint = data.getWtc();

            graphics.remove(sarGraphics);
            graphics.remove(sarArea);

            sarArea = new SarAreaGraphic(A, B, C, D);
            graphics.add(sarArea);
            // public SarGraphics(Position datumDownWind, Position datumMin,
            // Position datumMax, double radiusDownWind, double radiusMin, double
            // radiusMax, Position LKP, Position current) {
            sarGraphics = new SarGraphics(datumDownWind, datumMin, datumMax, radiusDownWind, radiusMin, radiusMax, LKP, WTCPoint);

            graphics.add(sarGraphics);

        }

        if (sarData instanceof RapidResponseData) {

            RapidResponseData data = (RapidResponseData) sarData;

            Position A = data.getA();
            Position B = data.getB();
            Position C = data.getC();
            Position D = data.getD();

            Position datum = data.getDatum();
            double radius = data.getRadius();

            Position LKP = data.getLKP();

            graphics.remove(sarArea);

            sarArea = new SarAreaGraphic(A, B, C, D);
            graphics.add(sarArea);
            graphics.remove(sarGraphics);

            sarGraphics = new SarGraphics(datum, radius, LKP, data.getCurrentList(), data.getWindList());
            graphics.add(sarGraphics);
        }

        doPrepare();
    }

    @Override
    public void findAndInit(Object obj) {
        super.findAndInit(obj);

    }
}
