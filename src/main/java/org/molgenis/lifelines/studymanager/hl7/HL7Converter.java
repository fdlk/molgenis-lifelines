package org.molgenis.lifelines.studymanager.hl7;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.umcg.hl7.service.studydefinition.ActClass;
import nl.umcg.hl7.service.studydefinition.ActMood;
import nl.umcg.hl7.service.studydefinition.ActRelationshipType;
import nl.umcg.hl7.service.studydefinition.CD;
import nl.umcg.hl7.service.studydefinition.ED;
import nl.umcg.hl7.service.studydefinition.ObjectFactory;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVComponent2;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVEntry;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVQualityMeasureDocument;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVSection;
import nl.umcg.hl7.service.studydefinition.POQMMT000002UVEncounter;
import nl.umcg.hl7.service.studydefinition.POQMMT000002UVObservation;
import nl.umcg.hl7.service.studydefinition.POQMMT000002UVSourceOf;
import nl.umcg.hl7.service.studydefinition.ST;
import nl.umcg.hl7.service.studydefinition.StrucDocItem;
import nl.umcg.hl7.service.studydefinition.StrucDocList;
import nl.umcg.hl7.service.studydefinition.StrucDocText;

/**
 * Converts a {@link StudyDefinitionBean} to HL7
 * 
 * @author fkelpin
 *
 */
public class HL7Converter
{
	private static POQMMT000002UVEncounter createEncounter(String measurementCode, String measurementCodeSystem)
	{
		POQMMT000002UVEncounter encounter = new POQMMT000002UVEncounter();
		encounter.setClassCode(ActClass.ENC);
		encounter.setMoodCode(ActMood.CRT);
		CD encounterCode = new CD();
		encounterCode.setCode(measurementCode);
		encounterCode.setCodeSystem(measurementCodeSystem);
		encounter.setCode(encounterCode);
		return encounter;
	}

	private POQMMT000002UVObservation createObservation(String displayName, String observationCodeCode,
			String observationCodeCodesystem)
	{
		final POQMMT000002UVObservation observation;
		observation = new POQMMT000002UVObservation();
		observation.setClassCode(ActClass.OBS);
		observation.setMoodCode(ActMood.CRT);
		CD observationCode = new CD();
		observationCode.setDisplayName(displayName);
		observationCode.setCode(observationCodeCode);
		observationCode.setCodeSystem(observationCodeCodesystem);
		observation.setCode(observationCode);
		return observation;
	}

	public void updateQualityMeasureDocument(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument,
			StudyDefinitionBean info)
	{
		ST title = new ST();
		title.getContent().add(info.getName());
		qualityMeasureDocument.setTitle(title);

		ED text = new ED();
		text.getContent().add(info.getCreatedBy());
		qualityMeasureDocument.setText(text);

		POQMMT000001UVComponent2 component = new POQMMT000001UVComponent2();
		POQMMT000001UVSection section = new POQMMT000001UVSection();

		CD sectionCode = new CD();
		sectionCode.setCode("57025-9");
		sectionCode.setCodeSystem("2.16.840.1.113883.6.1");
		sectionCode.setDisplayName("Data Criteria section");
		section.setCode(sectionCode);

		ED sectionTitle = new ED();
		sectionTitle.getContent().add("Data criteria");
		section.setTitle(sectionTitle);

		StrucDocText sectionText = new StrucDocText();
		StrucDocList strucDocList = new StrucDocList();
		for (MeasurementBean observationInfo : info.getObservationInfo())
		{
			StrucDocItem strucDocItem = new StrucDocItem();
			strucDocItem.getContent().add(observationInfo.getTextCode());
			strucDocList.getItem().add(strucDocItem);
		}
		sectionText.getContent().add(new ObjectFactory().createStrucDocTextList(strucDocList));
		section.setText(sectionText);

		Map<String, POQMMT000001UVEntry> entries = new LinkedHashMap<String, POQMMT000001UVEntry>();
		for (MeasurementBean observationInfo : info.getObservationInfo())
		{
			String observationCodeCode = observationInfo.getCode();
			final POQMMT000001UVEntry entry;
			final POQMMT000002UVObservation observation;

			if (!entries.containsKey(observationCodeCode))
			{
				entry = new POQMMT000001UVEntry();
				entry.setTypeCode("DRIV");
				observation = createObservation(observationInfo.getDisplayName(), observationCodeCode,
						observationInfo.getCodeSystem());
				entry.setObservation(observation);
				section.getEntry().add(entry);
			}
			else
			{
				entry = entries.get(observationCodeCode);
				observation = entry.getObservation();
			}

			POQMMT000002UVEncounter encounter = createEncounter(observationInfo.getMeasurementCode(),
					observationInfo.getMeasurementCodeSystem());

			boolean exists = false;
			for (POQMMT000002UVSourceOf sourceOfItem : observation.getSourceOf())
			{
				if (sourceOfItem.getEncounter().getCode().getCode().equals(encounter.getCode().getCode()))
				{
					exists = true;
					break;
				}
			}

			// add sourceOf (e.g. baseline, follow-up) after determine if it already exists
			if (!exists)
			{
				POQMMT000002UVSourceOf sourceOf = new POQMMT000002UVSourceOf();
				sourceOf.setTypeCode(ActRelationshipType.DRIV);
				sourceOf.setEncounter(encounter);
				observation.getSourceOf().add(sourceOf);
			}
		}

		List<POQMMT000001UVComponent2> components = qualityMeasureDocument.getComponent();
		component.setSection(section);
		if (components.size() > 0)
		{
			components.set(0, component);
		}
		else
		{
			components.add(component);
		}
	}

}
