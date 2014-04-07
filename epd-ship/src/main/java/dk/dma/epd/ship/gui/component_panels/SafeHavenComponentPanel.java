/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.epd.ship.gui.component_panels;

import java.awt.BorderLayout;

import javax.swing.border.EtchedBorder;

import com.bbn.openmap.gui.OMComponentPanel;

import dk.dma.epd.common.prototype.ais.VesselTarget;
import dk.dma.epd.common.prototype.model.route.IRoutesUpdateListener;
import dk.dma.epd.common.prototype.model.route.RoutesUpdateEvent;
import dk.dma.epd.common.prototype.route.RouteManagerCommon;
import dk.dma.epd.ship.gui.panels.SafeHavenPanel;
import dk.dma.epd.ship.ownship.IOwnShipListener;
import dk.dma.epd.ship.ownship.OwnShipHandler;

/**
 * Displays the safe haven and target speed relative to the planned route
 */
public class SafeHavenComponentPanel extends OMComponentPanel implements IOwnShipListener, IRoutesUpdateListener, DockableComponentPanel {

    private static final long serialVersionUID = 1L;
    
    SafeHavenPanel safeHavenPanel = new SafeHavenPanel();
    RouteManagerCommon routeManager;
    
    /**
     * Constructor
     */
    public SafeHavenComponentPanel() {
        super();
        
        safeHavenPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        setBorder(null);
        
        setLayout(new BorderLayout(0, 0));
        add(safeHavenPanel, BorderLayout.NORTH);
        setVisible(false);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void findAndInit(Object obj) {
        super.findAndInit(obj);
        
        if (obj instanceof OwnShipHandler) {
            ((OwnShipHandler)obj).addListener(this);
            ownShipUpdated((OwnShipHandler)obj);
            
        } else if (obj instanceof RouteManagerCommon) {
            routeManager = (RouteManagerCommon)obj;
            routeManager.addListener(this);
            routesChanged(null);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void findAndUndo(Object obj) {        
        if (obj instanceof OwnShipHandler) {
            ((OwnShipHandler)obj).removeListener(this);
        
        } else if (obj == routeManager) {
            routeManager.removeListener(this);
        }
        
        super.findAndUndo(obj);
    }
    
    /****************************************/
    /** Listener methods                   **/
    /****************************************/
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void ownShipUpdated(final OwnShipHandler ownShipHandler) {
        // Update safe haven panel
        safeHavenPanel.shipPntDataChanged(ownShipHandler.getPntData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ownShipChanged(VesselTarget oldValue, VesselTarget newValue) {
        // Update safe haven panel
        safeHavenPanel.shipPntDataChanged(null);
        safeHavenPanel.updatePanel();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void routesChanged(RoutesUpdateEvent e) {

        // Only update on relevant events
        if (e != null &&
            !e.is(RoutesUpdateEvent.ROUTE_ACTIVATED, 
                RoutesUpdateEvent.ROUTE_DEACTIVATED, 
                RoutesUpdateEvent.ACTIVE_ROUTE_UPDATE, 
                RoutesUpdateEvent.ACTIVE_ROUTE_FINISHED, 
                RoutesUpdateEvent.ROUTE_VISIBILITY_CHANGED)) {
            return;
        }
        
        safeHavenPanel.activeRouteUpdated(routeManager.getActiveRoute());
    }
    
    /****************************************/
    /** DockableComponentPanel methods     **/
    /****************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDockableComponentName() {
        return "Safe Haven";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeInDefaultLayout() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean includeInPanelsMenu() {
        return true;
    }
}