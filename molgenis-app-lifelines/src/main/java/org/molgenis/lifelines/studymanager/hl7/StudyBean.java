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
 * All the information regarding a study. Used as a common model to create the HL7 document.
 */
public class StudyBean
{
	private String createdBy;
	private String name;
	private List<SelectedCatalogFolderBean> selectedCatalogFolders = new ArrayList<SelectedCatalogFolderBean>();

	/**
	 * Creates a StudyBean with all the information in a {@link StudyDefinition} that gets converted by the
	 * {@link HL7StudyConverter}
	 * 
	 * @param studyDefinition
	 *            The {@link StudyDefinition} to get the information from.
	 * @param dataService
	 *            The {@link DataService} used to retrieve {@link ObservableFeature}s with extra information about the
	 *            selected catalog folders.
	 */
	public StudyBean(StudyDefinition studyDefinition, DataService dataService)
	{
		StringBuilder textBuilder = new StringBuilder("Created by ")
				.append(StringUtils.join(studyDefinition.getAuthors(), ' ')).append(" (")
				.append(studyDefinition.getAuthorEmail()).append(')');
		setCreatedBy(textBuilder.toString());
		setName(studyDefinition.getName());

		for (CatalogFolder item : studyDefinition.getItems())
		{
			SelectedCatalogFolderBean folderBean = new SelectedCatalogFolderBean();
			folderBean.setTextCode(item.getName());
			addSelectedCatalogFolder(folderBean);

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
			folderBean.setCode(observationCodeCode);
			folderBean.setCodeSystem(observationCodeCodesystem);
			folderBean.setDisplayName(item.getName());

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
			folderBean.setMeasurementCode(MeasurementIdConverter.getMeasurementCode(measurementItemId));
			folderBean
					.setMeasurementCodeSystem(MeasurementIdConverter.getMeasurementCodeSystem(measurementItemId));
		}
	}

	/**
	 * Creates a {@link StudyBean} filled with the information from a {@link StudyDataRequest} that gets converted by
	 * the {@link HL7StudyConverter}.
	 * 
	 * @param studyDataRequest
	 *            The {@link StudyDataRequest} to get the information from.
	 */
	public StudyBean(StudyDataRequest studyDataRequest)
	{
		MolgenisUser molgenisUser = studyDataRequest.getMolgenisUser();
		StringBuilder textBuilder = new StringBuilder("Created by ").append(molgenisUser.getUsername()).append(" (")
				.append(molgenisUser.getEmail()).append(')');
		setCreatedBy(textBuilder.toString());
		setName(studyDataRequest.getName());

		for (Protocol protocol : studyDataRequest.getProtocols())
		{
			SelectedCatalogFolderBean measurementBean = new SelectedCatalogFolderBean();
			measurementBean.setTextCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			measurementBean.setCode(ObservationIdConverter.getObservationCode(protocol.getIdentifier()));
			measurementBean.setCodeSystem("2.16.840.1.113883.2.4.3.8.1000.54.8");
			measurementBean.setDisplayName(protocol.getName());
			addSelectedCatalogFolder(measurementBean);
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

	public List<SelectedCatalogFolderBean> getSelectedCatalogFolders()
	{
		return selectedCatalogFolders;
	}

	public void addSelectedCatalogFolder(SelectedCatalogFolderBean selection)
	{
		selectedCatalogFolders.add(selection);
	}
}