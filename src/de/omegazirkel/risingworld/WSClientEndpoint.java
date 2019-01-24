/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

import java.io.IOException;
import java.net.URI;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.OnError;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

/**
 *
 * @author Maik
 */
@ClientEndpoint
public class WSClientEndpoint {

	private static WSClientEndpoint HIGHLANDER = null;

	public static WSClientEndpoint getInstance(URI endpointURI) {
		if (HIGHLANDER == null) {
			HIGHLANDER = new WSClientEndpoint(endpointURI);
		}
		return HIGHLANDER;
	}

	Session session = null;
	private MessageHandler messageHandler;
	private URI endpointURI = null;
	// private int disconnectCount = 0;
	public boolean isConnected = false;
	private ClientManager client;

	/**
	 *
	 * @param endpointURI
	 */
	private WSClientEndpoint(URI endpointURI) {
		this.endpointURI = endpointURI;
		this.client = ClientManager.createClient();
		ClientManager.ReconnectHandler reconnectHandler = new ClientManager.ReconnectHandler() {
			private int counter = 0;

			@Override
			public boolean onDisconnect(CloseReason closeReason) {
				final int i = ++counter;
				GlobalIntercom.log.out("WebSocket got disconnected: " + closeReason.toString(), 999);
				if (closeReason.getCloseCode() == CloseCodes.CLOSED_ABNORMALLY) {
					GlobalIntercom.log.out("WebSocket reconnecting... " + i, 0);
					return true;
				} else {
					GlobalIntercom.log.out("WebSocket not reconnecting.", 0);
					return false;
				}
			}

			@Override
			public boolean onConnectFailure(Exception exception) {
				final int i = ++counter;
				GlobalIntercom.log.out("WebSocket failed to connect: " + exception.getMessage(), 999);
				// if (i <= 30) {
				GlobalIntercom.log.out("WebSocket reconnecting... " + i, 0);
				return true;
				// } else {
				// GlobalIntercom.log.out("WebSocket not reconnecting.", 0);
				// return false;
				// }
			}
		};
		this.client.getProperties().put(ClientProperties.RECONNECT_HANDLER, reconnectHandler);
		this.connect();
	}

	/**
	 *
	 */
	private void connect() {
		try {
			GlobalIntercom.log.out("WebSocket connecting to " + this.endpointURI, 0);
			this.session = this.client.connectToServer(this, this.endpointURI);
		} catch (IOException | DeploymentException e) {
			GlobalIntercom.log.out(e.getMessage(), 999);
		}
	}

	/**
	 *
	 * @param session
	 * @param t
	 */
	@OnError
	public void onError(Session session, Throwable t) {
		GlobalIntercom.log.out(t.toString(), 999);

	}

	/**
	 *
	 * @param session
	 */
	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		this.isConnected = true;
		GlobalIntercom.log.out("WebSocket connected!", 0);
	}

	/**
	 *
	 * @param session
	 * @param reason
	 */
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		this.session = null;
		this.isConnected = false;
		// disconnectCount++;
		GlobalIntercom.log.out("WebSocket closed: " + reason.toString(), 999);
		// todo: do not retry after x disconnects
		// try {
		// this.connect(); // reconnect
		// } catch (Exception e) {
		// System.out.println(e.getMessage());
		// }
	}

	/**
	 *
	 * @param message
	 * @param session
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		if (this.messageHandler != null) {
			this.messageHandler.handleMessage(message);
		}
	}

	/**
	 *
	 * @param msgHandler
	 */
	public void setMessageHandler(MessageHandler msgHandler) {
		this.messageHandler = msgHandler;
	}

	/**
	 *
	 * @param message
	 */
	public void sendMessage(String message) {
		this.session.getAsyncRemote().sendText(message);
	}

	/**
	 *
	 */
	public static interface MessageHandler {

		public void handleMessage(String message);
	}
}
