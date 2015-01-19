package org.molgenis.lifelines.utils;

/**
 * Converts measurement identifiers to omx protocol identifiers and back:
 * - catalog release id: 4 
 * - cohort code system : 2.16.840.1.113883.2.4.3.8.1000.54.5.2 
 * - cohort code : 1 
 * - measurement code system: 2.16.840.1.113883.2.4.3.8.1000.54.5.6
 * - measurement code : 11
 * = omx protocol identifier: 4_2.16.840.1.113883.2.4.3.8.1000.54.5.2_1/2.16.840.1.113883.2.4.3.8.1000.54.5.6_11
 * 
 * @author Dennis
 */
public class MeasurementIdConverter
{
	private static final char CODESYSTEM_CODE_SEPARATOR = '_';
	private static final char COHORT_MEASUREMENT_SEPARATOR = '/';
	private static final char CATALOGRELEASE_COHORT_SEPARATOR = ':';

	private MeasurementIdConverter()
	{
	}

	public static String getMeasurementCode(String measurementProtocolIdentifier)
	{
		int off = measurementProtocolIdentifier.indexOf(COHORT_MEASUREMENT_SEPARATOR);
		if (off == -1 || off == 0 || off == measurementProtocolIdentifier.length() - 1)
		{
			throw new IllegalArgumentException("Invalid measurement id [" + measurementProtocolIdentifier + "]");
		}

		int idx = measurementProtocolIdentifier.lastIndexOf(CODESYSTEM_CODE_SEPARATOR);
		if (idx == -1 || idx == 0 || idx == measurementProtocolIdentifier.length() - 1)
		{
			throw new IllegalArgumentException("Invalid measurement id [" + measurementProtocolIdentifier + "]");
		}

		String measurementCode = measurementProtocolIdentifier.substring(idx + 1);
		return measurementCode;
	}

	public static String getMeasurementCodeSystem(String measurementProtocolIdentifier)
	{
		int off = measurementProtocolIdentifier.indexOf(COHORT_MEASUREMENT_SEPARATOR);
		if (off == -1 || off == 0 || off == measurementProtocolIdentifier.length() - 1)
		{
			throw new IllegalArgumentException("Invalid measurement id [" + measurementProtocolIdentifier + "]");
		}

		int idx = measurementProtocolIdentifier.lastIndexOf(CODESYSTEM_CODE_SEPARATOR);
		if (idx == -1 || idx == 0 || idx == measurementProtocolIdentifier.length() - 1)
		{
			throw new IllegalArgumentException("Invalid measurement id [" + measurementProtocolIdentifier + "]");
		}

		String measurementCodeSystem = measurementProtocolIdentifier.substring(off + 1, idx);
		return measurementCodeSystem;
	}

	public static String toOmxProtocolIdentifier(String catalogReleaseId, String cohortCode, String cohortCodeSystem,
			String measurementCode, String measurementCodeSystem)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(catalogReleaseId);
		stringBuilder.append(CATALOGRELEASE_COHORT_SEPARATOR);
		stringBuilder.append(cohortCodeSystem);
		stringBuilder.append(CODESYSTEM_CODE_SEPARATOR);
		stringBuilder.append(cohortCode);
		stringBuilder.append(COHORT_MEASUREMENT_SEPARATOR);
		stringBuilder.append(measurementCodeSystem);
		stringBuilder.append(CODESYSTEM_CODE_SEPARATOR);
		stringBuilder.append(measurementCode);
		return stringBuilder.toString();
	}
}
