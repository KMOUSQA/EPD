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
package dk.dma.epd.common.prototype.gui.voct;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class SARPanelSimpelSAR extends JPanel{

    private JLabel datumLat;
    private JLabel datumLon;
    private JLabel rdvDistance;
    private JLabel datumRadius;
    
    private static final long serialVersionUID = 1L;

    public SARPanelSimpelSAR(){
        
        setBorder(new TitledBorder(null, "Position of Datum to LKP",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        GridBagConstraints gbc_datumPanel = new GridBagConstraints();
        gbc_datumPanel.insets = new Insets(0, 0, 5, 0);
        gbc_datumPanel.fill = GridBagConstraints.BOTH;
        gbc_datumPanel.gridx = 0;
        gbc_datumPanel.gridy = 4;
        
        
        
        GridBagLayout gbl_datumPanel = new GridBagLayout();
        gbl_datumPanel.columnWidths = new int[] { 0, 0, 0, 0, 0 };
        gbl_datumPanel.rowHeights = new int[] { 0, 0 };
        gbl_datumPanel.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0 };
        gbl_datumPanel.rowWeights = new double[] { 1.0, 1.0 };
        setLayout(gbl_datumPanel);

        JLabel lblLatitude = new JLabel("Latitude");
        GridBagConstraints gbc_lblLatitude = new GridBagConstraints();
        gbc_lblLatitude.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblLatitude.insets = new Insets(0, 0, 5, 5);
        gbc_lblLatitude.gridx = 1;
        gbc_lblLatitude.gridy = 0;
        add(lblLatitude, gbc_lblLatitude);

        JLabel lblLongitude = new JLabel("Longitude");
        GridBagConstraints gbc_lblLongitude = new GridBagConstraints();
        gbc_lblLongitude.insets = new Insets(0, 0, 5, 5);
        gbc_lblLongitude.gridx = 2;
        gbc_lblLongitude.gridy = 0;
        add(lblLongitude, gbc_lblLongitude);

        JLabel lblRdv = new JLabel("RDV");
        GridBagConstraints gbc_lblRdv = new GridBagConstraints();
        gbc_lblRdv.insets = new Insets(0, 0, 5, 5);
        gbc_lblRdv.gridx = 3;
        gbc_lblRdv.gridy = 0;
        add(lblRdv, gbc_lblRdv);

        JLabel lblRadius = new JLabel("Radius");
        GridBagConstraints gbc_lblRadius = new GridBagConstraints();
        gbc_lblRadius.insets = new Insets(0, 0, 5, 0);
        gbc_lblRadius.gridx = 4;
        gbc_lblRadius.gridy = 0;
        add(lblRadius, gbc_lblRadius);

        JLabel lblDownwind = new JLabel("Downwind:");
        GridBagConstraints gbc_lblDownwind = new GridBagConstraints();
        gbc_lblDownwind.insets = new Insets(0, 0, 0, 5);
        gbc_lblDownwind.gridx = 0;
        gbc_lblDownwind.gridy = 1;
        add(lblDownwind, gbc_lblDownwind);

        datumLat = new JLabel("N/A");
        GridBagConstraints gbc_datumLat = new GridBagConstraints();
        gbc_datumLat.insets = new Insets(0, 0, 0, 5);
        gbc_datumLat.gridx = 1;
        gbc_datumLat.gridy = 1;
        add(datumLat, gbc_datumLat);

        datumLon = new JLabel("N/A");
        GridBagConstraints gbc_datumLon = new GridBagConstraints();
        gbc_datumLon.insets = new Insets(0, 0, 0, 5);
        gbc_datumLon.gridx = 2;
        gbc_datumLon.gridy = 1;
        add(datumLon, gbc_datumLon);

        rdvDistance = new JLabel("N/A");
        GridBagConstraints gbc_rdvDistance = new GridBagConstraints();
        gbc_rdvDistance.insets = new Insets(0, 0, 0, 5);
        gbc_rdvDistance.gridx = 3;
        gbc_rdvDistance.gridy = 1;
        add(rdvDistance, gbc_rdvDistance);

        datumRadius = new JLabel("N/A");
        GridBagConstraints gbc_datumRadius = new GridBagConstraints();
        gbc_datumRadius.gridx = 4;
        gbc_datumRadius.gridy = 1;
        add(datumRadius, gbc_datumRadius);
    }
    
    public void setDatumLat(String datumLatTxt){
        datumLat.setText(datumLatTxt);
    }
    
    public void setDatumLon(String datumLonTxt){
        datumLon.setText(datumLonTxt);
    }
    
    public void setrdvDistance(String rdvDistanceTxt){
        rdvDistance.setText(rdvDistanceTxt);
    }
    
    public void setdatumRadius(String datumRadiusTxt){
        datumRadius.setText(datumRadiusTxt);
    }
}
