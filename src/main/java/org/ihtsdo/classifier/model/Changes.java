package org.ihtsdo.classifier.model;

import java.util.List;

import org.ihtsdo.json.model.RelationshipVersion;

public class Changes {

	List<RelationshipVersion> newInferredIsas;
	List<RelationshipVersion> lostInferredIsas;
	List<RelationshipVersion> newAttributes;
	List<RelationshipVersion> lostAttributes;
	public List<RelationshipVersion> getNewInferredIsas() {
		return newInferredIsas;
	}
	public void setNewInferredIsas(List<RelationshipVersion> newInferredIsas) {
		this.newInferredIsas = newInferredIsas;
	}
	public List<RelationshipVersion> getLostInferredIsas() {
		return lostInferredIsas;
	}
	public void setLostInferredIsas(List<RelationshipVersion> lostInferredIsas) {
		this.lostInferredIsas = lostInferredIsas;
	}
	public List<RelationshipVersion> getNewAttributes() {
		return newAttributes;
	}
	public void setNewAttributes(List<RelationshipVersion> newAttributes) {
		this.newAttributes = newAttributes;
	}
	public List<RelationshipVersion> getLostAttributes() {
		return lostAttributes;
	}
	public void setLostAttributes(List<RelationshipVersion> lostAttributes) {
		this.lostAttributes = lostAttributes;
	}

}
