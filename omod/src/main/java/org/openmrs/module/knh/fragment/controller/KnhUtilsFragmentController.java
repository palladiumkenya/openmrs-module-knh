/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.knh.fragment.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.VisitType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.calculation.result.CalculationResult;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.kenyacore.CoreContext;
import org.openmrs.module.kenyacore.form.FormDescriptor;
import org.openmrs.module.kenyacore.form.FormManager;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.EmrConstants;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.calculation.EmrCalculationUtils;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.metadata.IPTMetadata;
import org.openmrs.module.kenyaemr.regimen.RegimenChange;
import org.openmrs.module.kenyaemr.regimen.RegimenChangeHistory;
import org.openmrs.module.kenyaemr.regimen.RegimenManager;
import org.openmrs.module.kenyaemr.util.EmrUiUtils;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.module.kenyaui.annotation.PublicAction;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.action.SuccessResult;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Fragment actions generally useful for KNH
 */
public class KnhUtilsFragmentController {
	
	protected static final Log log = LogFactory
	        .getLog(org.openmrs.module.kenyaemr.fragment.controller.EmrUtilsFragmentController.class);
	
	/**
	 * gets latest hiv green card encounter
	 * 
	 * @param patient tca to be updated
	 */
	public SimpleObject updateTCADate(@RequestParam("patientId") Patient patient,
									  @RequestParam("tcaDate") Date tcaDate) {
		
		// check last hiv Greencard encounter
		PatientWrapper patientWrapper = new PatientWrapper(patient);
		EncounterService encounterService = Context.getEncounterService();
		ObsService obsService = Context.getObsService();
		EncounterType et = encounterService.getEncounterTypeByUuid(HivMetadata._EncounterType.HIV_CONSULTATION);
		Encounter lastHivGreenCard = patientWrapper.lastEncounter(et);
		Concept RETURN_VISIT_DATE = Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE);
		Date returnVisitDate = null;
		if (lastHivGreenCard != null) {
			Set<Obs> allObs;
			allObs = lastHivGreenCard.getAllObs();
			for (Obs obs : allObs) {
				if (obs.getConcept().equals(RETURN_VISIT_DATE)) {
					obsService.voidObs(obs, "KenyaEMR Updating Patient TCA"); //Voiding the old TCA Date
					
					Obs o = new Obs();
					o.setConcept(Context.getConceptService().getConceptByUuid(Dictionary.RETURN_VISIT_DATE)); // Updating the new TCA Date
					o.setValueDatetime(tcaDate);
				}
			}
		}
		assignToVisit(lastHivGreenCard, Context.getVisitService().getVisitTypeByUuid(CommonMetadata._VisitType.OUTPATIENT));
		try {
			encounterService.saveEncounter(lastHivGreenCard);
			return SimpleObject.create("status", "Success", "message", "TCA for Patient updated successfully");
		}
		catch (Exception e) {
			return SimpleObject.create("status", "Error", "message", "There was an error updating TCA for Patient");
		}
	}
	
	/**
	 * Does the actual assignment of the encounter to a visit
	 * 
	 * @param encounter the encounter
	 * @param newVisitType the type of the new visit if one is created
	 */
	protected void assignToVisit(Encounter encounter, VisitType newVisitType) {
		// Do nothing if the encounter already belongs to a visit and can't be moved
		if (encounter.getVisit() != null && newVisitType == null) {
			return;
		}
		// Try using an existing visit
		if (!useExistingVisit(encounter)) {
			if (newVisitType != null) {
				useNewVisit(encounter, newVisitType, encounter.getForm());
			}
		}
	}
	
	/**
	 * Uses an existing a visit for the given encounter
	 * 
	 * @param encounter the encounter
	 * @return true if a suitable visit was found
	 */
	protected boolean useExistingVisit(Encounter encounter) {
		// If encounter has time, then we need an exact fit for an existing visit
		if (EmrUtils.dateHasTime(encounter.getEncounterDatetime())) {
			List<Visit> visits = Context.getVisitService().getVisits(null,
			    Collections.singletonList(encounter.getPatient()), null, null, null, encounter.getEncounterDatetime(), null,
			    null, null, true, false);
			
			for (Visit visit : visits) {
				// Skip visits which ended before the encounter date
				if (visit.getStopDatetime() != null && visit.getStopDatetime().before(encounter.getEncounterDatetime())) {
					continue;
				}
				
				if (checkLocations(visit, encounter)) {
					setVisitOfEncounter(visit, encounter);
					return true;
				}
			}
		}
		// If encounter does not have time, we can move it to fit any visit that day
		else {
			List<Visit> existingVisitsOnDay = Context.getService(KenyaEmrService.class).getVisitsByPatientAndDay(
			    encounter.getPatient(), encounter.getEncounterDatetime());
			if (existingVisitsOnDay.size() > 0) {
				Visit visit = existingVisitsOnDay.get(0);
				
				if (checkLocations(visit, encounter)) {
					setVisitOfEncounter(visit, encounter);
					
					// Adjust encounter start if its before visit start
					if (encounter.getEncounterDatetime().before(visit.getStartDatetime())) {
						encounter.setEncounterDatetime(visit.getStartDatetime());
					}
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Uses a new visit for the given encounter
	 * 
	 * @param encounter the encounter
	 * @param type the visit type
	 * @param sourceForm the source form
	 */
	protected static void useNewVisit(Encounter encounter, VisitType type, Form sourceForm) {
		Visit visit = new Visit();
		visit.setStartDatetime(OpenmrsUtil.firstSecondOfDay(encounter.getEncounterDatetime()));
		visit.setStopDatetime(OpenmrsUtil.getLastMomentOfDay(encounter.getEncounterDatetime()));
		visit.setLocation(encounter.getLocation());
		visit.setPatient(encounter.getPatient());
		visit.setVisitType(type);
		
		VisitAttribute sourceAttr = new VisitAttribute();
		sourceAttr.setAttributeType(MetadataUtils.existing(VisitAttributeType.class,
		    CommonMetadata._VisitAttributeType.SOURCE_FORM));
		sourceAttr.setOwner(visit);
		sourceAttr.setValue(sourceForm);
		visit.addAttribute(sourceAttr);
		
		Context.getVisitService().saveVisit(visit);
		
		setVisitOfEncounter(visit, encounter);
	}
	
	/**
	 * Sets the visit of an encounter, updating the both the old visit and the new visit. This is
	 * used rather than just encounter.setVisit(...) so that we don't have to reload the visit
	 * objects to update their set of encounters
	 * 
	 * @param visit the visit
	 * @param encounter the encounter
	 */
	protected static void setVisitOfEncounter(Visit visit, Encounter encounter) {
		// Remove from old visit
		if (encounter.getVisit() != null) {
			encounter.getVisit().getEncounters().remove(encounter);
		}
		
		// Set to new visit
		encounter.setVisit(visit);
		
		if (visit != null) {
			visit.addEncounter(encounter);
		}
	}
	
	/**
	 * Convenience method to check whether the location of a visit and an encounter are compatible
	 * 
	 * @param visit the visit
	 * @param encounter the encounter
	 * @return true if locations won't conflict
	 */
	protected static boolean checkLocations(Visit visit, Encounter encounter) {
		return visit.getLocation() == null || Location.isInHierarchy(encounter.getLocation(), visit.getLocation());
	}
	
}
