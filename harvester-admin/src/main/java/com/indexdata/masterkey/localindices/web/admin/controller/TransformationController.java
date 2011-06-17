/*
 * Copyright (c) 1995-2008, Index Data
 * All rights reserved.
 * See the file LICENCE for details.
 */

package com.indexdata.masterkey.localindices.web.admin.controller;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.indexdata.masterkey.localindices.dao.DAOException;
import com.indexdata.masterkey.localindices.dao.TransformationDAO;
import com.indexdata.masterkey.localindices.dao.TransformationDAOFactory;
import com.indexdata.masterkey.localindices.entity.BasicTransformation;
import com.indexdata.masterkey.localindices.entity.BasicTransformationStep;
import com.indexdata.masterkey.localindices.entity.Transformation;
import com.indexdata.masterkey.localindices.entity.TransformationStep;
import com.indexdata.masterkey.localindices.entity.TransformationStepAssociation;
import com.indexdata.masterkey.localindices.web.service.converter.TransformationBrief;

/**
 * The controller for the Admin interface for Transformations, implements all the business logic and
 * controls data access through DAO object
 * @author Dennis 
 */
public class TransformationController {
    private Logger logger = Logger.getLogger(getClass());
    // Transformation
    private TransformationDAO dao;
    private Transformation current;
    
    private DataModel model;
    @SuppressWarnings("rawtypes")
	/* Transformations */
    private List resources;
	/* Steps for current transformations */
	private List<TransformationStepAssociation> stepAssociation = null;
	private String stepMode = "hideEditStep();"; // which JS function should be called on load
    private TransformationStepAssociation currentStepAssociation;
   
	Stack<String> backActions = new Stack<String>();
	String homeAction = "home";

