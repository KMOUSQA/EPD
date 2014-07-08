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
package dk.dma.epd.common.prototype.layers.ais;

import javax.swing.ImageIcon;

import dk.dma.enav.model.geometry.Position;
import dk.dma.epd.common.graphics.CenterRaster;
import dk.dma.epd.common.prototype.ais.AisTarget;
import dk.dma.epd.common.prototype.ais.AtoNTarget;
import dk.dma.epd.common.prototype.settings.AisSettings;
import dk.dma.epd.common.prototype.settings.NavSettings;

/**
 * Graphic for AtoN target
 */
public class AtonTargetGraphic extends TargetGraphic {

    private static final long serialVersionUID = 1L;
    private ImageIcon atonImage; // = new
                                 // ImageIcon(AtonTargetGraphic.class.getResource("/images/aton/aton.png"));
    private CenterRaster atonMark;
    private AtoNTarget atonTarget;

    public AtonTargetGraphic() {
        super();
        setVague(true);
    }

    @Override
    public void update(AisTarget aisTarget, AisSettings aisSettings, NavSettings navSettings, float mapScale) {
     
        try{
            atonTarget = (AtoNTarget) aisTarget;
            Position pos = atonTarget.getPos();
            if (pos == null) {
                return;
            }
            float lat = (float) pos.getLatitude();
            float lon = (float) pos.getLongitude();

            // Check to see if image icon needs to be initialized
            if (this.atonImage == null) {
                this.setIconForAtonMark();
            }

            if (atonMark == null) {
                atonMark = new CenterRaster(lat, lon, atonImage.getIconWidth(), atonImage.getIconHeight(), atonImage);
                add(atonMark);
            } else {
                atonMark.setLat(lat);
                atonMark.setLon(lon);
            }
        }catch(Exception e){
          System.out.println("Failed to cast AisTarget to AtoNTarget");  
        }
        

    }

    public AtoNTarget getAtonTarget() {
        return atonTarget;
    }

    /**
     * Load the proper image icon for the current AtoN mark.
     */
    private void setIconForAtonMark() {
        // Specifies if this is a virtual or physical AtoN target (icons differ
        // according to this)
        boolean isVirtual = this.atonTarget.getVirtual() == 1;
        StringBuilder sb = new StringBuilder();
        sb.append("/images/aton/");
        if (isVirtual) {
            // Go to folder with icons for virtual AtoNs
            sb.append("virtual/");
        } else {
            // Go to folder with icons for physical AtoNs
            sb.append("physical/");
        }
        // TODO Implement check for invalid atontype + virtual combination ?
        switch (this.atonTarget.getAtonType()) {
        // < Group using same icon >
        case DEFAULT:
        case REFERENCE_POINT:
        case FIXED_STRUCTURE_OFFSHORE:
        case LIGHT_WITHOUT_SECTORS:
        case LIGHT_WITH_SECTORS:
        case LEADING_LIGHT_FRONT:
        case LEADING_LIGHT_REAR:
        case LIGHT_VESSEL_OR_LANBY_OR_RIGS:
            sb.append("aton-default.PNG");
            break;
        // </Group using same icon>
        case RACON:
            sb.append("aton-racon.PNG");
            break;
        case EMERGENCY_WRECK_MARK:
            sb.append("aton-emergency-wreck-mark.PNG");
            break;
        case BEACON_CARDINAL_N:
        case FLOATING_CARDINAL_MARK_N:
            sb.append("aton-north-cardinal-mark.PNG");
            break;
        case BEACON_CARDINAL_E:
        case FLOATING_CARDINAL_MARK_E:
            sb.append("aton-east-cardinal-mark.PNG");
            break;
        case BEACON_CARDINAL_S:
        case FLOATING_CARDINAL_MARK_S:
            sb.append("aton-south-cardinal-mark.PNG");
            break;
        case BEACON_CARDINAL_W:
        case FLOATING_CARDINAL_MARK_W:
            sb.append("aton-west-cardinal-mark.PNG");
            break;
        case BEACON_PORT_HAND:
        case BEACON_PREFERRED_CHANNEL_PORT_HAND:
        case PORT_HAND_MARK:
        case PREFERRED_CHANNEL_PORT_HAND:
            sb.append("aton-port-hand-mark.PNG");
            break;
        case BEACON_STARBOARD_HAND:
        case BEACON_PREFERRED_CHANNEL_STARBOARD_HAND:
        case STARBOARD_HAND_MARK:
        case PREFERRED_CHANNEL_STARBOARD_HAND:
            sb.append("aton-starboard-hand.PNG");
            break;
        case BEACON_ISOLATED_DANGER:
        case ISOLATED_DANGER:
            sb.append("aton-isolated-danger.PNG");
            break;
        case BEACON_SAFE_WATER:
        case SAFE_WATER:
            sb.append("aton-safe-water.PNG");
            break;
        case BEACON_SPECIAL_MARK:
        case SPECIAL_MARK:
            sb.append("aton-special-mark.PNG");
            break;
        default:
            // TODO how to handle unknown AtoN type?
            System.err.println("###### INVALID ATON TYPE: " + atonTarget.getAtonType() + " VIRTUAL = " + atonTarget.getVirtual()
                    + " ######");
            // sb.append("aton-default.png");
            // Use old default aton image when we cannot decipher type+virtual/physical
            sb = new StringBuilder();
            sb.append("/images/aton/aton.png");
            break;
        }
        this.atonImage = new ImageIcon(AtonTargetGraphic.class.getResource(sb.toString()));
    }
}
