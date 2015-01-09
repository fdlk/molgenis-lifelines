package org.molgenis.lifelines.studymanager;

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

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.molgenis.catalog.CatalogFolder;
import org.molgenis.data.DataService;
import org.molgenis.lifelines.utils.MeasurementIdConverter;
import org.molgenis.lifelines.utils.ObservationIdConverter;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.catalogmanager.OmxCatalogFolder;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.study.StudyDefinition;

/**
 * Converts to HL7
 * 
 * @author fkelpin
 *
 */
public class HL7Converter
{
	private final DataService dataService;

	public HL7Converter(DataService dataService)
	{
		this.dataService = dataService;
	}
	
	static POQMMT000002UVEncounter createEncounter(final Protocol protocol){
        POQMMT000002UVEncounter encounter = new POQMMT000002UVEncounter();
        encounter.setClassCode(ActClass.ENC);
        encounter.setMoodCode(ActMood.CRT);

        List<CatalogFolder> itemPath = Lists.newArrayList(new OmxCatalogFolder(protocol).getPath());
        if (itemPath.size() <= 2)
        {
            throw new RuntimeException("Missing measurement for catalog item with id [" + protocol.getIdentifier()
                    + "]");
        }
        String measurementItemId = itemPath.get(2).getExternalId();
        String measurementCode = MeasurementIdConverter.getMeasurementCode(measurementItemId);
        String measurementCodeSystem = MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId);

