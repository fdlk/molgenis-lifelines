package org.molgenis.lifelines.studymanager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.umcg.hl7.service.studydefinition.ActClass;
import nl.umcg.hl7.service.studydefinition.ActMood;
import nl.umcg.hl7.service.studydefinition.ArrayOfXElement;
import nl.umcg.hl7.service.studydefinition.CD;
import nl.umcg.hl7.service.studydefinition.CE;
import nl.umcg.hl7.service.studydefinition.COCTMT090107UVAssignedPerson;
import nl.umcg.hl7.service.studydefinition.COCTMT090107UVPerson;
import nl.umcg.hl7.service.studydefinition.COCTMT150007UVContactParty;
import nl.umcg.hl7.service.studydefinition.COCTMT150007UVOrganization;
import nl.umcg.hl7.service.studydefinition.CS;
import nl.umcg.hl7.service.studydefinition.ED;
import nl.umcg.hl7.service.studydefinition.EntityClassOrganization;
import nl.umcg.hl7.service.studydefinition.EntityClassPerson;
import nl.umcg.hl7.service.studydefinition.EntityDeterminerSpecific;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionService;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceCreateFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GetSubmittedResponse;
import nl.umcg.hl7.service.studydefinition.HL7Container;
import nl.umcg.hl7.service.studydefinition.II;
import nl.umcg.hl7.service.studydefinition.INT;
import nl.umcg.hl7.service.studydefinition.NullFlavor;
import nl.umcg.hl7.service.studydefinition.ON;
import nl.umcg.hl7.service.studydefinition.ObjectFactory;
import nl.umcg.hl7.service.studydefinition.PN;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVAuthor;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVComponent2;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVCustodian;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVEntry;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVQualityMeasureDocument;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVSection;
import nl.umcg.hl7.service.studydefinition.POQMMT000002UVObservation;
import nl.umcg.hl7.service.studydefinition.ParticipationType;
import nl.umcg.hl7.service.studydefinition.Revise;
import nl.umcg.hl7.service.studydefinition.RoleClassAssignedEntity;
import nl.umcg.hl7.service.studydefinition.RoleClassContact;
import nl.umcg.hl7.service.studydefinition.ST;
import nl.umcg.hl7.service.studydefinition.StrucDocItem;
import nl.umcg.hl7.service.studydefinition.StrucDocList;
import nl.umcg.hl7.service.studydefinition.StrucDocText;
import nl.umcg.hl7.service.studydefinition.TEL;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.catalog.CatalogItem;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.omx.utils.I18nTools;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.StudyDefinitionMeta;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.molgenis.studymanager.StudyManagerService;

public class GenericLayerStudyManagerService implements StudyManagerService
{
	private static final Logger logger = Logger.getLogger(GenericLayerStudyManagerService.class);

	private final GenericLayerStudyDefinitionService studyDefinitionService;
	private final CatalogManagerService catalogLoaderService;
	private final GenericLayerDataQueryService dataQueryService;

	public GenericLayerStudyManagerService(GenericLayerStudyDefinitionService studyDefinitionService,
			CatalogManagerService catalogLoaderService, GenericLayerDataQueryService dataQueryService)
	{
		if (studyDefinitionService == null) throw new IllegalArgumentException("Study definition service is null");
		if (catalogLoaderService == null) throw new IllegalArgumentException("Catalog manager service is null");
		if (dataQueryService == null) throw new IllegalArgumentException("Data query service is null");
		this.studyDefinitionService = studyDefinitionService;
		this.catalogLoaderService = catalogLoaderService;
		this.dataQueryService = dataQueryService;
	}

