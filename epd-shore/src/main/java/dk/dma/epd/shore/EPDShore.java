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
package dk.dma.epd.shore;

import java.beans.beancontext.BeanContextServicesSupport;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.maritimecloud.core.id.MaritimeId;
import net.maritimecloud.core.id.MmsiId;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.openmap.proj.coords.LatLonPoint;
import com.jtattoo.plaf.hifi.HiFiLookAndFeel;

import dk.dma.ais.reader.AisReader;
import dk.dma.ais.virtualnet.transponder.gui.TransponderFrame;
import dk.dma.commons.app.OneInstanceGuard;
import dk.dma.enav.model.geometry.Position;
import dk.dma.epd.common.ExceptionHandler;
import dk.dma.epd.common.graphics.Resources;
import dk.dma.epd.common.prototype.Bootstrap;
import dk.dma.epd.common.prototype.EPD;
import dk.dma.epd.common.prototype.PluginLoader;
import dk.dma.epd.common.prototype.gui.SystemTrayCommon;
import dk.dma.epd.common.prototype.model.identity.IdentityHandler;
import dk.dma.epd.common.prototype.model.voyage.VoyageEventDispatcher;
import dk.dma.epd.common.prototype.sensor.nmea.NmeaFileSensor;
import dk.dma.epd.common.prototype.sensor.nmea.NmeaSensor;
import dk.dma.epd.common.prototype.sensor.nmea.NmeaSerialSensorFactory;
import dk.dma.epd.common.prototype.sensor.nmea.NmeaStdinSensor;
import dk.dma.epd.common.prototype.sensor.nmea.NmeaTcpSensor;
import dk.dma.epd.common.prototype.sensor.pnt.PntTime;
import dk.dma.epd.common.prototype.service.ChatServiceHandlerCommon;
import dk.dma.epd.common.prototype.service.IntendedRouteHandlerCommon;
import dk.dma.epd.common.prototype.service.MaritimeCloudService;
import dk.dma.epd.common.prototype.service.MaritimeCloudUtils;
import dk.dma.epd.common.prototype.service.MsiNmServiceHandlerCommon;
import dk.dma.epd.common.prototype.settings.SensorSettings;
import dk.dma.epd.common.prototype.shoreservice.ShoreServicesCommon;
import dk.dma.epd.common.util.VersionInfo;
import dk.dma.epd.shore.ais.AisHandler;
import dk.dma.epd.shore.event.DragMouseMode;
import dk.dma.epd.shore.event.NavigationMouseMode;
import dk.dma.epd.shore.event.SelectMouseMode;
import dk.dma.epd.shore.fal.FALManager;
import dk.dma.epd.shore.gui.notification.NotificationCenter;
import dk.dma.epd.shore.gui.utils.StaticImages;
import dk.dma.epd.shore.gui.views.MainFrame;
import dk.dma.epd.shore.route.RouteManager;
import dk.dma.epd.shore.service.FALHandler;
import dk.dma.epd.shore.service.IntendedRouteHandler;
import dk.dma.epd.shore.service.MonaLisaRouteOptimization;
import dk.dma.epd.shore.service.RouteSuggestionHandler;
import dk.dma.epd.shore.service.StrategicRouteHandler;
import dk.dma.epd.shore.service.VoctHandler;
import dk.dma.epd.shore.services.shore.ShoreServices;
import dk.dma.epd.shore.settings.EPDSensorSettings;
import dk.dma.epd.shore.settings.EPDSettings;
import dk.dma.epd.shore.voct.SRUManager;
import dk.dma.epd.shore.voct.VOCTManager;
import dk.dma.epd.shore.voyage.VoyageManager;

/**
 * Main class with main method.
 * 
 * Starts up components, bean context and GUI.
 * 
 */
public final class EPDShore extends EPD {

    private static Logger LOG;
    private MainFrame mainFrame;
    private BeanContextServicesSupport beanHandler;
    private NmeaSensor aisSensor;
    private AisReader aisReader;
    private ShoreServicesCommon shoreServicesCommon;
    private StaticImages staticImages;
    private TransponderFrame transponderFrame;
    private MonaLisaRouteOptimization monaLisaRouteExchange;

    private SRUManager sruManager;

    private VoyageManager voyageManager;

    // Maritime Cloud services
    private IntendedRouteHandlerCommon intendedRouteHandler;
    // private VoctHandler voctHandler;

