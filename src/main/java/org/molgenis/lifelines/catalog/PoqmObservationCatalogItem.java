package org.molgenis.lifelines.catalog;

import nl.umcg.hl7.service.studydefinition.POQMMT000002UVObservation;

import org.molgenis.omx.catalogmanager.OmxCatalogFolder;
import org.molgenis.omx.observ.Protocol;

public class PoqmObservationCatalogItem extends OmxCatalogFolder
{
	private final POQMMT000002UVObservation observation;

	public PoqmObservationCatalogItem(POQMMT000002UVObservation observation, Protocol protocol)
	{
		super(protocol);
		if (observation == null) throw new IllegalArgumentException("observation is null");
		this.observation = observation;
	}

	@Override
	public String getName()
	{
		return observation.getCode().getDisplayName();
	}

	@Override
	public String getDescription()
	{
		return observation.getCode().getOriginalText().getContent().toString();
	}

	@Override
	public String getCode()
	{
		return observation.getCode().getCode();
	}

	@Override
	public String getCodeSystem()
	{
		return observation.getCode().getCodeSystem();
	}

}
