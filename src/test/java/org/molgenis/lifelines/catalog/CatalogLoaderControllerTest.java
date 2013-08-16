package org.molgenis.lifelines.catalog;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Arrays;
import java.util.Collections;

import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.Query;
import org.molgenis.omx.catalog.CatalogInfo;
import org.molgenis.omx.catalog.CatalogLoaderService;
import org.molgenis.omx.catalog.CatalogPreview;
import org.molgenis.omx.catalog.CatalogPreview.CatalogPreviewNode;
import org.molgenis.omx.catalog.UnknownCatalogException;
import org.molgenis.omx.observ.Characteristic;
import org.molgenis.util.GsonHttpMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@WebAppConfiguration
@ContextConfiguration
public class CatalogLoaderControllerTest extends AbstractTestNGSpringContextTests
{
	@Autowired
	private CatalogLoaderController catalogLoaderController;

	@Autowired
	private CatalogLoaderService catalogLoaderService;

	@Autowired
	private Database database;

	private MockMvc mockMvc;

	@BeforeMethod
	public void setUp()
	{
		mockMvc = MockMvcBuilders.standaloneSetup(catalogLoaderController)
				.setMessageConverters(new GsonHttpMessageConverter(), new FormHttpMessageConverter()).build();
	}

	@Configuration
	public static class Config
	{
		@Bean
		public CatalogLoaderController catalogLoaderController() throws DatabaseException
		{
			return new CatalogLoaderController(catalogLoaderService(), database());
		}

		@Bean
		public CatalogLoaderService catalogLoaderService()
		{
			return mock(CatalogLoaderService.class);
		}

		@Bean
		public Database database() throws DatabaseException
		{
			return mock(Database.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listUnloadedCatalogs() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Collections.<Characteristic> emptyList());
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc.perform(get("/plugin/catalog")).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", false))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listLoadedCatalogs() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Arrays.<Characteristic> asList(mock(Characteristic.class)));
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc.perform(get("/plugin/catalog")).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", true))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void loadCatalog() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Collections.<Characteristic> emptyList());
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "1").param("load", "load")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("successMessage"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", false))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void loadLoadedCatalog() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Arrays.<Characteristic> asList(mock(Characteristic.class)));
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "1").param("load", "load")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("errorMessage"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", true))));
	}

	@Test
	public void loadNonExistingCatalog() throws Exception
	{
		doThrow(new UnknownCatalogException("catalog does not exist")).when(catalogLoaderService).loadCatalog("bogus");

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "bogus").param("load", "load")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("errorMessage"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void unloadLoadedCatalog() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Arrays.<Characteristic> asList(mock(Characteristic.class)));
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "id1").param("unload", "unload")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("successMessage"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", true))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void unloadUnloadedCatalog() throws Exception
	{
		CatalogInfo catalogInfo1 = new CatalogInfo("id1", "name1");
		when(catalogLoaderService.findCatalogs()).thenReturn(Arrays.asList(catalogInfo1));

		Query<Characteristic> query = mock(Query.class);
		when(query.eq(eq(Characteristic.IDENTIFIER), anyObject())).thenReturn(query);
		when(query.find()).thenReturn(Collections.<Characteristic> emptyList());
		when(database.query(Characteristic.class)).thenReturn(query);

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "1").param("unload", "unload")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("errorMessage"))
				.andExpect(model().attribute("catalogs", Arrays.asList(new CatalogModel("id1", "name1", false))));
	}

	@Test
	public void unloadNonExistingCatalog() throws Exception
	{
		doThrow(new UnknownCatalogException("catalog does not exist")).when(catalogLoaderService)
				.unloadCatalog("bogus");

		this.mockMvc
				.perform(
						post("/plugin/catalog/load").param("id", "bogus").param("unload", "unload")
								.contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isOk())
				.andExpect(view().name("catalog-loader")).andExpect(model().attributeExists("errorMessage"));
	}

	@Test
	public void previewCatalog() throws Exception
	{
		CatalogPreview catalogPreview = new CatalogPreview();
		catalogPreview.setTitle("title");

		CatalogPreviewNode rootNode = new CatalogPreviewNode();
		rootNode.setName("root");
		rootNode.addItem("item1");

		CatalogPreviewNode childNode = new CatalogPreviewNode();
		childNode.setName("child");
		childNode.addItem("item2");
		rootNode.addChild(childNode);

		catalogPreview.setRoot(rootNode);

		String expectedContent = "{\"title\":\"title\",\"root\":{\"name\":\"root\",\"children\":[{\"name\":\"child\",\"items\":[\"item2\"]}],\"items\":[\"item1\"]}}";
		when(catalogLoaderService.getCatalogPreview("1")).thenReturn(catalogPreview);
		this.mockMvc.perform(get("/plugin/catalog/preview/1")).andExpect(status().isOk())
				.andExpect(content().string(expectedContent));
	}

	@Test
	public void previewNonExistingCatalog() throws Exception
	{
		doThrow(new UnknownCatalogException("catalog does not exist")).when(catalogLoaderService).getCatalogPreview(
				"bogus");
		this.mockMvc.perform(get("/plugin/catalog/preview/bogus")).andExpect(status().isBadRequest());
	}
}
