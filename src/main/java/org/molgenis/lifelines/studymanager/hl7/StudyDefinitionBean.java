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
	private List<MeasurementBean> observationInfo = new ArrayList<MeasurementBean>();

	public StudyDefinitionBean(StudyDefinition studyDefinition, DataService dataService)
	{
		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');
		setCreatedBy(textBuilder.toString());
		setName(studyDefinition.getName());

		for (CatalogFolder item : studyDefinition.getItems())
		{
			MeasurementBean observationInfo = new MeasurementBean();
			observationInfo.setTextCode(item.getName());
			addObservationInfo(observationInfo);

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
			MeasurementBean observationInfo = new MeasurementBean();
			observationInfo.setTextCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			observationInfo.setCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			observationInfo.setCodeSystem("2.16.840.1.113883.2.4.3.8.1000.54.8");
			observationInfo.setDisplayName(protocol.getName());
			addObservationInfo(observationInfo);
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

	public List<MeasurementBean> getObservationInfo()
	{
		return observationInfo;
	}

	public void addObservationInfo(MeasurementBean info)
	{
		observationInfo.add(info);
	}
}