        CD encounterCode = new CD();
        encounterCode.setCode(measurementCode);
        encounterCode.setCodeSystem(measurementCodeSystem);
        encounter.setCode(encounterCode);
        return encounter;
    }
	
	void updateQualityMeasureDocumentFixed(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument, StudyDataRequest studyDataRequest)
	{
		MolgenisUser molgenisUser = studyDataRequest.getMolgenisUser();
	
		ST title = new ST();
		title.getContent().add(studyDataRequest.getName());
		qualityMeasureDocument.setTitle(title);
	
		StringBuilder textBuilder = new StringBuilder("Created by ").append(molgenisUser.getUsername()).append(" (")
				.append(molgenisUser.getEmail()).append(')');
	
		ED text = new ED();
		text.getContent().add(textBuilder.toString());
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
		for (Protocol protocol : studyDataRequest.getProtocols())
		{
			StrucDocItem strucDocItem = new StrucDocItem();
			strucDocItem.getContent().add(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			strucDocList.getItem().add(strucDocItem);
		}
		sectionText.getContent().add(new ObjectFactory().createStrucDocTextList(strucDocList));
		section.setText(sectionText);
	
		Map<String, POQMMT000001UVEntry> entries = new LinkedHashMap<String, POQMMT000001UVEntry>(); 
		for (Protocol protocol : studyDataRequest.getProtocols())
		{
			String observationCodeCode = ObservationIdConverter.getObservationCode(protocol.getIdentifier());
			final POQMMT000001UVEntry entry;
			final POQMMT000002UVObservation observation;
			
			if(!entries.containsKey(observationCodeCode)){
				entry = new POQMMT000001UVEntry();
				entry.setTypeCode("DRIV");
				observation = createObservation(protocol.getName(), observationCodeCode);
				entry.setObservation(observation);
				section.getEntry().add(entry);	
			}else{
				entry = entries.get(observationCodeCode);
				observation = entry.getObservation();
			}
			
			POQMMT000002UVEncounter encounter = createEncounter(protocol);
			
			boolean exists = false;
			for (POQMMT000002UVSourceOf sourceOfItem :observation.getSourceOf()) {
				if(sourceOfItem.getEncounter().getCode().getCode().equals(encounter.getCode().getCode())){
					exists = true;
					break;
				} 
			}
			
			// add sourceOf (e.g. baseline, follow-up) after determine if it already exists
			if(!exists){
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

	/**
	 * Writes a {@link StudyDefinition} to a preexisting {@link POQMMT000001UVQualityMeasureDocument}.
	 */
	public void updateQualityMeasureDocument(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument, StudyDefinition studyDefinition)
	{
		ST title = new ST();
		title.getContent().add(studyDefinition.getName());
		qualityMeasureDocument.setTitle(title);
	
		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');
	
		ED text = new ED();
		text.getContent().add(textBuilder.toString());
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
		for (CatalogFolder item : studyDefinition.getItems())
		{
			StrucDocItem strucDocItem = new StrucDocItem();
			strucDocItem.getContent().add(item.getName());
			strucDocList.getItem().add(strucDocItem);
		}
		sectionText.getContent().add(new ObjectFactory().createStrucDocTextList(strucDocList));
		section.setText(sectionText);
	
		for (CatalogFolder item : studyDefinition.getItems())
		{
			POQMMT000001UVEntry entry = new POQMMT000001UVEntry();
			entry.setTypeCode("DRIV");
			POQMMT000002UVObservation observation = new POQMMT000002UVObservation();
			observation.setClassCode(ActClass.OBS);
			observation.setMoodCode(ActMood.CRT);
	
			String observationCodeCode = item.getCode();
			String observationCodeCodesystem = item.getCodeSystem();
			if (observationCodeCode == null || observationCodeCodesystem == null)
			{
				ObservableFeature of = dataService.findOne(ObservableFeature.ENTITY_NAME,
						Integer.valueOf(item.getId()), ObservableFeature.class);
				if (of == null)
				{
					throw new RuntimeException("Unknown Observablefeature with id [" + item.getId() + "]");
				}
	
				observationCodeCode = of.getIdentifier();
				observationCodeCodesystem = "2.16.840.1.113883.2.4.3.8.1000.54.8";
			}
			CD observationCode = new CD();
			observationCode.setDisplayName(item.getName());
			observationCode.setCode(observationCodeCode);
			observationCode.setCodeSystem(observationCodeCodesystem);
			observation.setCode(observationCode);
	
			// set measurement (e.g. baseline, follow-up) for item
			POQMMT000002UVSourceOf sourceOf = new POQMMT000002UVSourceOf();
			sourceOf.setTypeCode(ActRelationshipType.DRIV);
	
			POQMMT000002UVEncounter encounter = new POQMMT000002UVEncounter();
			encounter.setClassCode(ActClass.ENC);
			encounter.setMoodCode(ActMood.CRT);
	
			List<CatalogFolder> itemPath = Lists.newArrayList(item.getPath());
			if (itemPath.size() <= 2)
			{
				throw new RuntimeException("Missing measurement for catalog item with id [" + item.getId() + "]");
			}
			String measurementItemId = itemPath.get(2).getId();
	
			int idx = measurementItemId.lastIndexOf('_');
			if (idx == -1 || idx == measurementItemId.length() - 1)
			{
				throw new RuntimeException("Invalid Measurement id [" + measurementItemId + "]");
			}
			String measurementCode = MeasurementIdConverter.getMeasurementCode(measurementItemId);
			String measurementCodeSystem = MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId);
	
			CD encounterCode = new CD();
			encounterCode.setCode(measurementCode);
			encounterCode.setCodeSystem(measurementCodeSystem);
			encounter.setCode(encounterCode);
	
			sourceOf.setEncounter(encounter);
			observation.getSourceOf().add(sourceOf);
	
			entry.setObservation(observation);
			section.getEntry().add(entry);
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

	POQMMT000002UVObservation createObservation(String displayName, String observationCodeCode)
	{
		final POQMMT000002UVObservation observation;
		observation = new POQMMT000002UVObservation();
		observation.setClassCode(ActClass.OBS);
		observation.setMoodCode(ActMood.CRT);
		String observationCodeCodesystem = "2.16.840.1.113883.2.4.3.8.1000.54.8";
		CD observationCode = new CD();
		observationCode.setDisplayName(displayName);
		observationCode.setCode(observationCodeCode);
		observationCode.setCodeSystem(observationCodeCodesystem);
		observation.setCode(observationCode);
		return observation;
	}
	
}
