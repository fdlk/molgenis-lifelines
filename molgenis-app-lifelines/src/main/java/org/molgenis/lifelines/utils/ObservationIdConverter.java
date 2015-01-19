package org.molgenis.lifelines.utils;

public class ObservationIdConverter
{
	private static final char SEPARATOR = '.';

	public static String getObservationCode(String observationProtocolIdentifier)
	{
		String observationCode = observationProtocolIdentifier;
		int idx;

		// remove catalog version
		idx = observationCode.lastIndexOf(SEPARATOR);
		if (idx == -1)
		{
			throw new IllegalArgumentException("Invalid observation id [" + observationProtocolIdentifier + "]");
		}
		observationCode = observationProtocolIdentifier.substring(0, idx);

		// remove measurement code
		idx = observationCode.lastIndexOf(SEPARATOR);
		if (idx == -1)
		{
			throw new IllegalArgumentException("Invalid observation id [" + observationProtocolIdentifier + "]");
		}
		observationCode = observationProtocolIdentifier.substring(0, idx);

		// remove cohort code
		idx = observationCode.lastIndexOf(SEPARATOR);
		if (idx == -1)
		{
			throw new IllegalArgumentException("Invalid observation id [" + observationProtocolIdentifier + "]");
		}
		observationCode = observationProtocolIdentifier.substring(0, idx);

		return observationCode;
	}

	public static String toOmxProtocolIdentifier(String observationCode, String cohortCode, String measurementCode,
			String catalogVersion)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(observationCode);
		stringBuilder.append(SEPARATOR);
		stringBuilder.append(cohortCode);
		stringBuilder.append(SEPARATOR);
		stringBuilder.append(measurementCode);
		stringBuilder.append(SEPARATOR);
		stringBuilder.append(catalogVersion);
		return stringBuilder.toString();
	}
}
