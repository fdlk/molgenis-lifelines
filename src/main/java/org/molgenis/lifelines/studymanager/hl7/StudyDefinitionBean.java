package org.molgenis.lifelines.studymanager.hl7;

import java.util.ArrayList;
import java.util.List;

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
 * All the information needed to create a POQMMT000001UVQualityMeasureDocument.
 * 
 * @author fkelpin
 *
 */
public class StudyDefinitionBean
{
	private String createdBy;
	private String name;
	private List<MeasurementBean> measurements = new ArrayList<MeasurementBean>();

	public StudyDefinitionBean(StudyDefinition studyDefinition, DataService dataService)
	{
		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');
		setCreatedBy(textBuilder.toString());
		setName(studyDefinition.getName());

		for (CatalogFolder item : studyDefinition.getItems())
		{
			MeasurementBean measurementBean = new MeasurementBean();
			measurementBean.setTextCode(item.getName());
			addMeasurement(measurementBean);

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
			measurementBean.setCode(observationCodeCode);
			measurementBean.setCodeSystem(observationCodeCodesystem);
			measurementBean.setDisplayName(item.getName());

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
			measurementBean.setMeasurementCode(MeasurementIdConverter.getMeasurementCode(measurementItemId));
			measurementBean
					.setMeasurementCodeSystem(MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId));
		}
	}

	public StudyDefinitionBean(StudyDataRequest studyDataRequest)
	{
		MolgenisUser molgenisUser = studyDataRequest.getMolgenisUser();
		StringBuilder textBuilder = new StringBuilder("Created by ").append(molgenisUser.getUsername()).append(" (")
				.append(molgenisUser.getEmail()).append(')');
		setCreatedBy(textBuilder.toString());
		setName(studyDataRequest.getName());

		for (Protocol protocol : studyDataRequest.getProtocols())
		{
			MeasurementBean measurementBean = new MeasurementBean();
			measurementBean.setTextCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			measurementBean.setCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			measurementBean.setCodeSystem("2.16.840.1.113883.2.4.3.8.1000.54.8");
			measurementBean.setDisplayName(protocol.getName());
			addMeasurement(measurementBean);
			List<CatalogFolder> itemPath = Lists.newArrayList(new OmxCatalogFolder(protocol).getPath());
			if (itemPath.size() <= 2)
			{
				throw new RuntimeException("Missing measurement for catalog item with id [" + protocol.getIdentifier()
						+ "]");
			}
			String measurementItemId = itemPath.get(2).getExternalId();
			measurementBean.setMeasurementCode(MeasurementIdConverter.getMeasurementCode(measurementItemId));
			measurementBean
					.setMeasurementCodeSystem(MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId));

		}
	}

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

	public List<MeasurementBean> getMeasurements()
	{
		return measurements;
	}

	public void addMeasurement(MeasurementBean info)
	{
		measurements.add(info);
	}
}