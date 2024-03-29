/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.knh.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Miscellaneous utility methods
 */
public class EmrUtils {
	
	/**
	 * Checks whether a date has any time value
	 * 
	 * @param date the date
	 * @return true if the date has time
	 * @should return true only if date has time
	 */
	public static boolean dateHasTime(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal.get(Calendar.HOUR) != 0 || cal.get(Calendar.MINUTE) != 0 || cal.get(Calendar.SECOND) != 0
		        || cal.get(Calendar.MILLISECOND) != 0;
	}
	
	/**
	 * Checks if a given date is today
	 * 
	 * @param date the date
	 * @return true if date is today
	 */
	public static boolean isToday(Date date) {
		return DateUtils.isSameDay(date, new Date());
	}
	
	/**
	 * Parses a CSV list of strings, returning all trimmed non-empty values
	 * 
	 * @param csv the CSV string
	 * @return the concepts
	 */
	public static List<String> parseCsv(String csv) {
		List<String> values = new ArrayList<String>();
		
		for (String token : csv.split(",")) {
			token = token.trim();
			
			if (!StringUtils.isEmpty(token)) {
				values.add(token);
			}
		}
		return values;
	}
	
	/**
	 * Unlike in OpenMRS core, a user can only be one provider in KenyaEMR
	 * 
	 * @param user the user
	 * @return the provider or null
	 */
	public static Provider getProvider(User user) {
		Person person = user.getPerson();
		Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(person);
		return providers.size() > 0 ? providers.iterator().next() : null;
	}
	
	/**
	 * Finds the last encounter during the program enrollment with the given encounter type
	 * 
	 * @param type the encounter type
	 * @return the encounter
	 */
	public static Encounter lastEncounter(Patient patient, EncounterType type) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null,
		    Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}
	
	public static Encounter lastEncounter(Patient patient, EncounterType type, Form form) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null,
		    Collections.singleton(form), Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}
	
	public static List<Encounter> AllEncounters(Patient patient, EncounterType type, Form form) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null,
		    Collections.singleton(form), Collections.singleton(type), null, null, null, false);
		return encounters;
	}
	
	/**
	 * Finds the first encounter during the program enrollment with the given encounter type
	 * 
	 * @param type the encounter type
	 * @return the encounter
	 */
	public static Encounter firstEncounter(Patient patient, EncounterType type) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null,
		    Collections.singleton(type), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(0) : null;
	}
	
	/**
	 * Finds the last encounter of a given type entered via a given form.
	 * 
	 * @param encounterType the type of encounter
	 * @param form the form through which the encounter was entered.
	 */
	public static Encounter encounterByForm(Patient patient, EncounterType encounterType, Form form) {
		List<Form> forms = null;
		if (form != null) {
			forms = new ArrayList<Form>();
			forms.add(form);
		}
		EncounterService encounterService = Context.getEncounterService();
		List<Encounter> encounters = encounterService.getEncounters(patient, null, null, null, forms,
		    Collections.singleton(encounterType), null, null, null, false);
		return encounters.size() > 0 ? encounters.get(encounters.size() - 1) : null;
	}
	
	public static ObjectNode getDatasetMappingForReport(String reportName, String mappingString) throws IOException {
		
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode conf = (ArrayNode) mapper.readTree(mappingString);
		
		for (Iterator<JsonNode> it = conf.iterator(); it.hasNext();) {
			ObjectNode node = (ObjectNode) it.next();
			if (node.get("reportName").asText().equals(reportName)) {
				return node;
			}
		}
		
		return null;
	}
	
}
