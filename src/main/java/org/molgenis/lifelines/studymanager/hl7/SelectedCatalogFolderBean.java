package org.molgenis.lifelines.studymanager.hl7;

/**
 * All the information regarding a selected catalog folder. Used as a common model to create the HL7 document.
 */
public class SelectedCatalogFolderBean
{
	private String code;
	private String displayName;
	private String measurementCode;
	private String measurementCodeSystem;
	private String textCode;
	private String codeSystem;

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	public String getMeasurementCode()
	{
		return measurementCode;
	}

	public void setMeasurementCode(String measurementCode)
	{
		this.measurementCode = measurementCode;
	}

	public String getMeasurementCodeSystem()
	{
		return measurementCodeSystem;
	}

	public void setMeasurementCodeSystem(String measurementCodeSystem)
	{
		this.measurementCodeSystem = measurementCodeSystem;
	}

	public String getTextCode()
	{
		return textCode;
	}

	public void setTextCode(String textCode)
	{
		this.textCode = textCode;
	}

	public String getCodeSystem()
	{
		return codeSystem;
	}

	public void setCodeSystem(String codeSystem)
	{
		this.codeSystem = codeSystem;
	}
}