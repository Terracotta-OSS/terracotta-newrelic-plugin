package com.terracotta.nrplugin.pojo.tmc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Jeff
 * Date: 3/20/14
 * Time: 9:57 AM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientEntities extends TmcBase {

	private static final long serialVersionUID = -1440414121520091204L;

	Map<String, Object> attributes;

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("attributes", attributes)
				.toString();
	}
}