    private PluginLoader pluginLoader;

    /**
     * Event dispatcher used to notify listeners of voyage changes.
     */
    private VoyageEventDispatcher voyageEventDispatcher = new VoyageEventDispatcher();

    /**
     * Starts the program by initializing the various threads and spawning the
     * main GUI
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // Check if the home path has been specified via the command line
        String homePath = (args.length > 0) ? args[0] : null;
        new EPDShore(homePath);
    }

    /**
     * Constructor
     * 
     * @param path
     *            the home path to use
     */
    private EPDShore(String path) throws IOException {
        super();

        if (!StringUtils.isEmpty(path)) {
            homePath = Paths.get(path);
        } else {
            homePath = determineHomePath(Paths.get(
                    System.getProperty("user.home"), ".epd-shore"));
        }

        new Bootstrap().run(this, new String[] { "epd-shore.properties",
                "settings.properties", "transponder.xml" }, new String[] {
                "workspaces", "routes", "shape/GSHHS_shp", "identities" });

        // Set up log4j logging
        LOG = LoggerFactory.getLogger(EPDShore.class);

        // Set default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        LOG.info("Starting eNavigation Prototype Display Shore - version "
                + VersionInfo.getVersionAndBuild());
        LOG.info("Copyright (C) 2012 Danish Maritime Authority");
        LOG.info("This program comes with ABSOLUTELY NO WARRANTY.");
        LOG.info("This is free software, and you are welcome to redistribute it under certain conditions.");
        LOG.info("For details see LICENSE file.");

        // Create the bean context (map handler)
        // mapHandler = new MapHandler();
        beanHandler = new BeanContextServicesSupport();

        // Load settings or get defaults and add to bean context
        settings = new EPDSettings();
        LOG.info("Using settings file: " + getSettings().getSettingsFile());
        settings.loadFromFile();
        beanHandler.add(settings);

        // Determine if instance already running and if that is allowed

        OneInstanceGuard guard = new OneInstanceGuard(getHomePath().resolve(
                "epd.lock").toString());
        if (guard.isAlreadyRunning()) {
            handleEpdAlreadyRunning();
        }

        // Enable GPS timer by adding it to bean context
        PntTime.init(settings.getSensorSettings().isUseTimeFromPnt());
        beanHandler.add(PntTime.getInstance());

        // aisHandler = new AisHandlerCommon();
        aisHandler = new AisHandler(settings.getAisSettings());
        aisHandler.loadView();
        EPD.startThread(aisHandler, "AisHandler");
        beanHandler.add(aisHandler);

        // Add StaticImages handler
        staticImages = new StaticImages();
        beanHandler.add(staticImages);

        // Load routeManager
        routeManager = RouteManager.loadRouteManager();
        beanHandler.add(routeManager);

        falManager = FALManager.loadFALManager();
        beanHandler.add(falManager);

        // To be changed to load similar to routeManager
        // voyageManager = new VoyageManager();
        voyageManager = VoyageManager.loadVoyageManager();
        beanHandler.add(voyageManager);

        sruManager = SRUManager.loadSRUManager();
        beanHandler.add(sruManager);

        // Create shore services
        shoreServicesCommon = new ShoreServices(getSettings().getEnavSettings());
        beanHandler.add(shoreServicesCommon);

        // Create mona lisa route exchange
        monaLisaRouteExchange = new MonaLisaRouteOptimization();
        beanHandler.add(monaLisaRouteExchange);

        // Create Maritime Cloud service
        maritimeCloudService = new MaritimeCloudService();
        beanHandler.add(maritimeCloudService);
        maritimeCloudService.start();

        // Create strategic route Handler;
        strategicRouteHandler = new StrategicRouteHandler();
        beanHandler.add(strategicRouteHandler);

        // Create intended route handler
        intendedRouteHandler = new IntendedRouteHandler();
        beanHandler.add(intendedRouteHandler);

        // Create the route suggestion handler
        // routeSuggestionHandler = new RouteSuggestionHandler();
        routeSuggestionHandler = RouteSuggestionHandler
                .loadRouteSuggestionHandler();
        beanHandler.add(routeSuggestionHandler);

        // Create a new MSI-NM handler
        msiNmHandler = new MsiNmServiceHandlerCommon();
        beanHandler.add(msiNmHandler);

