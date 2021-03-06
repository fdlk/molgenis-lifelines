package org.molgenis.lifelines.studymanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.log4j.Logger;
import org.hl7.v3.ANY;
import org.hl7.v3.BL;
import org.hl7.v3.CD;
import org.hl7.v3.INT;
import org.hl7.v3.POQMMT000001UVQualityMeasureDocument;
import org.hl7.v3.PQ;
import org.hl7.v3.REAL;
import org.hl7.v3.REPCMT000100UV01Component3;
import org.hl7.v3.REPCMT000100UV01Observation;
import org.hl7.v3.REPCMT000100UV01Organizer;
import org.hl7.v3.REPCMT000400UV01ActCategory;
import org.hl7.v3.REPCMT000400UV01Component4;
import org.hl7.v3.ST;
import org.hl7.v3.TS;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.lifelines.catalog.CatalogIdConverter;
import org.molgenis.lifelines.utils.OmxIdentifierGenerator;
import org.molgenis.lifelines.utils.OutputStreamHttpEntity;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.ObservationSet;
import org.molgenis.omx.observ.ObservedValue;
import org.molgenis.omx.observ.value.BoolValue;
import org.molgenis.omx.observ.value.CategoricalValue;
import org.molgenis.omx.observ.value.LongValue;
import org.molgenis.omx.observ.value.StringValue;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenericLayerDataQueryService
{
	private static final Logger logger = Logger.getLogger(GenericLayerDataQueryService.class);

	private static final JAXBContext JAXB_CONTEXT_QUALITY_MEASURE_DOCUMENT;
	private static final JAXBContext JAXB_CONTEXT_ACT_CATEGORY;

	static
	{
		try
		{
			JAXB_CONTEXT_QUALITY_MEASURE_DOCUMENT = JAXBContext.newInstance(POQMMT000001UVQualityMeasureDocument.class);
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}
	}

	static
	{
		try
		{
			JAXB_CONTEXT_ACT_CATEGORY = JAXBContext.newInstance(REPCMT000400UV01ActCategory.class);
		}
		catch (JAXBException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Autowired
	private HttpClient httpClient;
	@Value("${lifelines.data.query.service.url}")
	private String dataQueryServiceUrl; // Specify in molgenis-server.properties
	@Autowired
	private DataService dataService;

	@Transactional
	public void loadStudyDefinitionData(final POQMMT000001UVQualityMeasureDocument studyDefinition)
	{
		try
		{
			// send eMeasure request to GL
			HttpPost httpPost = new HttpPost(dataQueryServiceUrl + "/data");
			httpPost.setHeader("Content-Type", "application/xml");
			httpPost.setEntity(new OutputStreamHttpEntity()
			{
				@Override
				public void writeTo(final OutputStream outstream) throws IOException
				{
					try
					{
						JAXB_CONTEXT_QUALITY_MEASURE_DOCUMENT.createMarshaller().marshal(studyDefinition,
								outstream);
					}
					catch (JAXBException e)
					{
						throw new RuntimeException(e);
					}
					outstream.close();
				}
			});

			// parse study data response from GL
			REPCMT000400UV01ActCategory actCategory;
			InputStream xmlStream = null;
			try
			{
				HttpResponse response = httpClient.execute(httpPost);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode < 200 || statusCode > 299) throw new IOException(
						"Error persisting study definition (statuscode " + statusCode + ")");
				xmlStream = response.getEntity().getContent();
				actCategory = JAXB_CONTEXT_ACT_CATEGORY.createUnmarshaller()
						.unmarshal(new StreamSource(xmlStream), REPCMT000400UV01ActCategory.class).getValue();
			}
			catch (RuntimeException e)
			{
				httpPost.abort();
				throw e;
			}
			finally
			{
				IOUtils.closeQuietly(xmlStream);
			}

			// convert REPCMT000400UV01ActCategory to OMX and put in database
			String id = studyDefinition.getId().getExtension();
			String omxId = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
			DataSet dataSet = dataService.findOne(DataSet.ENTITY_NAME, new QueryImpl().eq(DataSet.IDENTIFIER, omxId),
					DataSet.class);

			for (REPCMT000400UV01Component4 rootComponent : actCategory.getComponent())
			{
				// create observation set
				ObservationSet observationSet = new ObservationSet();
				observationSet.setPartOfDataSet(dataSet);

				REPCMT000100UV01Organizer organizer = rootComponent.getOrganizer().getValue();
				// COCTMT050000UV01Patient patient = organizer.getRecordTarget().getValue().getPatient().getValue();
				// JAXBElement<?> postalCodeSerializable = (JAXBElement<?>)
				// patient.getAddr().get(0).getContent().get(0);
				// if (postalCodeSerializable.getDeclaredType().equals(AdxpPostalCode.class))
				// {
				// AdxpPostalCode postalCode = (AdxpPostalCode) postalCodeSerializable;
				// postalCodez
				// }

				// create other features and values
				for (REPCMT000100UV01Component3 organizerComponent : organizer.getComponent())
				{
					REPCMT000100UV01Observation observation = organizerComponent.getObservation().getValue();
					String featureId = observation.getId().get(0).getRoot();
					ObservableFeature observableFeature = dataService.findOne(ObservableFeature.ENTITY_NAME,
							new QueryImpl().eq(ObservableFeature.IDENTIFIER, featureId), ObservableFeature.class);
					if (observableFeature == null) throw new RuntimeException(
							"missing ObservableFeature with identifier " + featureId);

					org.molgenis.omx.observ.value.Value value = toValue(observation.getValue());
					if (value != null)
					{
						ObservedValue observedValue = new ObservedValue();
						observedValue.setObservationSet(observationSet);
						observedValue.setFeature(observableFeature);
						observedValue.setValue(value);

						dataService.add(ObservedValue.ENTITY_NAME, observedValue);
					}
				}
				dataService.add(ObservationSet.ENTITY_NAME, observationSet);
			}
		}
		catch (IOException e)
		{
			logger.error(e);
			throw new RuntimeException(e);
		}
		catch (JAXBException e)
		{
			logger.error(e);
			throw new RuntimeException(e);
		}
		catch (RuntimeException e)
		{
			logger.error(e);
			throw new RuntimeException(e);
		}
	}

	public boolean isStudyDataLoaded(String id)
	{
		String dataSetId = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
		return dataService.count(DataSet.ENTITY_NAME, new QueryImpl().eq(DataSet.IDENTIFIER, dataSetId)) == 1;
	}

	public boolean isStudyDataActivated(String id) throws UnknownStudyDefinitionException, UnknownCatalogException
	{
		String dataSetId = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(id);
		DataSet dataset = (DataSet) dataService.findOne(DataSet.ENTITY_NAME,
				new QueryImpl().eq(DataSet.IDENTIFIER, dataSetId));
		if (dataset == null) throw new UnknownStudyDefinitionException("StudyData [" + id + "] does not exist");

		if (dataset.getProtocolUsed() == null) throw new UnknownCatalogException("StudyData [" + id
				+ "] does not exist");
		return dataset.getProtocolUsed().getActive();
	}

	private org.molgenis.omx.observ.value.Value toValue(ANY anyValue)
	{
		if (anyValue instanceof INT)
		{
			// integer
			INT value = (INT) anyValue;
			// convert to long, not to int
			LongValue longValue = new LongValue();
			longValue.setValue(value.getValue().longValue());
			return longValue;
		}
		else if (anyValue instanceof ST)
		{
			// string
			ST value = (ST) anyValue;
			StringValue stringValue = new StringValue();
			stringValue.setValue(value.getRepresentation().value());
			return stringValue;
		}
		else if (anyValue instanceof PQ)
		{
			// physical quantity
			PQ value = (PQ) anyValue;
			StringValue stringValue = new StringValue();
			stringValue.setValue(value.getValue());
			return stringValue;
		}
		else if (anyValue instanceof TS)
		{
			// time
			TS value = (TS) anyValue;
			StringValue stringValue = new StringValue();
			stringValue.setValue(value.getValue());
			return stringValue;
		}
		else if (anyValue instanceof REAL)
		{
			// fractional number
			REAL value = (REAL) anyValue;
			// conversion to double not always possible, see HL7 docs
			StringValue stringValue = new StringValue();
			stringValue.setValue(value.getValue());
			return stringValue;
		}
		else if (anyValue instanceof BL)
		{
			// boolean
			BL value = (BL) anyValue;
			BoolValue boolValue = new BoolValue();
			boolValue.setValue(value.isValue());
			return boolValue;
		}
		else if (anyValue instanceof CD) // for CD and CO values
		{
			// categorical
			CD value = (CD) anyValue;
			String identifier = OmxIdentifierGenerator.from(Category.class, value.getCodeSystem(), value.getCode());
			Category category = dataService.findOne(Category.ENTITY_NAME,
					new QueryImpl().eq(Category.IDENTIFIER, identifier), Category.class);
			if (category == null)
			{
				logger.error("missing category identifier: " + identifier);
				return null;
			}
			CategoricalValue categoricalValue = new CategoricalValue();
			categoricalValue.setValue(category);
			return categoricalValue;
		}

		throw new UnsupportedOperationException("ANY instance not supported: " + anyValue.getClass());
	}
}