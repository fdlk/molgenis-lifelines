package org.molgenis.lifelines.catalog;

import java.util.Collections;
import java.util.List;

import org.molgenis.catalog.Catalog;
import org.molgenis.catalog.CatalogFolder;
import org.molgenis.catalog.CatalogItem;
import org.molgenis.catalog.CatalogMeta;
import org.molgenis.hl7.REPCMT000100UV01Component3;
import org.molgenis.hl7.REPCMT000100UV01Organizer;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class OrganizerCatalog implements Catalog
{
	private final REPCMT000100UV01Organizer organizer;
	private final CatalogMeta catalogMeta;

	public OrganizerCatalog(REPCMT000100UV01Organizer organizer, CatalogMeta catalogMeta)
	{
		if (organizer == null) throw new IllegalArgumentException("Organizer is null");
		if (catalogMeta == null) throw new IllegalArgumentException("Catalog meta is null");
		this.organizer = organizer;
		this.catalogMeta = catalogMeta;
	}

	@Override
	public String getId()
	{
		return catalogMeta.getId();
	}

	@Override
	public String getName()
	{
		return catalogMeta.getName();
	}

	@Override
	public String getDescription()
	{
		return catalogMeta.getDescription();
	}

	@Override
	public List<CatalogFolder> getChildren()
	{
		return Lists.transform(organizer.getComponent(), new Function<REPCMT000100UV01Component3, CatalogFolder>()
		{
			@Override
			public CatalogFolder apply(REPCMT000100UV01Component3 component)
			{
				return new OrganizerCatalogFolder(component.getOrganizer().getValue());
			}
		});
	}

	@Override
	public List<CatalogItem> getItems()
	{
		return Collections.emptyList();
	}

	@Override
	public String getVersion()
	{
		return catalogMeta.getVersion();
	}

	@Override
	public List<String> getAuthors()
	{
		return catalogMeta.getAuthors();
	}

	@Override
	public String getAuthorEmail()
	{
		return null;
	}

	@Override
	public CatalogItem findItem(String catalogItemId)
	{
		// FIXME implement
		throw new UnsupportedOperationException();
	}
}
