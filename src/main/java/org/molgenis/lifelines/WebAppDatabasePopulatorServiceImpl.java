package org.molgenis.lifelines;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.catalogmanager.CatalogManagerController;
import org.molgenis.data.DataService;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.dataexplorer.controller.DataExplorerController;
import org.molgenis.framework.db.WebAppDatabasePopulatorService;
import org.molgenis.lifelines.controller.HomeController;
import org.molgenis.omx.auth.GroupAuthority;
import org.molgenis.omx.auth.MolgenisGroup;
import org.molgenis.omx.auth.MolgenisGroupMember;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.core.RuntimeProperty;
import org.molgenis.omx.protocolviewer.ProtocolViewerController;
import org.molgenis.search.SearchSecurityHandlerInterceptor;
import org.molgenis.security.SecurityUtils;
import org.molgenis.security.account.AccountService;
import org.molgenis.security.runas.RunAsSystem;
import org.molgenis.security.user.UserAccountController;
import org.molgenis.studymanager.StudyManagerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebAppDatabasePopulatorServiceImpl implements WebAppDatabasePopulatorService
{
	static final String KEY_APP_NAME = "app.name";
	static final String KEY_APP_HREF_LOGO = "app.href.logo";
	static final String KEY_APP_HREF_CSS = "app.href.css";

	private static final String USERNAME_ADMIN = "admin";
	private static final String USERNAME_USER = "user";

	@Value("${admin.password:@null}")
	private String adminPassword;
	@Value("${admin.email:molgenis+admin@gmail.com}")
	private String adminEmail;
	@Value("${user.password:@null}")
	private String userPassword;
	@Value("${user.email:molgenis+user@gmail.com}")
	private String userEmail;

	private static final String GROUP_DATAMANAGERS = "datamanagers";
	private static final String GROUP_RESEARCHERS = "researchers";
	private static final String USERNAME_RESEARCHER = "researcher";
	private static final String USERNAME_DATAMANAGER = "datamanager";

	@Value("${lifelines.profile:@null}")
	private String appProfileStr;
	@Value("${lifelines.datamanager.password:@null}")
	private String dataManagerPassword;
	@Value("${lifelines.datamanager.email:molgenis+datamanager@gmail.com}")
	private String dataManagerEmail;
	@Value("${lifelines.researcher.password:@null}")
	private String researcherPassword;
	@Value("${lifelines.researcher.email:molgenis+researcher@gmail.com}")
	private String researcherEmail;

	private final DataService dataService;

	@Autowired
	public WebAppDatabasePopulatorServiceImpl(DataService dataService)
	{
		if (dataService == null) throw new IllegalArgumentException("dataService is null");
		this.dataService = dataService;
	}

	@Override
	@Transactional
	@RunAsSystem
	public void populateDatabase()
	{
		if (adminPassword == null || userPassword == null || appProfileStr == null || dataManagerPassword == null
				|| researcherPassword == null || adminPassword == null)
		{
			StringBuilder message = new StringBuilder("please configure: ");
			if (adminPassword == null) message.append("default admin.password, ");
			if (userPassword == null) message.append("default user.password, ");
			if (appProfileStr == null) message.append("lifelines.profile(possible values: workspace or website), ");
			if (dataManagerPassword == null) message.append("default lifelines.datamanager.password, ");
			if (researcherPassword == null) message.append("default lifelines.researcher.password ");
			message.append("in your molgenis-server.properties.");
			throw new RuntimeException(message.toString());
		}

		String firstName = "John";
		String lastName = "Doe";

		// FIXME create users and groups through service class
		MolgenisUser userAdmin = new MolgenisUser();
		userAdmin.setUsername(USERNAME_ADMIN);
		userAdmin.setPassword(new BCryptPasswordEncoder().encode(adminPassword));
		userAdmin.setEmail(adminEmail);
		userAdmin.setFirstName(firstName);
		userAdmin.setLastName(lastName);
		userAdmin.setActive(true);
		userAdmin.setSuperuser(true);
		dataService.add(MolgenisUser.ENTITY_NAME, userAdmin);

		MolgenisUser userUser = new MolgenisUser();
		userUser.setUsername(USERNAME_USER);
		userUser.setPassword(new BCryptPasswordEncoder().encode(userPassword));
		userUser.setEmail(userEmail);
		userUser.setFirstName(firstName);
		userUser.setLastName(lastName);
		userUser.setActive(true);
		userUser.setSuperuser(false);
		dataService.add(MolgenisUser.ENTITY_NAME, userUser);

		MolgenisGroup allUsersGroup = new MolgenisGroup();
		allUsersGroup.setName(AccountService.ALL_USER_GROUP);
		dataService.add(MolgenisGroup.ENTITY_NAME, allUsersGroup);

		MolgenisGroupMember userAllUsersMember = new MolgenisGroupMember();
		userAllUsersMember.setMolgenisGroup(allUsersGroup);
		userAllUsersMember.setMolgenisUser(userUser);
		dataService.add(MolgenisGroupMember.ENTITY_NAME, userAllUsersMember);

		for (String entityName : dataService.getEntityNames())
		{
			GroupAuthority entityAuthority = new GroupAuthority();
			entityAuthority.setMolgenisGroup(allUsersGroup);
			entityAuthority.setRole(SecurityUtils.AUTHORITY_ENTITY_READ_PREFIX + entityName.toUpperCase());
			dataService.add(GroupAuthority.ENTITY_NAME, entityAuthority);
		}

		// lifelines database populator
		LifeLinesAppProfile appProfile = LifeLinesAppProfile.valueOf(appProfileStr.toUpperCase());

		Map<String, String> runtimePropertyMap = new HashMap<String, String>();
		runtimePropertyMap.put(KEY_APP_NAME, "LifeLines");
		runtimePropertyMap.put(KEY_APP_HREF_LOGO, "/img/lifelines_letterbox_270x100.png");
		runtimePropertyMap.put(KEY_APP_HREF_CSS, "lifelines.css");
		runtimePropertyMap.put(AccountService.KEY_PLUGIN_AUTH_ACTIVATIONMODE, "user");
		if (appProfile == LifeLinesAppProfile.WEBSITE)
		{
			runtimePropertyMap.put(SearchSecurityHandlerInterceptor.KEY_ACTION_ALLOW_ANONYMOUS_SEARCH, "true");
		}
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

		for (Entry<String, String> entry : runtimePropertyMap.entrySet())
		{
			RuntimeProperty runtimeProperty = new RuntimeProperty();
			String propertyKey = entry.getKey();
			runtimeProperty.setIdentifier(RuntimeProperty.class.getSimpleName() + '_' + propertyKey);
			runtimeProperty.setName(propertyKey);
			runtimeProperty.setValue(entry.getValue());
			dataService.add(RuntimeProperty.ENTITY_NAME, runtimeProperty);
		}

		// FIXME create users and groups through service class
		MolgenisUser datamanagerUser = new MolgenisUser();
		datamanagerUser.setUsername(USERNAME_DATAMANAGER);
		datamanagerUser.setPassword(new BCryptPasswordEncoder().encode(dataManagerPassword));
		datamanagerUser.setEmail(dataManagerEmail);
		datamanagerUser.setFirstName(firstName);
		datamanagerUser.setLastName(lastName);
		datamanagerUser.setActive(true);
		datamanagerUser.setSuperuser(false);
		dataService.add(MolgenisUser.ENTITY_NAME, datamanagerUser);

		MolgenisGroupMember datamanagerUserAllUsersMember = new MolgenisGroupMember();
		datamanagerUserAllUsersMember.setMolgenisUser(datamanagerUser);
		datamanagerUserAllUsersMember.setMolgenisGroup(allUsersGroup);
		dataService.add(MolgenisGroupMember.ENTITY_NAME, datamanagerUserAllUsersMember);

		MolgenisGroup datamanagerGroup = new MolgenisGroup();
		datamanagerGroup.setName(GROUP_DATAMANAGERS);
		dataService.add(MolgenisGroup.ENTITY_NAME, datamanagerGroup);

		MolgenisGroupMember datamanagerUserDataManagerGroupMember = new MolgenisGroupMember();
		datamanagerUserDataManagerGroupMember.setMolgenisUser(datamanagerUser);
		datamanagerUserDataManagerGroupMember.setMolgenisGroup(datamanagerGroup);
		dataService.add(MolgenisGroupMember.ENTITY_NAME, datamanagerUserDataManagerGroupMember);

		GroupAuthority allUsersHomeAuthority = new GroupAuthority();
		allUsersHomeAuthority.setMolgenisGroup(allUsersGroup);
		allUsersHomeAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX + HomeController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, allUsersHomeAuthority);

		GroupAuthority allUsersProtocolViewerAuthority = new GroupAuthority();
		allUsersProtocolViewerAuthority.setMolgenisGroup(allUsersGroup);
		allUsersProtocolViewerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ ProtocolViewerController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, allUsersProtocolViewerAuthority);

		GroupAuthority allUsersAccountAuthority = new GroupAuthority();
		allUsersAccountAuthority.setMolgenisGroup(allUsersGroup);
		allUsersAccountAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ UserAccountController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, allUsersAccountAuthority);

		GroupAuthority datamanagerStudyManagerAuthority = new GroupAuthority();
		datamanagerStudyManagerAuthority.setMolgenisGroup(allUsersGroup);
		datamanagerStudyManagerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ StudyManagerController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, datamanagerStudyManagerAuthority);

		GroupAuthority datamanagerCatalogManagerAuthority = new GroupAuthority();
		datamanagerCatalogManagerAuthority.setMolgenisGroup(allUsersGroup);
		datamanagerCatalogManagerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ CatalogManagerController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, datamanagerCatalogManagerAuthority);

		if (appProfile == LifeLinesAppProfile.WORKSPACE)
		{
			MolgenisUser researcherUser = new MolgenisUser();
			researcherUser.setUsername(USERNAME_RESEARCHER);
			researcherUser.setPassword(new BCryptPasswordEncoder().encode(researcherPassword));
			researcherUser.setEmail(researcherEmail);
			researcherUser.setActive(true);
			researcherUser.setFirstName(firstName);
			researcherUser.setLastName(lastName);
			researcherUser.setSuperuser(false);
			dataService.add(MolgenisUser.ENTITY_NAME, researcherUser);

			MolgenisGroupMember researcherUserAllUsersMember = new MolgenisGroupMember();
			researcherUserAllUsersMember.setMolgenisUser(researcherUser);
			researcherUserAllUsersMember.setMolgenisGroup(allUsersGroup);
			dataService.add(MolgenisGroupMember.ENTITY_NAME, researcherUserAllUsersMember);

			MolgenisGroup researcherGroup = new MolgenisGroup();
			researcherGroup.setName(GROUP_RESEARCHERS);
			dataService.add(MolgenisGroup.ENTITY_NAME, researcherGroup);

			MolgenisGroupMember researcherUserDataManagerGroupMember = new MolgenisGroupMember();
			researcherUserDataManagerGroupMember.setMolgenisUser(researcherUser);
			researcherUserDataManagerGroupMember.setMolgenisGroup(researcherGroup);
			dataService.add(MolgenisGroupMember.ENTITY_NAME, researcherUserDataManagerGroupMember);

			GroupAuthority allUsersDataExplorerAuthority = new GroupAuthority();
			allUsersDataExplorerAuthority.setMolgenisGroup(allUsersGroup);
			allUsersDataExplorerAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
					+ DataExplorerController.ID.toUpperCase());
			dataService.add(GroupAuthority.ENTITY_NAME, allUsersDataExplorerAuthority);
		}
	}

	@Override
	@Transactional(readOnly = true)
	@RunAsSystem
	public boolean isDatabasePopulated()
	{
		return dataService.count(MolgenisUser.ENTITY_NAME, new QueryImpl()) > 0;
	}
}