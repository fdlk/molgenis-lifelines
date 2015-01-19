package org.molgenis.lifelines.catalog;

import java.util.ArrayList;
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
		this.organizer = organizer;
		this.catalogMeta = catalogMeta;
	}

	@Override
	public String getId()
	{
		return catalogMeta != null ? catalogMeta.getId() : null;
	}

	@Override
	public String getName()
	{
		return catalogMeta != null ? catalogMeta.getName() : null;
	}

	@Override
	public String getDescription()
	{
		return catalogMeta != null ? catalogMeta.getDescription() : null;
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
		List<CatalogItem> items = new ArrayList<CatalogItem>();
		for (CatalogFolder child : getChildren())
			items.addAll(child.getItems());
		return items;
	}

	@Override
	public String getVersion()
	{
		return catalogMeta != null ? catalogMeta.getVersion() : null;
	}

	@Override
	public List<String> getAuthors()
	{
		return catalogMeta != null ? catalogMeta.getAuthors() : null;
	}

	@Override
	public String getAuthorEmail()
	{
		return null;
	}

	@Override
	public CatalogFolder findItem(String catalogItemId)
	{
		for (CatalogFolder item : getChildren())
		{
			if (item.getId().equals(catalogItemId)) return item;
		}
		return null;
	}

	@Override
	public String getCode()
	{
		return organizer.getCode().getCode();
	}

	@Override
	public String getCodeSystem()
	{
		return organizer.getCode().getCodeSystem();
	}

	@Override
	public Iterable<CatalogFolder> getPath()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<String> getGroup()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getExternalId()
	{
		return getId();
	}
}
