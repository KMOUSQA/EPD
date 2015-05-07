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
package dk.dma.epd.ship.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.maritimecloud.core.id.MaritimeId;
import net.maritimecloud.net.EndpointInvocationFuture;
import net.maritimecloud.net.MessageHeader;
import net.maritimecloud.net.mms.MmsClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.epd.common.prototype.EPD;
import dk.dma.epd.common.prototype.service.VoctHandlerCommon;
import dk.dma.epd.common.prototype.voct.VOCTUpdateEvent;
import dk.dma.epd.common.prototype.voct.VOCTUpdateListener;
import dk.dma.epd.ship.service.voct.VOCTManager;
import dma.voct.AbstractVOCTEndpoint;
import dma.voct.VOCTMessage;
import dma.voct.VOCTReply;
import dma.voct.VOCTReplyEndpoint;
import dma.voct.VOCTReplyStatus;

/**
 * Ship specific intended route service implementation.
 * <p>
 * Listens for changes to the active route and broadcasts it. Also broadcasts
 * the route periodically.
 * <p>
 * Improvements:
 * <ul>
 * <li>Use a worker pool rather than spawning a new thread for each broadcast.</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class VoctHandler extends VoctHandlerCommon implements Runnable,
        VOCTUpdateListener {

    /**
     * Protocols needed for VOCT Communication - may be further split or
     * combined in future
     */
    private boolean running;

    // private IntendedRouteLayerCommon intendedRouteLayerCommon;

    private static final Logger LOG = LoggerFactory
            .getLogger(VoctHandlerCommon.class);

    // ID, MMSI
    protected Map<Long, MaritimeId> voctInvitations = new ConcurrentHashMap<>();

    private boolean hasReconnect;

    /**
     * Constructor
     */
    public VoctHandler() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cloudConnected(MmsClient connection) {
        super.cloudConnected(connection);

        // Register a cloud chat service
        try {
            getMmsClient().endpointRegister(new AbstractVOCTEndpoint() {

                @Override
                protected void SendVOCTData(MessageHeader header,
                        VOCTMessage voctMessage) {
                    // TODO Auto-generated method stub
                    voctInvitations.put(voctMessage.getId(), header.getSender());
                    voctManager.handleSARDataPackage(voctMessage);
                    System.out.println("Recieved some VOCT data!");
                }

            }).awaitRegistered(4, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            LOG.error("Error hooking up services", e);
        }

        getScheduler().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (hasReconnect) {
                    System.out.println("Running reconnect to osc");
                    reconnectToOSC();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);

        // TODO: Maritime Cloud 0.2 re-factoring
        // // Register for RapidResponse
        // try {
        // getMmsClient()
        // .serviceRegister(
        // VOCTCommunicationService.INIT,
        // new
        // InvocationCallback<VOCTCommunicationService.VOCTCommunicationMessage,
        // VOCTCommunicationService.VOCTCommunicationReply>() {
        // public void process(VOCTCommunicationMessage message,
        // InvocationCallback.Context<VOCTCommunicationService.VOCTCommunicationReply>
        // context) {
        //
        // // LOG.info("Shore received a VOCT reply");
        // System.out.println("Received SAR Payload!");
        //
        // MaritimeId caller = context.getCaller();
        // long mmsi = MaritimeCloudUtils.toMmsi(context.getCaller());
        // //
        // voctInvitations.put(message.getId(), mmsi);
        // // cloudStatus.markCloudReception();
        // //
        // // voctContextRapidResponse = context;
        // //
        // voctManager.handleSARDataPackage(message);
        //
        // context.complete(new VOCTCommunicationReply(message.getId(),
        // EPDShip.getInstance()
        // .getOwnShipHandler().getMmsi(), System.currentTimeMillis()));
        // }
        // }).awaitRegistered(4, TimeUnit.SECONDS);
        //
        // // Register for DatumPoint
        // // getMmsClient()
        // // .serviceRegister(
        // // VOCTCommunicationServiceDatumPoint.INIT,
        // // new
        // InvocationCallback<VOCTCommunicationServiceDatumPoint.VOCTCommunicationMessageDatumPoint,
        // //
        // VOCTCommunicationServiceDatumPoint.VOCTCommunicationReplyDatumPoint>()
        // {
        // // public void process(
        // // VOCTCommunicationMessageDatumPoint message,
        // //
        // InvocationCallback.Context<VOCTCommunicationServiceDatumPoint.VOCTCommunicationReplyDatumPoint>
        // context) {
        // //
        // // System.out.println("Received SAR Payload!");
        // // MaritimeId caller = context.getCaller();
        // // long mmsi = MaritimeCloudUtils.toMmsi(context.getCaller());
        // // //
        // // voctInvitations.put(message.getId(), mmsi);
        // // // cloudStatus.markCloudReception();
        // // //
        // // // voctContextDatumPoint = context;
        // // //
        // // voctManager.handleSARDataPackage(message);
        // //
        // // // message.get
        // // // context.complete(new RouteSuggestionReply(message.getId()));
        // // context.complete(new
        // VOCTCommunicationReplyDatumPoint(message.getId(),
        // EPDShip.getInstance()
        // // .getOwnShipHandler().getMmsi(), System.currentTimeMillis()));
        // // }
        // // }).awaitRegistered(4, TimeUnit.SECONDS);
        //
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //
        // // Start broadcasting our own active route
        // running = true;
        // new Thread(this).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cloudDisconnected() {
        running = false;
    }

    /**
     * Main thread run method. Broadcasts the intended route
     */
    public void run() {

    }

    private void reconnectToOSC() {

        if (voctManager.getSarData().getOscId() != null
                && voctManager.getSarData().getTransactionId() != null) {
            try {
                System.out.println("Reconnect time");
                EPD.getInstance()
                        .getVoctHandler()
                        .sendVOCTReply(VOCTReplyStatus.ACCEPTED, "Reconnected",
                                voctManager.getSarData().getTransactionId(),
                                voctManager.getSarData().getOscId());

                hasReconnect = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void sendVOCTReply(VOCTReplyStatus recievedAccepted, String message,
            long messageId, Long oscId) throws InterruptedException,
            ExecutionException {

        if (voctInvitations.containsKey(messageId)) {

            oscId = (long) voctInvitations.get(messageId).getIdAsInt();
        }

        List<VOCTReplyEndpoint> availableEndpoints = getMmsClient()
                .endpointLocate(VOCTReplyEndpoint.class).findAll().get();
        VOCTReplyEndpoint endPoint = null;
        for (int i = 0; i < availableEndpoints.size(); i++) {
            System.out.println("Comparing "
                    + availableEndpoints.get(i).getRemoteId().getIdAsInt()
                    + " to stored " + oscId);
            if (availableEndpoints.get(i).getRemoteId().getIdAsInt() == oscId) {
                System.out.println("Found endpoint!");
                endPoint = availableEndpoints.get(i);
                break;
            }

        }

        if (endPoint == null) {
            System.out.println("failed to find VOCT reply endpoint for "
                    + oscId);
            return;
        }

        VOCTReply reply = new VOCTReply();

        long transactionId = -1;

        reply.setId(transactionId);
        reply.setReplyText(message);
        reply.setStatus(recievedAccepted);

        EndpointInvocationFuture<Void> returnVal = endPoint
                .sendVOCTReply(reply);

        // if (type == SAR_TYPE.RAPID_RESPONSE) {
        // try {

        // VOCTCommunicationMessage voctMessage = new
        // VOCTCommunicationMessage(id, message, recievedAccepted);

        // RouteSuggestionMessage routeMessage = new
        // RouteSuggestionMessage(null, null, null);
        // System.out.println("Replying to : " + (long)
        // voctInvitations.get(id));

        // TODO: Maritime Cloud 0.2 re-factoring
        // boolean toSend = sendMaritimeCloudMessage(new MmsiId((int) (long)
        // voctInvitations.get(id)), voctMessage, this);

        // System.out.println("To Send is " + toSend);

        // sendMaritimeCloudMessage(new MmsiId((int)100), routeMessage,
        // EPDShip.getInstance()
        // .getRouteSuggestionHandler());

        // } catch (Exception e) {
        // System.out.println("Failed to reply " + e);
        // }
        // }
        //
        // if (type == SAR_TYPE.DATUM_POINT) {
        // try {
        // voctContextDatumPoint.complete(new
        // VOCTCommunicationServiceDatumPoint.VOCTCommunicationReplyDatumPoint(message,
        // id,
        // ownShipHandler.getMmsi(), System.currentTimeMillis(),
        // recievedAccepted));
        // cloudStatus.markSuccesfullSend();
        // } catch (Exception e) {
        // cloudStatus.markFailedSend();
        // System.out.println("Failed to reply");
        // }
        // }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void findAndInit(Object obj) {
        super.findAndInit(obj);

        if (obj instanceof VOCTManager) {
            voctManager = (VOCTManager) obj;
            voctManager.addListener(this);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void findAndUndo(Object obj) {
        // if (obj instanceof RouteManager) {
        // routeManager.removeListener(this);
        // routeManager = null;
        // }
        super.findAndUndo(obj);
    }

    @Override
    public void voctUpdated(VOCTUpdateEvent e) {

        if (e == VOCTUpdateEvent.SAR_CANCEL) {

            // IS IT RAPID RESPONSE / DOES IT MATTER
            // if (voctManager.getCurrentID() != -1) {
            //
            // // VOCTCommunicationMessage voctMessage = new
            // // VOCTCommunicationMessage(voctManager.getCurrentID(),
            // // VoctMsgStatus.WITHDRAWN);
            //
            // // TODO: Maritime Cloud 0.2 re-factoring
            // // boolean toSend = sendMaritimeCloudMessage(new MmsiId((int)
            // // (long) voctInvitations.get(voctManager.getCurrentID())),
            // // voctMessage, this);
            // }
        }

    }

    /**
     * @param hasReconnect
     *            the hasReconnect to set
     */
    public void setHasReconnect(boolean hasReconnect) {
        System.out.println("Has reconnect!");
        this.hasReconnect = hasReconnect;
    }

}
