package org.molgenis.lifelines.catalog;

import java.util.List;

import nl.umcg.hl7.service.catalog.CD;
import nl.umcg.hl7.service.catalog.REPCMT000100UV01Observation;

import org.molgenis.catalog.CatalogFolder;
import org.molgenis.catalog.CatalogItem;

public class RepcObservationCatalogItem implements CatalogItem
{
	private final REPCMT000100UV01Observation observation;

	public RepcObservationCatalogItem(REPCMT000100UV01Observation observation)
	{
		if (observation == null) throw new IllegalArgumentException("observation is null");
		this.observation = observation;
	}

	@Override
	public String getId()
	{
		return getCode();
	}

	@Override
	public String getName()
	{
		String displayName = observation.getCode().getDisplayName();
		return displayName != null ? displayName : getCode();
	}

	@Override
	public String getDescription()
	{
		return observation.getCode().getOriginalText().getContent().toString();
	}

	@Override
	public String getCode()
	{
		CD code = observation.getCode();

		// 1. get code from code
		if (code.getCode() != null) return code.getCode();

		// 2. use code from first translation in code
		List<CD> translations = code.getTranslation();
		if (translations != null && !translations.isEmpty()) return translations.get(0).getCode();

		// 3. use code system name + display name
		return code.getCodeSystemName() + '.' + code.getDisplayName();
	}

	@Override
	public String getCodeSystem()
	{
		return observation.getCode().getCodeSystem();
	}

	@Override
	public Iterable<CatalogFolder> getPath()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getGroup()
	{
		throw new UnsupportedOperationException();
	}
}
