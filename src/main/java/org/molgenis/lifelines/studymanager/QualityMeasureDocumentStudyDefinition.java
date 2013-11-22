package org.molgenis.lifelines.studymanager;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.molgenis.catalog.CatalogItem;
import org.molgenis.hl7.COCTMT090107UVAssignedPerson;
import org.molgenis.hl7.COCTMT090107UVPerson;
import org.molgenis.hl7.COCTMT150007UVOrganization;
import org.molgenis.hl7.ED;
import org.molgenis.hl7.INT;
import org.molgenis.hl7.ON;
import org.molgenis.hl7.PN;
import org.molgenis.hl7.POQMMT000001UVAuthor;
import org.molgenis.hl7.POQMMT000001UVComponent2;
import org.molgenis.hl7.POQMMT000001UVEntry;
import org.molgenis.hl7.POQMMT000001UVQualityMeasureDocument;
import org.molgenis.lifelines.catalog.PoqmObservationCatalogItem;
import org.molgenis.study.StudyDefinition;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class QualityMeasureDocumentStudyDefinition implements StudyDefinition
{
	private final POQMMT000001UVQualityMeasureDocument qualityMeasureDocument;

	public QualityMeasureDocumentStudyDefinition(POQMMT000001UVQualityMeasureDocument qualityMeasureDocument)
	{
		if (qualityMeasureDocument == null) throw new IllegalArgumentException("qualityMeasureDocument is null");
		this.qualityMeasureDocument = qualityMeasureDocument;
	}

	@Override
	public String getId()
	{
		return qualityMeasureDocument.getId().getExtension();
	}

	@Override
	public void setId(String id)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName()
	{
		return qualityMeasureDocument.getTitle().getContent().toString();
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
	public List<CatalogItem> getItems()
	{
		List<POQMMT000001UVComponent2> components = qualityMeasureDocument.getComponent();
		if (components == null || components.isEmpty() || components.size() > 1) throw new RuntimeException(
				"expected exactly one component");
		POQMMT000001UVComponent2 component = components.iterator().next();

		return Lists.newArrayList(Lists.transform(component.getSection().getEntry(),
				new Function<POQMMT000001UVEntry, CatalogItem>()
				{
					@Override
					public CatalogItem apply(POQMMT000001UVEntry input)
					{
						return new PoqmObservationCatalogItem(input.getObservation());
					}
				}));
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
		return null;
	}

	@Override
	public boolean containsItem(CatalogItem anItem)
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
}
