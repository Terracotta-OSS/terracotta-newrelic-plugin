package com.terracotta.nrplugin.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Jeff on 9/30/2014.
 */
@Service
public class LockManager {

	final Logger log = LoggerFactory.getLogger(this.getClass());

	ReentrantLock cacheLock = new ReentrantLock();

	public void lockCache() {
		log.debug(Thread.currentThread().getName() + " locking stats cache...");
		cacheLock.lock();
	}

	public void unlockCache() {
		log.debug(Thread.currentThread().getName() + " unlocking stats cache...");
		cacheLock.unlock();
	}

}
