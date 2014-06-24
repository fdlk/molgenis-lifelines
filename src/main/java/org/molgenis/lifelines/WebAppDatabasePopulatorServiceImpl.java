package org.molgenis.lifelines;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.data.DataService;
import org.molgenis.data.support.GenomeConfig;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.dataexplorer.controller.DataExplorerController;
import org.molgenis.framework.db.WebAppDatabasePopulatorService;
import org.molgenis.lifelines.controller.HomeController;
import org.molgenis.omx.auth.GroupAuthority;
import org.molgenis.omx.auth.MolgenisGroup;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.auth.UserAuthority;
import org.molgenis.omx.core.RuntimeProperty;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.omx.protocolviewer.ProtocolViewerController;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.security.MolgenisSecurityWebAppDatabasePopulatorService;
import org.molgenis.security.account.AccountService;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.runas.RunAsSystem;
import org.molgenis.studymanager.StudyManagerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebAppDatabasePopulatorServiceImpl implements WebAppDatabasePopulatorService
{
	private final DataService dataService;
	private final MolgenisSecurityWebAppDatabasePopulatorService molgenisSecurityWebAppDatabasePopulatorService;

	static final String KEY_APP_NAME = "app.name";
	static final String KEY_APP_HREF_LOGO = "app.href.logo";
	static final String KEY_APP_HREF_CSS = "app.href.css";

	@Value("${lifelines.datamanager.password:@null}")
	private String dataManagerPassword;
	@Value("${lifelines.datamanager.email:molgenis+datamanager@gmail.com}")
	private String dataManagerEmail;
	@Value("${lifelines.researcher.password:@null}")
	private String researcherPassword;
	@Value("${lifelines.researcher.email:molgenis+researcher@gmail.com}")
	private String researcherEmail;

	@Autowired
	public WebAppDatabasePopulatorServiceImpl(DataService dataService,
			MolgenisSecurityWebAppDatabasePopulatorService molgenisSecurityWebAppDatabasePopulatorService)
	{
		if (dataService == null) throw new IllegalArgumentException("DataService is null");
		this.dataService = dataService;

		if (molgenisSecurityWebAppDatabasePopulatorService == null) throw new IllegalArgumentException(
				"MolgenisSecurityWebAppDatabasePopulator is null");
		this.molgenisSecurityWebAppDatabasePopulatorService = molgenisSecurityWebAppDatabasePopulatorService;

	}

	@Override
	@Transactional
	@RunAsSystem
	public void populateDatabase()
	{
		molgenisSecurityWebAppDatabasePopulatorService.populateDatabase(this.dataService, HomeController.ID);

		// Genomebrowser stuff
		Map<String, String> runtimePropertyMap = new HashMap<String, String>();

		runtimePropertyMap.put(DataExplorerController.INITLOCATION,
				"chr:'1',viewStart:10000000,viewEnd:10100000,cookieKey:'human',nopersist:true");
		runtimePropertyMap.put(DataExplorerController.COORDSYSTEM,
				"{speciesName: 'Human',taxon: 9606,auth: 'GRCh',version: '37',ucscName: 'hg19'}");
		runtimePropertyMap
				.put(DataExplorerController.CHAINS,
						"{hg18ToHg19: new Chainset('http://www.derkholm.net:8080/das/hg18ToHg19/', 'NCBI36', 'GRCh37',{speciesName: 'Human',taxon: 9606,auth: 'NCBI',version: 36,ucscName: 'hg18'})}");
		// for use of the demo dataset add to
		// SOURCES:",{name:'molgenis mutations',uri:'http://localhost:8080/das/molgenis/',desc:'Default from WebAppDatabasePopulatorService'}"
		runtimePropertyMap
				.put(DataExplorerController.SOURCES,
						"[{name:'Genome',twoBitURI:'http://www.biodalliance.org/datasets/hg19.2bit',tier_type: 'sequence'},{name: 'Genes',desc: 'Gene structures from GENCODE 19',bwgURI: 'http://www.biodalliance.org/datasets/gencode.bb',stylesheet_uri: 'http://www.biodalliance.org/stylesheets/gencode.xml',collapseSuperGroups: true,trixURI:'http://www.biodalliance.org/datasets/geneIndex.ix'},{name: 'Repeats',desc: 'Repeat annotation from Ensembl 59',bwgURI: 'http://www.biodalliance.org/datasets/repeats.bb',stylesheet_uri: 'http://www.biodalliance.org/stylesheets/bb-repeats.xml'},{name: 'Conservation',desc: 'Conservation',bwgURI: 'http://www.biodalliance.org/datasets/phastCons46way.bw',noDownsample: true}]");
		runtimePropertyMap
				.put(DataExplorerController.BROWSERLINKS,
						"{Ensembl: 'http://www.ensembl.org/Homo_sapiens/Location/View?r=${chr}:${start}-${end}',UCSC: 'http://genome.ucsc.edu/cgi-bin/hgTracks?db=hg19&position=chr${chr}:${start}-${end}',Sequence: 'http://www.derkholm.net:8080/das/hg19comp/sequence?segment=${chr}:${start},${end}'}");

		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_START, "POS,start_nucleotide");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_STOP, "stop_pos,stop_nucleotide,end_nucleotide");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_CHROM, "CHROM,#CHROM,chromosome");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_ID, "ID,Mutation_id");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_DESCRIPTION, "INFO");
		runtimePropertyMap.put(GenomeConfig.GENOMEBROWSER_PATIENT_ID, "patient_id");

		runtimePropertyMap.put(StudyManagerController.EXPORT_BTN_TITLE, "Export");
		runtimePropertyMap.put(StudyManagerController.EXPORT_ENABLED, String.valueOf(false));

		// lifelines database populator
		runtimePropertyMap.put(KEY_APP_NAME, "LifeLines");
		runtimePropertyMap.put(KEY_APP_HREF_LOGO, "/img/lifelines_letterbox_65x24.png");
		runtimePropertyMap.put(KEY_APP_HREF_CSS, "lifelines.css");
		runtimePropertyMap.put(AccountService.KEY_PLUGIN_AUTH_ACTIVATIONMODE, "user");
		runtimePropertyMap
				.put(HomeController.KEY_APP_HOME_HTML,
						"<div class=\"container-fluid\">"
								+ "<div class=\"row-fluid\">"
								+ "<div class=\"span6\">"
								+ "<h3>Welcome at the LifeLines Data Catalogue!</h3>"
								+ "<p>The LifeLines Data Catalogue provides an overview of all the data collected in LifeLines and is only available for researcher with a research proposal fitting within the theme of Healthy Ageing and which is approved by the Scientific Board.</p>"
								+ "<p>When you click 'catalogue' you can browse all available data items from questionnaires,  measurements and (blood and urine) sample analysis. Also, you can make a selection of data  items that you will need for your research, and download the list.</p>"
								+ "<p>If you want to save your selection and apply for LifeLines data, you need to  register first. You can register by clicking the 'login' button on top. After you  have registered, you will receive a confirmation email. Subsequently, you are able  to download your selection or submit the selection together with you proposal.</p>"
								+ "<p>The catalogue will regularly be updated with new collected data items.  For questions regarding the catalogue or submission of your proposal, please contact the  LifeLines Research Office  <a href=\"mailto:LLscience@umcg.nl\">LLscience@umcg.nl</a></p>"
								+ "<p>The catalogue is working in the newest browsers. <u>If you are experiencing any problems  please switch to a modern browser (IE9+, Chrome, Firefox, Safari).</u></p>"
								+ "</div>" + "<div class=\"span6\">"
								+ "<img src=\"/img/lifelines_family.png\" alt=\"LifeLines family\">" + "</div>"
								+ "</div>" + "</div>");

		runtimePropertyMap.put(StudyManagerController.EXPORT_BTN_TITLE, "Export to Generic Layer");
		runtimePropertyMap.put(StudyManagerController.EXPORT_ENABLED, String.valueOf(true));

		for (Entry<String, String> entry : runtimePropertyMap.entrySet())
		{
			RuntimeProperty runtimeProperty = new RuntimeProperty();
			String propertyKey = entry.getKey();
			runtimeProperty.setIdentifier(RuntimeProperty.class.getSimpleName() + '_' + propertyKey);
			runtimeProperty.setName(propertyKey);
			runtimeProperty.setValue(entry.getValue());
			dataService.add(RuntimeProperty.ENTITY_NAME, runtimeProperty);
		}

		MolgenisUser anonymousUser = molgenisSecurityWebAppDatabasePopulatorService.getAnonymousUser();
		MolgenisGroup allUsersGroup = molgenisSecurityWebAppDatabasePopulatorService.getAllUsersGroup();

		// home plugin
		UserAuthority anonymousUserHomeAuthority = new UserAuthority();
		anonymousUserHomeAuthority.setMolgenisUser(anonymousUser);
		anonymousUserHomeAuthority
				.setRole(SecurityUtils.AUTHORITY_PLUGIN_READ_PREFIX + HomeController.ID.toUpperCase());
		dataService.add(UserAuthority.ENTITY_NAME, anonymousUserHomeAuthority);

		// catalog plugin

		// anonymous + all users
		UserAuthority anonymousUserRuntimePropertyAuthority = new UserAuthority();
		anonymousUserRuntimePropertyAuthority.setMolgenisUser(anonymousUser);
		anonymousUserRuntimePropertyAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX
				+ RuntimeProperty.ENTITY_NAME.toUpperCase());
		dataService.add(UserAuthority.ENTITY_NAME, anonymousUserRuntimePropertyAuthority);

		// anonymous
		UserAuthority anonymousUserProtocolViewerAuthority = new UserAuthority();
		anonymousUserProtocolViewerAuthority.setMolgenisUser(anonymousUser);
		anonymousUserProtocolViewerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_READ_PREFIX
				+ ProtocolViewerController.ID.toUpperCase());
		dataService.add(UserAuthority.ENTITY_NAME, anonymousUserProtocolViewerAuthority);

		// all users
		GroupAuthority allUsersGroupProtocolViewerAuthority = new GroupAuthority();
		allUsersGroupProtocolViewerAuthority.setMolgenisGroup(allUsersGroup);
		allUsersGroupProtocolViewerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ ProtocolViewerController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, allUsersGroupProtocolViewerAuthority);

		// anonymous + all users
		List<String> entityNames;
		entityNames = Arrays.asList(Protocol.ENTITY_NAME, ObservableFeature.ENTITY_NAME, Category.ENTITY_NAME,
				Ontology.ENTITY_NAME, OntologyTerm.ENTITY_NAME);
		for (String entityName : entityNames)
		{
			String role = SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX + entityName.toUpperCase();

			UserAuthority anonymousUserEntityAuthority = new UserAuthority();
			anonymousUserEntityAuthority.setMolgenisUser(anonymousUser);
			anonymousUserEntityAuthority.setRole(role);
			dataService.add(UserAuthority.ENTITY_NAME, anonymousUserEntityAuthority);

			GroupAuthority allUsersGroupEntityAuthority = new GroupAuthority();
			allUsersGroupEntityAuthority.setMolgenisGroup(allUsersGroup);
			allUsersGroupEntityAuthority.setRole(role);
			dataService.add(GroupAuthority.ENTITY_NAME, allUsersGroupEntityAuthority);
		}

		// all users
		GroupAuthority allUsersGroupStudyDataRequestAuthority = new GroupAuthority();
		allUsersGroupStudyDataRequestAuthority.setMolgenisGroup(allUsersGroup);
		allUsersGroupStudyDataRequestAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_WRITE_PREFIX
				+ StudyDataRequest.ENTITY_NAME.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, allUsersGroupStudyDataRequestAuthority);
	}

	@Override
	@Transactional(readOnly = true)
	@RunAsSystem
	public boolean isDatabasePopulated()
	{
		return dataService.count(MolgenisUser.ENTITY_NAME, new QueryImpl()) > 0;
	}
}