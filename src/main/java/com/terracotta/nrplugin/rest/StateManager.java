package com.terracotta.nrplugin.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Created by Jeff on 9/23/2014.
 */
@Service
public class StateManager {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	public enum TmcState {available, unavailable, unknown}

	protected TmcState tmcState = TmcState.unknown;

	public TmcState getTmcState() {
		return tmcState;
	}

	public void setTmcState(TmcState tmcState) {
		this.tmcState = tmcState;
	}

}
