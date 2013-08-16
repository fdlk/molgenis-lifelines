package org.molgenis.lifelines.catalog;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Test;

public class CatalogModelTest
{

	@Test
	public void equals()
	{
		assertEquals(new CatalogModel("1", "name", true), new CatalogModel("1", "name", true));
		assertNotEquals(new CatalogModel("1", "name", true), new CatalogModel("1", "name", false));
		assertNotEquals(new CatalogModel("1", "name", true), new CatalogModel("1", "other name", true));
		assertNotEquals(new CatalogModel("1", "name", true), new CatalogModel("other id", "name", true));
	}
}
