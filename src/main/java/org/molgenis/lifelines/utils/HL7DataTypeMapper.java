package org.molgenis.lifelines.utils;

import nl.umcg.hl7.service.catalog.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps HL7v3 data types to OMX data types
 */
public class HL7DataTypeMapper
{
	private static final Map<Class<? extends ANY>, String> dataTypeMap;

	static
	{
		dataTypeMap = new HashMap<Class<? extends ANY>, String>();
		dataTypeMap.put(INT.class, "int");
		dataTypeMap.put(ST.class, "string");
		dataTypeMap.put(CO.class, "categorical");
		dataTypeMap.put(CD.class, "categorical");
		dataTypeMap.put(PQ.class, "decimal");
		dataTypeMap.put(TS.class, "datetime");
		dataTypeMap.put(REAL.class, "decimal");
		dataTypeMap.put(BL.class, "bool");
	}

	public static String get(Class<? extends ANY> clazz)
	{
		return dataTypeMap.get(clazz);
	}

	public static String get(ANY any)
	{
		return get(any.getClass());
	}
}