        // Create a chat service handler
        chatServiceHandler = new ChatServiceHandlerCommon();
        beanHandler.add(chatServiceHandler);

        // Create identity handler
        identityHandler = new IdentityHandler();
        beanHandler.add(identityHandler);

        // Start sensors
        startSensors();

        pluginLoader = new PluginLoader(getProperties(), getHomePath(),
                getPropertyFileName());

        try {
            pluginLoader.createPluginComponents(new Consumer<Object>() {
                public void accept(Object comp) {
                    beanHandler.add(comp);
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to load plugin container " + e.getMessage());
        }

        final CountDownLatch guiCreated = new CountDownLatch(1);

        // Create and show GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
                guiCreated.countDown();
            }
        });

        // Wait for gui to be created
        try {
            guiCreated.await();

        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for GUI to be created", e);
        }

        // Create vocthandler
        voctHandler = new VoctHandler();
        beanHandler.add(voctHandler);

        // Create voct manager
        voctManager = new VOCTManager();
        beanHandler.add(voctManager);
        ((VOCTManager) voctManager).loadVOCTManager();

        // Create FAL Handler
        falHandler = new FALHandler();
        beanHandler.add(falHandler);

        // Create embedded transponder frame
        transponderFrame = new TransponderFrame(getHomePath().resolve(
                "transponder.xml").toString(), true, mainFrame);
        mainFrame.getTopMenu().setTransponderFrame(transponderFrame);
        beanHandler.add(transponderFrame);

