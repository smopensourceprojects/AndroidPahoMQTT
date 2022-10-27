/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.source.pahomqtt.service;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import android.app.Service;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default ping sender implementation on Android. It is based on ScheduledExecutorService.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
	// Identifier for Intents, log messages, etc..
	private static final String TAG = "AlarmPingSender";

	private ScheduledExecutorService executor;
	private ScheduledFuture scheduledFuture;
	private ClientComms comms;
	private MqttService service;
	private WakeLock wakelock;
	private String wakeLockTag;
	private volatile boolean hasStarted = false;

	public AlarmPingSender(MqttService service) {
		if (service == null) {
			throw new IllegalArgumentException(
					"Neither service nor client can be null.");
		}
		this.service = service;
	}

	@Override
	public void init(ClientComms comms) {
		this.comms = comms;
		wakelock = null;
		wakeLockTag = MqttServiceConstants.PING_WAKELOCK + comms.getClient().getClientId();
		reScheduleTimer();
	}

	private void reScheduleTimer() {
		if(null != scheduledFuture) {
			scheduledFuture.cancel(true);
		}
		if(null == executor) {
			executor = Executors.newScheduledThreadPool(1);
		}
	}

	@Override
	public void start() {
		String action = MqttServiceConstants.PING_SENDER + comms.getClient().getClientId();
		Log.d(TAG, "Register alarmreceiver to MqttService"+ action);
		schedule(comms.getKeepAlive());
		hasStarted = true;
	}

	@Override
	public void stop() {
		Log.d(TAG, "Unregister alarmreceiver to MqttService"+comms.getClient().getClientId());
		if(hasStarted){
			if(null != scheduledFuture) {
				scheduledFuture.cancel(true);
			}
			if(null != executor) {
				//Return List<Runnable>
				executor.shutdownNow();
			}
			scheduledFuture = null;
			executor = null;
			hasStarted = false;
		}
	}

	@Override
	public void schedule(long delayInMilliseconds) {
		long nextAlarmInMilliseconds = System.currentTimeMillis() + delayInMilliseconds;
		Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
		reScheduleTimer();
		scheduledFuture = executor.schedule(() ->
				doPeriodicProcess(), nextAlarmInMilliseconds, TimeUnit.MILLISECONDS);
	}

	/*
	 * This class sends PingReq packet to MQTT broker
	 */
	private void doPeriodicProcess() {
		// According to the docs, "ScheduledExecutorService holds a CPU wake lock as
		// long as the alarm receiver's schedule() method is executing.
		// This guarantees that the phone will not sleep until you have
		// finished handling the broadcast.", but this class still get
		// a wake lock to wait for ping finished.

		Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis());

		PowerManager pm = (PowerManager) service
				.getSystemService(Service.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
		wakelock.acquire();

		// Assign new callback to token to execute code after PingResq
		// arrives. Get another wakelock even receiver already has one,
		// release it until ping response returns.
		IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
						+ System.currentTimeMillis());
				//Release wakelock when it is done.
				wakelock.release();
			}

			@Override
			public void onFailure(IMqttToken asyncActionToken,
								  Throwable exception) {
				Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
						+ System.currentTimeMillis());
				//Release wakelock when it is done.
				wakelock.release();
			}
		});

		if (token == null && wakelock.isHeld()) {
			wakelock.release();
		}
	}
}
