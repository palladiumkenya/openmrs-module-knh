/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.knh.fragment.controller.appointmentRescheduling;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.kenyaemr.wrapper.PatientWrapper;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * serves appointmentsHistory fragment
 */
public class AppointmentsHistoryFragmentController {
	
	ConceptService conceptService = Context.getConceptService();
	
	SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");
	
	public void controller(FragmentModel model, @FragmentParam("patient") Patient patient) {
		
		PatientWrapper patientWrapper = new PatientWrapper(patient);
		EncounterService encounterService = Context.getEncounterService();
		EncounterType et = encounterService.getEncounterTypeByUuid(HivMetadata._EncounterType.HIV_CONSULTATION);
		Encounter lastHivGreenCard = patientWrapper.lastEncounter(et);
		
		// get hiv greencard list of observations
		List<SimpleObject> encDetails = new ArrayList<SimpleObject>();
		if (lastHivGreenCard != null) {
			SimpleObject o = getEncDetails(lastHivGreenCard.getObs(), lastHivGreenCard);
			encDetails.add(o);
			model.put("encounters", encDetails);
		}
		
	}
	
	/**
	 * Extract TCA information from encounters and order them based on Date
	 * 
	 * @param
	 * @return
	 */
	SimpleObject getEncDetails(Set<Obs> obsList, Encounter e) {
		
		Integer tcaDateConcept = 5096;
		String tcaDateString = null;
		Date tcaDate = null;
		for (Obs obs : obsList) {
			
			if (obs.getConcept().getConceptId().equals(tcaDateConcept)) {
				tcaDate = obs.getValueDate();
				tcaDateString = tcaDate != null ? DATE_FORMAT.format(tcaDate) : "";
			}
		}
		return SimpleObject.create("encDate", DATE_FORMAT.format(e.getEncounterDatetime()), "tcaDate",
		    tcaDateString != null ? tcaDateString : "", "encounter", Arrays.asList(e), "form", e.getForm());
	}
}
