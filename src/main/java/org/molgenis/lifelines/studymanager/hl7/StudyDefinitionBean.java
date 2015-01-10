package org.molgenis.lifelines.studymanager.hl7;

import java.util.ArrayList;
import java.util.List;

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