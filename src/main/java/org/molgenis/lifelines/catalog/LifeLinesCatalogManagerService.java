package org.molgenis.lifelines.catalog;

import org.molgenis.catalog.Catalog;
import org.molgenis.catalog.CatalogMeta;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.study.UnknownStudyDefinitionException;

public class LifeLinesCatalogManagerService implements CatalogManagerService
{
	private final CatalogManagerService omxCatalogManagerService;
	private final GenericLayerCatalogManagerService genericLayerCatalogManagerService;
	private final DataService dataService;

	public LifeLinesCatalogManagerService(CatalogManagerService omxCatalogManagerService,
			GenericLayerCatalogManagerService genericLayerCatalogManagerService, DataService dataService)
	{
		if (omxCatalogManagerService == null) throw new IllegalArgumentException("omxCatalogManagerService is null");
		if (genericLayerCatalogManagerService == null)
		{
			throw new IllegalArgumentException("genericLayerCatalogManagerService is null");
		}
		this.omxCatalogManagerService = omxCatalogManagerService;
		this.genericLayerCatalogManagerService = genericLayerCatalogManagerService;
		this.dataService = dataService;
	}

	@Override
	public void loadCatalog(String id) throws UnknownCatalogException
	{
		genericLayerCatalogManagerService.loadCatalog(id);
	}

	@Override
	public void unloadCatalog(String id) throws UnknownCatalogException
	{
		genericLayerCatalogManagerService.unloadCatalog(id);
	}

	@Override
	public void activateCatalog(String id) throws UnknownCatalogException
	{
		genericLayerCatalogManagerService.activateCatalog(id);
	}

	@Override
	public void deactivateCatalog(String id) throws UnknownCatalogException
	{
		genericLayerCatalogManagerService.deactivateCatalog(id);
	}

	@Override
	public void loadCatalogOfStudyDefinition(String id) throws UnknownCatalogException, UnknownStudyDefinitionException
	{
		omxCatalogManagerService.loadCatalogOfStudyDefinition(id);
	}

	@Override
	public void unloadCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		omxCatalogManagerService.unloadCatalogOfStudyDefinition(id);
	}

	@Override
	public Iterable<CatalogMeta> getCatalogs()
	{
		return genericLayerCatalogManagerService.getCatalogs();
	}

	@Override
	public Catalog getCatalog(String id) throws UnknownCatalogException
	{
		Catalog catalog;
		if (genericLayerCatalogManagerService.isCatalogActivated(id))
		{
			String omxId = dataService
					.findOne(Protocol.ENTITY_NAME,
							new QueryImpl().eq(Protocol.IDENTIFIER, CatalogIdConverter.catalogIdToOmxIdentifier(id)))
					.getIdValue().toString();
			catalog = omxCatalogManagerService.getCatalog(omxId);
		}
		else
		{
			catalog = genericLayerCatalogManagerService.getCatalog(id);
		}
		return catalog;
	}

	@Override
	public boolean isCatalogLoaded(String id) throws UnknownCatalogException
	{
		return genericLayerCatalogManagerService.isCatalogLoaded(id);
	}

	@Override
	public boolean isCatalogActivated(String id) throws UnknownCatalogException
	{
		return genericLayerCatalogManagerService.isCatalogActivated(id);
	}

	@Override
	public Catalog getCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		return omxCatalogManagerService.getCatalogOfStudyDefinition(id);
	}

	@Override
	public boolean isCatalogOfStudyDefinitionLoaded(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		return omxCatalogManagerService.isCatalogOfStudyDefinitionLoaded(id);
	}
}
