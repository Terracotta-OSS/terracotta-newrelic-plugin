package com.terracotta.nrplugin.pojo;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Fabien
 * Date: 3/12/14
 * Time: 4:28 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
@Scope("prototype")
public abstract class MetricDatasetComponent implements Serializable, Cloneable {
    private static final long serialVersionUID = 483809302495395084L;

    public abstract String getComponentName();

    @Override
    public MetricDatasetComponent clone() throws CloneNotSupportedException {
        return (MetricDatasetComponent) super.clone();
    }
}