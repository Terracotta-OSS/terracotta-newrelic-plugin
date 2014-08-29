package com.terracotta.nrplugin.pojo.tmc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientStatistics extends BaseStatistics {

    private static final long serialVersionUID = 144647955717932276L;

}
