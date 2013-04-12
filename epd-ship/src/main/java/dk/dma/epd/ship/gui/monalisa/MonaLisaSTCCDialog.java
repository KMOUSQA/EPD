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
package dk.dma.epd.ship.gui.monalisa;

import java.awt.BorderLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import dk.dma.epd.ship.EPDShip;

public class MonaLisaSTCCDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    JLabel routeName;
    JLabel lblDate;
    JLabel lblTime;
    JLabel lblStatus;

    JLabel dateField;
    JLabel timeField;
    JLabel statusField;

    private boolean isActive;

    /**
     * Create the dialog.
     */
    public MonaLisaSTCCDialog() {

        super(EPDShip.getMainFrame(), "STCC Info", false);
        setAlwaysOnTop(true);
        setResizable(false);

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setLocationRelativeTo(EPDShip.getMainFrame());

        setBounds(100, 100, 165, 181);

        setResizable(false);

        initGui();

        this.setVisible(false);

    }

    private void initGui() {
        JPanel routeAcceptedPanel = new JPanel();

        // JPanel routeNotAcceptedPanel = new JPanel();

        getContentPane().add(routeAcceptedPanel, BorderLayout.CENTER);
        routeAcceptedPanel.setLayout(null);

        JLabel lblRouteName = new JLabel("Route");
        lblRouteName.setBounds(10, 5, 34, 14);
        lblRouteName.setFont(new Font("Tahoma", Font.BOLD, 11));
        routeAcceptedPanel.add(lblRouteName);

        routeName = new JLabel("N/A");
        routeName.setBounds(54, 5, 197, 14);
        routeName.setFont(new Font("Tahoma", Font.BOLD, 11));
        routeAcceptedPanel.add(routeName);

        JLabel lblPostRoute = new JLabel("sent to STCC");
        lblPostRoute.setBounds(10, 20, 71, 14);
        lblPostRoute.setFont(new Font("Tahoma", Font.BOLD, 11));
        routeAcceptedPanel.add(lblPostRoute);

        lblDate = new JLabel("Date:");
        lblDate.setBounds(10, 43, 27, 14);
        routeAcceptedPanel.add(lblDate);

        lblTime = new JLabel("Time:");
        lblTime.setBounds(11, 59, 26, 14);
        routeAcceptedPanel.add(lblTime);

        lblStatus = new JLabel("Status:");
        lblStatus.setBounds(9, 77, 35, 14);
        routeAcceptedPanel.add(lblStatus);

        JButton btnCancelRequest = new JButton("Cancel request");
        btnCancelRequest.setBounds(10, 119, 139, 23);
        routeAcceptedPanel.add(btnCancelRequest);

        dateField = new JLabel("N/A");
        dateField.setBounds(54, 43, 128, 14);
        routeAcceptedPanel.add(dateField);

        timeField = new JLabel("N/A");
        timeField.setBounds(54, 59, 128, 14);
        routeAcceptedPanel.add(timeField);

        statusField = new JLabel("N/A");
        statusField.setBounds(54, 77, 128, 14);
        routeAcceptedPanel.add(statusField);

        // getContentPane().add(routeNotAcceptedPanel, BorderLayout.CENTER);

        // routeNotAcceptedPanel.setVisible(false);

    }

    public void setRouteName(String name) {

        isActive = true;

        routeName.setText("\"" + name + "\"");

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = sdf.format(date);
        dateField.setText(formattedDate);

        SimpleDateFormat sdt = new SimpleDateFormat("HH:mm");
        String formattedTime = sdt.format(date);
        timeField.setText(formattedTime);

        statusField.setText("Pending");
    }

    public boolean isActive() {
        return isActive;
    }

}
