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
		assertEquals(new CatalogMetaModel("1", "name", true, true), new CatalogMetaModel("1", "name", true, true));
		assertNotEquals(new CatalogMetaModel("1", "name", true, true), new CatalogMetaModel("1", "name", false, false));
		assertNotEquals(new CatalogMetaModel("1", "name", true, true), new CatalogMetaModel("1", "other name", true,
				true));
		assertNotEquals(new CatalogMetaModel("1", "name", true, true), new CatalogMetaModel("other id", "name", true,
				true));
	}
}
