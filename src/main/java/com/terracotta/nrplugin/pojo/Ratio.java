package com.terracotta.nrplugin.pojo;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 4/7/14
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Ratio {

    String key;
    long hitCount;
    long missCount;

    public Ratio() {
    }

    public Ratio(String key, long hitCount, long missCount) {
        this.key = key;
        this.hitCount = hitCount;
        this.missCount = missCount;
    }

    public double getHitRatio() {
        return hitCount / (hitCount + missCount);
    }

    public double getMissRatio() {
        return missCount / (hitCount + missCount);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public void setMissCount(long missCount) {
        this.missCount = missCount;
    }
}
