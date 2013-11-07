package org.molgenis.lifelines.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import nl.umcg.hl7.service.catalog.CatalogService;
import nl.umcg.hl7.service.catalog.GenericLayerCatalogService;
import nl.umcg.hl7.service.catalog.GetCatalogResponse.GetCatalogResult;
import nl.umcg.hl7.service.catalog.GetValuesetsResponse.GetValuesetsResult;

import org.apache.log4j.Logger;
import org.molgenis.catalog.Catalog;
import org.molgenis.catalog.CatalogMeta;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.hl7.ANY;
import org.molgenis.hl7.CD;
import org.molgenis.hl7.ED;
import org.molgenis.hl7.II;
import org.molgenis.hl7.PQ;
import org.molgenis.hl7.REPCMT000100UV01Component3;
import org.molgenis.hl7.REPCMT000100UV01Observation;
import org.molgenis.hl7.REPCMT000100UV01Organizer;
import org.molgenis.hl7.ValueSets;
import org.molgenis.hl7.ValueSets.ValueSet;
import org.molgenis.hl7.ValueSets.ValueSet.Code;
import org.molgenis.lifelines.resourcemanager.GenericLayerResourceManagerService;
import org.molgenis.lifelines.utils.HL7DataTypeMapper;
import org.molgenis.lifelines.utils.OmxIdentifierGenerator;
import org.molgenis.omx.catalogmanager.OmxCatalog;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.omx.utils.ProtocolUtils;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

public class GenericLayerCatalogueManagerService implements CatalogManagerService
{
	private static final Logger logger = Logger.getLogger(GenericLayerCatalogueManagerService.class);

	private static final JAXBContext JAXB_CONTEXT_VALUESETS;
	private static final JAXBContext JAXB_CONTEXT_ORGANIZER;

	static
	{
		try
		{
			JAXB_CONTEXT_VALUESETS = JAXBContext.newInstance(ValueSets.class);
			JAXB_CONTEXT_ORGANIZER = JAXBContext.newInstance(REPCMT000100UV01Organizer.class);
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}
	}

	private final Database database;
	private final GenericLayerCatalogService genericLayerCatalogService;
	private final GenericLayerResourceManagerService resourceManagerService;

	public GenericLayerCatalogueManagerService(Database database,
			GenericLayerCatalogService genericLayerCatalogService,
			GenericLayerResourceManagerService resourceManagerService)
	{
		if (database == null) throw new IllegalArgumentException("database is null");
		if (genericLayerCatalogService == null) throw new IllegalArgumentException("genericLayerCatalogService is null");
		if (resourceManagerService == null) throw new IllegalArgumentException("resourceManagerService is null");
		this.database = database;
		this.genericLayerCatalogService = new CatalogService().getBasicHttpBindingGenericLayerCatalogService();
		this.resourceManagerService = resourceManagerService;
	}

	@Override
	public List<CatalogMeta> findCatalogs()
	{
		return resourceManagerService.findCatalogs();
	}

	@Override
	public Catalog getCatalog(String id) throws UnknownCatalogException
	{
		// retrieve catalog from database
		String omxIdentifier = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		return getCatalog(id, omxIdentifier);
	}

