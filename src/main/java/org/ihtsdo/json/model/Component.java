/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ihtsdo.json.model;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author Alejandro Rodriguez
 */
public class Component implements Serializable {

    private UUID uuid;
    private String active;
    private String effectiveTime;
    private String module;
    
    public Component() {
    }

    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
