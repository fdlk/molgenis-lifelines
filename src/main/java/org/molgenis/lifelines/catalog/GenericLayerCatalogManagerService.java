package org.molgenis.lifelines.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nl.umcg.hl7.service.catalog.CatalogService;
import nl.umcg.hl7.service.catalog.GenericLayerCatalogService;
import nl.umcg.hl7.service.catalog.GenericLayerCatalogServiceGetCatalogFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerCatalogServiceGetCatalogReleasesFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GenericLayerCatalogServiceGetValueSetsFAULTFaultMessage;
import nl.umcg.hl7.service.catalog.GetCatalogReleasesResponse;
import nl.umcg.hl7.service.catalog.HumanLanguage;

import org.apache.log4j.Logger;
import org.hl7.v3.ANY;
import org.hl7.v3.ArrayOfXElement;
import org.hl7.v3.BL;
import org.hl7.v3.CD;
import org.hl7.v3.CO;
import org.hl7.v3.ED;
import org.hl7.v3.HL7Container;
import org.hl7.v3.II;
import org.hl7.v3.INT;
import org.hl7.v3.PN;
import org.hl7.v3.POQMMT000001UVAuthor;
import org.hl7.v3.POQMMT000001UVQualityMeasureDocument;
import org.hl7.v3.PQ;
import org.hl7.v3.REAL;
import org.hl7.v3.REPCMT000100UV01Component3;
import org.hl7.v3.REPCMT000100UV01Observation;
import org.hl7.v3.REPCMT000100UV01Organizer;
import org.hl7.v3.ST;
import org.hl7.v3.TS;
import org.hl7.v3.ValueSet;
import org.hl7.v3.ValueSet.Code;
import org.hl7.v3.ValueSets;
import org.molgenis.catalog.Catalog;
import org.molgenis.catalog.CatalogMeta;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.lifelines.utils.MeasurementIdConverter;
import org.molgenis.lifelines.utils.ObservationIdConverter;
import org.molgenis.omx.catalogmanager.OmxCatalog;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.omx.search.DataSetsIndexer;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.omx.utils.ProtocolUtils;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;

public class GenericLayerCatalogManagerService implements CatalogManagerService
{
	private static final Logger logger = Logger.getLogger(GenericLayerCatalogManagerService.class);

	private final DataService dataService;
	private final GenericLayerCatalogService genericLayerCatalogService;
	private final DataSetsIndexer dataSetsIndexer;

	public GenericLayerCatalogManagerService(DataService dataService,
			GenericLayerCatalogService genericLayerCatalogService, DataSetsIndexer dataSetsIndexer)
	{
		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		if (genericLayerCatalogService == null) throw new IllegalArgumentException("genericLayerCatalogService is null");
		this.dataService = dataService;
		this.genericLayerCatalogService = genericLayerCatalogService;
		this.dataSetsIndexer = dataSetsIndexer;
	}

