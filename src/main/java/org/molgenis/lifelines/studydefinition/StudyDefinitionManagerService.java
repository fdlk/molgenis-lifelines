package org.molgenis.lifelines.studydefinition;

import java.util.List;

import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.omx.catalog.CatalogLoaderService;
import org.molgenis.omx.catalog.CatalogPreview;
import org.molgenis.omx.catalog.UnknownCatalogException;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.study.OmxStudyDefinitionItem;
import org.molgenis.omx.study.StudyDefinition;
import org.molgenis.omx.study.StudyDefinitionInfo;
import org.molgenis.omx.study.StudyDefinitionService;
import org.molgenis.omx.study.StudyDefinitionTree;
import org.molgenis.omx.study.StudyDefinitionTreeItem;
import org.molgenis.omx.study.StudyDefinitionTreeNode;
import org.molgenis.omx.study.StudyDefinitionTreeNodeImpl;
import org.molgenis.omx.study.UnknownStudyDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudyDefinitionManagerService
{
	private final Database database;
	private final StudyDefinitionService studyDefinitionService;
	private final CatalogLoaderService catalogLoaderService;

	@Autowired
	public StudyDefinitionManagerService(Database database, StudyDefinitionService studyDefinitionService,
			CatalogLoaderService catalogLoaderService)
	{
		if (database == null) throw new IllegalArgumentException("database is null");
		if (studyDefinitionService == null) throw new IllegalArgumentException("study definition service is null");
		if (catalogLoaderService == null) throw new IllegalArgumentException("catalog loader service is null");
		this.database = database;
		this.studyDefinitionService = studyDefinitionService;
		this.catalogLoaderService = catalogLoaderService;
	}

	/**
	 * Find all study definitions
	 * 
	 * @return
	 */
	public List<StudyDefinitionInfo> findStudyDefinitions()
	{
		return studyDefinitionService.findStudyDefinitions();
	}

	/**
	 * Retrieve a study definition and save it in the database
	 * 
	 * @param id
	 * @throws UnknownStudyDefinitionException
	 */
	public void loadStudyDefinition(String id) throws UnknownStudyDefinitionException
	{
		studyDefinitionService.loadStudyDefinition(id);
	}

	/**
	 * Returns a preview of a catalog of a study definition without storing it in the database
	 * 
	 * @param id
	 * @return
	 */
	public CatalogPreview getCatalogOfStudyDefinitionPreview(String id) throws UnknownCatalogException
	{
		return catalogLoaderService.getCatalogOfStudyDefinitionPreview(id);
	}

	public StudyDefinitionTree getStudyDefinitionCatalog(String id) throws UnknownStudyDefinitionException
	{
		List<DataSet> dataSets;
		try
		{
			dataSets = database.find(DataSet.class);
		}
		catch (DatabaseException e)
		{
			throw new RuntimeException(e);
		}

		if (dataSets == null || dataSets.isEmpty()) throw new RuntimeException("database contains no catalogs");
		if (dataSets.size() > 1) throw new RuntimeException(
				"can't determine catalog for study definition, because database contains multiple catalogs");

		DataSet dataSet = dataSets.iterator().next();
		StudyDefinition studyDefinition = studyDefinitionService.getStudyDefinition(id);

		StudyDefinitionTree studyDefinitionCatalog = new StudyDefinitionTree();
		studyDefinitionCatalog.setTitle(studyDefinition.getName());
		studyDefinitionCatalog.setDescription(studyDefinition.getDescription());
		studyDefinitionCatalog.setVersion(studyDefinition.getCatalogVersion());
		studyDefinitionCatalog.setAuthors(studyDefinition.getAuthors());

		createStudyDefinitionCatalogRec(dataSet.getProtocolUsed(), studyDefinitionCatalog, studyDefinition);
		studyDefinitionCatalog.sort();

		return studyDefinitionCatalog;
	}

	private void createStudyDefinitionCatalogRec(Protocol protocol, StudyDefinitionTreeNode node,
			StudyDefinition studyDefinition)
	{
		StudyDefinitionTreeNodeImpl childNode = new StudyDefinitionTreeNodeImpl(protocol.getIdentifier(),
				protocol.getName(), false);
		node.addChild(childNode);

		List<ObservableFeature> features = protocol.getFeatures();
		if (features != null)
		{
			for (ObservableFeature feature : features)
			{
				OmxStudyDefinitionItem studyDefinitionItem = new OmxStudyDefinitionItem(feature);

				String id = studyDefinitionItem.getId();
				String name = studyDefinitionItem.getName();
				boolean selected = studyDefinition.containsItem(studyDefinitionItem);

				childNode.addItem(new StudyDefinitionTreeItem(id, name, selected));
			}
		}

		List<Protocol> subProtocols = protocol.getSubprotocols();
		if (subProtocols != null)
		{
			for (Protocol subProtocol : subProtocols)
				createStudyDefinitionCatalogRec(subProtocol, childNode, studyDefinition);
		}
	}
}
