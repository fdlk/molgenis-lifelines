package org.molgenis.lifelines.studymanager;

import java.util.List;

import org.molgenis.catalog.UnknownCatalogException;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.lifelines.LifeLinesAppProfile;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.omx.studymanager.OmxStudyManagerService;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.StudyDefinition.Status;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.molgenis.studymanager.StudyManagerService;

public class LifeLinesStudyManagerService implements StudyManagerService
{
	private final GenericLayerStudyManagerService genericLayerStudyManagerService;
	private final OmxStudyManagerService omxStudyManagerService;
	private final DataService dataService;
	private final LifeLinesAppProfile lifeLinesAppProfile;

	public LifeLinesStudyManagerService(GenericLayerStudyManagerService genericLayerStudyManagerService,
			OmxStudyManagerService omxCatalogManagerService, DataService dataService,
			LifeLinesAppProfile lifeLinesAppProfile)
	{
		if (genericLayerStudyManagerService == null) throw new IllegalArgumentException(
				"genericLayerStudyManagerService is null");
		if (omxCatalogManagerService == null) throw new IllegalArgumentException("omxCatalogManagerService is null");
		if (dataService == null) throw new IllegalArgumentException("Data service is null");
		this.genericLayerStudyManagerService = genericLayerStudyManagerService;
		this.omxStudyManagerService = omxCatalogManagerService;
		this.lifeLinesAppProfile = lifeLinesAppProfile;
		this.dataService = dataService;
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions()
	{
		return genericLayerStudyManagerService.getStudyDefinitions();
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(Status status)
	{
		return genericLayerStudyManagerService.getStudyDefinitions(status);
	}

	@Override
	public List<StudyDefinition> getStudyDefinitions(String username)
	{
		return genericLayerStudyManagerService.getStudyDefinitions(username);
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
	public boolean isStudyDataActivated(String id) throws UnknownStudyDefinitionException, UnknownCatalogException
	{
		return genericLayerStudyManagerService.isStudyDataActivated(id);
	}

	@Override
	public StudyDefinition createStudyDefinition(String username, String catalogId) throws UnknownCatalogException
	{
		StudyDefinition studyDefinition = genericLayerStudyManagerService.createStudyDefinition(username, catalogId);
		// convert study data request generic layer id to omx identifier
		String omxIdentifier = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(studyDefinition.getId());
		omxStudyManagerService.createStudyDefinition(username, catalogId, omxIdentifier);
		return studyDefinition;
	}

	@Override
	public void updateStudyDefinition(StudyDefinition studyDefinition) throws UnknownStudyDefinitionException
	{
		genericLayerStudyManagerService.updateStudyDefinition(studyDefinition);
		studyDefinition.setId(studyDefinitionGenericLayerIdToOmxId(studyDefinition.getId()));
		omxStudyManagerService.updateStudyDefinition(studyDefinition);
	}

	@Override
	public void submitStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException,
			UnknownCatalogException
	{
		StudyDataRequest sdr = dataService.findOne(StudyDataRequest.ENTITY_NAME, Integer.valueOf(id),
				StudyDataRequest.class);

		if (sdr == null)
		{
			throw new UnknownStudyDefinitionException("Ubnknown StudyDataRequest with id [" + id + "]");
		}

		genericLayerStudyManagerService.submitStudyDefinition(
				StudyDefinitionIdConverter.omxIdentifierToStudyDefinitionId(sdr.getIdentifier()), catalogId);
		omxStudyManagerService.submitStudyDefinition(id, catalogId);
	}

	private String studyDefinitionGenericLayerIdToOmxId(String id) throws UnknownStudyDefinitionException
	{
		String omxIdentifier = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(id);
		StudyDataRequest studyDataRequest = dataService.findOne(StudyDataRequest.ENTITY_NAME,
				new QueryImpl().eq(StudyDataRequest.IDENTIFIER, omxIdentifier), StudyDataRequest.class);
		if (studyDataRequest == null)
		{
			throw new UnknownStudyDefinitionException("Study definition [" + id + "] does not exist");
		}
		return studyDataRequest.getId().toString();
	}

    @Override
    public void exportStudyDefinition(String id, String catalogId) throws UnknownStudyDefinitionException, UnknownCatalogException {
        throw new UnsupportedOperationException("No export functionality available for OMX");
    }
}
