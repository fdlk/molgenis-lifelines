package org.molgenis.lifelines.catalog;

import java.util.List;

import org.molgenis.catalog.Catalog;
import org.molgenis.catalog.CatalogMeta;
import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.lifelines.studymanager.StudyDefinitionIdConverter;
import org.molgenis.omx.catalogmanager.OmxCatalogManagerService;
import org.molgenis.study.UnknownStudyDefinitionException;

public class LifeLinesCatalogManagerService implements CatalogManagerService
{
	private final OmxCatalogManagerService omxCatalogManagerService;

	public LifeLinesCatalogManagerService(OmxCatalogManagerService omxCatalogManagerService)
	{
		if (omxCatalogManagerService == null) throw new IllegalArgumentException("omxCatalogManagerService is null");
		this.omxCatalogManagerService = omxCatalogManagerService;
	}

	@Override
	public List<CatalogMeta> findCatalogs()
	{
		return omxCatalogManagerService.findCatalogs();
	}

	@Override
	public Catalog getCatalog(String id) throws UnknownCatalogException
	{
		return omxCatalogManagerService.getCatalog(id);
	}

	@Override
	public Catalog getCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		return omxCatalogManagerService.getCatalogOfStudyDefinition(id);
	}

	@Override
	public void loadCatalog(String id) throws UnknownCatalogException
	{
		omxCatalogManagerService.loadCatalog(id);
	}

	@Override
	public void unloadCatalog(String id) throws UnknownCatalogException
	{
		omxCatalogManagerService.unloadCatalog(id);
	}

	@Override
	public boolean isCatalogLoaded(String id) throws UnknownCatalogException
	{
		return omxCatalogManagerService.isCatalogLoaded(id);
	}

	@Override
	public void loadCatalogOfStudyDefinition(String id) throws UnknownCatalogException, UnknownStudyDefinitionException
	{
		String omxId = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(id);
		omxCatalogManagerService.loadCatalogOfStudyDefinition(omxId);
	}

	@Override
	public void unloadCatalogOfStudyDefinition(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		String omxId = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(id);
		omxCatalogManagerService.unloadCatalogOfStudyDefinition(omxId);
	}

	@Override
	public boolean isCatalogOfStudyDefinitionLoaded(String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		String omxId = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(id);
		return omxCatalogManagerService.isCatalogOfStudyDefinitionLoaded(omxId);
	}
}
