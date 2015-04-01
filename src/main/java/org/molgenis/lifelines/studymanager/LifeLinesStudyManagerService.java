package org.molgenis.lifelines.studymanager;

import java.util.List;

import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.lifelines.LifeLinesAppProfile;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.StudyDefinition.Status;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.molgenis.studymanager.StudyManagerService;
import org.springframework.transaction.annotation.Transactional;

public class LifeLinesStudyManagerService implements StudyManagerService
{
	private final StudyManagerService omxStudyManagerService;
	private final StudyManagerService genericLayerStudyManagerService;
	private final LifeLinesAppProfile lifeLinesAppProfile;

	public LifeLinesStudyManagerService(StudyManagerService omxStudyManagerService,
			StudyManagerService genericLayerStudyManagerService, LifeLinesAppProfile lifeLinesAppProfile)
	{
		if (omxStudyManagerService == null) throw new IllegalArgumentException("studyManagerService is null");
		if (genericLayerStudyManagerService == null)
		{
			throw new IllegalArgumentException("genericLayerStudyManagerService is null");
		}
		if (lifeLinesAppProfile == null) throw new IllegalArgumentException("lifeLinesAppProfile is null");
		this.omxStudyManagerService = omxStudyManagerService;
		this.genericLayerStudyManagerService = genericLayerStudyManagerService;
		this.lifeLinesAppProfile = lifeLinesAppProfile;
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions()
	{
		return omxStudyManagerService.getStudyDefinitions();
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(Status status)
	{
		return omxStudyManagerService.getStudyDefinitions(status);
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(String username)
	{
		return omxStudyManagerService.getStudyDefinitions(username);
	}

	@Override
	public StudyDefinition getStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		return omxStudyManagerService.getStudyDefinition(id);
	}

	@Override
	public boolean canLoadStudyData()
	{
		switch (lifeLinesAppProfile)
		{
			case WEBSITE:
				return false;
			case WORKSPACE:
				return omxStudyManagerService.canLoadStudyData();
			default:
				throw new RuntimeException("Unknown " + LifeLinesAppProfile.class.getSimpleName() + ": "
						+ lifeLinesAppProfile);
		}
	}

	@Override
	public void loadStudyData(String id) throws UnknownStudyDefinitionException
	{
		omxStudyManagerService.loadStudyData(id);
	}

	@Override
	public boolean isStudyDataLoaded(String id) throws UnknownStudyDefinitionException
	{
		return omxStudyManagerService.isStudyDataLoaded(id);
	}

	@Override
	public boolean isStudyDataActivated(String id) throws UnknownStudyDefinitionException, UnknownCatalogException
	{
		return omxStudyManagerService.isStudyDataActivated(id);
	}

	@Override
	public StudyDefinition createStudyDefinition(String username, String catalogId) throws UnknownCatalogException
	{
		return omxStudyManagerService.createStudyDefinition(username, catalogId);
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		omxStudyManagerService.updateStudyDefinition(studyDefinition);
	}

	@Override
	public void submitStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException,
			UnknownCatalogException
	{
		omxStudyManagerService.submitStudyDefinition(id, catalogId);
	}

	@Override
	public List<StudyDefinition> findStudyDefinitions(Status status, String search)
	{
		return omxStudyManagerService.findStudyDefinitions(status, search);
	}

	@Override
	public void exportStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException,
			UnknownCatalogException
	{
		// omxStudyManagerService.exportStudyDefinition(id, catalogId);
		genericLayerStudyManagerService.exportStudyDefinition(id, catalogId);
	}

	@Override
    @Transactional
	public void withdrawStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		omxStudyManagerService.withdrawStudyDefinition(id);
		genericLayerStudyManagerService.withdrawStudyDefinition(id);
	}
}
