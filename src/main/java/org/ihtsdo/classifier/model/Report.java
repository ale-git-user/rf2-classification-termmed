package org.ihtsdo.classifier.model;

import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.json.model.ConceptDescriptor;

public class Report {

	String executionId;
	
	String equivalentsDefinitionsDetected;
	List<ArrayList<ConceptDescriptor>> equivalentConceptGroups;
	
	String cyclesDetected;
	List<ConceptDescriptor> conceptInCycles;
	
	Changes changes;

	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public String getEquivalentsDefinitionsDetected() {
		return equivalentsDefinitionsDetected;
	}

	public void setEquivalentsDefinitionsDetected(
			String equivalentsDefinitionsDetected) {
		this.equivalentsDefinitionsDetected = equivalentsDefinitionsDetected;
	}

	public List<ArrayList<ConceptDescriptor>> getEquivalentConceptGroups() {
		return equivalentConceptGroups;
	}

	public void setEquivalentConceptGroups(
			List<ArrayList<ConceptDescriptor>> equivalentConceptGroups) {
		this.equivalentConceptGroups = equivalentConceptGroups;
	}

	public String getCyclesDetected() {
		return cyclesDetected;
	}

	public void setCyclesDetected(String cyclesDetected) {
		this.cyclesDetected = cyclesDetected;
	}

	public List<ConceptDescriptor> getConceptInCycles() {
		return conceptInCycles;
	}

	public void setConceptInCycles(List<ConceptDescriptor> conceptInCycles) {
		this.conceptInCycles = conceptInCycles;
	}

	public Changes getChanges() {
		return changes;
	}

	public void setChanges(Changes changes) {
		this.changes = changes;
	}
	
	
}
