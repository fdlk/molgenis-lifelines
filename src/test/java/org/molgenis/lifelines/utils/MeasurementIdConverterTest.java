package org.molgenis.lifelines.utils;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class MeasurementIdConverterTest
{

	@Test
	public void getMeasurementCode()
	{
		String measurementProtocolIdentifier = "2.16.840.1.113883.2.4.3.8.1000.54.5.2_1/2.16.840.1.113883.2.4.3.8.1000.54.5.6_11";
		String measurementCode = MeasurementIdConverter.getMeasurementCode(measurementProtocolIdentifier);
		assertEquals(measurementCode, "11");
	}

	@Test
	public void getMeasurementCodeSystem()
	{
		String measurementProtocolIdentifier = "2.16.840.1.113883.2.4.3.8.1000.54.5.2_1/2.16.840.1.113883.2.4.3.8.1000.54.5.6_11";
		String measurementCode = MeasurementIdConverter.getMeasurementCodeSystem(measurementProtocolIdentifier);
		assertEquals(measurementCode, "2.16.840.1.113883.2.4.3.8.1000.54.5.6");
	}

	@Test
	public void toOmxProtocolIdentifier()
	{
		String cohortCode = "1";
		String cohortCodeSystem = "2.16.840.1.113883.2.4.3.8.1000.54.5.2";
		String measurementCode = "11";
		String measurementCodeSystem = "2.16.840.1.113883.2.4.3.8.1000.54.5.6";
		String measurementProtocolIdentifier = MeasurementIdConverter.toOmxProtocolIdentifier(cohortCode,
				cohortCodeSystem, measurementCode, measurementCodeSystem);
		assertEquals(measurementProtocolIdentifier,
				"2.16.840.1.113883.2.4.3.8.1000.54.5.2_1/2.16.840.1.113883.2.4.3.8.1000.54.5.6_11");
	}
}
