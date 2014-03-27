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
package dk.dma.epd.common.prototype.gui.route;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DateEditor;
import javax.swing.SpinnerDateModel;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultFormatter;

import org.jdesktop.swingx.JXDatePicker;

import dk.dma.epd.common.prototype.model.route.Route.EtaAdjust;
import dk.dma.epd.common.prototype.model.route.Route.EtaAdjustType;
import dk.dma.epd.common.util.ParseUtils;

/**
 * Dialog for editing an ETA
 * 
 * @author oleborup
 * 
 */
public class EtaEditDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 1L;

    Date eta;
    JLabel lblEta = new JLabel("ETA");
    JButton btnSave = new JButton("Save");
    JButton btnCancel = new JButton("Cancel");
    ButtonGroup adjustGroup;
    JXDatePicker etaDatePicker;
    JSpinner etaTimeSpinner;
    JRadioButton adjust1;
    JRadioButton adjust2;
    JRadioButton adjust3;

    public EtaEditDialog(JDialog owner, Date eta) {
        super(owner, "ETA edit", true);
        this.eta = eta;
        setSize(250, 160);
        setLocationRelativeTo(owner);
        setResizable(false);
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(null);

        etaDatePicker = new JXDatePicker(eta);
        etaDatePicker.setFormats(new SimpleDateFormat("E dd/MM/yyyy"));
        etaTimeSpinner = new JSpinner(new SpinnerDateModel(eta, null, null, Calendar.HOUR_OF_DAY));
        DateEditor editor = new JSpinner.DateEditor(etaTimeSpinner, "HH:mm");
        ((DefaultFormatter) editor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
        etaTimeSpinner.setEditor(editor);

        lblEta.setBounds(6, 6, 37, 16);
        getContentPane().add(lblEta);

        etaDatePicker.setBounds(38, 6, 103, 16);
        getContentPane().add(etaDatePicker);

        etaTimeSpinner.setBounds(153, 6, 71, 16);
        getContentPane().add(etaTimeSpinner);

        adjust1 = new JRadioButton("Adjust leg in and out speeds");
        adjust2 = new JRadioButton("Adjust all ETA's");
        adjust3 = new JRadioButton("Adjust speeds fixed start and end time");
        adjustGroup = new ButtonGroup();
        adjustGroup.add(adjust1);
        adjustGroup.add(adjust2);
        adjustGroup.add(adjust3);

        adjust1.setBounds(6, 34, 250, 16);
        getContentPane().add(adjust1);

        adjust2.setBounds(6, 56, 250, 16);
        getContentPane().add(adjust2);

        adjust3.setBounds(6, 78, 250, 16);
        getContentPane().add(adjust3);
        adjust3.setSelected(true);

        btnSave.setBounds(6, 106, 75, 16);
        btnSave.addActionListener(this);
        getContentPane().add(btnSave);

        btnCancel.setBounds(86, 106, 71, 16);
        btnCancel.addActionListener(this);
        getContentPane().add(btnCancel);
    }

    public EtaAdjust getEtaAdjust() {
        if (eta == null) {
            return null;
        }
        return new EtaAdjust(eta, getAdjustType());
    }

    public EtaAdjustType getAdjustType() {
        if (adjust1.isSelected()) {
            return EtaAdjustType.ADJUST_ADJACENT_LEG_SPEEDS;
        }
        if (adjust2.isSelected()) {
            return EtaAdjustType.ADJUST_ALL_ETA;
        }
        return EtaAdjustType.ADJUST_FIXED_START_AND_END;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            eta = ParseUtils.combineDateTime(etaDatePicker.getDate(), (Date) etaTimeSpinner.getValue());
        } else {
            eta = null;
        }
        setVisible(false);
    }
}