	/**
	 * Find the SUBMITTED study definition with the given id
	 * 
	 * @return
	 * @throws UnknownStudyDefinitionException
	 */
	@Override
	public StudyDefinition getStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(id);
		if (qualityMeasureDocument == null)
		{
			throw new UnknownStudyDefinitionException("unknown submitted study definition id [" + id + "]");
		}
		return new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument);
	}

	/**
	 * Find all SUBMITTED studydefinitions
	 * 
	 * @return List of StudyDefinitionInfo
	 */
	@Override
	public List<StudyDefinitionMeta> getStudyDefinitions()
	{
		GetSubmittedResponse submittedResponse;
		try
		{
			submittedResponse = studyDefinitionService.getSubmitted();
		}
		catch (GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}

		List<StudyDefinitionMeta> studyDefinitionsMeta = new ArrayList<StudyDefinitionMeta>();

		ArrayOfXElement elements = submittedResponse.getHL7Containers();
		if (elements != null)
		{
			List<HL7Container> hl7Containers = elements.getHL7Container();
			if (hl7Containers != null)
			{
				for (HL7Container hl7Container : hl7Containers)
				{
					POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container
							.getQualityMeasureDocument();
					String id = qualityMeasureDocument.getId().getExtension();
					ST title = qualityMeasureDocument.getTitle();
					String name = title != null ? title.getContent().toString() : null;

					// An eMeasure SHALL contain exactly 1 author that is the primary applicant.
					List<POQMMT000001UVAuthor> authors = qualityMeasureDocument.getAuthor();
					if (authors == null || authors.size() != 1)
					{
						throw new RuntimeException("expected exactly one author in study definition with id [" + id
								+ "]");
					}
					POQMMT000001UVAuthor author = authors.get(0);

					// telecom SHALL contain at least 1 “mailto:” email adres.
					String email = null;
					List<TEL> telecoms = author.getAssignedPerson().getTelecom();
					if (telecoms == null)
					{
						throw new RuntimeException(
								"expected at least one telecom for author in study definition with id [" + id + "]");
					}
					for (TEL telecom : telecoms)
					{
						String telecomStr = telecom.getValue();
						if (telecomStr.startsWith("mailto:"))
						{
							email = telecomStr.substring("mailto:".length());
							break;
						}
						else
						{
							// FIXME remove else clause when TCC response is valid
							email = telecomStr;
						}
					}
					if (email == null)
					{
						throw new RuntimeException(
								"expected at least one email for author in study definition with id [" + id + "]");
					}

					// TODO add date
					Date date = null;
					studyDefinitionsMeta.add(new StudyDefinitionMeta(id, name, email, date));
				}
			}
		}
		return studyDefinitionsMeta;
	}

	@Override
	public boolean canLoadStudyData()
	{
		return true;
	}

	/**
	 * Get a specific studydefinition and save it in the database
	 */
	@Override
	public void loadStudyData(String id) throws UnknownStudyDefinitionException
	{
		try
		{
			catalogLoaderService.loadCatalogOfStudyDefinition(id);
		}
		catch (UnknownCatalogException e)
		{
			throw new UnknownStudyDefinitionException(e);
		}

		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(id);
		if (qualityMeasureDocument == null)
		{
			throw new UnknownStudyDefinitionException("unknown submitted study definition id [" + id + "]");
		}
		dataQueryService.loadStudyDefinitionData(qualityMeasureDocument);
	}

	@Override
	public boolean isStudyDataLoaded(String id) throws UnknownStudyDefinitionException
	{
		return dataQueryService.isStudyDataLoaded(id);
	}

	@Override
	public StudyDefinition persistStudyDefinition(StudyDefinition studyDefinition)
	{
		// create empty study definition
		HL7Container hl7Container;
		try
		{
			hl7Container = studyDefinitionService.create(studyDefinition.getAuthorEmail());
		}
		catch (GenericLayerStudyDefinitionServiceCreateFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container.getQualityMeasureDocument();
		String id = qualityMeasureDocument.getId().getExtension();

		// update study definition
		studyDefinition.setId(id);
		try
		{
			updateStudyDefinition(studyDefinition);
		}
		catch (UnknownStudyDefinitionException e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}

		// FIXME do not set converted id at this location
		studyDefinition.setId(StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(id));

		return studyDefinition;
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		POQMMT000001UVQualityMeasureDocument eMeasure = createQualityMeasureDocument(studyDefinition);
		HL7Container hl7Container = new HL7Container();
		hl7Container.setQualityMeasureDocument(eMeasure);

		Revise revise = new Revise();
		revise.setHL7Container(hl7Container);
		try
		{
			studyDefinitionService.revise(revise);
		}
		catch (GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private POQMMT000001UVQualityMeasureDocument createQualityMeasureDocument(StudyDefinition studyDefinition)
	{
		POQMMT000001UVQualityMeasureDocument eMeasure = new POQMMT000001UVQualityMeasureDocument();
		II typeId = new II();
		typeId.setRoot("2.16.840.1.113883.1.3");
		typeId.setExtension("POQM_HD000001");
		eMeasure.setTypeId(typeId);

		II id = new II();
		id.setRoot("2.16.840.1.113883.2.4.3.8.1000.54.7");
		eMeasure.setId(id); // id placeholder

		CE code = new CE();
		code.setCode("57024-2");
		code.setCodeSystem("2.16.840.1.113883.6.1");
		code.setDisplayName("Health Quality Measure document");
		eMeasure.setCode(code);

		ST title = new ST();
		title.getContent().add(studyDefinition.getName());
		eMeasure.setTitle(title);

		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');

		ED text = new ED();
		text.getContent().add(textBuilder.toString());
		eMeasure.setText(text);

		CS statusCode = new CS();
		statusCode.setCode("active");
		eMeasure.setStatusCode(statusCode);

		II setId = new II();
		setId.setRoot("1.1.1");
		setId.setExtension("example");
		eMeasure.setSetId(setId);

		INT versionNumber = new INT();
		versionNumber.setValue(new BigInteger("1"));
		eMeasure.setVersionNumber(versionNumber);

		POQMMT000001UVAuthor author = new POQMMT000001UVAuthor();
		author.setTypeCode(ParticipationType.AUT);
		COCTMT090107UVAssignedPerson assignedPerson = new COCTMT090107UVAssignedPerson();
		assignedPerson.setClassCode(RoleClassAssignedEntity.ASSIGNED);
		COCTMT090107UVPerson assignedPersonAssignedPerson = new COCTMT090107UVPerson();
		assignedPersonAssignedPerson.setClassCode(EntityClassPerson.PSN);
		assignedPersonAssignedPerson.setDeterminerCode(EntityDeterminerSpecific.INSTANCE);

		PN name = new PN();
		name.getContent().add("Onderzoeker X");
		assignedPersonAssignedPerson.getName().add(name);
		assignedPerson.setAssignedPerson(new ObjectFactory()
				.createCOCTMT090107UVAssignedPersonAssignedPerson(assignedPersonAssignedPerson));

		COCTMT150007UVOrganization representedOrganization = new COCTMT150007UVOrganization();
		representedOrganization.setClassCode(EntityClassOrganization.ORG);
		representedOrganization.setDeterminerCode(EntityDeterminerSpecific.INSTANCE);

		II representedOrganizationId = new II();
		representedOrganizationId.setRoot("2.16.840.1.113883.19.5");
		representedOrganization.getId().add(representedOrganizationId);
		ON representedOrganizationName = new ON();
		representedOrganizationName.getContent().add("UMCG");
		representedOrganization.getName().add(representedOrganizationName);
		COCTMT150007UVContactParty contactParty = new COCTMT150007UVContactParty();
		contactParty.setClassCode(RoleClassContact.CON);
		contactParty.setNullFlavor(NullFlavor.UNK);
		representedOrganization.getContactParty().add(contactParty);

		assignedPerson.setRepresentedOrganization(new ObjectFactory()
				.createCOCTMT090107UVAssignedPersonRepresentedOrganization(representedOrganization));

		author.setAssignedPerson(assignedPerson);
		eMeasure.getAuthor().add(author);

		POQMMT000001UVCustodian custodian = new POQMMT000001UVCustodian();
		custodian.setTypeCode(ParticipationType.CST);
		COCTMT090107UVAssignedPerson custodianAssignedPerson = new COCTMT090107UVAssignedPerson();
		custodianAssignedPerson.setClassCode(RoleClassAssignedEntity.ASSIGNED);
		custodian.setAssignedPerson(custodianAssignedPerson);
		eMeasure.setCustodian(custodian);

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
		for (CatalogItem item : studyDefinition.getItems())
		{
			StrucDocItem strucDocItem = new StrucDocItem();
			strucDocItem.getContent().add(item.getName());
			strucDocList.getItem().add(strucDocItem);
		}
		sectionText.getContent().add(new ObjectFactory().createStrucDocTextList(strucDocList));
		section.setText(sectionText);

		for (CatalogItem item : studyDefinition.getItems())
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
				// TODO remove once catalogues are always loaded from LL GL
				observationCodeCode = item.getId();
				observationCodeCodesystem = "2.16.840.1.113883.2.4.3.8.1000.54.4";
			}
			CD observationCode = new CD();
			observationCode.setDisplayName(item.getName());
			observationCode.setCode(observationCodeCode);
			observationCode.setCodeSystem(observationCodeCodesystem);
			ED observationOriginalText = new ED();
			observationOriginalText.getContent().add(I18nTools.get(item.getDescription()));
			observationCode.setOriginalText(observationOriginalText);
			observation.setCode(observationCode);
			entry.setObservation(observation);
			section.getEntry().add(entry);
		}

		component.setSection(section);
		eMeasure.getComponent().add(component);

		return eMeasure;
	}

	private POQMMT000001UVQualityMeasureDocument getStudyDefinitionAsQualityMeasureDocument(String id)
			throws UnknownStudyDefinitionException
	{
		GetSubmittedResponse submittedResponse;
		try
		{
			submittedResponse = studyDefinitionService.getSubmitted();
		}
		catch (GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}

		ArrayOfXElement elements = submittedResponse.getHL7Containers();
		if (elements != null)
		{
			List<HL7Container> hl7Containers = elements.getHL7Container();
			if (hl7Containers != null)
			{
				for (HL7Container hl7Container : hl7Containers)
				{
					POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container
							.getQualityMeasureDocument();
					if (id.equals(qualityMeasureDocument.getId().getExtension()))
					{
						return qualityMeasureDocument;
					}
				}
			}
		}
		return null;
	}
}