	@Override
	public Catalog getCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		String omxIdentifier = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
		return getCatalog(id, omxIdentifier);
	}

	private Catalog getCatalog(String id, String omxIdentifier)
	{
		// retrieve catalog from database
		DataSet dataSet;
		try
		{
			dataSet = DataSet.findByIdentifier(database, omxIdentifier);
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
		if (dataSet != null) return new OmxCatalog(dataSet);

		// retrieve catalog from generic layer
		CatalogMeta catalogMeta = resourceManagerService.findCatalog(id);
		REPCMT000100UV01Organizer catalog = retrieveCatalog(id);
		return new OrganizerCatalog(catalog, catalogMeta);
	}

	@Transactional
	@Override
	public void loadCatalog(String id)
	{
		try
		{
			// retrieve catalog data from LifeLines Generic Layer catalog service
			REPCMT000100UV01Organizer catalog = retrieveCatalog(id);

			// convert to MOLGENIS OMX model and add to database
			DataSet dataSet = new DataSet();
			dataSet.setIdentifier(CatalogIdConverter.catalogIdToOmxIdentifier(id));
			dataSet.setName("Catalogue #" + id);

			Protocol rootProtocol = new Protocol();
			rootProtocol.setIdentifier(UUID.randomUUID().toString());
			rootProtocol.setName("LifeLines");

			dataSet.setProtocolUsed(rootProtocol);

			Map<String, String> valueSetMap = parseCatalog(dataSet, rootProtocol, catalog);

			database.add(rootProtocol);
			database.add(dataSet);

			// retrieve catalog data from LifeLines Generic Layer catalog service
			GetValuesetsResult valueSetsResult = genericLayerCatalogService.getValuesets(id, null);
			loadValueSets(valueSetsResult, valueSetMap);
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isCatalogLoaded(String id) throws UnknownCatalogException
	{
		String dataSetId = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		try
		{
			return DataSet.findByIdentifier(database, dataSetId) != null;
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Transactional
	@Override
	public void unloadCatalog(String id) throws UnknownCatalogException
	{
		String dataSetId = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		deleteDataSetAndProtocols(dataSetId);
	}

	@Transactional
	@Override
	public void loadCatalogOfStudyDefinition(String id) throws UnknownCatalogException
	{
		try
		{
			// retrieve catalog data from LifeLines Generic Layer catalog service
			REPCMT000100UV01Organizer catalog = retrieveCatalogOfStudyDefinition(id);

			// convert to MOLGENIS OMX model and add to database
			DataSet dataSet = new DataSet();
			dataSet.setIdentifier(CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id));
			dataSet.setName("Study definition #" + id);

			Protocol rootProtocol = new Protocol();
			rootProtocol.setIdentifier(UUID.randomUUID().toString());
			rootProtocol.setName("LifeLines");

			dataSet.setProtocolUsed(rootProtocol);

			// FIXME add patient protocol/features

			Map<String, String> valueSetMap = parseCatalog(dataSet, rootProtocol, catalog);

			database.add(rootProtocol);
			database.add(dataSet);

			// retrieve catalog data from LifeLines Generic Layer catalog service
			GetValuesetsResult valueSetsResult = genericLayerCatalogService.getValuesets(null, id);
			loadValueSets(valueSetsResult, valueSetMap);
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isCatalogOfStudyDefinitionLoaded(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		// FIXME implement
		throw new UnsupportedOperationException();
	}

	private void deleteDataSetAndProtocols(String dataSetIdentifier) throws UnknownCatalogException
	{
		try
		{
			DataSet dataSet = DataSet.findByIdentifier(database, dataSetIdentifier);
			if (dataSet == null)
			{
				throw new UnknownCatalogException("unknown catalog identifier [" + dataSetIdentifier + "]");
			}
			List<Protocol> protocols = ProtocolUtils.getProtocolDescendants(dataSet.getProtocolUsed());

			database.remove(dataSet);
			database.remove(protocols);
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Transactional
	@Override
	public void unloadCatalogOfStudyDefinition(String id) throws UnknownCatalogException
	{
		String dataSetIdentifier = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
		deleteDataSetAndProtocols(dataSetIdentifier);
	}

	private void loadValueSets(GetValuesetsResult valueSetsResult, Map<String, String> featureMap)
	{
		// convert to HL7 organizer
		ValueSets valueSets;
		try
		{
			Unmarshaller um = JAXB_CONTEXT_VALUESETS.createUnmarshaller();
			valueSets = um.unmarshal((Node) valueSetsResult.getAny(), ValueSets.class).getValue();
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}

		OntologyIndex ontologyIndex = new OntologyIndex();

		try
		{
			for (ValueSet valueSet : valueSets.getValueSet())
			{
				String identifier = featureMap.get(valueSet.getName());
				ObservableFeature observableFeature = ObservableFeature.findByIdentifier(database, identifier);
				if (observableFeature == null)
				{
					throw new RuntimeException("missing ObservableFeature with name '" + identifier + "'");
				}

				for (Code code : valueSet.getCode())
				{
					OntologyTerm ontologyTerm = toOntologyTerm(code, ontologyIndex);

					// create category
					String categoryIdentifier = OmxIdentifierGenerator.from(Category.class, code.getCodeSystem(),
							code.getCode());
					Category category = Category.findByIdentifier(database, categoryIdentifier);
					if (category == null)
					{
						category = new Category();
						category.setIdentifier(categoryIdentifier);
						category.setName(code.getDisplayName());
						category.setObservableFeature(observableFeature);
						category.setDefinition(ontologyTerm);
						category.setValueCode(code.getCodeSystemName() + ':' + code.getCode());

						database.add(category);
					}
				}
			}
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}
	}

	private REPCMT000100UV01Organizer retrieveCatalog(String id)
	{
		// retrieve catalog data from LifeLines Generic Layer catalog service
		GetCatalogResult catalogResult = genericLayerCatalogService.getCatalog(id, null);

		// convert to HL7 organizer
		REPCMT000100UV01Organizer catalog;
		try
		{
			Unmarshaller um = JAXB_CONTEXT_ORGANIZER.createUnmarshaller();
			catalog = um.unmarshal((Node) catalogResult.getAny(), REPCMT000100UV01Organizer.class).getValue();
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}

		return catalog;
	}

	private REPCMT000100UV01Organizer retrieveCatalogOfStudyDefinition(String id)
	{
		// retrieve catalog data from LifeLines Generic Layer catalog service
		GetCatalogResult catalogResult = genericLayerCatalogService.getCatalog(null, id);

		// convert to HL7 organizer
		REPCMT000100UV01Organizer catalog;
		try
		{
			Unmarshaller um = JAXB_CONTEXT_ORGANIZER.createUnmarshaller();
			catalog = um.unmarshal((Node) catalogResult.getAny(), REPCMT000100UV01Organizer.class).getValue();
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}

		return catalog;
	}

	private Map<String, String> parseCatalog(DataSet dataSet, Protocol rootProtocol, REPCMT000100UV01Organizer catalog)
			throws DatabaseException
	{
		Map<String, String> featureMap = new HashMap<String, String>();
		OntologyIndex ontologyIndex = new OntologyIndex();

		// parse protocols between root protocols
		for (REPCMT000100UV01Component3 rootComponent : catalog.getComponent())
		{
			parseComponent(rootComponent, rootProtocol, database, featureMap, ontologyIndex);
		}
		return featureMap;
	}

	private void parseComponent(REPCMT000100UV01Component3 component, Protocol parentProtocol, Database database,
			Map<String, String> featureMap, OntologyIndex ontologyIndex) throws DatabaseException
	{

		// parse feature
		if (component.getObservation() != null)
		{
			REPCMT000100UV01Observation observation = component.getObservation().getValue();
			CD observationCode = observation.getCode();

			logger.debug("parsing observation " + observationCode.getDisplayName());

			// get or create feature
			List<II> observationId = observation.getId();
			String featureId = (observationId != null && !observationId.isEmpty()) ? observationId.get(0).getRoot() : UUID
					.randomUUID().toString();
			ANY anyValue = observation.getValue();
			if (anyValue instanceof CD)
			{
				CD value = (CD) anyValue;
				featureMap.put(value.getCodeSystemName(), featureId);
			}

			ObservableFeature observableFeature = ObservableFeature.findByIdentifier(database, featureId);
			if (observableFeature == null)
			{
				String observationName = observationCode.getDisplayName();
				if (observationName == null)
				{
					logger.warn("observation does not have a display name '" + observationCode.getCode() + "'");
					observationName = observationCode.getCode();
				}

				// TODO what to do in case of multiple translations?
				OntologyTerm ontologyTerm;
				List<CD> translationCodes = observationCode.getTranslation();
				if (translationCodes != null && !translationCodes.isEmpty()) ontologyTerm = toOntologyTerm(
						translationCodes.get(0), ontologyIndex);
				else ontologyTerm = null;

				// determine data type
				String dataType = HL7DataTypeMapper.get(anyValue);
				if (dataType == null) logger
						.warn("HL7 data type not supported: " + anyValue.getClass().getSimpleName());

				observableFeature = new ObservableFeature();
				observableFeature.setIdentifier(featureId);
				observableFeature.setName(observationName);
				ED originalText = observationCode.getOriginalText();
				if (originalText != null) observableFeature.setDescription(originalText.getContent().get(0).toString());
				if (dataType != null) observableFeature.setDataType(dataType);
				observableFeature.setDefinitions(ontologyTerm);

				// determine unit
				if (anyValue instanceof PQ)
				{
					@SuppressWarnings("unused")
					PQ value = (PQ) anyValue;
					// TODO how to determine ontologyterms for units and do observableFeature.setUnit()
				}
				database.add(observableFeature);
			}

			// add feature to protocol
			parentProtocol.getFeatures().add(observableFeature);
		}

		// parse sub-protocol
		if (component.getOrganizer() != null)
		{
			REPCMT000100UV01Organizer organizer = component.getOrganizer().getValue();
			CD organizerCode = organizer.getCode();
			logger.debug("parsing organizer " + organizerCode.getCode() + " " + organizerCode.getDisplayName());

			// get or create protocol
			List<II> organizerId = organizer.getId();
			String protocolId = (organizerId != null && !organizerId.isEmpty()) ? organizerId.get(0).getRoot() : UUID
					.randomUUID().toString();
			Protocol protocol = Protocol.findByIdentifier(database, protocolId);
			if (protocol == null)
			{
				String organizerName = organizerCode.getDisplayName();
				if (organizerName == null)
				{
					logger.warn("organizer does not have a display name '" + organizerCode.getCode() + "'");
					organizerName = organizerCode.getCode();
				}

				OntologyTerm ontologyTerm = toOntologyTerm(organizerCode, ontologyIndex);

				protocol = new Protocol();
				protocol.setIdentifier(protocolId);
				protocol.setName(organizerName);
				ED originalText = organizerCode.getOriginalText();
				if (originalText != null) protocol.setDescription(originalText.getContent().get(0).toString());
				protocol.setProtocolType(ontologyTerm);

			}

			// recurse over nested protocols
			for (REPCMT000100UV01Component3 subComponent : organizer.getComponent())
				parseComponent(subComponent, protocol, database, featureMap, ontologyIndex);

			// add protocol to parent protocol
			parentProtocol.getSubprotocols().add(protocol);
		}

		database.add(parentProtocol);
	}

	private OntologyTerm toOntologyTerm(Code code, OntologyIndex ontologyIndex) throws DatabaseException
	{
		return toOntologyTerm(code.getCodeSystem(), code.getCodeSystemName(), code.getDisplayName(), code.getCode(),
				ontologyIndex);
	}

	private OntologyTerm toOntologyTerm(CD code, OntologyIndex ontologyIndex) throws DatabaseException
	{
		return toOntologyTerm(code.getCodeSystem(), code.getCodeSystemName(), code.getDisplayName(), code.getCode(),
				ontologyIndex);
	}

	private OntologyTerm toOntologyTerm(String codeSystem, String codeSystemName, String displayName, String codeCode,
			OntologyIndex ontologyIndex) throws DatabaseException
	{
		// create and index ontology
		Ontology ontology = ontologyIndex.get(codeSystem);
		if (ontology == null)
		{
			if (codeSystem == null || codeSystemName == null)
			{
				logger.warn("missing code system or code system name for ontology term '" + displayName + "'");
			}
			else
			{
				String ontologyIdentifier = OmxIdentifierGenerator.from(Ontology.class, codeSystem);
				ontology = Ontology.findByIdentifier(database, ontologyIdentifier);
				if (ontology == null)
				{
					// create ontology for each code system
					ontology = new Ontology();
					ontology.setIdentifier(ontologyIdentifier);
					ontology.setName(codeSystemName);
					ontology.setOntologyAccession(codeSystem);

					database.add(ontology);
				}
				ontologyIndex.put(codeSystem, ontology);
			}
		}

		// create and index ontology term
		OntologyTerm ontologyTerm = ontologyIndex.get(codeSystem, codeCode);
		if (ontologyTerm == null)
		{
			String ontologyTermIdentifier = OmxIdentifierGenerator.from(OntologyTerm.class, codeSystem, codeCode);
			ontologyTerm = OntologyTerm.findByIdentifier(database, ontologyTermIdentifier);
			if (ontologyTerm == null)
			{
				ontologyTerm = new OntologyTerm();
				ontologyTerm.setIdentifier(ontologyTermIdentifier);
				ontologyTerm.setName(displayName != null ? displayName : "");
				ontologyTerm.setTermAccession(codeCode);
				if (ontology != null) ontologyTerm.setOntology(ontology);

				database.add(ontologyTerm);
			}
			ontologyIndex.put(codeSystem, codeCode, ontologyTerm);
		}
		return ontologyTerm;
	}

	private static class OntologyIndex
	{
		private final Map<String, Ontology> ontologyMap;
		private final Map<String, Map<String, OntologyTerm>> ontologyTermMap;

		public OntologyIndex()
		{
			ontologyMap = new HashMap<String, Ontology>();
			ontologyTermMap = new HashMap<String, Map<String, OntologyTerm>>();
		}

		public void put(String codeSystem, Ontology ontology)
		{
			ontologyMap.put(codeSystem, ontology);
		}

		public void put(String codeSystem, String code, OntologyTerm ontologyTerm)
		{
			Map<String, OntologyTerm> codeMap = ontologyTermMap.get(codeSystem);
			if (codeMap == null)
			{
				codeMap = new HashMap<String, OntologyTerm>();
				ontologyTermMap.put(codeSystem, codeMap);
			}
			codeMap.put(code, ontologyTerm);
		}

		public Ontology get(String codeSystem)
		{
			return ontologyMap.get(codeSystem);
		}

		public OntologyTerm get(String codeSystem, String code)
		{
			Map<String, OntologyTerm> codeMap = ontologyTermMap.get(codeSystem);
			return codeMap != null ? codeMap.get(code) : null;
		}
	}
}