        if (settings.getSensorSettings().isStartTransponder()) {
            transponderFrame.startTransponder();
        }
    }

    /**
     * Returns the current {@code EPDShore} instance
     * 
     * @return the current {@code EPDShore} instance
     */
    public static EPDShore getInstance() {
        return (EPDShore) instance;
    }

    /**
     * Returns the type of this EPD instance
     *
     * @return the type of this EPD instance
     */
    @Override
    public EPDType getType() {
        return EPDType.SHORE;
    }

    /**
     * Returns the settings associated with the EPD system
     * 
     * @return the settings associated with the EPD system
     */
    @Override
    public EPDSettings getSettings() {
        return (EPDSettings) settings;
    }

    /**
     * @return the voctManager
     */
    public VOCTManager getVoctManager() {
        return (VOCTManager) voctManager;
    }

    /**
     * Returns the default shore mouse mode service list
     * 
     * @return the default shore mouse mode service list
     */
    @Override
    public String[] getDefaultMouseModeServiceList() {
        String[] ret = new String[3];
        ret[0] = DragMouseMode.MODEID; // "DragMouseMode"
        ret[1] = NavigationMouseMode.MODEID; // "ZoomMouseMode"
        ret[2] = SelectMouseMode.MODEID; // "SelectMouseMode"
        return ret;
    }

    @Override
    public Path getHomePath() {
        return homePath;
    }

    /**
     * {@inheritDoc}
     */
    public RouteSuggestionHandler getRouteSuggestionHandler() {
        return (RouteSuggestionHandler) routeSuggestionHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StrategicRouteHandler getStrategicRouteHandler() {
        return (StrategicRouteHandler) strategicRouteHandler;
    }

    /**
     * Close app routine with possibility for restart - not implemented
     * 
     * @param restart
     *            - boolean value for program restart
     */
    public void closeApp(boolean restart) {
        // Shutdown routine

        // Chart panels

        // Window state

        // Window state has a
        // Name, Size, Location, Locked status, on top status
        // Chart panel has a zoom level, position

        // Main application

        String filename = "temp.workspace";
        mainFrame.saveWorkSpace(filename);

        mainFrame.saveSettings();
        settings.saveToFile();

        // GuiSettings
        // Handler settings
        voyageManager.saveToFile();
        routeManager.saveToFile();
        msiNmHandler.saveToFile();
        aisHandler.saveView();
        transponderFrame.shutdown();
        falManager.saveToFile();

        // Maritime cloud services
        maritimeCloudService.stop();
        strategicRouteHandler.shutdown();
        routeSuggestionHandler.shutdown();
        intendedRouteHandler.shutdown();
        chatServiceHandler.shutdown();
        msiNmHandler.shutdown();

        // Stop the system tray
        systemTray.shutdown();

        // Stop sensors
        stopSensors();

        // Close all plugins
        pluginLoader.closePlugins();

        LOG.info("Closing EPDShore");
        this.restart = restart;
        System.exit(restart ? 2 : 0);
    }

    /**
     * Creates and shows the GUI
     */
    private void createAndShowGUI() {
        // Set the look and feel.
        initLookAndFeel();

        // Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(false);

        // Create and set up the main window
        mainFrame = new MainFrame();
        mainFrame.setVisible(true);

        // Create the system tray
        systemTray = new SystemTrayCommon();
        beanHandler.add(systemTray);

        // Create the notification center
        notificationCenter = new NotificationCenter(getMainFrame());
        beanHandler.add(notificationCenter);

    }

    /**
     * Function used to measure time
     * 
     * @param start
     *            - Startime
     * @return - Elapsed time
     */
    public double elapsed(long start) {
        double elapsed = System.nanoTime() - start;
        return elapsed / 1000000.0;
    }

    /**
     * Returns a reference to the AIS handler
     * 
     * @return a reference to the AIS handler
     */
    @Override
    public AisHandler getAisHandler() {
        return (AisHandler) aisHandler;
    }

    /**
     * Returns a reference to the intended route handler
     * 
     * @return a reference to the intended route handler
     */
    public IntendedRouteHandlerCommon getIntendedRouteHandler() {
        return intendedRouteHandler;
    }

    /**
     * BeanHandler for program structure
     * 
     * @return - beanHandler
     */
    public BeanContextServicesSupport getBeanHandler() {
        return beanHandler;
    }

    // /**
    // * Return the GpsHandler
    // * @return - GpsHandler
    // */
    // public static GpsHandler getGpsHandler() {
    // return gpsHandler;
    // }

    /**
     * Return the mainFrame gui element
     * 
     * @return - mainframe gui
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    @Override
    public FALManager getFalManager() {
        return (FALManager) falManager;
    }

    /**
     * Returns the MMSI of the shore center, or null if not defined
     * 
     * @return the MMSI of the shore center
     */
    @Override
    public Long getMmsi() {
        String shoreID = getSettings().getEnavSettings().getShoreId();
        if (shoreID == null || !StringUtils.isNumeric(shoreID)
                || !shoreID.startsWith(MaritimeCloudUtils.STCC_MMSI_PREFIX)) {
            return null;
        }
        return Long.parseLong((String) shoreID.subSequence(0, 9));
    }

    /**
     * Returns the maritime id of the shore center, or null if not defined
     * 
     * @return the maritime id of the shore center
     */
    @Override
    public MaritimeId getMaritimeId() {
        Long mmsi = getMmsi();
        return (mmsi != null) ? new MmsiId(getMmsi().intValue()) : null;
    }

    /**
     * Returns the current position of the shore center
     * 
     * @return the current position of the shore center
     */
    @Override
    public Position getPosition() {
        LatLonPoint pos = getSettings().getEnavSettings().getShorePos();
        return Position.create(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    public RouteManager getRouteManager() {
        return (RouteManager) routeManager;
    }

    /**
     * Return the shoreService used in shore connections like MSI
     * 
     * @return - shoreServicesCommon
     */
    public ShoreServicesCommon getShoreServices() {
        return shoreServicesCommon;
    }

    /**
     * Set the used theme using lookAndFeel
     */
    private void initLookAndFeel() {
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            Properties props = new Properties();
            props.put("logoString", "EPD-Shore");
            props.put("backgroundPattern", "false");
            props.put("textAntiAliasingMode", "TEXT_ANTIALIAS_VBGR");
            // props.put("menuOpaque", "true");
            // props.put("tooltipCastShadow", "true");

            // small font
            props.setProperty("controlTextFont", "Dialog 12");
            props.setProperty("systemTextFont", "Dialog 12");
            props.setProperty("userTextFont", "Dialog 12");
            props.setProperty("menuTextFont", "Dialog 12");
            props.setProperty("windowTitleFont", "Dialog bold 12");
            props.setProperty("subTextFont", "Dialog 10");
//            props.setProperty("controlTextFont", "Dialog 10");
//            props.setProperty("systemTextFont", "Dialog 10");
//            props.setProperty("userTextFont", "Dialog 10");
//            props.setProperty("menuTextFont", "Dialog 10");
//            props.setProperty("windowTitleFont", "Dialog bold 10");
//            props.setProperty("subTextFont", "Dialog 8");

            // props.put("tooltipBorderSize", "15");
            // props.put("tooltipShadowSize", "15");

            // NoireLookAndFeel laf = new NoireLookAndFeel();
            HiFiLookAndFeel laf = new HiFiLookAndFeel();
            // NoireLookAndFeel.setCurrentTheme(props);
            HiFiLookAndFeel.setCurrentTheme(props);

            UIManager.setLookAndFeel(laf);

        } catch (Exception e) {
            LOG.error("Failed to set look and feed: " + e.getMessage());
        }

    }

    @Override
    protected String getPropertyFileName() {
        return "epd-shore.properties";
    }

    @Override
    protected void propertyLoadError(String msg, IOException e) {
        LOG.error(msg + e.getMessage());
    }

    /**
     * Starts the sensors defined in the {@linkplain SensorSettings} and hook up
     * listeners
     */
    @Override
    protected void startSensors() {
        EPDSensorSettings sensorSettings = getSettings().getSensorSettings();
        switch (sensorSettings.getAisConnectionType()) {
        case NONE:
            aisSensor = new NmeaStdinSensor();
            break;
        case TCP:
            aisSensor = new NmeaTcpSensor(
                    sensorSettings.getAisHostOrSerialPort(),
                    sensorSettings.getAisTcpOrUdpPort());
            break;
        case SERIAL:
            aisSensor = NmeaSerialSensorFactory.create(
                    sensorSettings.getAisHostOrSerialPort(),
                    sensorSettings.getAisSerialPortBaudRate());
            break;
        case FILE:
            aisSensor = new NmeaFileSensor(sensorSettings.getAisFilename(),
                    sensorSettings);
            break;
        default:
            LOG.error("Unknown sensor connection type: "
                    + sensorSettings.getAisConnectionType());
        }

        if (aisSensor != null) {
            aisSensor.setVessel(false);
            aisSensor.addAisListener(aisHandler);
            aisSensor.addPntListener(PntTime.getInstance());
            aisSensor.start();
            // Add ais sensor to bean context
            beanHandler.add(aisSensor);
        }

    }

    /**
     * Stops all sensors and remove listeners
     */
    @Override
    protected void stopSensors() {
        // Stop AIS sensor
        if (aisSensor != null) {
            beanHandler.remove(aisSensor);
            aisSensor.removeAisListener(aisHandler);
            stopSensor(aisSensor, 3000L);
            aisSensor = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void settingsChanged(Type type) {
        if (type == Type.SENSOR) {
            LOG.warn("Restarting all sensors");
            stopSensors();
            startSensors();

        } else if (type == Type.CLOUD) {
            LOG.warn("Restarting all eNav Service");
            maritimeCloudService.stop();
            maritimeCloudService.start();
        } else if (type == Type.ENAV) {
            // Update the intended route handler such that it can re apply its
            // filter.
            this.intendedRouteHandler.updateSettings(this.settings
                    .getEnavSettings());
        }

    }

    public StaticImages getStaticImages() {
        return staticImages;
    }

    /**
     * Function used to call sleep on a thread
     * 
     * @param ms
     *            - time in ms of how long to sleep
     */
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
        }
    }

    public AisReader getAisReader() {
        return aisReader;
    }

    public VoyageManager getVoyageManager() {
        return voyageManager;
    }

    /**
     * @return the sruManager
     */
    public SRUManager getSruManager() {
        return sruManager;
    }

    /**
     * @return the monaLisaRouteExchange
     */
    public MonaLisaRouteOptimization getMonaLisaRouteExchange() {
        return monaLisaRouteExchange;
    }

    /**
     * Get the application-wide voyage event dispatcher.
     * 
     * @return The application-wide voyage event dispatcher.
     */
    public VoyageEventDispatcher getVoyageEventDispatcher() {
        return voyageEventDispatcher;
    }

    /**
     * @return the voctHandler
     */
    public VoctHandler getVoctHandler() {
        return (VoctHandler) voctHandler;
    }

    /**
     * Returns a {@code Resource} instance which loads resource from the same
     * class-loader/jar-file as the {@code EPDShore} class.
     * 
     * @return a new {@code Resource} instance
     */
    public static Resources res() {
        return Resources.get(EPDShore.class);
    }

}
