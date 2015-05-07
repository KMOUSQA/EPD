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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.joda.time.DateTime;

import dk.dma.enav.model.geometry.Position;
import dk.dma.epd.common.prototype.model.voct.SARISXMLParser;
import dk.dma.epd.common.prototype.model.voct.SAR_TYPE;
import dk.dma.epd.common.prototype.model.voct.sardata.SARWeatherData;
import dk.dma.epd.common.prototype.voct.VOCTManagerCommon;
import dk.dma.epd.common.prototype.voct.VOCTUpdateEvent;
import dk.dma.epd.common.prototype.voct.VOCTUpdateListener;

public class SARInputCommon extends JDialog implements ActionListener,
        DocumentListener, VOCTUpdateListener {

    private static final long serialVersionUID = 1L;

    private final JPanel initPanel = new JPanel();

    private JComboBox<String> typeSelectionComboBox;
    private JButton nextButton;
    private JButton cancelButton;
    private JButton backButton;

    private JLabel descriptiveImage;
    private JTextPane descriptiveText;

    private String simpleSarTxt = "Simple SAR - A basic visualization based on an external data file containing the search area coordinates and a datum";
    private String rapidresponseTxt = "Rapid Response - Rapid Response should be used when the rescue vessel is within the designated search area in a relatively short timespan (1-2 hours after LKP).";
    private String datumPointTxt = "Datum Pont - Datum point is a calculation method used when the rescue vessel arrives to the designated search area after 2 or more hours after LKP";
    private String datumLineTxt = "Datum line - Datum line is used when an object is mising and a LKP is unkown but a assumed route is known";
    // private String backtrackTxt =
    // "Back track - Back track is used when a object has been located that is connected to the missing vessel. By reversing the objects movements a possible search area can be established";

    private ImageIcon rapidResponseIcon = scaleImage(new ImageIcon(
            SARInputCommon.class.getClassLoader().getResource(
                    "images/voct/generic.png")));

    private ImageIcon datumPointIcon = scaleImage(new ImageIcon(
            SARInputCommon.class.getClassLoader().getResource(
                    "images/voct/datumpoint.png")));

    private ImageIcon datumLineIcon = scaleImage(new ImageIcon(
            SARInputCommon.class.getClassLoader().getResource(
                    "images/voct/datumline.png")));

    // private ImageIcon backtrackIcon = scaleImage(new ImageIcon(
    // SARInputCommon.class.getClassLoader().getResource(
    // "images/voct/generic.png")));

    private ImageIcon simpleSarIcon = scaleImage(new ImageIcon(
            SARInputCommon.class.getClassLoader().getResource(
                    "images/voct/simplesar.png")));

    private JPanel masterPanel;

    private static final String SELECTSARTYPE = "Select SAR Type";
    private static final String INPUTSARRAPIDRESPONSEDATUM = "Rapid Response And Datum Input Panel";
    private static final String CALCULATIONSPANEL = "Rapid Response Calculations Panel";
    private static final String INPUTPANELDATUMLINE = "Datum Line Calculations Panel";
    private static final String SIMPLESAR = "Simple SAR Input Panel";

    // First card shown is the select sar type
    private String currentCard = SELECTSARTYPE;

    private JLabel calculationsText = new JLabel();
    private RapidResponseDatumPointInputPanel rapidResponseDatumPointInputPanel;
    private DatumLineInputPanel datumLineInputPanel;
    private SimpleSARInputPanel simpleSarInputPanel;

    private VOCTManagerCommon voctManager;

    private boolean sarReady;
    private JLabel lblOrImportSar;

    private JButton btnImport;

    /**
     * 
     * Create the dialog.
     * 
     * @param voctManager
     */
    public SARInputCommon(VOCTManagerCommon voctManager) {
        this.voctManager = voctManager;

        voctManager.addListener(this);

        setTitle("SAR Operation");
        this.setModal(true);
        this.setResizable(false);

        // setBounds(100, 100, 559, 733);
        setBounds(100, 100, 559, 500);

        masterPanel = new JPanel();

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(masterPanel, BorderLayout.CENTER);

        buttomBar();

        // We initialize it with the init pane, this is where you select the
        // type of operation

        masterPanel.setLayout(new CardLayout());

        initPanel();
        inputPanel();
        calculationsPanel();

        // CardLayout cl = (CardLayout) (masterPanel.getLayout());
        // backButton.setEnabled(true);
        //
        // inititateSarType();
        //
        // System.out.println("Setting panel to " + currentCard);
        //
        // // The type select determines which panel we show
        // cl.show(masterPanel, currentCard);
    }

    private void calculationsPanel() {
        JPanel calculationsPanel = new JPanel();
        JScrollPane calculationsScrollPanel = new JScrollPane(calculationsPanel);

        masterPanel.add(calculationsScrollPanel, CALCULATIONSPANEL);
        calculationsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        calculationsText.setVerticalAlignment(SwingConstants.TOP);
        calculationsText.setHorizontalAlignment(SwingConstants.LEFT);

        calculationsPanel.add(calculationsText);

    }

    private void inputPanel() {

        rapidResponseDatumPointInputPanel = new RapidResponseDatumPointInputPanel();

        JScrollPane rapidResponseScrollPanel = new JScrollPane(
                rapidResponseDatumPointInputPanel);
        rapidResponseScrollPanel.getVerticalScrollBar().setUnitIncrement(16);
        rapidResponseScrollPanel.setPreferredSize(new Dimension(559, 363));
        masterPanel.add(rapidResponseScrollPanel, INPUTSARRAPIDRESPONSEDATUM);

        datumLineInputPanel = new DatumLineInputPanel();

        JScrollPane datumLineScrollPanel = new JScrollPane(datumLineInputPanel);
        datumLineScrollPanel.setPreferredSize(new Dimension(559, 363));
        datumLineScrollPanel.getVerticalScrollBar().setUnitIncrement(16);

        masterPanel.add(datumLineScrollPanel, INPUTPANELDATUMLINE);

        simpleSarInputPanel = new SimpleSARInputPanel();

        JScrollPane simpleSARScrollPanel = new JScrollPane(simpleSarInputPanel);
        simpleSARScrollPanel.setPreferredSize(new Dimension(559, 363));
        simpleSARScrollPanel.getVerticalScrollBar().setUnitIncrement(16);

        masterPanel.add(simpleSARScrollPanel, SIMPLESAR);

    }

    private void initPanel() {
        masterPanel.add(initPanel, SELECTSARTYPE);

        initPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        initPanel.setLayout(null);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Inititate New SAR Operation",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panel.setBounds(10, 11, 523, 340);
        initPanel.add(panel);
        panel.setLayout(null);

        typeSelectionComboBox = new JComboBox<String>();
        typeSelectionComboBox.setBounds(126, 21, 102, 20);
        panel.add(typeSelectionComboBox);
        typeSelectionComboBox.setModel(new DefaultComboBoxModel<String>(
                new String[] { "Simple SAR", "Rapid Response", "Datum Point",
                        "Datum Line", "Back Track" }));

        typeSelectionComboBox.addActionListener(this);

        JLabel lblSelectSarType = new JLabel("Select SAR Type");
        lblSelectSarType.setBounds(10, 24, 140, 14);
        panel.add(lblSelectSarType);

        descriptiveText = new JTextPane();

        descriptiveText.setBounds(10, 52, 503, 112);
        panel.add(descriptiveText);
        // descriptiveText.setBackground(UIManager.getColor("Button.background"));
        descriptiveText.setOpaque(false);
        descriptiveText.setEditable(false);
        descriptiveText.setText(rapidresponseTxt);

        descriptiveImage = new JLabel(" ");
        descriptiveImage.setHorizontalAlignment(SwingConstants.CENTER);
        descriptiveImage.setBounds(20, 131, 493, 200);
        panel.add(descriptiveImage);
        descriptiveImage.setIcon(simpleSarIcon);

        lblOrImportSar = new JLabel("Or import SAR from external file:");
        lblOrImportSar.setBounds(238, 24, 176, 14);
        panel.add(lblOrImportSar);

        btnImport = new JButton("Import");
        btnImport.setBounds(408, 20, 89, 23);
        panel.add(btnImport);

        btnImport.addActionListener(this);

    }

    private void buttomBar() {
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttonPane, BorderLayout.SOUTH);

        backButton = new JButton("Back");
        buttonPane.add(backButton);
        backButton.addActionListener(this);
        backButton.setEnabled(false);

        nextButton = new JButton("Next");
        buttonPane.add(nextButton);
        getRootPane().setDefaultButton(nextButton);
        nextButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        buttonPane.add(cancelButton);
        cancelButton.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent arg0) {

        if (arg0.getSource() == typeSelectionComboBox) {
            int selectedIndex = typeSelectionComboBox.getSelectedIndex();
            // 0 Simple SAR
            // 1 Rapid Response
            // 2 Datum Point
            // 3 Datum Line
            // 4 Back track

            switch (selectedIndex) {
            case 0:
                descriptiveImage.setIcon(simpleSarIcon);
                descriptiveText.setText(simpleSarTxt);
                nextButton.setEnabled(true);
                break;
            case 1:
                descriptiveImage.setIcon(rapidResponseIcon);
                descriptiveText.setText(rapidresponseTxt);
                nextButton.setEnabled(true);
                break;
            case 2:
                descriptiveImage.setIcon(datumPointIcon);
                descriptiveText.setText(datumPointTxt);
                nextButton.setEnabled(true);
                break;
            case 3:
                descriptiveImage.setIcon(datumLineIcon);
                descriptiveText.setText(datumLineTxt);
                nextButton.setEnabled(true);
                break;
            // case 4:
            // descriptiveImage.setIcon(backtrackIcon);
            // descriptiveText.setText(backtrackTxt);
            // nextButton.setEnabled(false);
            // break;
            }

        }

        if (arg0.getSource() == btnImport) {
            // Import started

            // Open a file menu select for xml files

            // Create SARIS parser for returned object

            JFileChooser fileChooser = new JFileChooser(new File("."));
            fileChooser.addChoosableFileFilter(new XmlFilter());
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.showOpenDialog(null);

            if (fileChooser.getSelectedFile() != null) {

                String fileChoosen = fileChooser.getSelectedFile()
                        .getAbsolutePath();

                try {

                    // if (fileChoosen.contains(".sar")) {
                    // SARFileParser parser = new SARFileParser(fileChoosen);
                    //
                    // // voctManager.setSarType(SAR_TYPE.SARIS_DATUM_POINT);
                    // // voctManager.setSarData(parser.getSarData());
                    // // calculationsText.setText("SARIS PARSE SUCCESSFULL");
                    // // backButton.setEnabled(true);
                    // // nextButton.setText("Finish");
                    // // nextButton.setEnabled(true);
                    //
                    // } else {
                    SARISXMLParser parser = new SARISXMLParser(fileChoosen);

                    voctManager.setSarType(SAR_TYPE.SARIS_DATUM_POINT);
                    voctManager.setSarData(parser.getSarData());
                    calculationsText.setText("SARIS PARSE SUCCESSFULL");
                    backButton.setEnabled(true);
                    nextButton.setText("Finish");
                    nextButton.setEnabled(true);

                    // }

                    CardLayout cl = (CardLayout) (masterPanel.getLayout());
                    cl.show(masterPanel, CALCULATIONSPANEL);
                    currentCard = CALCULATIONSPANEL;

                    // Move to final page where we show the info
                } catch (Exception e) {
                    calculationsText.setText("SARIS Parse failed on file "
                            + fileChoosen);
                    backButton.setEnabled(true);
                    nextButton.setEnabled(false);
                    voctManager.setSarType(SAR_TYPE.SARIS_DATUM_POINT);
                    CardLayout cl = (CardLayout) (masterPanel.getLayout());
                    cl.show(masterPanel, CALCULATIONSPANEL);
                    currentCard = CALCULATIONSPANEL;
                }
            }
        }

        if (arg0.getSource() == nextButton) {
            // Get the current card and action depends on that

            // System.out.println("Next button pressed, currently at :"
            // + currentCard);

            // We're at SAR selection screen
            if (currentCard == SELECTSARTYPE) {
                CardLayout cl = (CardLayout) (masterPanel.getLayout());
                backButton.setEnabled(true);

                inititateSarType();

                // System.out.println("Setting panel to " + currentCard);

                // The type select determines which panel we show
                cl.show(masterPanel, currentCard);

                return;
            }

            // We're at input screen
            if (currentCard == INPUTSARRAPIDRESPONSEDATUM
                    || currentCard == INPUTPANELDATUMLINE
                    || currentCard == SIMPLESAR) {
                CardLayout cl = (CardLayout) (masterPanel.getLayout());

                if (validateInputAndInititate()) {
                    // System.out.println("Validated");
                    calculationsText.setText(voctManager.getSarData()
                            .generateHTML());
                    backButton.setEnabled(true);
                    nextButton.setText("Finish");

                    cl.show(masterPanel, CALCULATIONSPANEL);
                    currentCard = CALCULATIONSPANEL;
                } else {
                    // do nothing, missing data, internally sorted
                }

                return;
            }

            // We're at confirmation screen
            if (currentCard == CALCULATIONSPANEL) {
                // System.out.println(currentCard);
                CardLayout cl = (CardLayout) (masterPanel.getLayout());
                backButton.setEnabled(true);
                nextButton.setText("Next");
                nextButton.setEnabled(true);

                SAR_TYPE type = voctManager.getSarType();

                if (type == SAR_TYPE.RAPID_RESPONSE) {
                    currentCard = INPUTSARRAPIDRESPONSEDATUM;
                }

                if (type == SAR_TYPE.DATUM_POINT) {
                    currentCard = INPUTSARRAPIDRESPONSEDATUM;
                }

                if (type == SAR_TYPE.DATUM_LINE) {
                    currentCard = INPUTPANELDATUMLINE;
                }

                if (type == SAR_TYPE.BACKTRACK) {
                    // To be implemented
                }

                if (type == SAR_TYPE.SARIS_DATUM_POINT) {
                    // To be implemented
                    currentCard = SELECTSARTYPE;
                }

                if (type == SAR_TYPE.SIMPLE_SAR) {
                    currentCard = SIMPLESAR;
                }

                if (sarReady) {

                    this.setVisible(false);

                    // Set the dialog back to input screen for reentering
                    cl.show(masterPanel, currentCard);

                    // System.out.println("Hiding");

                    // Display SAR command
                    voctManager.setLoadSarFromSerialize(false);
                    voctManager.displaySar();
                    sarReady = false;
                }

                return;
            }

        }

        if (arg0.getSource() == backButton) {

            // If we're at Rapid Response or Datum or Back back go back to
            // init
            if (currentCard == INPUTSARRAPIDRESPONSEDATUM
                    || currentCard == INPUTPANELDATUMLINE
                    || currentCard == SIMPLESAR) {
                CardLayout cl = (CardLayout) (masterPanel.getLayout());
                cl.show(masterPanel, SELECTSARTYPE);
                backButton.setEnabled(false);
                currentCard = SELECTSARTYPE;
                return;
            }

            // We're at confirmation
            if (currentCard == CALCULATIONSPANEL) {
                CardLayout cl = (CardLayout) (masterPanel.getLayout());

                SAR_TYPE type = voctManager.getSarType();

                if (type == SAR_TYPE.RAPID_RESPONSE) {
                    currentCard = INPUTSARRAPIDRESPONSEDATUM;
                }

                if (type == SAR_TYPE.DATUM_POINT) {
                    currentCard = INPUTSARRAPIDRESPONSEDATUM;
                }

                if (type == SAR_TYPE.DATUM_LINE) {
                    currentCard = INPUTPANELDATUMLINE;
                }

                if (type == SAR_TYPE.SARIS_DATUM_POINT) {
                    // To be implemented
                    currentCard = SELECTSARTYPE;
                }

                if (type == SAR_TYPE.SIMPLE_SAR) {
                    currentCard = SIMPLESAR;
                }

                cl.show(masterPanel, currentCard);
                backButton.setEnabled(true);
                nextButton.setText("Next");
                return;
            }

        }

        if (arg0.getSource() == cancelButton) {
            this.setVisible(false);
            // voctManager.cancelSarOperation();
            return;
        }

    }

    private static ImageIcon scaleImage(ImageIcon icon) {
        // Scale it?
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(444, 200,
                java.awt.Image.SCALE_SMOOTH);

        ImageIcon newIcon = new ImageIcon(newimg);

        return newIcon;
    }

    private void inititateSarType() {
        int selectedIndex = typeSelectionComboBox.getSelectedIndex();
        // 0 Simple SAR
        // 1 Rapid Response
        // 2 Datum Point
        // 3 Datum Line
        // 4 Back track

        switch (selectedIndex) {
        case 0:
            voctManager.setSarType(SAR_TYPE.SIMPLE_SAR);
            // rapidResponseDatumPointInputPanel
            // .setSARType(SAR_TYPE.RAPID_RESPONSE);

            currentCard = SIMPLESAR;
            break;
        case 1:
            voctManager.setSarType(SAR_TYPE.RAPID_RESPONSE);
            rapidResponseDatumPointInputPanel
                    .setSARType(SAR_TYPE.RAPID_RESPONSE);
            currentCard = INPUTSARRAPIDRESPONSEDATUM;
            break;
        case 2:
            voctManager.setSarType(SAR_TYPE.DATUM_POINT);
            rapidResponseDatumPointInputPanel.setSARType(SAR_TYPE.DATUM_POINT);
            currentCard = INPUTSARRAPIDRESPONSEDATUM;
            break;
        case 3:
            voctManager.setSarType(SAR_TYPE.DATUM_LINE);
            currentCard = INPUTPANELDATUMLINE;
            break;
        // case 3:
        // voctManager.setSarType(SAR_TYPE.BACKTRACK);
        // // currentCard = INPUTSARRAPIDRESPONSEDATUM;
        // break;
        }
    }

    private boolean validateInputAndInititate() {
        SAR_TYPE type = voctManager.getSarType();

        switch (type) {
        case RAPID_RESPONSE:
            return validateRapidResponse();
        case DATUM_POINT:
            return validateRapidResponse();
        case DATUM_LINE:
            return validateDatumLine();
        case BACKTRACK:
            return false;
        case NONE:
            return false;
        case SIMPLE_SAR:
            return validateSimpleSar();
        default:
            return false;
        }

    }

    private boolean validateSimpleSar() {

        // Datum
        if (simpleSarInputPanel.getDatumLat() == -9999) {
            return false;
        }

        if (simpleSarInputPanel.getDatumLon() == -9999) {
            return false;
        }

        Position datum = Position.create(simpleSarInputPanel.getDatumLat(),
                simpleSarInputPanel.getDatumLon());

        // A position
        if (simpleSarInputPanel.getALat() == -9999) {
            return false;
        }

        if (simpleSarInputPanel.getALon() == -9999) {
            return false;
        }

        Position aPos = Position.create(simpleSarInputPanel.getALat(),
                simpleSarInputPanel.getALon());

        // B Position
        if (simpleSarInputPanel.getBLat() == -9999) {
            return false;
        }

        if (simpleSarInputPanel.getBLon() == -9999) {
            return false;
        }

        Position bPos = Position.create(simpleSarInputPanel.getBLat(),
                simpleSarInputPanel.getBLon());

        // C Position
        if (simpleSarInputPanel.getCLat() == -9999) {
            return false;
        }

        if (simpleSarInputPanel.getCLon() == -9999) {
            return false;
        }

        Position cPos = Position.create(simpleSarInputPanel.getCLat(),
                simpleSarInputPanel.getCLon());

        // D Position
        if (simpleSarInputPanel.getDLat() == -9999) {
            return false;
        }

        if (simpleSarInputPanel.getDLon() == -9999) {
            return false;
        }

        Position dPos = Position.create(simpleSarInputPanel.getDLat(),
                simpleSarInputPanel.getDLon());

        if (!simpleSarInputPanel.checkTime()) {
            return false;
        }

        double xError = simpleSarInputPanel.getInitialPositionError();

        if (xError == -9999) {
            // Error message is handled within function
            return false;
        }

        double yError = simpleSarInputPanel.getNavError();

        if (yError == -9999) {
            // Error message is handled within function
            return false;
        }

        double safetyFactor = simpleSarInputPanel.getSafetyFactor();

        if (safetyFactor == -9999) {
            // Error message is handled within function
            return false;
        }

        int searchObject = simpleSarInputPanel.getSearchItemID();

        // Only valid search objects is value 0 to 19
        if (searchObject < 0 || searchObject > 20) {
            // Error message is handled within function
            // System.out.println("failed search object with id " +
            // searchObject);
            return false;
        }

        voctManager.inputSimpleSarData(simpleSarInputPanel.getSARID(),
                simpleSarInputPanel.getLKPDate(),
                simpleSarInputPanel.getCSSDate(), xError, yError, safetyFactor,
                searchObject, aPos, bPos, cPos, dPos, datum);

        return true;
    }

    private boolean validateDatumLine() {
        // System.out.println("Validating Datum Line");

        double datumLineDSP1Lat = datumLineInputPanel.getLKPLat();

        if (datumLineDSP1Lat == -9999) {
            return false;
        }

        double datumLineDSP1Lon = datumLineInputPanel.getLKPLon();

        Position dsp1;

        if (datumLineDSP1Lat != -9999 && datumLineDSP1Lon != -9999) {
            dsp1 = Position.create(datumLineDSP1Lat, datumLineDSP1Lon);
        } else {
            // msgbox
            // System.out.println("Failed lat");
            return false;
        }

        double datumLineDSP2Lat = datumLineInputPanel.getDsp2LKPLat();

        if (datumLineDSP2Lat == -9999) {
            return false;
        }

        double datumLineDSP2Lon = datumLineInputPanel.getDsp2LKPLon();

        Position dsp2;

        if (datumLineDSP2Lat != -9999 && datumLineDSP2Lon != -9999) {
            dsp2 = Position.create(datumLineDSP2Lat, datumLineDSP2Lon);
        } else {
            // msgbox
            // System.out.println("Failed lat");
            return false;
        }

        double datumLineDSP3Lat = datumLineInputPanel.getDsp3LKPLat();

        if (datumLineDSP3Lat == -9999) {
            return false;
        }

        double datumLineDSP3Lon = datumLineInputPanel.getDsp3LKPLon();

        Position dsp3;

        if (datumLineDSP3Lat != -9999 && datumLineDSP3Lon != -9999) {
            dsp3 = Position.create(datumLineDSP3Lat, datumLineDSP3Lon);
        } else {
            // msgbox
            // System.out.println("Failed lat");
            return false;
        }

        // System.out.println("All validated correctly, we got positions");

        // System.out.println("DSp1 Date is " +
        // datumLineInputPanel.getLKPDate());

        // System.out.println("DSp2 Date is " +
        // datumLineInputPanel.getDSP2Date());

        // System.out.println("DSp3 Date is " +
        // datumLineInputPanel.getDSP2Date());

        // System.out.println("CSS Date is " +
        // datumLineInputPanel.getCSSDate());

        // Time and date will be automatically sorted

        List<SurfaceDriftPanel> weatherList = datumLineInputPanel
                .getSurfaceDriftPanelList();

        List<SARWeatherData> sarWeatherDataPoints = new ArrayList<SARWeatherData>();

        for (int i = 0; i < weatherList.size(); i++) {
            // Get weather
            SurfaceDriftPanel weatherPanel = weatherList.get(i);

            double TWCKnots = weatherPanel.getTWCKnots();

            if (TWCKnots == -9999) {
                // Error message is handled within function
                return false;
            }

            double leewayKnots = weatherPanel.getLeeway();

            if (leewayKnots == -9999) {
                // Error message is handled within function
                return false;
            }

            double twcHeading = weatherPanel.getTWCHeading();

            if (twcHeading == -9999) {
                // Error message is handled within function
                return false;
            }

            double leewayHeading = weatherPanel.getLeewayHeading();

            if (leewayHeading == -9999) {
                // Error message is handled within function
                return false;
            }

            DateTime dateTime = new DateTime(weatherPanel.getDateTime()
                    .getTime());

            SARWeatherData sarWeatherData = new SARWeatherData(twcHeading,
                    TWCKnots, leewayKnots, leewayHeading, dateTime);

            sarWeatherDataPoints.add(sarWeatherData);
        }

        double xError = datumLineInputPanel.getInitialPositionError();

        if (xError == -9999) {
            // Error message is handled within function
            return false;
        }

        double yError = datumLineInputPanel.getNavError();

        if (yError == -9999) {
            // Error message is handled within function
            return false;
        }

        double safetyFactor = datumLineInputPanel.getSafetyFactor();

        if (safetyFactor == -9999) {
            // Error message is handled within function
            return false;
        }

        int searchObject = datumLineInputPanel.getSearchItemID();

        // Only valid search objects is value 0 to 19
        if (searchObject < 0 || searchObject > 20) {
            // Error message is handled within function
            // System.out.println("failed search object with id " +
            // searchObject);
            return false;
        }

        // rapidResponsePosition
        // commenceStartPosition
        // TWCKnots
        // twcHeading
        // leewayKnots
        // leewayHeading
        // xError
        // yError
        // safetyFactor
        // searchObject

        if (!datumLineInputPanel.checkTime()) {
            return false;
        }

        if (!datumLineInputPanel.checkMetocTime()) {
            return false;
        }

        voctManager.inputDatumLineData(datumLineInputPanel.getSARID(),
                datumLineInputPanel.getLKPDate(),
                datumLineInputPanel.getDSP2Date(),
                datumLineInputPanel.getDSP3Date(),
                datumLineInputPanel.getCSSDate(), dsp1, dsp2, dsp3, xError,
                yError, safetyFactor, searchObject, sarWeatherDataPoints);

        return true;

    }

    private boolean validateRapidResponse() {

        // System.out.println("Validating Rapid Response");

        // Get LKP values
        double rapidResponseLKPLat = rapidResponseDatumPointInputPanel
                .getRapidResponseDatumLKPLat();

        if (rapidResponseLKPLat == -9999) {
            return false;
        }

        double rapidResponseLKPLon = rapidResponseDatumPointInputPanel
                .getRapidResponseDatumLKPLon();

        Position rapidResponsePosition;

        if (rapidResponseLKPLat != -9999 && rapidResponseLKPLon != -9999) {
            rapidResponsePosition = Position.create(rapidResponseLKPLat,
                    rapidResponseLKPLon);
        } else {
            // msgbox
            // System.out.println("Failed lat");
            return false;
        }

        // System.out.println("All validated correctly, we got positions");

        // System.out.println("LKP Date is "
        // + rapidResponseDatumPointInputPanel.getLKPDate());
        // System.out.println("CSS Date is "
        // + rapidResponseDatumPointInputPanel.getCSSDate());

        // Time and date will be automatically sorted

        List<SurfaceDriftPanel> weatherList = rapidResponseDatumPointInputPanel
                .getSurfaceDriftPanelList();

        List<SARWeatherData> sarWeatherDataPoints = new ArrayList<SARWeatherData>();

        for (int i = 0; i < weatherList.size(); i++) {
            // Get weather
            SurfaceDriftPanel weatherPanel = weatherList.get(i);

            double TWCKnots = weatherPanel.getTWCKnots();

            if (TWCKnots == -9999) {
                // Error message is handled within function
                return false;
            }

            double leewayKnots = weatherPanel.getLeeway();

            if (leewayKnots == -9999) {
                // Error message is handled within function
                return false;
            }

            double twcHeading = weatherPanel.getTWCHeading();

            if (twcHeading == -9999) {
                // Error message is handled within function
                return false;
            }

            double leewayHeading = weatherPanel.getLeewayHeading();

            if (leewayHeading == -9999) {
                // Error message is handled within function
                return false;
            }

            DateTime dateTime = new DateTime(weatherPanel.getDateTime()
                    .getTime());

            SARWeatherData sarWeatherData = new SARWeatherData(twcHeading,
                    TWCKnots, leewayKnots, leewayHeading, dateTime);

            sarWeatherDataPoints.add(sarWeatherData);
        }

        double xError = rapidResponseDatumPointInputPanel
                .getInitialPositionError();

        if (xError == -9999) {
            // Error message is handled within function
            return false;
        }

        double yError = rapidResponseDatumPointInputPanel.getNavError();

        if (yError == -9999) {
            // Error message is handled within function
            return false;
        }

        double safetyFactor = rapidResponseDatumPointInputPanel
                .getSafetyFactor();

        if (safetyFactor == -9999) {
            // Error message is handled within function
            return false;
        }

        int searchObject = rapidResponseDatumPointInputPanel.getSearchItemID();

        // Only valid search objects is value 0 to 19
        if (searchObject < 0 || searchObject > 20) {
            // Error message is handled within function
            // System.out.println("failed search object with id " +
            // searchObject);
            return false;
        }

        // rapidResponsePosition
        // commenceStartPosition
        // TWCKnots
        // twcHeading
        // leewayKnots
        // leewayHeading
        // xError
        // yError
        // safetyFactor
        // searchObject

        if (!rapidResponseDatumPointInputPanel.checkTime()) {
            return false;
        }

        if (!rapidResponseDatumPointInputPanel.checkMetocTime()) {
            return false;
        }

        voctManager.inputRapidResponseDatumData(
                rapidResponseDatumPointInputPanel.getSARID(),
                rapidResponseDatumPointInputPanel.getLKPDate(),
                rapidResponseDatumPointInputPanel.getCSSDate(),
                rapidResponsePosition, xError, yError, safetyFactor,
                searchObject, sarWeatherDataPoints);

        return true;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {

    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void voctUpdated(VOCTUpdateEvent e) {

        // System.out.println(e);
        // System.out.println(currentCard);
        if (e == VOCTUpdateEvent.SAR_READY) {
            sarReady = true;
        } else {
            sarReady = false;
        }

    }

    class XmlFilter extends javax.swing.filechooser.FileFilter {
        public boolean accept(File file) {
            String filename = file.getName();
            return filename.endsWith(".xml");
        }

        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
