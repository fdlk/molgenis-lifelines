package org.molgenis.lifelines.studymanager;

import java.util.ArrayList;
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

	static POQMMT000002UVEncounter createEncounter(String measurementCode, String measurementCodeSystem)
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

	/**
	 * All the information needed to create a POQMMT000001UVQualityMeasureDocument.
	 * 
	 * @author fkelpin
	 *
	 */
	public static class QualityMeasureDocumentInfo
	{
		private String createdBy;
		private String name;

		private List<ObservationInfo> observationInfo = new ArrayList<ObservationInfo>();

		public String getCreatedBy()
		{
			return createdBy;
		}

		public void setCreatedBy(String createdBy)
		{
			this.createdBy = createdBy;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public List<ObservationInfo> getObservationInfo()
		{
			return observationInfo;
		}

		public void addObservationInfo(ObservationInfo info)
		{
			observationInfo.add(info);
		}
	}

	public static class ObservationInfo
	{
		private String code;
		private String displayName;
		private String measurementCode;
		private String measurementCodeSystem;
		private String textCode;
		private String codeSystem;

		public String getCode()
		{
			return code;
		}

		public void setCode(String code)
		{
			this.code = code;
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public void setDisplayName(String displayName)
		{
			this.displayName = displayName;
		}

		public String getMeasurementCode()
		{
			return measurementCode;
		}

		public void setMeasurementCode(String measurementCode)
		{
			this.measurementCode = measurementCode;
		}

		public String getMeasurementCodeSystem()
		{
			return measurementCodeSystem;
		}

		public void setMeasurementCodeSystem(String measurementCodeSystem)
		{
			this.measurementCodeSystem = measurementCodeSystem;
		}

		public String getTextCode()
		{
			return textCode;
		}

		public void setTextCode(String textCode)
		{
			this.textCode = textCode;
		}

		public String getCodeSystem()
		{
			return codeSystem;
		}

		public void setCodeSystem(String codeSystem)
		{
			this.codeSystem = codeSystem;
		}
	}

	public QualityMeasureDocumentInfo createInfo(StudyDataRequest studyDataRequest)
	{
		QualityMeasureDocumentInfo info = new QualityMeasureDocumentInfo();
		MolgenisUser molgenisUser = studyDataRequest.getMolgenisUser();
		StringBuilder textBuilder = new StringBuilder("Created by ").append(molgenisUser.getUsername()).append(" (")
				.append(molgenisUser.getEmail()).append(')');
		info.createdBy = textBuilder.toString();
		info.name = studyDataRequest.getName();

		for (Protocol protocol : studyDataRequest.getProtocols())
		{
			ObservationInfo observationInfo = new ObservationInfo();
			observationInfo.setTextCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			observationInfo.setCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			observationInfo.setCodeSystem("2.16.840.1.113883.2.4.3.8.1000.54.8");
			observationInfo.setDisplayName(protocol.getName());
			info.addObservationInfo(observationInfo);
			List<CatalogFolder> itemPath = Lists.newArrayList(new OmxCatalogFolder(protocol).getPath());
			if (itemPath.size() <= 2)
			{
				throw new RuntimeException("Missing measurement for catalog item with id [" + protocol.getIdentifier()
						+ "]");
			}
			String measurementItemId = itemPath.get(2).getExternalId();
			observationInfo.setMeasurementCode(MeasurementIdConverter.getMeasurementCode(measurementItemId));
			observationInfo
					.setMeasurementCodeSystem(MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId));

		}
		return info;
	}

	public void updateQualityMeasureDocument(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument,
			QualityMeasureDocumentInfo info)
	{
		ST title = new ST();
		title.getContent().add(info.name);
		qualityMeasureDocument.setTitle(title);

		ED text = new ED();
		text.getContent().add(info.createdBy);
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
		for (ObservationInfo observationInfo : info.getObservationInfo())
		{
			StrucDocItem strucDocItem = new StrucDocItem();
			strucDocItem.getContent().add(observationInfo.getTextCode());
			strucDocList.getItem().add(strucDocItem);
		}
		sectionText.getContent().add(new ObjectFactory().createStrucDocTextList(strucDocList));
		section.setText(sectionText);

		Map<String, POQMMT000001UVEntry> entries = new LinkedHashMap<String, POQMMT000001UVEntry>();
		for (ObservationInfo observationInfo : info.getObservationInfo())
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

	public QualityMeasureDocumentInfo createInfo(StudyDefinition studyDefinition)
	{
		QualityMeasureDocumentInfo result = new QualityMeasureDocumentInfo();

		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');
		result.setCreatedBy(textBuilder.toString());
		result.setName(studyDefinition.getName());

		for (CatalogFolder item : studyDefinition.getItems())
		{
			ObservationInfo observationInfo = new ObservationInfo();
			observationInfo.setTextCode(item.getName());
			result.addObservationInfo(observationInfo);

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
			observationInfo.setCode(observationCodeCode);
			observationInfo.setCodeSystem(observationCodeCodesystem);
			observationInfo.setDisplayName(item.getName());

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
			observationInfo.setMeasurementCode(MeasurementIdConverter.getMeasurementCode(measurementItemId));
			observationInfo
					.setMeasurementCodeSystem(MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId));
		}

		return result;
	}

	POQMMT000002UVObservation createObservation(String displayName, String observationCodeCode,
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

}
