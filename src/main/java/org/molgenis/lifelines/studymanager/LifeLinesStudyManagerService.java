package org.molgenis.lifelines.studymanager;

import java.util.List;

import org.molgenis.lifelines.LifeLinesAppProfile;
import org.molgenis.omx.studymanager.OmxStudyManagerService;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.StudyDefinitionMeta;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.molgenis.studymanager.StudyManagerService;

public class LifeLinesStudyManagerService implements StudyManagerService
{
	private final GenericLayerStudyManagerService genericLayerStudyManagerService;
	private final OmxStudyManagerService omxStudyManagerService;
	private final LifeLinesAppProfile lifeLinesAppProfile;

	public LifeLinesStudyManagerService(GenericLayerStudyManagerService genericLayerStudyManagerService,
			OmxStudyManagerService omxCatalogManagerService, LifeLinesAppProfile lifeLinesAppProfile)
	{
		if (genericLayerStudyManagerService == null) throw new IllegalArgumentException(
				"genericLayerStudyManagerService is null");
		if (omxCatalogManagerService == null) throw new IllegalArgumentException("omxCatalogManagerService is null");
		this.genericLayerStudyManagerService = genericLayerStudyManagerService;
		this.omxStudyManagerService = omxCatalogManagerService;
		this.lifeLinesAppProfile = lifeLinesAppProfile;
	}

	@Override
	public List<StudyDefinitionMeta> getStudyDefinitions()
	{
		return genericLayerStudyManagerService.getStudyDefinitions();
	}

	@Override
	public StudyDefinition getStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		return genericLayerStudyManagerService.getStudyDefinition(id);
	}

	@Override
	public boolean canLoadStudyData()
	{
		switch (lifeLinesAppProfile)
		{
			case WEBSITE:
				return false;
			case WORKSPACE:
				return genericLayerStudyManagerService.canLoadStudyData();
			default:
				throw new RuntimeException("Unknown " + LifeLinesAppProfile.class.getSimpleName() + ": "
						+ lifeLinesAppProfile);
		}
	}

	@Override
	public void loadStudyData(String id) throws UnknownStudyDefinitionException
	{
		genericLayerStudyManagerService.loadStudyData(id);
	}

	@Override
	public boolean isStudyDataLoaded(String id) throws UnknownStudyDefinitionException
	{
		return genericLayerStudyManagerService.isStudyDataLoaded(id);
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		genericLayerStudyManagerService.updateStudyDefinition(studyDefinition);
		studyDefinition.setId(StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(studyDefinition.getId()));
		omxStudyManagerService.updateStudyDefinition(studyDefinition);
	}

	@Override
	public StudyDefinition persistStudyDefinition(StudyDefinition studyDefinition)
	{
		return genericLayerStudyManagerService.persistStudyDefinition(studyDefinition);
	}
}
