package org.molgenis.lifelines.catalog;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.omx.catalog.CatalogInfo;
import org.molgenis.omx.catalog.CatalogLoaderService;
import org.molgenis.omx.catalog.CatalogPreview;
import org.molgenis.omx.catalog.UnknownCatalogException;
import org.molgenis.omx.observ.Characteristic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller for showing available catalogs and loading a specific catalog
 * 
 * @author erwin
 * 
 */
@Controller
@RequestMapping(CatalogLoaderController.BASE_URL)
public class CatalogLoaderController
{
	private static final Logger LOG = Logger.getLogger(CatalogLoaderController.class);

	public static final String BASE_URL = "/plugin/catalog";
	public static final String VIEW_NAME = "catalog-loader";

	private final CatalogLoaderService catalogLoaderService;
	private final Database database;

	@Autowired
	public CatalogLoaderController(CatalogLoaderService catalogLoaderService, Database database)
	{
		if (catalogLoaderService == null) throw new IllegalArgumentException("CatalogLoaderService is null");
		if (database == null) throw new IllegalArgumentException("Database is null");
		this.catalogLoaderService = catalogLoaderService;
		this.database = database;
	}

	/**
	 * Show the available catalogs.
	 * 
	 * Catalogs are exposed via a 'catalogs' model attribute that contains a list of CatalogModel objects.
	 * 
	 * @param model
	 * @return
	 * @throws DatabaseException
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String listCatalogs(Model model) throws DatabaseException
	{
		List<CatalogInfo> catalogs = catalogLoaderService.findCatalogs();
		LOG.debug("Got [" + catalogs.size() + "] catalogs from service");

		List<CatalogModel> models = new ArrayList<CatalogModel>(catalogs.size());
		for (CatalogInfo catalog : catalogs)
		{
			boolean catalogLoaded = isCatalogLoaded(catalog.getId());
			models.add(new CatalogModel(catalog.getId(), catalog.getName(), catalogLoaded));
		}

		model.addAttribute("catalogs", models);

		return VIEW_NAME;
	}

	/**
	 * Loads a catalog by it's id.
	 * 
	 * If an error occurred an 'errorMessage' model attribute is exposed.
	 * 
	 * If the catalog was successfully loaded a 'successMessage' model attribute is exposed.
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws DatabaseException
	 */
	@RequestMapping(value = "/load", params = "load", method = RequestMethod.POST)
	public String loadCatalog(@RequestParam(value = "id", required = false)
	String id, Model model) throws DatabaseException
	{
		try
		{
			if (id != null)
			{
				catalogLoaderService.loadCatalog(id);
				model.addAttribute("successMessage", "Catalog loaded");
				LOG.info("Loaded catalog with id [" + id + "]");
			}
			else
			{
				model.addAttribute("errorMessage", "Please select a catalogue");
			}
		}
		catch (UnknownCatalogException e)
		{
			model.addAttribute("errorMessage", e.getMessage());
		}

		return listCatalogs(model);
	}

	@RequestMapping(value = "/load", params = "unload", method = RequestMethod.POST)
	public String unloadCatalog(@RequestParam(value = "id", required = false)
	String id, Model model) throws DatabaseException
	{
		try
		{
			if (id != null)
			{
				if (isCatalogLoaded(id))
				{
					catalogLoaderService.unloadCatalog(id);
					model.addAttribute("successMessage", "Catalog unloaded");
					LOG.info("Unloaded catalog with id [" + id + "]");
				}
				else
				{
					model.addAttribute("errorMessage", "Catalog not loaded");
				}
			}
			else
			{
				model.addAttribute("errorMessage", "Please select a catalogue");
			}
		}
		catch (UnknownCatalogException e)
		{
			model.addAttribute("errorMessage", e.getMessage());
		}

		return listCatalogs(model);
	}

	@RequestMapping(value = "/preview/{id}", method = RequestMethod.GET)
	@ResponseBody
	public CatalogPreview previewCatalog(@PathVariable
	String id) throws UnknownCatalogException
	{
		return catalogLoaderService.getCatalogPreview(id);
	}

	@ExceptionHandler(Exception.class)
	public String handleException(Exception e, HttpServletRequest request)
	{
		LOG.error("An exception occured in the CatalogLoaderController", e);

		request.setAttribute("errorMessage",
				"An error occured. Please contact the administrator.<br />Message:" + e.getMessage());

		return VIEW_NAME;
	}

	@ExceptionHandler(UnknownCatalogException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public void handleUnknownCatalogException()
	{
	}

	boolean isCatalogLoaded(String id) throws DatabaseException
	{
		String identifier = CatalogIdConverter.catalogIdToOmxIdentifier(id);
		Characteristic dataset = Characteristic.findByIdentifier(database, identifier);
		return dataset != null;
	}
}
