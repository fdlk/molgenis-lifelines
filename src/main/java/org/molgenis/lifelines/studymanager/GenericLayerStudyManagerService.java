package org.molgenis.lifelines.studymanager;

import java.util.ArrayList;
import java.util.List;

import nl.umcg.hl7.service.studydefinition.ActClass;
import nl.umcg.hl7.service.studydefinition.ActMood;
import nl.umcg.hl7.service.studydefinition.ArrayOfXElement;
import nl.umcg.hl7.service.studydefinition.CD;
import nl.umcg.hl7.service.studydefinition.ED;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionService;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceCreateFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetApprovedFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetByEmailFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetDraftFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceSubmitFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GetApprovedResponse;
import nl.umcg.hl7.service.studydefinition.GetDraftResponse;
import nl.umcg.hl7.service.studydefinition.GetSubmittedResponse;
import nl.umcg.hl7.service.studydefinition.HL7Container;
import nl.umcg.hl7.service.studydefinition.ObjectFactory;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVComponent2;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVEntry;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVQualityMeasureDocument;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVSection;
import nl.umcg.hl7.service.studydefinition.POQMMT000002UVObservation;
import nl.umcg.hl7.service.studydefinition.Revise;
import nl.umcg.hl7.service.studydefinition.ST;
import nl.umcg.hl7.service.studydefinition.StrucDocItem;
import nl.umcg.hl7.service.studydefinition.StrucDocList;
import nl.umcg.hl7.service.studydefinition.StrucDocText;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.molgenis.catalog.CatalogItem;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.utils.I18nTools;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.StudyDefinition.Status;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.molgenis.studymanager.StudyManagerService;

public class GenericLayerStudyManagerService implements StudyManagerService
{
	private static final Logger logger = Logger.getLogger(GenericLayerStudyManagerService.class);

	private final GenericLayerStudyDefinitionService studyDefinitionService;
	private final CatalogManagerService catalogLoaderService;
	private final GenericLayerDataQueryService dataQueryService;
	private final MolgenisUserService userService;

	public GenericLayerStudyManagerService(GenericLayerStudyDefinitionService studyDefinitionService,
			CatalogManagerService catalogLoaderService, GenericLayerDataQueryService dataQueryService,
			MolgenisUserService userService)
	{
		if (studyDefinitionService == null) throw new IllegalArgumentException("Study definition service is null");
		if (catalogLoaderService == null) throw new IllegalArgumentException("Catalog manager service is null");
		if (dataQueryService == null) throw new IllegalArgumentException("Data query service is null");
		if (userService == null) throw new IllegalArgumentException("User service is null");
		this.studyDefinitionService = studyDefinitionService;
		this.catalogLoaderService = catalogLoaderService;
		this.dataQueryService = dataQueryService;
		this.userService = userService;
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
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(id,
				Status.SUBMITTED);
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
	public List<StudyDefinition> getStudyDefinitions()
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

		List<StudyDefinition> studyDefinitions = new ArrayList<StudyDefinition>();
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
					studyDefinitions.add(new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument));
				}
			}
		}
		return studyDefinitions;
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(String username, StudyDefinition.Status status)
	{
		MolgenisUser user = userService.getUser(username);
		if (user == null) throw new RuntimeException("Unknown user [" + username + "]");

		List<StudyDefinition> studyDefinitions = new ArrayList<StudyDefinition>();
		ArrayOfXElement elements;
		try
		{
			elements = studyDefinitionService.getByEmail(user.getEmail());
		}
		catch (GenericLayerStudyDefinitionServiceGetByEmailFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		if (elements != null)
		{
			List<HL7Container> hl7Containers = elements.getHL7Container();
			if (hl7Containers != null)
			{
				for (HL7Container hl7Container : hl7Containers)
				{
					POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container
							.getQualityMeasureDocument();
					studyDefinitions.add(new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument));
				}
			}
		}
		return studyDefinitions;
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

		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(id,
				Status.APPROVED);
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
	public StudyDefinition createStudyDefinition(String username, String catalogId)
	{
		MolgenisUser user = userService.getUser(username);
		if (user == null) throw new RuntimeException("User with username '" + username + "' does not exist");

		// create empty study definition
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument;
		try
		{
			HL7Container hl7Container = studyDefinitionService.create(user.getEmail());
			if (hl7Container == null) throw new RuntimeException("HL7Container is null");
			qualityMeasureDocument = hl7Container.getQualityMeasureDocument();
		}
		catch (GenericLayerStudyDefinitionServiceCreateFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		return new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument);
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(
				studyDefinition.getId(), studyDefinition.getStatus());

		// update study definition
		updateQualityMeasureDocument(qualityMeasureDocument, studyDefinition);

		// submit study definition
		try
		{
			HL7Container hl7Container = new HL7Container();
			hl7Container.setQualityMeasureDocument(qualityMeasureDocument);
			Revise revise = new Revise();
			revise.setHL7Container(hl7Container);
			studyDefinitionService.revise(revise);
		}
		catch (GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public void submitStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException,
			UnknownCatalogException
	{
		try
		{
			studyDefinitionService.submit(id);
		}
		catch (GenericLayerStudyDefinitionServiceSubmitFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void updateQualityMeasureDocument(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument,
			StudyDefinition studyDefinition)
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
		qualityMeasureDocument.getComponent().add(component);
	}

	private POQMMT000001UVQualityMeasureDocument getStudyDefinitionAsQualityMeasureDocument(String id, Status status)
			throws UnknownStudyDefinitionException
	{
		ArrayOfXElement elements;
		switch (status)
		{
			case APPROVED:
				GetApprovedResponse approvedResponse;
				try
				{
					approvedResponse = studyDefinitionService.getApproved();
				}
				catch (GenericLayerStudyDefinitionServiceGetApprovedFAULTFaultMessage e)
				{
					logger.error(e.getMessage());
					throw new RuntimeException(e);
				}
				elements = approvedResponse.getHL7Containers();
				break;
			case DRAFT:
				GetDraftResponse draftResponse;
				try
				{
					draftResponse = studyDefinitionService.getDraft();
				}
				catch (GenericLayerStudyDefinitionServiceGetDraftFAULTFaultMessage e)
				{
					logger.error(e.getMessage());
					throw new RuntimeException(e);
				}
				elements = draftResponse.getHL7Containers();
				break;
			case REJECTED:
				throw new UnsupportedOperationException("Get rejected study definitions not implemented");
			case SUBMITTED:
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
				elements = submittedResponse.getHL7Containers();
				break;
			default:
				throw new UnsupportedOperationException("Unknown study definition status [" + status + "]");
		}

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
		throw new UnknownStudyDefinitionException("unknown submitted study definition id [" + id + "]");
	}
}