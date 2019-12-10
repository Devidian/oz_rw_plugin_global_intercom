/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.omegazirkel.risingworld;

/**
 *
 * @author Maik
 */
public class WSMessage<T> {

	public final String event;
	public final T payload;
	// 
	public final String subject = null;
	// i18n codes
	public final String infoCode = null;
	public final String successCode = null;
	public final String errorCode = null;

	WSMessage(String event, T payload) {
		this.payload = payload;
		this.event = event;
	}

	@Override
	public String toString() {
		return "WSMessage Object\nEvent: " + this.event + "\nPayload: " + this.payload;
	}
}
