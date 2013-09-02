package org.molgenis.lifelines.studydefinition;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.ui.MolgenisPlugin;
import org.molgenis.lifelines.catalog.CatalogIdConverter;
import org.molgenis.omx.catalog.CatalogLoaderService;
import org.molgenis.omx.catalog.CatalogPreview;
import org.molgenis.omx.catalog.UnknownCatalogException;
import org.molgenis.omx.observ.Characteristic;
import org.molgenis.omx.study.StudyDefinitionInfo;
import org.molgenis.omx.study.StudyDefinitionService;
import org.molgenis.omx.study.UnknownStudyDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(StudyDefinitionLoaderController.URI)
public class StudyDefinitionLoaderController extends MolgenisPlugin
{
	public static final String URI = MolgenisPlugin.PLUGIN_URI_PREFIX + "studydefinition";
	public static final String LOAD_LIST_URI = "/load-list";
	public static final String VIEW_NAME = "view-studydefinitionloader";
	private static final Logger LOG = Logger.getLogger(StudyDefinitionLoaderController.class);
	private final Database database;
	private final StudyDefinitionService studyDefinitionService;
	private final CatalogLoaderService catalogLoaderService;

	@Autowired
	public StudyDefinitionLoaderController(Database database, StudyDefinitionService studyDefinitionService,
			CatalogLoaderService catalogLoaderService)
	{
		super(URI);
		if (database == null) throw new IllegalArgumentException("database is null");
		if (studyDefinitionService == null) throw new IllegalArgumentException("study definition service is null");
		if (catalogLoaderService == null) throw new IllegalArgumentException("catalog loader service is null");
		this.database = database;
		this.studyDefinitionService = studyDefinitionService;
		this.catalogLoaderService = catalogLoaderService;
	}

	/**
	 * Shows a loading spinner in the iframe and loads the studydefinitions list page
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(LOAD_LIST_URI)
	public String showSpinner(Model model)
	{
		model.addAttribute("url", URI);
		return "spinner";
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
	public String listStudyDefinitions(Model model) throws DatabaseException
	{
		List<StudyDefinitionInfo> studyDefinitions = studyDefinitionService.findStudyDefinitions();
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

	@RequestMapping(value = "/preview/{id}", method = RequestMethod.GET)
	@ResponseBody
	public CatalogPreview previewStudyDefinition(@PathVariable String id) throws UnknownCatalogException
	{
		return catalogLoaderService.getCatalogOfStudyDefinitionPreview(id);
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
				studyDefinitionService.loadStudyDefinition(id);
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

		return listStudyDefinitions(model);
	}

}
