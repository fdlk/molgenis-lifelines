package org.molgenis.lifelines.catalog;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class CatalogIdConverterTest
{

	@Test
	public void catalogIdToOmxIdentifier()
	{
		assertEquals(CatalogIdConverter.catalogIdToOmxIdentifier("4"), "catalog_4");
		assertEquals(CatalogIdConverter.catalogIdToOmxIdentifier("catalog_4"), "catalog_catalog_4");
	}

	@Test
	public void omxIdentifierToCatalogId()
	{
		assertEquals(CatalogIdConverter.omxIdentifierToCatalogId("catalog_4"), "4");
		assertEquals(CatalogIdConverter.omxIdentifierToCatalogId("catalog_catalog_4"), "catalog_4");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void omxIdentifierToCatalogIdWithInvalidIdentifier()
	{
		CatalogIdConverter.omxIdentifierToCatalogId("bogus");
	}

	@Test
	public void catalogOfStudyDefinitionIdToOmxIdentifier()
	{
		assertEquals(CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier("4"), "catalog_studydefinition_4");
		assertEquals(CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier("catalog_studydefinition_4"),
				"catalog_studydefinition_catalog_studydefinition_4");
	}

	@Test
	public void omxIdentifierToCatalogOfStudyDefinitionId()
	{
		assertEquals(CatalogIdConverter.omxIdentifierToCatalogOfStudyDefinitionId("catalog_studydefinition_4"), "4");
		assertEquals(
				CatalogIdConverter
						.omxIdentifierToCatalogOfStudyDefinitionId("catalog_studydefinition_catalog_studydefinition_4"),
				"catalog_studydefinition_4");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void omxIdentifierToCatalogOfStudyDefinitionIdWithInvalidIdentifier()
	{
		CatalogIdConverter.omxIdentifierToCatalogOfStudyDefinitionId("bogus");
	}
}