	@Override
	public Iterable<CatalogMeta> getCatalogs()
	{
		try
		{
			GetCatalogReleasesResponse catalogReleases = genericLayerCatalogService.getCatalogReleases();
			return convertCatalogReleases(catalogReleases);
		}
		catch (GenericLayerCatalogServiceGetCatalogReleasesFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Catalog getCatalog(String id) throws UnknownCatalogException
	{
		CatalogMeta catalogMeta = null;
		for (CatalogMeta aCatalogMeta : getCatalogs())
		{
			if (aCatalogMeta.getId().equals(id))
			{
				catalogMeta = aCatalogMeta;
				break;
			}
		}
		if (catalogMeta == null) throw new UnknownCatalogException("Unknown catalog id [" + id + "]");

		REPCMT000100UV01Organizer catalog = retrieveCatalog(id, null, true);
		return new OrganizerCatalog(catalog, catalogMeta);
	}

	@Override
	public Catalog getCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		// use catalog of study data request in the molgenis db
		StudyDataRequest studyDataRequest = dataService.findOne(StudyDataRequest.ENTITY_NAME,
				new QueryImpl().eq(StudyDataRequest.ID, Integer.valueOf(id)), StudyDataRequest.class);
		if (studyDataRequest == null) throw new UnknownStudyDefinitionException("Study definition [" + id
				+ "] does not exist");
		Protocol protocol = studyDataRequest.getProtocol();
		if (protocol == null) throw new UnknownCatalogException("Catalog [" + id + "] does not exist");
		return new OmxCatalog(protocol, dataService);
	}

	@Transactional
	@Override
	public void loadCatalog(String id)
	{
		loadCatalog(id, null);
	}

	@Transactional
	@Override
	public void loadCatalogOfStudyDefinition(String id) throws UnknownCatalogException
	{
		loadCatalog(null, id);
	}

	private void loadCatalog(String catalogReleaseId, String studyDefinitionId)
	{
		boolean useOntology = true;
		Map<String, List<Code>> valueSetsIndex = createValueSetsIndex(catalogReleaseId, studyDefinitionId);
		REPCMT000100UV01Organizer catalog = retrieveCatalog(catalogReleaseId, studyDefinitionId, useOntology);

		// create catalog root protocol
		Protocol rootProtocol = new Protocol();
		rootProtocol.setIdentifier(getCatalogIdentifier(catalogReleaseId, studyDefinitionId));
		rootProtocol.setName("LifeLines");
		rootProtocol.setRoot(true);
		rootProtocol.setActive(true);

		List<Protocol> subprotocols = new ArrayList<Protocol>();
		for (REPCMT000100UV01Component3 component : catalog.getComponent())
		{
			REPCMT000100UV01Organizer organizer = component.getOrganizer().getValue();
			String code = organizer.getCode().getCode();

			if (code.equals("Generic"))
			{
				Protocol genericProtocol = new Protocol();
				genericProtocol.setIdentifier(UUID.randomUUID().toString());
				genericProtocol.setName(code);
				genericProtocol.setActive(isAvailable(organizer));

				List<Protocol> genericSubprotocols = new ArrayList<Protocol>();
				if (organizer.getComponent() != null)
				{
					for (REPCMT000100UV01Component3 genericComponent : organizer.getComponent())
					{
						Protocol protocol = parseGenericCatalogOrganizer(genericComponent.getOrganizer().getValue(),
								useOntology, valueSetsIndex);
						genericSubprotocols.add(protocol);
					}
				}
				if (!genericSubprotocols.isEmpty()) genericProtocol.setSubprotocols(genericSubprotocols);
				subprotocols.add(genericProtocol);
				dataService.add(Protocol.ENTITY_NAME, genericProtocol);
			}
			else
			{
				if (catalog.getComponent().size() != 1)
				{
					throw new RuntimeException("Expected exactly one catalog component for code != Generic");
				}
				rootProtocol.setName(code);

				List<Protocol> dataSourceSubprotocols = new ArrayList<Protocol>();
				if (organizer.getComponent() != null)
				{
					for (REPCMT000100UV01Component3 dataSourceComponent : organizer.getComponent())
					{
						Protocol protocol = parseDataSourceCatalogOrganizer(dataSourceComponent.getOrganizer()
								.getValue(), useOntology, valueSetsIndex, catalogReleaseId);
						dataSourceSubprotocols.add(protocol);
					}
				}
				if (!dataSourceSubprotocols.isEmpty()) rootProtocol.setSubprotocols(dataSourceSubprotocols);
			}
		}
		if (!subprotocols.isEmpty()) rootProtocol.setSubprotocols(subprotocols);

		dataService.add(Protocol.ENTITY_NAME, rootProtocol);

		dataService.getCrudRepository(Protocol.ENTITY_NAME).flush();
		dataService.getCrudRepository(Protocol.ENTITY_NAME).clearCache();
		dataSetsIndexer.indexProtocolsSynced(Collections.<Object> singletonList(rootProtocol.getId()));
	}

	private Map<String, List<Code>> createValueSetsIndex(String catalogReleaseId, String studyDefinitionId)
	{
		Map<String, List<Code>> valueSetMap = new HashMap<String, List<Code>>();
		ValueSets valueSets = retrieveValueSets(catalogReleaseId, studyDefinitionId);
		for (ValueSet valueSet : valueSets.getValueSet())
		{
			logger.info(valueSet.getName() + " | " + valueSet.getCode());
			valueSetMap.put(valueSet.getName(), valueSet.getCode());
		}
		return valueSetMap;
	}

	private Protocol parseDataSourceCatalogOrganizer(REPCMT000100UV01Organizer cohortOrganizer, boolean useOntology,
			Map<String, List<Code>> valueSetsIndex, String catalogReleaseId)
	{
		Protocol cohortProtocol;
		if (useOntology)
		{
			CD cohortOrganizerCode = cohortOrganizer.getCode();

			cohortProtocol = new Protocol();
			cohortProtocol.setIdentifier(UUID.randomUUID().toString());
			cohortProtocol.setName(cohortOrganizer.getCode().getDisplayName());
			cohortProtocol.setActive(isAvailable(cohortOrganizer));

			String cohortId = cohortOrganizerCode.getCode();

			// measurement components
			List<Protocol> cohortSubprotocols = new ArrayList<Protocol>();
			for (REPCMT000100UV01Component3 measurementComponent : cohortOrganizer.getComponent())
			{
				if (measurementComponent.getOrganizer() == null) break; // FIXME remove
				REPCMT000100UV01Organizer measurementOrganizer = measurementComponent.getOrganizer().getValue();

				CD measurementCode = measurementOrganizer.getCode();
				String measurementCodeCode = measurementCode.getCode();
				if (measurementCodeCode == null || measurementCodeCode.isEmpty())
				{
					throw new RuntimeException("Expected code for measurement [" + measurementCode.getDisplayName()
							+ "]");
				}

				String measurementCodeCodeSystem = measurementCode.getCodeSystem();
				if (measurementCodeCodeSystem == null || measurementCodeCodeSystem.isEmpty())
				{
					throw new RuntimeException("Expected code system for measurement ["
							+ measurementCode.getDisplayName() + "]");
				}

				String cohortCodeCode = cohortOrganizerCode.getCode();
				if (cohortCodeCode == null || cohortCodeCode.isEmpty())
				{
					throw new RuntimeException("Expected code for cohort [" + cohortOrganizerCode.getDisplayName()
							+ "]");
				}

				String cohortCodeCodeSystem = cohortOrganizerCode.getCodeSystem();
				if (cohortCodeCodeSystem == null || cohortCodeCodeSystem.isEmpty())
				{
					throw new RuntimeException("Expected code system for cohort ["
							+ cohortOrganizerCode.getDisplayName() + "]");
				}

				String omxProtocolIdentifier = MeasurementIdConverter.toOmxProtocolIdentifier(catalogReleaseId,
						cohortCodeCode, cohortCodeCodeSystem, measurementCodeCode, measurementCodeCodeSystem);
				Protocol measurementProtocol = new Protocol();
				measurementProtocol.setIdentifier(omxProtocolIdentifier);
				measurementProtocol.setName(measurementCode.getDisplayName());
				String measurementId = measurementCode.getCode();

				List<Protocol> measurementSubprotocols = new ArrayList<Protocol>();
				for (REPCMT000100UV01Component3 component : measurementOrganizer.getComponent())
				{
					REPCMT000100UV01Organizer componentOrganizer = component.getOrganizer().getValue();
					Protocol measurementSubprotocol = parseDataSourceCatalogOrganizerRec(componentOrganizer,
							useOntology, valueSetsIndex, cohortId, measurementId, catalogReleaseId);
					measurementSubprotocols.add(measurementSubprotocol);
				}
				if (!measurementSubprotocols.isEmpty()) measurementProtocol.setSubprotocols(measurementSubprotocols);

				cohortSubprotocols.add(measurementProtocol);
				dataService.add(Protocol.ENTITY_NAME, measurementProtocol);
			}
			if (!cohortSubprotocols.isEmpty()) cohortProtocol.setSubprotocols(cohortSubprotocols);
		}
		else
		{
			throw new UnsupportedOperationException("load generic catalog for useOntology=false not implemented");
		}

		dataService.add(Protocol.ENTITY_NAME, cohortProtocol);
		return cohortProtocol;
	}

	private Protocol parseDataSourceCatalogOrganizerRec(REPCMT000100UV01Organizer organizer, boolean useOntology,
			Map<String, List<Code>> valueSetsIndex, String cohortId, String measurementId, String catalogReleaseId)
	{
		Protocol protocol;
		if (useOntology)
		{
			protocol = new Protocol();
			String code = organizer.getCode().getCode();
			if (code != null)
			{
				protocol.setIdentifier(ObservationIdConverter.toOmxProtocolIdentifier(code, cohortId, measurementId,
						catalogReleaseId));
			}
			else
			{
				protocol.setIdentifier(UUID.randomUUID().toString());
			}
			protocol.setName(organizer.getCode().getDisplayName());

			List<Protocol> subprotocols = new ArrayList<Protocol>();
			List<ObservableFeature> features = new ArrayList<ObservableFeature>();
			for (REPCMT000100UV01Component3 component : organizer.getComponent())
			{
				// create subprotocol recursively
				if (component.getOrganizer() != null)
				{
					REPCMT000100UV01Organizer componentOrganizer = component.getOrganizer().getValue();
					Protocol subProtocol = parseDataSourceCatalogOrganizerRec(componentOrganizer, useOntology,
							valueSetsIndex, cohortId, measurementId, catalogReleaseId);
					subprotocols.add(subProtocol);
				}
				// create feature
				else if (component.getObservation() != null)
				{
					REPCMT000100UV01Observation componentObservation = component.getObservation().getValue();
					ObservableFeature feature = parseDataSourceCatalogObservation(componentObservation, valueSetsIndex,
							cohortId, measurementId, catalogReleaseId);
					features.add(feature);
				}
			}

			boolean active = isAvailable(organizer);
			if (active && !subprotocols.isEmpty())
			{
				boolean activeSubprotocol = false;
				for (Protocol subprotocol : subprotocols)
				{
					if (subprotocol.getActive() == null || subprotocol.getActive().booleanValue() == true)
					{
						activeSubprotocol = true;
						break;
					}
				}
				if (!activeSubprotocol) active = false;
			}
			protocol.setActive(active);

			if (!subprotocols.isEmpty()) protocol.setSubprotocols(subprotocols);
			if (!features.isEmpty()) protocol.setFeatures(features);
		}
		else
		{
			throw new UnsupportedOperationException("load data source catalog for useOntology=false not implemented");
		}

		dataService.add(Protocol.ENTITY_NAME, protocol);
		return protocol;
	}

	private ObservableFeature parseDataSourceCatalogObservation(REPCMT000100UV01Observation observation,
			Map<String, List<Code>> valueSetsIndex, String cohortId, String measurementId, String catalogReleaseId)
	{
		Gson gson = new Gson();
		CD code = observation.getCode();
		ObservableFeature observableFeature = new ObservableFeature();
		observableFeature.setIdentifier(code.getCode() + '.' + cohortId + '.' + measurementId + '.' + catalogReleaseId);
		observableFeature.setName(code.getDisplayName());

		Map<String, String> descriptions = new HashMap<String, String>();
		ED originalText = code.getOriginalText();
		if (originalText != null)
		{
			// Content has always exactly one item
			descriptions.put(originalText.getLanguage(), originalText.getContent().get(0).toString());
		}
		// get all the translations for the description
		for (CD translation : code.getTranslation())
		{
			descriptions.put(translation.getOriginalText().getLanguage(), translation.getOriginalText().getContent()
					.get(0).toString());
		}

		if (!descriptions.isEmpty())
		{
			observableFeature.setDescription(gson.toJson(descriptions).toString());
		}

		String dataType;
		ANY value = observation.getValue();
		if (value instanceof BL) dataType = "bool";
		else if (value instanceof CD) dataType = "categorical";
		else if (value instanceof CO) dataType = "categorical";
		else if (value instanceof II) dataType = "string";
		else if (value instanceof INT) dataType = "int";
		else if (value instanceof PQ) dataType = "string";
		else if (value instanceof REAL) dataType = "decimal";
		else if (value instanceof ST) dataType = "string";
		else if (value instanceof TS) dataType = "datetime";
		else throw new RuntimeException("Unsupported data type [" + value.getClass().getSimpleName() + "]");
		observableFeature.setDataType(dataType);

		if (value instanceof PQ)
		{
			OntologyTerm unitOntologyTerm = new OntologyTerm();
			unitOntologyTerm.setIdentifier(UUID.randomUUID().toString());
			unitOntologyTerm.setName(((PQ) value).getUnit());
			dataService.add(OntologyTerm.ENTITY_NAME, unitOntologyTerm);

			observableFeature.setUnit(unitOntologyTerm);
		}

		dataService.add(ObservableFeature.ENTITY_NAME, observableFeature);

		if (value instanceof CD || value instanceof CO)
		{
			CD cdValue = (CD) value;
			String valueSetName = cdValue.getCodeSystemName();
			if (valueSetsIndex.get(valueSetName) == null) throw new RuntimeException(valueSetName);
			for (Code valueCode : valueSetsIndex.get(valueSetName))
			{
				Category category = new Category();
				category.setIdentifier(UUID.randomUUID().toString());
				category.setName(valueCode.getDisplayName());
				category.setValueCode(valueCode.getCode());
				category.setObservableFeature(observableFeature);
				dataService.add(Category.ENTITY_NAME, category);
			}
		}

		return observableFeature;
	}

	private Protocol parseGenericCatalogOrganizer(REPCMT000100UV01Organizer organizer, boolean useOntology,
			Map<String, List<Code>> valueSetsIndex)
	{
		Protocol protocol;
		if (useOntology)
		{
			protocol = new Protocol();
			protocol.setIdentifier(UUID.randomUUID().toString());
			protocol.setName(organizer.getCode().getDisplayName());

			List<Protocol> subprotocols = new ArrayList<Protocol>();
			List<ObservableFeature> features = new ArrayList<ObservableFeature>();
			for (REPCMT000100UV01Component3 component : organizer.getComponent())
			{
				// create subprotocol recursively
				if (component.getOrganizer() != null)
				{
					REPCMT000100UV01Organizer componentOrganizer = component.getOrganizer().getValue();
					Protocol subProtocol = parseGenericCatalogOrganizer(componentOrganizer, useOntology, valueSetsIndex);
					subprotocols.add(subProtocol);
				}
				// create feature
				else if (component.getObservation() != null)
				{
					REPCMT000100UV01Observation componentObservation = component.getObservation().getValue();
					ObservableFeature feature = parseGenericCatalogObservation(componentObservation, valueSetsIndex);
					features.add(feature);
				}
			}

			boolean active = isAvailable(organizer);
			if (active && !subprotocols.isEmpty())
			{
				boolean activeSubprotocol = false;
				for (Protocol subprotocol : subprotocols)
				{
					if (subprotocol.getActive() == null || subprotocol.getActive().booleanValue() == true)
					{
						activeSubprotocol = true;
						break;
					}
				}
				if (!activeSubprotocol) active = false;
			}
			protocol.setActive(active);

			if (!subprotocols.isEmpty()) protocol.setSubprotocols(subprotocols);
			if (!features.isEmpty()) protocol.setFeatures(features);
		}
		else
		{
			throw new UnsupportedOperationException("load generic catalog for useOntology=false not implemented");
		}

		dataService.add(Protocol.ENTITY_NAME, protocol);
		return protocol;
	}

	private ObservableFeature parseGenericCatalogObservation(REPCMT000100UV01Observation observation,
			Map<String, List<Code>> valueSetsIndex)
	{
		CD code = observation.getCode();

		ObservableFeature observableFeature = new ObservableFeature();
		observableFeature.setIdentifier(UUID.randomUUID().toString());
		observableFeature.setName(code.getDisplayName());

		dataService.add(ObservableFeature.ENTITY_NAME, observableFeature);
		return observableFeature;
	}

	@Transactional(readOnly = true)
	@Override
	public boolean isCatalogLoaded(String id) throws UnknownCatalogException
	{
		return isCatalogLoaded(id, null);
	}

	@Transactional(readOnly = true)
	@Override
	public boolean isCatalogOfStudyDefinitionLoaded(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		return isCatalogLoaded(null, id);
	}

	private boolean isCatalogLoaded(String catalogReleaseId, String studyDefinitionId)
	{
		String catalogIdentifier = getCatalogIdentifier(catalogReleaseId, studyDefinitionId);
		return dataService.count(Protocol.ENTITY_NAME, new QueryImpl().eq(Protocol.IDENTIFIER, catalogIdentifier)) == 1;
	}

	public String getCatalogIdentifier(String catalogReleaseId, String studyDefinitionId)
	{
		if (catalogReleaseId != null) return CatalogIdConverter.catalogIdToOmxIdentifier(catalogReleaseId);
		else return CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(studyDefinitionId);
	}

	@Transactional
	@Override
	public void unloadCatalog(String id) throws UnknownCatalogException
	{
		String protocolIdentifier = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		deleteCatalog(protocolIdentifier);
	}

	// FIXME does not delete full catalog
	private void deleteCatalog(String protocolIdentifier) throws UnknownCatalogException
	{
		Protocol protocol = dataService.findOne(Protocol.ENTITY_NAME,
				new QueryImpl().eq(Protocol.IDENTIFIER, protocolIdentifier), Protocol.class);
		if (protocol == null)
		{
			throw new UnknownCatalogException("unknown catalog identifier [" + protocolIdentifier + "]");
		}
		List<Protocol> protocols = ProtocolUtils.getProtocolDescendants(protocol);

		dataService.delete(Protocol.ENTITY_NAME, protocol);
		dataService.delete(Protocol.ENTITY_NAME, protocols);
	}

	@Transactional
	@Override
	public void unloadCatalogOfStudyDefinition(String id) throws UnknownCatalogException
	{
		String protocolIdentifier = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
		deleteCatalog(protocolIdentifier);
	}

	private REPCMT000100UV01Organizer retrieveCatalog(String catalogReleaseId, String studyDefinitionId,
			boolean useOntology)
	{
		// retrieve catalog data from LifeLines Generic Layer catalog service
		REPCMT000100UV01Organizer catalog;
		try
		{
			HL7Container hl7Container = genericLayerCatalogService.getCatalog(catalogReleaseId, null, useOntology);
			if (hl7Container == null) throw new RuntimeException("HL7Container is null");
			catalog = hl7Container.getCatalog();
		}
		catch (GenericLayerCatalogServiceGetCatalogFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
		return catalog;
	}

	private ValueSets retrieveValueSets(String catalogReleaseId, String studyDefinitionId)
	{
		HL7Container hl7Container;
		try
		{
			hl7Container = genericLayerCatalogService.getValueSets(catalogReleaseId, HumanLanguage.EN,
					studyDefinitionId);
			return hl7Container.getValueSets();
		}
		catch (GenericLayerCatalogServiceGetValueSetsFAULTFaultMessage e)
		{
			logger.error("", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isCatalogActivated(String id) throws UnknownCatalogException
	{
		String protocolIdentifier = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		Protocol protocol = dataService.findOne(Protocol.ENTITY_NAME,
				new QueryImpl().eq(Protocol.IDENTIFIER, protocolIdentifier), Protocol.class);
		return protocol != null && protocol.getActive();
	}

	private void updateCatalogActivation(String id, boolean activate) throws UnknownCatalogException
	{
		String protocolIdentifier = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		Protocol protocol = dataService.findOne(Protocol.ENTITY_NAME,
				new QueryImpl().eq(Protocol.IDENTIFIER, protocolIdentifier), Protocol.class);
		if (protocol == null)
		{
			throw new UnknownCatalogException("unknown catalog identifier [" + protocolIdentifier + "]");
		}
		protocol.setActive(activate);
		dataService.update(Protocol.ENTITY_NAME, protocol);
	}

	@Override
	public void activateCatalog(String id) throws UnknownCatalogException
	{
		updateCatalogActivation(id, true);
	}

	@Override
	public void deactivateCatalog(String id) throws UnknownCatalogException
	{
		updateCatalogActivation(id, false);
	}

	private Iterable<CatalogMeta> convertCatalogReleases(GetCatalogReleasesResponse catalogReleasesResponse)
	{
		ArrayOfXElement hl7Containers = catalogReleasesResponse.getHL7Containers();
		if (hl7Containers == null) return Collections.emptyList();

		List<HL7Container> hl7ContainerList = hl7Containers.getHL7Container();
		if (hl7ContainerList == null || hl7ContainerList.isEmpty()) return Collections.emptyList();

		return Iterables.transform(hl7ContainerList, new Function<HL7Container, CatalogMeta>()
		{
			@Override
			public CatalogMeta apply(HL7Container hl7Container)
			{
				POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = hl7Container.getQualityMeasureDocument();

				String id = qualityMeasureDocument.getId().getExtension().toString();
				String name = qualityMeasureDocument.getTitle().getContent().get(0).toString();
				String description = qualityMeasureDocument.getText().getContent().get(0).toString();
				String version = qualityMeasureDocument.getVersionNumber().getValue().toString();
				List<String> authors = new ArrayList<String>();

				List<POQMMT000001UVAuthor> qualityMeasureDocumentAuthors = qualityMeasureDocument.getAuthor();
				for (POQMMT000001UVAuthor author : qualityMeasureDocumentAuthors)
				{
					List<PN> authorNames = author.getAssignedPerson().getAssignedPerson().getValue().getName();
					for (PN authorName : authorNames)
					{
						authors.add(authorName.getContent().get(0).toString());
					}
				}

				CatalogMeta catalogMeta = new CatalogMeta(id, name);
				catalogMeta.setDescription(description);
				catalogMeta.setVersion(version);
				catalogMeta.setAuthors(authors);
				return catalogMeta;
			}
		});
	}

	// EffectiveTime.low is set when group is unavailable.
	private boolean isAvailable(REPCMT000100UV01Organizer organizer)
	{
		return (organizer.getEffectiveTime() == null);
	}
}