package com.indexdata.masterkey.localindices.web.admin.utils;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.log4j.Logger;

public class LifeCycleListener implements PhaseListener {

  private static Logger logger = Logger.getLogger(LifeCycleListener.class);
  private static final long serialVersionUID = 6683668955677049743L;

  public PhaseId getPhaseId() {
    return PhaseId.ANY_PHASE;
  }

  public void beforePhase(PhaseEvent event) {
    logger.debug("START PHASE " + event.getPhaseId() + " " + event.toString());
  }

  public void afterPhase(PhaseEvent event) {
    logger.debug("END PHASE " + event.getPhaseId() + " " + event.toString());
  }
}
