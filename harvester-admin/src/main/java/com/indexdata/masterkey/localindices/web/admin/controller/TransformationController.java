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
	private List<TransformationStep> steps = null; 
	private String stepMode = "hideEditStep();"; // which JS function should be called on load
    private TransformationStep currentStep;
   
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
        currentStep = current.getSteps().get(0);
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
        currentStep = null;
        steps = null;
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

	private void setupStep() {
		String idName = "stepID";
        String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(idName);
        if (param != null && !"null".equals(param)) {
	        try {
	        	Long id = new Long(param);
	        	currentStep = lookupStepByID(current.getSteps(), id);
	        } catch (Exception e) {
	        	logger.error("Unable to get Step from parameter '" + idName + "' " + param + ". Error: " + e.getMessage());
	        }
        }
		if (currentStep == null) {
			logger.error("Setting up new step.");
			BasicTransformationStep step = new BasicTransformationStep();
			step.setDescription("<Description>");
			step.setScript("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			step.setPosition(current.getSteps().size()+1);
			step.setName("<New step " + step.getPosition() + ">");
			currentStep = step;
		}
	}
	
	private TransformationStep lookupStepByID(List<TransformationStep> steps, Long id) {
		for (TransformationStep step: steps) {
			if (step.getId().equals(id)) 
				return step;
		}
		return null;
	}

	public String addStep() {
		setupStep();	
		stepMode = "showEditStep();";
		return "new_step";
	}

	public String editStep() {
		setupStep();
		stepMode = "showEditStep();";
		return "edit_step";
	}

	public String upStep() {
		setupStep();
		upDownStep(currentStep.getId(), -1);
		stepMode = "hideEditStep();";
		return "up_step";
	}

	private void upDownStep(Long id, int i) {
		
		for (TransformationStep step: steps) {
			
		}
	}

	public String downStep() {
		setupStep();
		upDownStep(currentStep.getId(), 1);
		stepMode = "hideEditStep();";
		return "down_step";
	}

	public String deleteStep() {
		setupStep();
		stepMode = "hideEditStep();";
		return "delete_step";
	}

	public TransformationStep getTransformationStep() {
		if (currentStep == null)
			setupStep();
		return currentStep;
	}

	public void setTransformationStep(TransformationStep currentStep) {
		this.currentStep = currentStep;
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
