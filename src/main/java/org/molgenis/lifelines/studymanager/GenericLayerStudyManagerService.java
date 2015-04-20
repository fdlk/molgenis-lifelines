package org.molgenis.lifelines.studymanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.umcg.hl7.service.catalog.CreateResponse;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionService;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceApproveFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceGetApprovedFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceGetByEmailFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceGetByIdFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceGetDraftFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceSubmitFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerStudyDefinitionServiceWithdrawFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GetByEmailResponse;
import nl.umcg.hl7.service.catalog.GetSubmittedResponse;

import org.apache.log4j.Logger;
import org.hl7.v3.ArrayOfXElement;
import org.hl7.v3.HL7Container;
import org.hl7.v3.POQMMT000001UVQualityMeasureDocument;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.data.CrudRepository;
import org.molgenis.data.DataService;
import org.molgenis.data.Query;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.lifelines.studymanager.hl7.HL7StudyConverter;
import org.molgenis.lifelines.studymanager.hl7.StudyBean;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.study.StudyDataRequest;
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
	final DataService dataService;
	private final HL7StudyConverter hl7Converter;

	public GenericLayerStudyManagerService(GenericLayerStudyDefinitionService studyDefinitionService,
			CatalogManagerService catalogLoaderService, GenericLayerDataQueryService dataQueryService,
			MolgenisUserService userService, DataService dataService)
	{
		if (studyDefinitionService == null) throw new IllegalArgumentException("Study definition service is null");
		if (catalogLoaderService == null) throw new IllegalArgumentException("Catalog manager service is null");
		if (dataQueryService == null) throw new IllegalArgumentException("Data query service is null");
		if (userService == null) throw new IllegalArgumentException("User service is null");
		this.studyDefinitionService = studyDefinitionService;
		this.catalogLoaderService = catalogLoaderService;
		this.dataQueryService = dataQueryService;
		this.userService = userService;
		this.dataService = dataService;
		this.hl7Converter = new HL7StudyConverter();
	}

	/**
	 * Find the study definition with the given id
	 * 
	 * @return
	 * @throws UnknownStudyDefinitionException
	 */
	@Override
	public StudyDefinition getStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		StudyDataRequest sdr = dataService.findOne(StudyDataRequest.ENTITY_NAME, Integer.valueOf(id),
				StudyDataRequest.class);
		if (sdr == null)
		{
			throw new UnknownStudyDefinitionException("Unknown StudyDataRequest with id [" + id + "]");
		}

		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(StudyDefinitionIdConverter
				.omxIdentifierToStudyDefinitionId(sdr.getIdentifier()));

		return new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument, dataService);
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
			submittedResponse = studyDefinitionService.getSubmitted(null);
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
					studyDefinitions
							.add(new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument, dataService));
				}
			}
		}
		return studyDefinitions;
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(Status status)
	{
		ArrayOfXElement hl7Containers;
		switch (status)
		{
			case APPROVED:
			case EXPORTED:
				try
				{
					hl7Containers = studyDefinitionService.getApproved(null).getHL7Containers();
				}
				catch (GenericLayerStudyDefinitionServiceGetApprovedFAULTFaultMessage e)
				{
					logger.error(e.getMessage());
					throw new RuntimeException(e);
				}
				break;
			case DRAFT:
				try
				{
					hl7Containers = studyDefinitionService.getDraft(null).getHL7Containers();
				}
				catch (GenericLayerStudyDefinitionServiceGetDraftFAULTFaultMessage e)
				{
					logger.error(e.getMessage());
					throw new RuntimeException(e);
				}
				break;
			case REJECTED:
				// Study manager service does not support state REJECTED
				return Collections.emptyList();
			case SUBMITTED:
				try
				{
					hl7Containers = studyDefinitionService.getSubmitted(null).getHL7Containers();
				}
				catch (GenericLayerStudyDefinitionServiceGetSubmittedFAULTFaultMessage e)
				{
					logger.error(e.getMessage());
					throw new RuntimeException(e);
				}
				break;
			default:
				throw new RuntimeException("Unknown status: " + status);
		}
		return toStudyDefinitionList(hl7Containers);
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(String username)
	{
		MolgenisUser user = userService.getUser(username);
		if (user == null) throw new RuntimeException("Unknown user [" + username + "]");

		GetByEmailResponse emailResponse;
		try
		{
			emailResponse = studyDefinitionService.getByEmail(user.getEmail());
		}
		catch (GenericLayerStudyDefinitionServiceGetByEmailFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
		return toStudyDefinitionList(emailResponse.getHL7Containers());
	}

	private List<StudyDefinition> toStudyDefinitionList(ArrayOfXElement elements)
	{
		List<StudyDefinition> studyDefinitions = new ArrayList<StudyDefinition>();
		if (elements != null)
		{
			List<HL7Container> hl7Containers = elements.getHL7Container();
			if (hl7Containers != null)
			{
				for (HL7Container hl7Container : hl7Containers)
				{
					POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container
							.getQualityMeasureDocument();

					String omxIdentifier = StudyDefinitionIdConverter
							.studyDefinitionIdToOmxIdentifier(qualityMeasureDocument.getId().getExtension());
					Query q = new QueryImpl().eq(StudyDataRequest.IDENTIFIER, omxIdentifier);
					StudyDataRequest sdr = dataService.findOne(StudyDataRequest.ENTITY_NAME, q, StudyDataRequest.class);
					if (sdr == null)
					{
						throw new RuntimeException("Unknow studydatarequest with idenfifier [" + omxIdentifier + "]");
					}
					qualityMeasureDocument.getId().setExtension(sdr.getId().toString());
					studyDefinitions
							.add(new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument, dataService));
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
	public boolean isStudyDataActivated(String id) throws UnknownStudyDefinitionException, UnknownCatalogException
	{
		return dataQueryService.isStudyDataActivated(id);
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
			CreateResponse createResponse = studyDefinitionService.create(user.getEmail());
			if (createResponse == null) throw new RuntimeException("CreateResponse is null");
			qualityMeasureDocument = createResponse.getHL7Container().getQualityMeasureDocument();
		}
		catch (Throwable t)
		{
			logger.error(t.getMessage());
			throw new RuntimeException(t);
		}
		return new QualityMeasureDocumentStudyDefinition(qualityMeasureDocument, dataService);
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		// Hack, but we need to finish this!
		// Check if it is a omx id -> convert to ll id
		StudyDataRequest sdr = dataService.findOne(StudyDataRequest.ENTITY_NAME,
				Integer.valueOf(studyDefinition.getId()), StudyDataRequest.class);
		if (sdr != null)
		{
			String llId = StudyDefinitionIdConverter.omxIdentifierToStudyDefinitionId(sdr.getIdentifier());
			studyDefinition.setId(llId);
		}

		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(studyDefinition
				.getId());

		// update study definition
		hl7Converter.updateQualityMeasureDocument(qualityMeasureDocument, new StudyBean(studyDefinition,
				dataService));

		// submit study definition
		try
		{
			HL7Container hl7Container = new HL7Container();
			hl7Container.setQualityMeasureDocument(qualityMeasureDocument);
			studyDefinitionService.revise(hl7Container);
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

	private POQMMT000001UVQualityMeasureDocument getStudyDefinitionAsQualityMeasureDocument(String id)
			throws UnknownStudyDefinitionException
	{
		try
		{
			HL7Container hl7Container = studyDefinitionService.getById(id);
			return hl7Container.getQualityMeasureDocument();
		}
		catch (GenericLayerStudyDefinitionServiceGetByIdFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<StudyDefinition> findStudyDefinitions(Status status, String search)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException,
			UnknownCatalogException
	{
		CrudRepository repository = dataService.getCrudRepository(StudyDataRequest.ENTITY_NAME);
		StudyDataRequest studyDataRequest = repository.findOne(id, StudyDataRequest.class);
		if (studyDataRequest == null)
		{
			throw new UnknownStudyDefinitionException("StudyDataRequest does not exist [" + id + "]");
		}
		String externalStudyDefinitionId = sendStudyDataRequestToGenericLayer(studyDataRequest);

		// store external id
		studyDataRequest.setExternalId(externalStudyDefinitionId);
		repository.update(studyDataRequest);
	}

	@Override
	public void withdrawStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		CrudRepository repository = dataService.getCrudRepository(StudyDataRequest.ENTITY_NAME);
		StudyDataRequest studyDataRequest = repository.findOne(id, StudyDataRequest.class);
		if (studyDataRequest == null)
		{
			throw new UnknownStudyDefinitionException("StudyDataRequest does not exist [" + id + "]");
		}
		String externalId = studyDataRequest.getExternalId();

		try
		{
			studyDefinitionService.withdraw(externalId);
		}
		catch (GenericLayerStudyDefinitionServiceWithdrawFAULTFaultMessage e)
		{
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private String sendStudyDataRequestToGenericLayer(StudyDataRequest studyDataRequest)
	{
		MolgenisUser molgenisUser = studyDataRequest.getMolgenisUser();

		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument;
		if (studyDataRequest.getExternalId() == null)
		{
			// create empty study definition
			qualityMeasureDocument = createStudyDefinition(molgenisUser);
		}
		else
		{
			// get existing study definition
			try
			{
				qualityMeasureDocument = getStudyDefinitionAsQualityMeasureDocument(studyDataRequest.getExternalId());
			}
			catch (UnknownStudyDefinitionException e)
			{
				logger.error(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		// update study definition
		updateStudyDefinition(qualityMeasureDocument, studyDataRequest);

		String studyDefinitionId = qualityMeasureDocument.getId().getExtension();
		if (studyDataRequest.getExternalId() == null)
		{
			// submit study definition
			submitStudyDefinition(studyDefinitionId);
		}

		// approve study definition
		approveStudyDefinition(studyDefinitionId);

		return studyDefinitionId;
	}

	private POQMMT000001UVQualityMeasureDocument createStudyDefinition(MolgenisUser molgenisUser)
	{
		try
		{
			CreateResponse createResponse = studyDefinitionService.create(molgenisUser.getEmail());
			if (createResponse == null) throw new RuntimeException("CreateResponse is null");
			HL7Container hl7Container = createResponse.getHL7Container();
			if (hl7Container == null) throw new RuntimeException("HL7Container is null");
			return hl7Container.getQualityMeasureDocument();
		}
		catch (Throwable t)
		{
			logger.error("", t);
			throw new RuntimeException(t);
		}
	}

	POQMMT000001UVQualityMeasureDocument updateStudyDefinition(
			POQMMT000001UVQualityMeasureDocument qualityMeasureDocument, StudyDataRequest studyDataRequest)
	{
		hl7Converter.updateQualityMeasureDocument(qualityMeasureDocument, new StudyBean(studyDataRequest));

		try
		{
			HL7Container hl7Container = new HL7Container();
			hl7Container.setQualityMeasureDocument(qualityMeasureDocument);
			studyDefinitionService.revise(hl7Container);
		}
		catch (GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}

		return qualityMeasureDocument;
	}

	private void submitStudyDefinition(String studyDefinitionId)
	{
		try
		{
			studyDefinitionService.submit(studyDefinitionId);
		}
		catch (GenericLayerStudyDefinitionServiceSubmitFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}

	private void approveStudyDefinition(String studyDefinitionId)
	{
		try
		{
			studyDefinitionService.approve(studyDefinitionId);
		}
		catch (GenericLayerStudyDefinitionServiceApproveFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}
}