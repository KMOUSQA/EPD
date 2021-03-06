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
package dk.dma.epd.shore.gui.notification;

import dk.dma.epd.common.graphics.GraphicsUtil;
import dk.dma.epd.common.prototype.gui.notification.NotificationCenterCommon;
import dk.dma.epd.common.prototype.gui.notification.NotificationDetailPanel;
import dk.dma.epd.common.prototype.gui.notification.NotificationPanel;
import dk.dma.epd.common.prototype.gui.notification.NotificationTableModel;
import dk.dma.epd.common.prototype.model.route.RouteSuggestionData;
import dk.dma.epd.common.prototype.notification.NotificationType;
import dk.dma.epd.common.text.Formatter;
import dk.dma.epd.shore.EPDShore;
import dk.dma.epd.shore.service.RouteSuggestionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A route suggestion implementation of the {@linkplain NotificationPanel} class
 */
public class RouteSuggestionNotificationPanel extends NotificationPanel<RouteSuggestionNotification> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(RouteSuggestionNotificationPanel.class);

    private static final String[] NAMES = { "", "MMSI", "Route Name", "Date", "Status" };

    protected JButton resendBtn;

    /**
     * Constructor
     */
    public RouteSuggestionNotificationPanel(NotificationCenterCommon notificationCenter) {
        super(notificationCenter);

        table.getColumnModel().getColumn(0).setMaxWidth(18);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        splitPane.setDividerLocation(350);
        setCellAlignment(1, JLabel.RIGHT);

        doRefreshNotifications();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ButtonPanel initButtonPanel() {
        ButtonPanel btnPanel = super.initButtonPanel();

        resendBtn = new JButton("Resend", EPDShore.res().getCachedImageIcon("images/notificationcenter/arrow-circle-315.png"));
        btnPanel.add(resendBtn);
        btnPanel.add(chatBtn);

        resendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resendSelectedRouteSuggestion();
            }
        });

        return btnPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateButtonEnabledState() {
        super.updateButtonEnabledState();
        RouteSuggestionNotification n = getSelectedNotification();
        resendBtn.setEnabled(n != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationType getNotitficationType() {
        return NotificationType.TACTICAL_ROUTE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationTableModel<RouteSuggestionNotification> initTableModel() {
        return new NotificationTableModel<RouteSuggestionNotification>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String[] getColumnNames() {
                return NAMES;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ImageIcon.class;
                } else {
                    return super.getColumnClass(columnIndex);
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                RouteSuggestionNotification notification = getNotification(rowIndex);

                switch (columnIndex) {
                case 0:
                    return !notification.isRead() ? ICON_UNREAD : (notification.isAcknowledged() ? ICON_ACKNOWLEDGED : null);
                case 1:
                    return "" + notification.get().getMmsi();
                case 2:
                    return notification.get().getRoute().getName();
                case 3:
                    return Formatter.formatShortDateTimeNoTz(notification.getDate());
                case 4:
                    return notification.get().getStatus().toString();
                default:
                }
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationDetailPanel<RouteSuggestionNotification> initNotificationDetailPanel() {
        return new RouteSuggestionDetailPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acknowledgeNotification(RouteSuggestionNotification notification) {
        if (notification != null && !notification.isAcknowledged()) {
            RouteSuggestionHandler routeSuggestionHandler = EPDShore.getInstance().getRouteSuggestionHandler();
            RouteSuggestionData routeSuggestion = notification.get();
            // NB: routeSuggestionHandler.setRouteSuggestionAcknowledged() will automatically trigger a table refresh
            routeSuggestionHandler.setRouteSuggestionAcknowledged(routeSuggestion.getId());
            selectFirstUnacknowledgedRow();
            notifyListeners();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNotification(RouteSuggestionNotification notification) {
        int row = table.getSelectedRow();
        if (notification != null) {
            RouteSuggestionHandler routeSuggestionHandler = EPDShore.getInstance().getRouteSuggestionHandler();
            RouteSuggestionData routeSuggestion = notification.get();
            // NB: routeSuggestionHandler.removeSuggestion() will automatically trigger a table refresh
            routeSuggestionHandler.removeSuggestion(routeSuggestion.getId());
            setSelectedRow(row - 1);
            notifyListeners();
        }
    }

    /**
     * Re-sends the selected route suggestion
     */
    protected void resendSelectedRouteSuggestion() {
        RouteSuggestionNotification notification = getSelectedNotification();
        if (notification != null) {
            RouteSuggestionHandler routeSuggestionHandler = EPDShore.getInstance().getRouteSuggestionHandler();
            RouteSuggestionData routeSuggestion = notification.get();
            try {
                routeSuggestionHandler.sendRouteSuggestion(routeSuggestion.getMmsi(),
                        routeSuggestion.getRoute(), routeSuggestion.getMessage().getTextMessage());

            } catch (Exception ex) {
                LOG.error("Error re-sending route suggestion", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRefreshNotifications() {
        RouteSuggestionHandler routeSuggestionHandler = EPDShore.getInstance().getRouteSuggestionHandler();

        // The back-end does not support the "read" flag, so, we store it
        Set<Long> readNotificationIds = new HashSet<>();
        for (RouteSuggestionNotification notificaiton : tableModel.getNotifications()) {
            if (notificaiton.isRead()) {
                readNotificationIds.add(notificaiton.getId());
            }
        }

        List<RouteSuggestionNotification> notifications = new ArrayList<>();
        for (RouteSuggestionData routeSuggestion : routeSuggestionHandler.getSortedRouteSuggestions()) {
            RouteSuggestionNotification notification = new RouteSuggestionNotification(routeSuggestion);

            // Restore the "read" flag
            if (readNotificationIds.contains(notification.getId())) {
                notification.setRead(true);
            }
            notifications.add(notification);
        }
        tableModel.setNotifications(notifications);
        refreshTableData();
        notifyListeners();
    }

}

/**
 * Displays relevant route suggestion detail information
 */
class RouteSuggestionDetailPanel extends NotificationDetailPanel<RouteSuggestionNotification> {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public RouteSuggestionDetailPanel() {
        super();

        buildGUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotification(RouteSuggestionNotification notification) {
        this.notification = notification;

        // Special case
        if (notification == null) {
            contentLbl.setText("");
            return;
        }

        RouteSuggestionData routeSuggestion = notification.get();

        StringBuilder html = new StringBuilder("<html>");
        html.append("<table>");
        append(html, "ID", routeSuggestion.getId());
        append(html, "MMSI", routeSuggestion.getMmsi());
        append(html, "Route Name", routeSuggestion.getRoute().getName());
        append(html, "Sent Date", Formatter.formatShortDateTime(routeSuggestion.getSendDate()));
        append(html, "Message", Formatter.formatHtml(routeSuggestion.getMessage().getTextMessage()));
        append(html, "Status", getStatus(routeSuggestion));
        if (routeSuggestion.isReplied()) {
            append(html, "Reply Sent", Formatter.formatShortDateTime(routeSuggestion.getReplyRecieveDate()));
            if (routeSuggestion.getReply().getReplyText().equals("")) {
                append(html, "Reply Message ", Formatter.formatHtml("No Reply Message Attached"));
            } else {
                append(html, "Reply Message ", Formatter.formatHtml(routeSuggestion.getReply().getReplyText()));
            }
        } else {
            append(html, "Reply Sent", "No reply received yet");
            append(html, "Reply Message", "No reply received yet");
        }
        html.append("</table>");
        html.append("</html>");
        contentLbl.setText(html.toString());
    }

    /**
     * Formats the status of the route suggestion as HTML by including the Maritime Cloud message status.
     * 
     * @param routeSuggestion
     *            the route suggestion
     * @return the status
     */
    private String getStatus(RouteSuggestionData routeSuggestion) {
        StringBuilder status = new StringBuilder();
        status.append(String.format("<span style='color:%s'>%s</span>",
                GraphicsUtil.toHtmlColor(routeSuggestion.replySuggestionColor()), routeSuggestion.getStatus().toString()));
        if (routeSuggestion.getReply() == null) {
            status.append("&nbsp;<small>(" + routeSuggestion.getCloudMessageStatus().getTitle() + ")</small>");
        }
        return status.toString();
    }

    /**
     * If non-empty, appends a table row with the given title and value
     * 
     * @param html
     *            the html to append to
     * @param title
     *            the title
     * @param value
     *            the value
     */
    private void append(StringBuilder html, String title, Object value) {
        if (value != null && value.toString().length() > 0) {
            html.append("<tr><td valign='top'><b>").append(title).append("</b></td><td>").append(value).append("</td></tr>");
        }
    }
}