    public TransformationController() {
        try {
            dao = TransformationDAOFactory.getTransformationDAO((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext() );
        } catch (DAOException ex) {
            logger.log(Level.FATAL, "Exception when retrieving DAO", ex);
        }
    }

    public Transformation getTransformation() {
        return current;
    }
    public void setTransformation(Transformation resource) {
        this.current = resource;
        //currentStepAssociation = current.getStepAssociations();
    }
    
    
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Transformation list paging functions">
    private int firstItem = 0;
    private int batchSize = 50;
    private int itemCount = -1;

    public int getBatchSize() {
        return batchSize;
    }

    public int getFirstItem() {
        return firstItem;
    }

    public int getLastItem() {
        int count = getItemCount();
        return (count < firstItem + batchSize) ? count : firstItem + batchSize;
    }

    public int getItemCount() {
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (itemCount == -1 || !isPb() && req.getAttribute("countRequestSeenFlag") == null) {
            req.setAttribute("countRequestSeenFlag", "yes");
            itemCount = dao.getTransformationCount();
        }
        return itemCount;
    }

    public String next() {
        if (firstItem + batchSize < getItemCount()) {
            firstItem += batchSize;
        }
        return list();
    }

    public String prev() {
        firstItem -= batchSize;
        if (firstItem < 0) {
            firstItem = 0;
        }
        return list();
    }

    public String list() {
        resources = null;
        itemCount = -1;
        return "list_transformations";
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="DAO methods">
    /* add new resource */
    public String prepareXsltTransformationToAdd() {
    	current = new BasicTransformation();
        return "new_xsl_transformation";
    }

    public String prepareCrossMapTransformationToAdd() {
        // TODO 
    	// current = new CrossMapTransformation();
        return "new_crossmap_transformation";
    }
    
    public String add() {
        prePersist();
        dao.createTransformation(current);
        current = null;
        currentStepAssociation = null;
        stepAssociation = null;
        return list();
    }

    /* update resource */
    public String prepareToEdit() {
        current = getResourceFromRequestParam();
        postDePersist();
        logger.log(Level.INFO, "Retrieved persisted resource of type " + current.getClass().getName());
        if (current instanceof Transformation) {
            return "edit_xsl_transformation";
        } 
        /* 
        else if (resource instanceof ZebraTransformation) {
            return "edit_zebraTransformation";
        } else if (resource instanceof ConsoleTransformation) {
            return "edit_console";
        } else if (resource instanceof XmlTransformation) {
            return "edit_xmlTransformation";
        } */
        else {
            logger.log(Level.INFO, "Unknown resource type. No matching form defined.");
            return "failure";
        }
    }

    public String save() {
        prePersist();
        current = dao.updateTransformation(current);
        current = null;
        return list();
    }

    private boolean isPb() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        return ctx.getRenderKit().getResponseStateManager().isPostback(ctx);
    }

    /* list resources */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public DataModel getTransformations() {
        //check if new request
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (resources == null || !isPb() && req.getAttribute("listRequestSeen") == null) {
            req.setAttribute("listRequestSeen", "yes");
            resources = (List) dao.retrieveTransformationBriefs(firstItem, batchSize);
        }
        if (resources != null)
            Collections.sort(resources);
        return new ListDataModel(resources);
    }
    
	@SuppressWarnings("unchecked")
	public DataModel getTransformationSteps() {
		List<TransformationStep> steps = new LinkedList<TransformationStep>();
        if (current != null)
        	steps = (List) current.getSteps();        
        return new ListDataModel(steps);
    }


    public String delete() {
        current = getResourceFromRequestParam();
        dao.deleteTransformation(current);
        current = null;
        return list();
    }
    
    public String saveAndPurge() {
        dao.deleteTransformation(current);
        prePersist();
        current.setId(null);
        dao.createTransformation(current);
        current = null;
        return list();
    }
    
    private void prePersist() {        

    }

    private void postDePersist() {

    }

    //</editor-fold>

    /* objects from request */
    public Transformation getResourceFromRequestParam() {
            Transformation o = null;
            if (model != null) {
                o = (Transformation) model.getRowData();
                //o = em.merge(o);
            } else {
                String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");
                Long id = new Long(param);
                o = dao.retrieveTransformationById(id);
            }
            return o;
    }

	public String stackBackAction(String newAction) {
		return backActions.push(newAction);
	}

	public String back() {
		if (backActions.isEmpty())
			return homeAction;
		return backActions.pop();
	}

	public String home() {
		return homeAction;
	}

	public String getHomeAction() {
		return homeAction;
	}

	public void setHomeAction(String homeAction) {
		this.homeAction = homeAction;
	}

	private int setupStep() {
		int index = -1; 
		String idName = "stepID";
        String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(idName);
        if (param != null && !"null".equals(param)) {
	        try {
	        	Long id = new Long(param);
	        	index = lookupIndexByID(id);
	        	if (index > 0)
	        		currentStepAssociation = current.getStepAssociations().get(index);
	        	else 
	        		currentStepAssociation = null;
	        } catch (Exception e) {
	        	logger.error("Unable to get Step from parameter '" + idName + "' " + param + ". Error: " + e.getMessage());
	        }
        }
    	logger.debug("Step from parameter '" + idName + "'=" + param + ": " + currentStepAssociation + " index: " + index);
		if (currentStepAssociation == null) {
			logger.error("Setting up new step.");
			BasicTransformationStep step = new BasicTransformationStep();
			step.setDescription("<Description>");
			step.setScript("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			step.setPosition(current.getSteps().size()+1);
			step.setName("<New step " + step.getPosition() + ">");
			currentStepAssociation.setStep(step);
		}
		return index;
	}
	
	private TransformationStep lookupStepByID(List<TransformationStep> steps, Long id) {
		for (TransformationStep step: steps) {
			if (step.getId().equals(id)) 
				return step;
		}
		return null;
	}

	private int lookupIndexByID(Long id) {
		for (int index = 0; index < current.getStepAssociations().size(); index++) {
			if (current.getStepAssociations().get(index).getStep().getId().equals(id)) 
				return index;
		}
		return -1;
	}

	
	public String addXslStep() {
		setupStep();	
		stepMode = "showEditStep();";
		return "new_xsl_step";
	}

	public String editStep() {
		setupStep();
		stepMode = "showEditStep();";
		//TODO edit depending on Step Type
		return "edit_xsl_step";
	}

	public String upStep() {
		setupStep();
		upDownStep(currentStepAssociation, -1);
		stepMode = "hideEditStep();";
		return "up_step";
	}

	private void upDownStep(TransformationStepAssociation association, int i) 
	{
		int index;
		for (index = 0; index < current.getStepAssociations().size(); index++) {
			if (current.getStepAssociations().get(index).equals(association)) {
				break;
			}
		}
		int newIndex = index + i;
		// If found and newIndex is within bounds
		if (index < current.getStepAssociations().size() && newIndex >= 0 && newIndex < current.getStepAssociations().size()) {
			TransformationStepAssociation cur = current.getStepAssociations().get(index);
			TransformationStepAssociation swap = current.getStepAssociations().get(newIndex);
			cur.setPosition(cur.getPosition()+i);
			swap.setPosition(swap.getPosition()-i);
			current.getStepAssociations().set(newIndex, cur);
			current.getStepAssociations().set(index, swap);
		}
	}

	public String downStep() {
		setupStep();
		upDownStep(currentStepAssociation, 1);
		stepMode = "hideEditStep();";
		return "down_step";
	}

	public String deleteStep() {
		//TransformationStepAssociation currentStep = null; 
		int index = setupStep();
		if (currentStepAssociation != null && currentStepAssociation.getTransformationId() != null) {
			currentStepAssociation = current.getStepAssociations().remove(index);
			logger.debug("" + currentStepAssociation + " was removed.");
			currentStepAssociation = null;
		}
		stepMode = "hideEditStep();";
        prePersist();
        current = dao.updateTransformation(current);
		
		stepMode = "hideEditStep();";
		return "delete_step";
	}

	public String saveStep() {
		// TODO persist current step. Not on list, add 
		if (currentStepAssociation != null && currentStepAssociation.getTransformationId() == null) {
			// TODO FIX UGLY
			current.addStep(currentStepAssociation.getStep(), currentStepAssociation.getStep().getPosition());
		}
		stepMode = "hideEditStep();";
        prePersist();
        current = dao.updateTransformation(current);
		return "save_step";
	}

	public String cancelStep() {
		currentStepAssociation = null;
		stepMode = "hideEditStep();";
		return "cancel_step";
	}

	
	public TransformationStep getTransformationStep() {
		if (currentStepAssociation == null) {
			TransformationStep tmpStep = new BasicTransformationStep("", "", "");
			return tmpStep;
		}
		return currentStepAssociation.getStep();
	}

	public void setTransformationStep(TransformationStepAssociation stepAssociation) {
		this.currentStepAssociation = stepAssociation;
	}

	
	@SuppressWarnings("unchecked")
	public List<SelectItem> getTransformationItems() {
		List<SelectItem> list = new LinkedList<SelectItem>();
		if (resources == null) {
			/* TODO We need all (enabled) storages. Not just a window */
			getTransformations();
		}
		list.add(new SelectItem("", "<Select Transformation>"));
		if (resources != null)
			for (TransformationBrief transformation : (List<TransformationBrief>) resources) {
				if (transformation.isEnabled()) {
					SelectItem selectItem = new SelectItem();
					selectItem.setLabel(transformation.getName());
					selectItem.setValue(transformation.getId());
					list.add(selectItem);
				}
			}
    	return list;
    }

	public String getStepMode() {
		return stepMode;
	}

	public void setStepMode(String stepMode) {
		this.stepMode = stepMode;
	}

}
