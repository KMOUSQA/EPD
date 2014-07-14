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
package dk.dma.epd.ship.layers.ais;

import dk.dma.epd.common.prototype.ais.AisTarget.Status;
import dk.dma.epd.common.prototype.ais.AtoNTarget;
import dk.dma.epd.common.prototype.ais.VesselPositionData;
import dk.dma.epd.common.prototype.ais.VesselStaticData;
import dk.dma.epd.common.prototype.ais.VesselTarget;
import dk.dma.epd.common.prototype.ais.VesselTarget.AisClass;
import dk.dma.epd.common.prototype.gui.util.InfoPanel;
import dk.dma.epd.common.text.Formatter;
import dk.dma.epd.common.util.Util;

/**
 * AIS target mouse over info
 */
public class AisTargetInfoPanel extends InfoPanel implements Runnable {
    private static final long serialVersionUID = 1L;

    private VesselTarget vesselTarget;

    public AisTargetInfoPanel() {
        super();
        new Thread(this).start();
    }

    public void showAtonInfo(AtoNTarget atonTarget) {
        String name = atonTarget.getTrimmedName();
        Status status = atonTarget.getStatus();
        StringBuilder str = new StringBuilder();
        str.append("<html>");

        if (atonTarget.getVirtual() == 1) {
            str.append("Virtual AtoN");
        } else {
            str.append("Physical AtoN");
        }
        str.append("<br>" + name + "</br>");
        str.append("<br>Type: " + atonTarget.getAtonType().getPrettyName()
                + "</br>");
        str.append("<br>Status: " + status + "</br>");
        str.append("</html>");
        showText(str.toString());
    }

    public void showAisInfo(VesselTarget vesselTarget) {
        this.vesselTarget = vesselTarget;
        VesselStaticData staticData = vesselTarget.getStaticData();
        String name = null;
        String callsign = null;
        if (staticData != null) {
            name = staticData.getTrimmedName();
            callsign = staticData.getTrimmedCallsign();
        }
        VesselPositionData positionData = vesselTarget.getPositionData();
        String cog = "N/A";
        String sog = "N/A";

        if (positionData != null) {
            cog = Formatter.formatDegrees((double) positionData.getCog(), 0);
            sog = Formatter.formatSpeed((double) positionData.getSog());
        }

        StringBuilder str = new StringBuilder();
        str.append("<html>");
        if (name != null) {
            str.append(name + " (" + vesselTarget.getMmsi() + ")");
        } else {
            str.append(vesselTarget.getMmsi());
        }
        if (vesselTarget.getAisClass() == AisClass.B) {
            str.append(" [<b>B</b>]");
        }
        str.append("<br/>");

        if (callsign != null) {
            str.append(callsign + "<br/>");
        }
        str.append("COG " + cog + "  SOG " + sog + "<br/>");

        str.append("</html>");

        showText(str.toString());
    }

    @Override
    public void run() {
        while (true) {
            Util.sleep(10000);
            if (this.isVisible() && vesselTarget != null) {
                showAisInfo(vesselTarget);
            }
        }
    }
}
