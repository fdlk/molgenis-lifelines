package org.molgenis.lifelines.utils;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ObservationIdConverterTest
{
	@Test
	public void getObservationCode()
	{
		String observationProtocolIdentifier = "UVFOODBB15.5.7.1";
		String observationCode = ObservationIdConverter.getObservationCode(observationProtocolIdentifier);
		assertEquals(observationCode, "UVFOODBB15");
	}

	@Test
	public void getObservationCodeWithSeparator()
	{
		String observationProtocolIdentifier = "UVFOODBB15.SOMETHING.5.7.1";
		String observationCode = ObservationIdConverter.getObservationCode(observationProtocolIdentifier);
		assertEquals(observationCode, "UVFOODBB15.SOMETHING");
	}

	@Test
	public void toOmxProtocolIdentifier()
	{
		String observationCode = "UVFOODBB15";
		String cohortCode = "5";
		String measurementCode = "7";
		String catalogVersion = "1";
		String observationProtocolIdentifier = ObservationIdConverter.toOmxProtocolIdentifier(observationCode,
				cohortCode, measurementCode, catalogVersion);
		assertEquals(observationProtocolIdentifier, "UVFOODBB15.5.7.1");
	}
}
