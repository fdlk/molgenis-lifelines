package org.molgenis.lifelines.studymanager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.bind.JAXB;

import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionService;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceGetByIdFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage;
import nl.umcg.hl7.service.studydefinition.HL7Container;
import nl.umcg.hl7.service.studydefinition.POQMMT000001UVQualityMeasureDocument;

import org.mockito.ArgumentCaptor;
import org.molgenis.catalog.CatalogFolder;
import org.molgenis.catalogmanager.CatalogManagerService;
import org.molgenis.data.DataService;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.security.user.MolgenisUserService;
import org.molgenis.study.StudyDefinition;
import org.molgenis.study.UnknownStudyDefinitionException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GenericLayerStudyManagerServiceTest
{

	private GenericLayerStudyManagerService service;
	private GenericLayerStudyDefinitionService studyDefinitionService;
	private CatalogManagerService catalogLoaderService;
	private GenericLayerDataQueryService dataQueryService;
	private MolgenisUserService userService;
	private DataService dataService;

	@BeforeMethod
	public void beforeMethod()
	{
		studyDefinitionService = mock(GenericLayerStudyDefinitionService.class);
		catalogLoaderService = mock(CatalogManagerService.class);
		dataQueryService = mock(GenericLayerDataQueryService.class);
		userService = mock(MolgenisUserService.class);
		dataService = mock(DataService.class);
		service = new GenericLayerStudyManagerService(studyDefinitionService, catalogLoaderService, dataQueryService,
				userService, dataService);

	}

	@BeforeClass
	public void beforeClass()
	{
	}

	@Test
	public void testUpdateStudyDefinitionMergesEncountersIntoSameObservation() throws UnknownStudyDefinitionException,
			GenericLayerStudyDefinitionServiceGetByIdFAULTFaultMessage,
			GenericLayerStudyDefinitionServiceReviseFAULTFaultMessage, IOException
	{
		CatalogFolder nullFolder = mock(CatalogFolder.class);
		CatalogFolder baseline = mock(CatalogFolder.class);
		CatalogFolder followup = mock(CatalogFolder.class);

		when(baseline.getId()).thenReturn("adult/baseline_1");
		when(followup.getId()).thenReturn("oldagepensioner/followup_6");

		CatalogFolder smokingBaseline = mock(CatalogFolder.class);
		when(smokingBaseline.getId()).thenReturn("folder1ID");
		when(smokingBaseline.getCodeSystem()).thenReturn("codeSystem1");
		when(smokingBaseline.getCode()).thenReturn("smokingCode");
		when(smokingBaseline.getName()).thenReturn("smoking");
		when(smokingBaseline.getPath()).thenReturn(Arrays.asList(nullFolder, nullFolder, baseline));

		CatalogFolder smokingFollowup = mock(CatalogFolder.class);
		when(smokingFollowup.getId()).thenReturn("folder2ID");
		when(smokingFollowup.getCodeSystem()).thenReturn("codeSystem1");
		when(smokingFollowup.getCode()).thenReturn("smokingCode");
		when(smokingFollowup.getName()).thenReturn("smoking");
		when(smokingFollowup.getPath()).thenReturn(Arrays.asList(nullFolder, nullFolder, followup));

		StudyDefinition studyDefinition = mock(StudyDefinition.class);
		when(studyDefinition.getId()).thenReturn("908");
		when(studyDefinition.getItems()).thenReturn(Arrays.asList(smokingBaseline, smokingFollowup));
		when(studyDefinition.getAuthors()).thenReturn(Arrays.asList("Author 1", "Author 2"));
		when(studyDefinition.getAuthorEmail()).thenReturn("author@email.com");

		when(dataService.findOne(StudyDataRequest.ENTITY_NAME, new Integer(908), StudyDataRequest.class)).thenReturn(
				null);

		HL7Container container = new HL7Container();
		POQMMT000001UVQualityMeasureDocument qualityMeasureDocument = new POQMMT000001UVQualityMeasureDocument();
		container.setQualityMeasureDocument(qualityMeasureDocument);
		when(studyDefinitionService.getById("908")).thenReturn(container);

		service.updateStudyDefinition(studyDefinition);

		ArgumentCaptor<HL7Container> containerCaptor = ArgumentCaptor.forClass(HL7Container.class);
		verify(studyDefinitionService).revise(containerCaptor.capture());
		HL7Container revisedContainer = containerCaptor.getValue();

		StringWriter sw = new StringWriter();
		JAXB.marshal(revisedContainer, sw);
		assertEquals(sw.toString(), readExpectedResult("revisedContainer.xml"));

	}

	private static String readExpectedResult(String name) throws IOException
	{
		InputStream is = GenericLayerStudyManagerServiceTest.class.getResourceAsStream(name);
		InputStreamReader reader = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(reader);
		StringBuilder expected = new StringBuilder();
		String line = br.readLine();
		while (line != null)
		{
			expected.append(line);
			expected.append('\n');
			line = br.readLine();
		}
		String expectedString = expected.toString();
		return expectedString;
	}
}
