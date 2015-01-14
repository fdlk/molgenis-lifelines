package org.molgenis.lifelines.catalog;

import java.util.List;

import nl.umcg.hl7.service.catalog.CD;
import nl.umcg.hl7.service.catalog.REPCMT000100UV01Component3;
import nl.umcg.hl7.service.catalog.REPCMT000100UV01Organizer;

import org.molgenis.catalog.CatalogFolder;
import org.molgenis.catalog.CatalogItem;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class OrganizerCatalogFolder implements CatalogFolder
{
	private final REPCMT000100UV01Organizer organizer;

	public OrganizerCatalogFolder(REPCMT000100UV01Organizer organizer)
	{
		if (organizer == null) throw new IllegalArgumentException("Organizer is null");
		this.organizer = organizer;
	}

	@Override
	public String getId()
	{
		CD code = organizer.getCode();
		return code.getCodeSystem() + '.' + code.getCode();
	}

	@Override
	public String getName()
	{
		CD code = organizer.getCode();
		if (null != code.getDisplayName()) return code.getDisplayName();
		else if (null != code.getCode()) return code.getCode();
		else return "null";
	}

	@Override
	public String getDescription()
	{
		return organizer.getCode().getOriginalText().getContent().toString();
	}

	@Override
	public List<CatalogFolder> getChildren()
	{
		return Lists.newArrayList(Iterables.filter(
				Iterables.transform(organizer.getComponent(), new Function<REPCMT000100UV01Component3, CatalogFolder>()
				{

					@Override
					public CatalogFolder apply(REPCMT000100UV01Component3 component)
					{
						return component.getOrganizer() != null ? new OrganizerCatalogFolder(component.getOrganizer()
								.getValue()) : null;
					}
				}), Predicates.notNull()));
	}

	@Override
	public List<CatalogItem> getItems()
	{
		return Lists.newArrayList(Iterables.filter(
				Iterables.transform(organizer.getComponent(), new Function<REPCMT000100UV01Component3, CatalogItem>()
				{

					@Override
					public CatalogItem apply(REPCMT000100UV01Component3 component)
					{
						return component.getObservation() != null ? new RepcObservationCatalogItem(component
								.getObservation().getValue()) : null;
					}
				}), Predicates.notNull()));
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
	
	public String getGroup()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getExternalId()
	{
		return getId();
	}
}
