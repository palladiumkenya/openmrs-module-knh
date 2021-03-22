/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.knh.reporting.builder;

import org.openmrs.EncounterType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.kenyacore.report.HybridReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractHybridReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyaemr.calculation.library.NumberOfDaysLateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.TelephoneNumberCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.LastReturnVisitDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.rdqa.PatientProgramEnrollmentCalculation;
import org.openmrs.module.kenyaemr.metadata.CommonMetadata;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.calculation.converter.EncounterDatetimeConverter;
import org.openmrs.module.kenyaemr.reporting.calculation.converter.PatientProgramEnrollmentConverter;
import org.openmrs.module.kenyaemr.reporting.data.converter.CalculationResultConverter;
import org.openmrs.module.kenyaemr.reporting.data.converter.CalculationResultDateYYMMDDConverter;
import org.openmrs.module.knh.reporting.cohort.definition.MissedAppointmentWithTraigeCohortDefinition;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.EncountersForPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ConvertedPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonIdDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Builds({ "kenyaemr.etl.common.report.missedAppointmentWithTriage" })
public class MissedAppointmentWithTriageRegisterReportBuilder extends AbstractHybridReportBuilder {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy";
	
	@Override
	protected Mapped<CohortDefinition> buildCohort(HybridReportDescriptor descriptor, PatientDataSetDefinition dsd) {
		return allPatientsCohort();
	}
	
	protected Mapped<CohortDefinition> allPatientsCohort() {
		CohortDefinition cd = new MissedAppointmentWithTraigeCohortDefinition();
		cd.setName("Triaged Missed Appoitments");
		return ReportUtils.map(cd, "");
	}
	
	@Override
	protected List<Mapped<DataSetDefinition>> buildDataSets(ReportDescriptor descriptor, ReportDefinition report) {
		
		PatientDataSetDefinition allPatients = triagedMissedAppoitmentDataSetDefinition();
		allPatients.addRowFilter(allPatientsCohort());
		//allPatients.addRowFilter(buildCohort(descriptor));
		DataSetDefinition allPatientsDSD = allPatients;
		
		return Arrays.asList(ReportUtils.map(allPatientsDSD, ""));
	}
	
	protected PatientDataSetDefinition triagedMissedAppoitmentDataSetDefinition() {
		PatientDataSetDefinition dsd = new PatientDataSetDefinition("TriagedMissedAppoitment");
		PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class,
	    HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(
		        upn.getName(), upn), identifierFormatter);
		
		DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName}");
		DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);
		dsd.addColumn("id", new PersonIdDataDefinition(), "");
		
		dsd.addColumn("Unique Patient No", identifierDef, "");
		dsd.addColumn("Name", nameDef, "");
		dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
		dsd.addColumn("Sex", new GenderDataDefinition(), "");
		
		// new columns
		EncountersForPatientDataDefinition definition = new EncountersForPatientDataDefinition();
		EncounterType hivConsultation = MetadataUtils.existing(EncounterType.class,
		    HivMetadata._EncounterType.HIV_CONSULTATION);
		EncounterType hivEnrollment = MetadataUtils.existing(EncounterType.class, HivMetadata._EncounterType.HIV_ENROLLMENT);
		EncounterType consultation = MetadataUtils.existing(EncounterType.class, CommonMetadata._EncounterType.CONSULTATION);
		
		List<EncounterType> encounterTypes = Arrays.asList(hivConsultation, consultation, hivEnrollment);
		
		definition.setWhich(TimeQualifier.LAST);
		definition.setTypes(encounterTypes);
		dsd.addColumn("Last Visit Date", definition, "", new EncounterDatetimeConverter());
		dsd.addColumn("Last Visit Date", definition, "", new EncounterDatetimeConverter());
		dsd.addColumn("Last Appointment date", new CalculationDataDefinition("Appointment date",
		        new LastReturnVisitDateCalculation()), "",
		    new DataConverter[] { new CalculationResultDateYYMMDDConverter() });
		dsd.addColumn("Number of days late", new CalculationDataDefinition("Number of days late",
		        new NumberOfDaysLateCalculation()), "", new DataConverter[] { new CalculationResultConverter() });
		dsd.addColumn("Program", new CalculationDataDefinition("Program", new PatientProgramEnrollmentCalculation()), "",
		    new PatientProgramEnrollmentConverter());
		dsd.addColumn("Phone number", new CalculationDataDefinition("Phone number", new TelephoneNumberCalculation()), "",
		    new DataConverter[] { new CalculationResultConverter() });
		return dsd;
	}
}
