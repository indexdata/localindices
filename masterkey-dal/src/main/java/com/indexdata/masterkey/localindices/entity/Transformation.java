/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */

package com.indexdata.masterkey.localindices.entity;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
//import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

/**
 * @author Dennis Schafroth
 */
@Entity
@NamedQueries({@NamedQuery(name = "Transformation.findById", query = "SELECT o FROM Transformation o WHERE o.id = :id")})
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Transformation implements Serializable, Cloneable {

    protected static final long serialVersionUID = 1L;
    // user-set properties
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    protected String name;
    @Column(length=4096)
    protected String description;
    protected Boolean enabled;

    @OneToMany
    protected List<TransformationStepAssociation> stepAssociations;

    @OneToMany
    private Set<Harvestable> harvestables;

    protected Transformation() {
		stepAssociations = new LinkedList<TransformationStepAssociation>();
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // change that so that it check the real class
        if (!(object instanceof Transformation)) {
            return false;
        }
        Transformation other = (Transformation) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + "[id=" + id + "]";
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

	public List<TransformationStep> getSteps() {
		List<TransformationStep> steps = new LinkedList<TransformationStep>();
		for (TransformationStepAssociation association : stepAssociations) {
			steps.add(association.getStep()); 
		}
		return steps;
	}

	public void addStep(TransformationStep step, int position) {
		TransformationStepAssociation association = new TransformationStepAssociation();
		association.setTransformation(this);
		association.setStep(step);
		association.setPosition(position);
		stepAssociations.add(association);
	}

	public void deleteStep(int associationId) {
		stepAssociations.remove(associationId);
	}

	public void setHarvestables(Set<Harvestable> harvestables) {
		this.harvestables = harvestables;
	}

	public Set<Harvestable> getHarvestables() {
		return harvestables;
	}

	public List<TransformationStepAssociation> getStepAssociations() {
		return stepAssociations;
	}

	public void setStepAssociations(
			List<TransformationStepAssociation> stepAssociations) {
		this.stepAssociations = stepAssociations;
	}
}
