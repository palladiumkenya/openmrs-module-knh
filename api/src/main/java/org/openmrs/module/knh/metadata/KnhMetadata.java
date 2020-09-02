/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.knh.metadata;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.*;

/**
 * KP metadata bundle
 */
@Component
public class KnhMetadata extends AbstractMetadataBundle {
	
	public static String inpatient_concept = "5485AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
	
	public static final class _EncounterType {
		
		public static final String KNH_INPATIENT = "46f29210-ed18-11ea-adc1-0242ac120002";
		
	}
	
	public static final class _Form {
		
		public static final String KNH_INPATIENT_FORM = "34a5224e-ed4a-11ea-adc1-0242ac120002";
		
	}
	
	public static final class _PatientIdentifierType {
		
		public static final String KNH_PATIENT_NUMBER = "46f28ea0-ed18-11ea-adc1-0242ac120002";
		
	}
	
	public static final class _PersonAttributeType {
		
	}
	
	public static final class _RelationshipType {
		
	}
	
	public static final class _Program {
		
		public static final String KNH_INPATIENT = "46f29120-ed18-11ea-adc1-0242ac120002";
	}
	
	/**
	 * @see org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		// Installing encounter types
		
		install(encounterType("KNH Inpatient", "Handles KNH inpatient care", _EncounterType.KNH_INPATIENT));
		
		// Installing forms
		install(form("KNH Inpatient Care Form", "Inpatient care form", _EncounterType.KNH_INPATIENT, "1",
		    _Form.KNH_INPATIENT_FORM));
		
		// Installing identifiers
		install(patientIdentifierType("KNH Number", "Unique Number assigned by KNH", null, null, null,
		    PatientIdentifierType.LocationBehavior.NOT_USED, false, _PatientIdentifierType.KNH_PATIENT_NUMBER));
		
		// Installing program
		install(program("Inpatient", "KNH inpatient program", inpatient_concept, _Program.KNH_INPATIENT));
		
	}
}
