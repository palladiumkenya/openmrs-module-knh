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
	public SimpleObject updateTCADate(@RequestParam("patientId") Patient patient, @RequestParam("tcaDate") Date tcaDate) {
		
		// check last hiv Greencard encounter
		PatientWrapper patientWrapper = new PatientWrapper(patient);
		EncounterService encounterService = Context.getEncounterService();
		ObsService obsService = Context.getObsService();
		EncounterType et = encounterService.getEncounterTypeByUuid(HivMetadata._EncounterType.HIV_CONSULTATION);
		Encounter lastHivGreenCard = patientWrapper.lastEncounter(et);
		Concept RETURN_VISIT_DATE = Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE);
		
		if (lastHivGreenCard != null) {
			Set<Obs> allObs;
			allObs = lastHivGreenCard.getAllObs();
			for (Obs obs : allObs) {
				if (obs.getConcept().equals(RETURN_VISIT_DATE)) {
					obsService.voidObs(obs, "Voiding TCA for a new one"); //Voiding the old TCA Date
					Obs o = new Obs();
					o.setConcept(Context.getConceptService().getConceptByUuid(Dictionary.RETURN_VISIT_DATE)); // Updating the new TCA Date
					o.setValueDatetime(tcaDate);
					lastHivGreenCard.addObs(o);
				}
			}
		}
		
		try {
			encounterService.saveEncounter(lastHivGreenCard);
			return SimpleObject.create("status", "Success", "message", "TCA for Patient updated successfully");
		}
		catch (Exception e) {
			return SimpleObject.create("status", "Error", "message", "There was an error updating TCA for Patient");
		}
	}
}
