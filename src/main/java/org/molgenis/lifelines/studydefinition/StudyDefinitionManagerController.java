package org.molgenis.lifelines.studydefinition;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.log4j.Logger;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.ui.MolgenisPlugin;
import org.molgenis.lifelines.catalog.CatalogIdConverter;
import org.molgenis.omx.catalog.CatalogPreview;
import org.molgenis.omx.catalog.UnknownCatalogException;
import org.molgenis.omx.observ.Characteristic;
import org.molgenis.omx.study.StudyDefinitionInfo;
import org.molgenis.omx.study.StudyDefinitionTree;
import org.molgenis.omx.study.UnknownStudyDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(StudyDefinitionManagerController.URI)
public class StudyDefinitionManagerController extends MolgenisPlugin
{
	private static final Logger LOG = Logger.getLogger(StudyDefinitionManagerController.class);

	public static final String URI = MolgenisPlugin.PLUGIN_URI_PREFIX + "studydefinitionmanager";
	public static final String LOAD_LIST_URI = "/load-list";
	public static final String VIEW_NAME = "view-studydefinitionmanager";

	private final StudyDefinitionManagerService studyDefinitionManagerService;
	private final Database database;

	@Autowired
	public StudyDefinitionManagerController(StudyDefinitionManagerService studyDefinitionManagerService,
			Database database)
	{
		super(URI);
		if (studyDefinitionManagerService == null)
		{
			throw new IllegalArgumentException("StudyDefinitionManagerService is null");
		}
		if (database == null) throw new IllegalArgumentException("database is null");
		this.studyDefinitionManagerService = studyDefinitionManagerService;
		this.database = database;
	}

	/**
	 * Show the available studydefinitions.
	 * 
	 * StudyDefinitions are exposed via a 'studyDefinitions' model attribute that contains a list of
	 * StudyDefinitionModel objects.
	 * 
	 * @param model
	 * @return
	 * @throws DatabaseException
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String getStudyDefinitions(Model model) throws DatabaseException
	{
		List<StudyDefinitionInfo> studyDefinitions = studyDefinitionManagerService.findStudyDefinitions();
		LOG.debug("Got [" + studyDefinitions.size() + "] catalogs from service");

		List<StudyDefinitionModel> models = new ArrayList<StudyDefinitionModel>(studyDefinitions.size());
		for (StudyDefinitionInfo studyDefinition : studyDefinitions)
		{
			String identifier = CatalogIdConverter.catalogOfStudyDefinitionIdToOmxIdentifier(studyDefinition.getId());
			Characteristic dataset = Characteristic.findByIdentifier(database, identifier);
			boolean studyDefinitionLoaded = dataset != null;
			models.add(new StudyDefinitionModel(studyDefinition.getId(), studyDefinition.getName(),
					studyDefinitionLoaded));
		}

		model.addAttribute("studyDefinitions", models);

		return VIEW_NAME;
	}

	@RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
	@ResponseBody
	public CatalogPreview previewStudyDefinition(@PathVariable String id) throws UnknownCatalogException
	{
		return studyDefinitionManagerService.getCatalogOfStudyDefinitionPreview(id);
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.GET)
	@ResponseBody
	public StudyDefinitionTree getStudyDefinitionCatalog(@PathVariable String id) throws UnknownCatalogException,
			UnknownStudyDefinitionException
	{
		return studyDefinitionManagerService.getStudyDefinitionCatalog(id);
	}

	// Tunnel PUT through POST
	@RequestMapping(value = "/{id}", method = RequestMethod.POST, params = "_method=PUT")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateStudyDefinition(@PathVariable Integer id, @Valid @RequestBody Object studyDefinitionRequest)
			throws DatabaseException
	{

	}

	/**
	 * Loads a studydefinition by it's id.
	 * 
	 * If an error occurred an 'errorMessage' model attribute is exposed.
	 * 
	 * If the studydefinition was successfully loaded a 'successMessage' model attribute is exposed.
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws DatabaseException
	 */
	@RequestMapping(value = "/load", method = RequestMethod.POST)
	public String loadStudyDefinition(@RequestParam(value = "id", required = false) String id, Model model)
			throws DatabaseException
	{
		try
		{
			if (id != null)
			{
				studyDefinitionManagerService.loadStudyDefinition(id);
				model.addAttribute("successMessage", "Studydefinition loaded");
				LOG.info("Loaded studydefinition with id [" + id + "]");
			}
			else
			{
				model.addAttribute("errorMessage", "Please select a studydefinition");
			}
		}
		catch (UnknownStudyDefinitionException e)
		{
			model.addAttribute("errorMessage", e.getMessage());
		}

		return getStudyDefinitions(model);
	}
}
