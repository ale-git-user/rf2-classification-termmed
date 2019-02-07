package org.ihtsdo.json.model;


/**
 *
 * @author Alejandro Rodriguez
 */

public class RelationshipVersion extends Component {

	private String relationshipId;
    private LightConceptDescriptor type;
    private ConceptDescriptor target;
    private String sourceId;
    private Integer groupId;
    private LightConceptDescriptor charType;
    private String modifier;
    private String p;

    public RelationshipVersion() {
        super();
    }

	public LightConceptDescriptor getType() {
		return type;
	}

	public void setType(LightConceptDescriptor type) {
		this.type = type;
	}

	public ConceptDescriptor getTarget() {
		return target;
	}

	public void setTarget(ConceptDescriptor target) {
		this.target = target;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public LightConceptDescriptor getCharType() {
		return charType;
	}

	public void setCharType(LightConceptDescriptor charType) {
		this.charType = charType;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	public String getRelationshipId() {
		return relationshipId;
	}

	public void setRelationshipId(String relationshipId) {
		this.relationshipId = relationshipId;
	}

	public String getP() {
		return p;
	}

	public void setP(String p) {
		this.p = p;
	}

}
