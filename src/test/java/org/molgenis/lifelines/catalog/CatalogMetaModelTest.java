package org.molgenis.lifelines.catalog;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.molgenis.catalogmanager.CatalogMetaModel;
import org.testng.annotations.Test;

public class CatalogMetaModelTest
{
	@Test
	public void equals()
	{
		assertEquals(new CatalogMetaModel("1", "name", true), new CatalogMetaModel("1", "name", true));
		assertNotEquals(new CatalogMetaModel("1", "name", true), new CatalogMetaModel("1", "name", false));
		assertNotEquals(new CatalogMetaModel("1", "name", true), new CatalogMetaModel("1", "other name", true));
		assertNotEquals(new CatalogMetaModel("1", "name", true), new CatalogMetaModel("other id", "name", true));
	}
}
