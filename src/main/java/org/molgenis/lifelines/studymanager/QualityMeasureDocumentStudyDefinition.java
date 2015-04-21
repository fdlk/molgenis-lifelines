package org.molgenis.lifelines.studymanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.hl7.v3.ActClass;
import org.hl7.v3.ActMood;
import org.hl7.v3.CD;
import org.hl7.v3.COCTMT090107UVAssignedPerson;
import org.hl7.v3.COCTMT090107UVPerson;
import org.hl7.v3.COCTMT150007UVOrganization;
import org.hl7.v3.ED;
import org.hl7.v3.INT;
import org.hl7.v3.ON;
import org.hl7.v3.PN;
import org.hl7.v3.POQMMT000001UVAuthor;
import org.hl7.v3.POQMMT000001UVComponent2;
import org.hl7.v3.POQMMT000001UVEntry;
import org.hl7.v3.POQMMT000001UVQualityMeasureDocument;
import org.hl7.v3.POQMMT000001UVSection;
import org.hl7.v3.POQMMT000002UVObservation;
import org.hl7.v3.ST;
import org.molgenis.catalog.CatalogFolder;
import org.molgenis.catalog.CatalogItem;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.lifelines.catalog.PoqmObservationCatalogItem;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.omx.utils.I18nTools;
import org.molgenis.study.StudyDefinition;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class QualityMeasureDocumentStudyDefinition implements StudyDefinition
{
	private final POQMMT000001UVQualityMeasureDocument qualityMeasureDocument;
	private final DataService dataService;

	public QualityMeasureDocumentStudyDefinition(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument,
			DataService dataService)
	{
		if (qualityMeasureDocument == null) throw new IllegalArgumentException("qualityMeasureDocument is null");
		this.qualityMeasureDocument = qualityMeasureDocument;
		this.dataService = dataService;
	}

	@Override
	public String getId()
	{
		return qualityMeasureDocument.getId().getExtension();
	}

	@Override
	public void setId(String id)
	{
		qualityMeasureDocument.getId().setExtension(id);
	}

	@Override
	public String getName()
	{
		return qualityMeasureDocument.getTitle().getContent().toString();
	}

	@Override
	public void setName(String name)
	{
		ST title = new ST();
		title.getContent().add(name);
		qualityMeasureDocument.setTitle(title);
	}

	@Override
	public String getDescription()
	{
		ED description = qualityMeasureDocument.getText();
		return description != null ? description.getContent().toString() : null;
	}

	@Override
	public String getVersion()
	{
		INT versionNumber = qualityMeasureDocument.getVersionNumber();
		return versionNumber != null ? versionNumber.getValue().toString() : null;
	}

	@Override
	public Date getDateCreated()
	{
		return null;
	}

	@Override
	public Status getStatus()
	{
		String code = qualityMeasureDocument.getStatusCode().getCode();
		if (code.equals("new")) return Status.DRAFT;
		else if (code.equals("active")) return Status.SUBMITTED;
		else throw new RuntimeException("Unknown status code [" + code + "]");
	}

	@Override
	public List<CatalogFolder> getItems()
	{
		List<POQMMT000001UVComponent2> components = qualityMeasureDocument.getComponent();
		if (components == null || components.isEmpty()) return Collections.emptyList();
		else if (components.size() > 1) throw new RuntimeException("expected exactly one component");

		POQMMT000001UVComponent2 component = components.iterator().next();
		POQMMT000001UVSection section = component.getSection();
		if (section == null) return Collections.emptyList();

		return Lists.newArrayList(Lists.transform(section.getEntry(),
				new Function<POQMMT000001UVEntry, CatalogFolder>()
				{
					@Override
					public CatalogFolder apply(POQMMT000001UVEntry entry)
					{
						Protocol protocol = dataService.findOne(Protocol.ENTITY_NAME,
								new QueryImpl().eq(Protocol.IDENTIFIER, entry.getObservation().getCode().getCode()),
								Protocol.class);

						if (protocol == null)
						{
							throw new RuntimeException("Unknown Protocol with identifier ["
									+ entry.getObservation().getCode().getCode() + "]");
						}
						return new PoqmObservationCatalogItem(entry.getObservation(), protocol);
					}
				}));
	}

	@Override
	public void setItems(Iterable<CatalogFolder> items)
	{
		List<POQMMT000001UVComponent2> components = qualityMeasureDocument.getComponent();
		POQMMT000001UVComponent2 component;
		if (components == null || components.isEmpty())
		{
			component = new POQMMT000001UVComponent2();
			qualityMeasureDocument.getComponent().add(component);
		}
		else if (components.size() > 1)
		{
			throw new RuntimeException("expected exactly one component");
		}
		else
		{
			component = components.iterator().next();
		}

		POQMMT000001UVSection section = component.getSection();
		if (section == null)
		{
			section = new POQMMT000001UVSection();

			CD sectionCode = new CD();
			sectionCode.setCode("57025-9");
			sectionCode.setCodeSystem("2.16.840.1.113883.6.1");
			sectionCode.setDisplayName("Data Criteria section");
			section.setCode(sectionCode);

			ED sectionTitle = new ED();
			sectionTitle.getContent().add("Data criteria");
			section.setTitle(sectionTitle);

			component.setSection(section);
		}

		List<POQMMT000001UVEntry> entries = section.getEntry();
		entries.clear();
		for (CatalogItem item : items)
		{
			POQMMT000001UVEntry entry = new POQMMT000001UVEntry();
			entry.setTypeCode("DRIV");
			POQMMT000002UVObservation observation = new POQMMT000002UVObservation();
			observation.setClassCode(ActClass.OBS);
			observation.setMoodCode(ActMood.CRT);

			String observationCodeCode = item.getCode();
			String observationCodeCodesystem = item.getCodeSystem();
			if (observationCodeCode == null || observationCodeCodesystem == null)
			{
				ObservableFeature of = dataService.findOne(ObservableFeature.ENTITY_NAME,
						Integer.valueOf(item.getId()), ObservableFeature.class);
				if (of == null)
				{
					throw new RuntimeException("Unknown Observablefeature with id [" + item.getId() + "]");
				}
				// TODO remove once catalogues are always loaded from LL GL
				observationCodeCode = of.getIdentifier();
				observationCodeCodesystem = "2.16.840.1.113883.2.4.3.8.1000.54.4";
			}
			CD observationCode = new CD();
			observationCode.setDisplayName(item.getName());
			observationCode.setCode(observationCodeCode);
			observationCode.setCodeSystem(observationCodeCodesystem);
			ED observationOriginalText = new ED();
			observationOriginalText.getContent().add(I18nTools.get(item.getDescription()));
			observationCode.setOriginalText(observationOriginalText);
			observation.setCode(observationCode);
			entry.setObservation(observation);
			entries.add(entry);
		}
	}

	@Override
	public List<String> getAuthors()
	{
		List<POQMMT000001UVAuthor> authors = qualityMeasureDocument.getAuthor();
		if (authors == null) return null;

		List<String> authorNames = new ArrayList<String>();
		for (POQMMT000001UVAuthor author : authors)
		{
			COCTMT090107UVAssignedPerson personContainer = author.getAssignedPerson();
			if (personContainer != null && personContainer.getAssignedPerson() != null)
			{

				COCTMT090107UVPerson person = personContainer.getAssignedPerson().getValue();

				if (person.getName() != null)
				{
					StringBuilder authorBuilder = new StringBuilder();

					// author name
					for (PN namePart : person.getName())
					{
						if (authorBuilder.length() > 0) authorBuilder.append(' ');
						authorBuilder.append(namePart.getContent().toString());
					}

					JAXBElement<COCTMT150007UVOrganization> organizationNode = personContainer
							.getRepresentedOrganization();
					if (organizationNode != null && organizationNode.getValue() != null)
					{
						COCTMT150007UVOrganization organization = organizationNode.getValue();

						if (organization.getName() != null)
						{
							// author organization
							StringBuilder organizationBuilder = new StringBuilder();
							for (ON namePart : organization.getName())
							{
								if (organizationBuilder.length() > 0) organizationBuilder.append(' ');
								organizationBuilder.append(namePart.getContent().toString());
							}
							if (organizationBuilder.length() > 0) authorBuilder.append(" (")
									.append(organizationBuilder).append(')');
						}
					}
					authorNames.add(authorBuilder.toString());
				}
			}
		}
		return authorNames;
	}

	@Override
	public String getAuthorEmail()
	{
		return qualityMeasureDocument.getAuthor().iterator().next().getAssignedPerson().getTelecom().get(0).getValue();
	}

	@Override
	public String getRequestProposalForm()
	{
		String studyDataRequestId = StudyDefinitionIdConverter.studyDefinitionIdToOmxIdentifier(getId());
		StudyDataRequest studyDataRequest = dataService.findOne(StudyDataRequest.ENTITY_NAME,
				new QueryImpl().eq(StudyDataRequest.IDENTIFIER, studyDataRequestId), StudyDataRequest.class);
		return studyDataRequest.getRequestForm();
	}

	@Override
	public boolean containsItem(CatalogFolder anItem)
	{
		boolean contains = false;
		for (CatalogItem item : getItems())
		{
			if (item.getId().equals(anItem.getId()))
			{
				contains = true;
				break;
			}
		}
		return contains;
	}

	@Override
	public void setRequestProposalForm(String fileName)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getExternalId()
	{
		return qualityMeasureDocument.getId().getExtension();
	}

	@Override
	public void setExternalId(String externalId)
	{
		qualityMeasureDocument.getId().setExtension(externalId);
	}
}
